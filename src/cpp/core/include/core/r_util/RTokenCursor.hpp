/*
 * RTokenCursor.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_MODULES_RTOKENCURSOR_HPP
#define SESSION_MODULES_RTOKENCURSOR_HPP

#include <iostream>
#include <string>

#include <core/Macros.hpp>
#include <core/r_util/RTokenizer.hpp>
#include <core/collection/Position.hpp>

#include <boost/function.hpp>

namespace rstudio {
namespace core {
namespace r_util {
namespace token_cursor {

using namespace core::collection;
using namespace core::r_util;
using namespace core::r_util::token_utils;

// NOTE: TokenCursors store a reference to the set of tokens
// they use, so they are only valid as long as the underlying
// tokens are valid.
class RTokenCursor
{
private:
   
   RTokenCursor(const core::r_util::RTokens &rTokens,
               std::size_t offset,
               std::size_t n)
      : rTokens_(rTokens),
        offset_(offset),
        n_(n)
   {}
   
public:
   
   explicit RTokenCursor(const core::r_util::RTokens& rTokens)
      : rTokens_(rTokens), offset_(0), n_(rTokens.size()) {}
   
   RTokenCursor(const core::r_util::RTokens &rTokens,
               std::size_t offset)
      : rTokens_(rTokens), offset_(offset), n_(rTokens.size()) {}
   
   RTokenCursor clone() const
   {
      return RTokenCursor(rTokens_, offset_, n_);
   }
   
   const core::r_util::RTokens& tokens() const
   {
      return rTokens_;
   }
   
   std::size_t offset() const
   {
      return offset_;
   }
   
   void setOffset(std::size_t offset)
   {
      offset_ = offset;
   }
   
   void moveToStartOfTokenStream()
   {
      offset_ = 0;
   }
   
   void moveToEndOfTokenStream()
   {
      offset_ = n_ - 1;
   }
   
   bool moveToNextToken()
   {
      if (UNLIKELY(offset_ == n_ - 1))
         return false;
      
      ++offset_;
      return true;
   }
   
   bool moveToPreviousToken()
   {
      if (UNLIKELY(offset_ == 0))
         return false;
      
      --offset_;
      return true;
   }
   
private:
   
   bool moveInDirection(bool forward)
   {
      return forward ? moveToNextToken() : moveToPreviousToken();
   }
   
public:
   
   // TODO: May be worthwhile to implement binary search here.
   bool moveToPosition(const Position& destination)
   {
      std::size_t n = n_;
      
      if (n == 0)
         return false;
      
      std::size_t offset = 1;
      while (offset < n)
      {
         if (destination < rTokens_.atUnsafe(offset).position())
         {
            offset_ = offset - 1;
            return true;
         }
         ++offset;
      }
      
      return false;
   }
   
   bool moveToPosition(std::size_t row, std::size_t column)
   {
      return moveToPosition(Position(row, column));
   }
   
   void moveToOffset(std::size_t destination,
                     boost::function<void(const RToken&)> operation)
   {
      destination = destination > n_ ? n_ : destination;
      std::size_t offset = offset_;
      const RTokens& rTokens = rTokens_;
      
      if (offset == destination)
      {
         operation(rTokens.atUnsafe(offset));
      }
      else if (destination > offset)
      {
         while (offset != destination)
         {
            operation(rTokens.atUnsafe(offset));
            ++offset;
         }
      }
      else
      {
         while (offset != destination)
         {
            operation(rTokens.atUnsafe(offset));
            offset--;
         }
      }
      offset_ = offset;
   }
   
   void moveToCursor(const RTokenCursor& other,
                     boost::function<void(const RToken&)> operation)
   {
      moveToOffset(other.offset(), operation);
   }
   
   const RToken& currentToken() const
   {
      return rTokens_.atUnsafe(offset_);
   }
   
   std::size_t currentRow() const
   {
      return currentToken().row();
   }
   
   std::size_t currentColumn() const
   {
      return currentToken().column();
   }
   
   Position currentPosition(bool endOfToken = false) const
   {
      const RToken& token = currentToken();
      return Position(token.row(), token.column() + (endOfToken ? token.length() : 0));
   }
   
   std::wstring::const_iterator begin() const
   {
      return currentToken().begin();
   }
   
   std::wstring::const_iterator end() const
   {
      return currentToken().end();
   }
   
   const RToken& nextToken(std::size_t offset = 1) const
   {
      return rTokens_.at(offset_ + offset);
   }
   
   const RToken& previousToken(std::size_t offset = 1) const
   {
      return rTokens_.at(offset_ - offset);
   }
   
   const RToken& nextSignificantToken(std::size_t times = 1) const
   {
      int offset = 0;
      while (times != 0)
      {
         ++offset;
         while (isWhitespaceOrComment(rTokens_.at(offset_ + offset)))
            ++offset;
         
         --times;
      }
      
      return rTokens_.at(offset_ + offset);
   }
   
   const RToken& previousSignificantToken(std::size_t times = 1) const
   {
      int offset = 0;
      while (times != 0)
      {
         ++offset;
         while (isWhitespaceOrComment(rTokens_.at(offset_ - offset)))
            ++offset;
         
         --times;
      }
      
      return rTokens_.at(offset_ - offset);
   }
   
   operator const RToken&() const
   {
      return rTokens_.at(offset_);
   }
   
   std::wstring content() const
   {
      return currentToken().content();
   }
   
   const std::string& contentAsUtf8() const
   {
      return currentToken().contentAsUtf8();
   }
   
   bool moveToNextSignificantToken()
   {
      if (!moveToNextToken())
         return false;
      
      if (!fwdOverWhitespaceAndComments())
         return false;
      
      return true;
   }
   
   bool moveToPreviousSignificantToken()
   {
      if (!moveToPreviousToken())
         return false;
      
      if (!bwdOverWhitespace())
         return false;
      
      return true;
   }
   
   bool contentEquals(const std::wstring& content) const
   {
      return currentToken().contentEquals(content);
   }
   
   bool contentEquals(wchar_t character) const
   {
      return currentToken().contentEquals(character);
   }
   
   RToken::TokenType type() const
   {
      return currentToken().type();
   }
   
   bool isType(RToken::TokenType type) const
   {
      return currentToken().isType(type);
   }
   
   bool contentContains(wchar_t character) const
   {
      return currentToken().contentContains(character);
   }
   
   bool fwdOverWhitespace()
   {
      while (currentToken().isType(RToken::WHITESPACE))
         if (!moveToNextToken())
            return false;
      return true;
   }
   
   bool bwdOverWhitespace()
   {
      while (currentToken().isType(RToken::WHITESPACE))
         if (!moveToPreviousToken())
            return false;
      return true;
   }
   
   bool fwdOverWhitespaceAndComments()
   {
      while (currentToken().isType(RToken::WHITESPACE) ||
             currentToken().isType(RToken::COMMENT))
         if (!moveToNextToken())
            return false;
      return true;
   }
   
   bool bwdOverWhitespaceAndComments()
   {
      while (currentToken().isType(RToken::WHITESPACE) ||
             currentToken().isType(RToken::COMMENT))
         if (!moveToPreviousToken())
            return false;
      return true;
   }
   
   // Move over whitespace tokens (that do not contain newlines)
   bool fwdOverBlank()
   {
      while (currentToken().isType(RToken::WHITESPACE) &&
             !currentToken().contentContains(L'\n'))
         if (!moveToNextToken())
            return false;
      return true;
   }
   
   bool bwdOverBlank()
   {
      while (currentToken().isType(RToken::WHITESPACE) &&
             !currentToken().contentContains(L'\n'))
         if (!moveToPreviousToken())
            return false;
      return true;
   }
   
   // We are at the end of the document if there are no
   // more significant tokens following.
   bool isAtEndOfDocument()
   {
      if (offset_ == n_ - 1)
         return true;
      
      RTokenCursor cursor = clone();
      ++cursor.offset_;
      
      if (!isWhitespaceOrComment(cursor))
         return false;
      
      cursor.fwdOverWhitespaceAndComments();
      if (isWhitespaceOrComment(cursor) && cursor.offset_ == n_ - 1)
         return true;
      
      return false;
   }
   
   bool finishesExpression()
   {
      const RToken& token = currentToken();
      bool isSemi = token.isType(RToken::SEMI);
      bool isComma = token.isType(RToken::COMMA);
      bool hasNewline = token.isType(RToken::WHITESPACE) &&
            token.contentContains(L'\n');
      bool isRightParen = isRightBracket(token);
      bool isFinalToken = offset_ == n_ - 1;

      return isSemi || hasNewline || isFinalToken || isRightParen || isComma;
   }
   
   friend std::ostream& operator <<(std::ostream& os,
                                    const RTokenCursor& cursor)
   {
      return os << cursor.currentToken().asString();
   }
   
   std::size_t row() const { return currentToken().row(); }
   std::size_t column() const { return currentToken().column(); }
   
private:
   
   bool doFwdToMatchingToken(RToken::TokenType leftTokenType,
                             RToken::TokenType rightTokenType)
   {
      if (!isType(leftTokenType))
         return false;
      
      RTokenCursor cursor = clone();
      int stack = 1;
      
      while (cursor.moveToNextToken())
      {
         stack += cursor.isType(leftTokenType);
         stack -= cursor.isType(rightTokenType);
         
         if (stack == 0)
         {
            offset_ = cursor.offset_;
            return true;
         }
      }
      
      return false;
   }
   
   bool doBwdToMatchingToken(RToken::TokenType rightTokenType,
                             RToken::TokenType leftTokenType)
   {
      if (!isType(rightTokenType))
         return false;
      
      RTokenCursor cursor = clone();
      int stack = 1;
      
      while (cursor.moveToPreviousToken())
      {
         stack += cursor.isType(rightTokenType) ? 1 : 0;
         stack -= cursor.isType(leftTokenType) ? 1 : 0;
         
         if (stack == 0)
         {
            offset_ = cursor.offset_;
            return true;
         }
      }
      
      return false;
   }
   
   static std::map<RToken::TokenType, RToken::TokenType> makeComplementMap()
   {
      std::map<RToken::TokenType, RToken::TokenType> map;

#define RSTUDIO_ADD_COMPLEMENT_2(__MAP__, __X__, __Y__)                        \
   do                                                                          \
   {                                                                           \
      __MAP__[__X__] = __Y__;                                                  \
      __MAP__[__Y__] = __X__;                                                  \
   } while (0)

#define RSTUDIO_ADD_COMPLEMENT(__MAP__, __BRACKET__)                                    \
   RSTUDIO_ADD_COMPLEMENT_2(__MAP__, RToken::L##__BRACKET__, RToken::R##__BRACKET__)

      RSTUDIO_ADD_COMPLEMENT(map, PAREN);
      RSTUDIO_ADD_COMPLEMENT(map, BRACKET);
      RSTUDIO_ADD_COMPLEMENT(map, BRACE);
      RSTUDIO_ADD_COMPLEMENT(map, DBRACKET);

#undef RSTUDIO_ADD_COMPLEMENT_2
#undef RSTUDIO_ADD_COMPLEMENT

      return map;
   }
   
   std::map<RToken::TokenType, RToken::TokenType>& complements()
   {
      static std::map<RToken::TokenType, RToken::TokenType> map = 
            makeComplementMap();
      
      return map;
   }

public:
   
  bool fwdToMatchingToken()
  {
     return isLeftBracket(*this) &&
            doFwdToMatchingToken(type(), complements()[type()]);
  }
  
  bool bwdToMatchingToken()
  {
     return isRightBracket(*this) &&
            doBwdToMatchingToken(type(), complements()[type()]);
  }

public:
  
  // Significant here means non-comma and non-whitespace.
  // This is slightly misleading as whitespace (newlines)
  // are occasionally 'significant' insofar as newlines
  // can end statements.
  bool isLastSignificantTokenOnLine()
  {
     RTokenCursor cursor = clone();
     
     if (!cursor.moveToNextSignificantToken())
        return true;
     
     return cursor.row() > row();
  }
  
  bool isFirstSignificantTokenOnLine()
  {
     RTokenCursor cursor = clone();
     
     if (!cursor.moveToPreviousSignificantToken())
        return true;
     
     return cursor.row() < row();
  }
  
  bool isAtEndOfStatement(bool inParentheticalScope)
  {
     if (isBinaryOp(*this) || isLeftBracket(*this))
        return false;
     
     // Whether we're in a parenthetical scope is important!
     // For example, these parse the same:
     //
     //     (foo\n(1))
     //     (foo  (1))
     //
     // while these parse differently:
     //
     //      foo\n(1)
     //      foo  (1)
     //
     if (!inParentheticalScope && isLastSignificantTokenOnLine())
        return true;
     
     const RToken& next = nextSignificantToken();
     return !(
              isBinaryOp(next) ||
              next.isType(RToken::LPAREN) ||
              next.isType(RToken::LBRACKET) ||
              next.isType(RToken::LDBRACKET));
     
  }
  
  bool isLookingAtNamedArgumentInFunctionCall()
  {
     return isValidAsIdentifier(*this) &&
            nextSignificantToken().contentEquals(L"=");
  }
  
  bool appearsToBeBinaryOperator() const
  {
     return isBinaryOp(currentToken()) &&
            isValidAsIdentifier(previousSignificantToken()) &&
            isValidAsIdentifier(nextSignificantToken());
  }

  // Move to the end of an 'evaluation', e.g.
  //
  //    x$foo[[1]]$bar(1, 2, 3)$baz
  //
  // Note that we don't move to the start of a _statement_; e.g., we don't
  // walk over all binary operators. For example:
  //
  //    foo + x$foo$bar(1)[1]
  //          ^   <--       ^
  //
  // We only move over extraction operators.
  bool moveToEndOfEvaluation()
  {
     RTokenCursor cursor = clone();
     while (true)
     {
        while (isRightBracket(cursor))
        {
           if (!cursor.fwdToMatchingToken())
              return false;

           if (!cursor.moveToNextSignificantToken())
              return false;
        }

        if (isExtractionOperator(cursor.nextSignificantToken()))
        {
           if (!cursor.moveToNextSignificantToken())
              return false;

           if (!cursor.moveToNextSignificantToken())
              return false;

           continue;
        }

        break;

     }

     setOffset(cursor.offset());
     return true;
  }

  // Move to the start of an 'evaluation' (from the end of a statement), e.g.
  //
  //    x$foo[[1]]$bar(1, 2, 3)$baz
  //
  // Note that we don't move to the start of a _statement_; e.g., we don't
  // walk over all binary operators. For example:
  //
  //    foo + x$foo$bar(1)[1]
  //          ^   <--       ^
  //
  // We only move over extraction operators.
  bool moveToStartOfEvaluation()
  {
     RTokenCursor cursor = clone();
     while (true)
     {
        while (isRightBracket(cursor))
        {
           if (!cursor.bwdToMatchingToken())
              return false;
           
           if (!cursor.moveToPreviousSignificantToken())
              return false;
        }
        
        if (isExtractionOperator(cursor.previousSignificantToken()))
        {
           if (!cursor.moveToPreviousSignificantToken())
              return false;
           
           if (!cursor.moveToPreviousSignificantToken())
              return false;
           
           continue;
        }
        
        break;
     }
     
     offset_ = cursor.offset_;
     return true;
  }
  
  // Get the 'evaluation' associated with a function call, e.g.
  //
  //    foo + bar::baz$bam()
  //          ^^^^^^^^^^^^
  //
  std::wstring getEvaluationAssociatedWithCall() const
  {
     RTokenCursor cursor = clone();
     
     if (canOpenArgumentList(cursor))
        if (!cursor.moveToPreviousSignificantToken())
           return std::wstring();
     
     std::wstring::const_iterator end = cursor.end();
     if (!cursor.moveToStartOfEvaluation())
        return std::wstring(cursor.begin(), cursor.end());
     
     std::wstring::const_iterator begin = cursor.begin();
     return std::wstring(begin, end);
  }
  
  // Get the entirety of a function call, e.g.
  //
  //    foo + bar::baz$bam(a, b, c)
  //          ^^^^^^^^^^^^^^^^^^^^^
  //
  std::wstring getFunctionCall() const
  {
     std::wstring evaluation = getEvaluationAssociatedWithCall();
     RTokenCursor cursor = clone();
     if (!cursor.moveToNextSignificantToken())
        return std::wstring();
     
     if (!cursor.fwdToMatchingToken())
        return std::wstring();
     
     return evaluation + std::wstring(this->end(), cursor.end());
  }
  
  // Check to see if this is an 'assignment' call, e.g.
  //
  //    body(f, where) <- x
  //
  bool isAssignmentCall() const
  {
     RTokenCursor cursor = clone();
     if (canOpenArgumentList(cursor.nextSignificantToken()))
        if (!cursor.moveToNextSignificantToken())
           return false;
     
     if (!canOpenArgumentList(cursor))
        return false;
     
     if (!cursor.fwdToMatchingToken())
        return false;
     
     return isLeftAssign(cursor.nextSignificantToken());
  }
  
  // Check if this is a 'simple' call; that is, the call is a single
  // call to a particular string / symbol, as in:
  //
  //    foo(1, 2)
  //
  bool isSimpleCall() const
  {
     if (isAssignmentCall())
        return false;
     
     RTokenCursor cursor = clone();
     if (canOpenArgumentList(cursor))
        if (!cursor.moveToPreviousSignificantToken())
           return false;
     
     if (!canOpenArgumentList(cursor.nextSignificantToken()))
        return false;
     
     if (cursor.isType(RToken::ID) ||
         cursor.isType(RToken::STRING))
     {
        return !isExtractionOperator(cursor.previousSignificantToken());
     }
     
     return false;
  }
  
  // Check if this is a 'namespace' call, e.g.
  //
  //    foo::bar(1, 2)
  //
  // We optimize for this case as we can then directly look up
  // 'bar' in the 'foo' namespace.
  bool isSimpleNamespaceCall() const
  {
     if (isAssignmentCall())
        return false;
     
     RTokenCursor cursor = clone();
     if (canOpenArgumentList(cursor))
        if (!cursor.moveToPreviousSignificantToken())
           return false;
     
     if (!canOpenArgumentList(cursor.nextSignificantToken()))
        return false;
     
     if (cursor.isType(RToken::ID) ||
         cursor.isType(RToken::STRING))
     {
        const RToken& prev = cursor.previousSignificantToken();
        if (token_utils::isNamespaceExtractionOperator(prev))
        {
           const RToken& beforeNs = cursor.previousSignificantToken(2);
           return beforeNs.isType(RToken::ID) ||
                  beforeNs.isType(RToken::STRING);
        }
     }
     
     return false;
  }
  
  // Move to the end of an R statement, e.g.
  //
  //    x <- x + I(a, b, c)
  //         ^~~~~~~>~~~~~^
  //
  // 'inParens' is necessary because we need to know whether
  // newlines are significant or not (they are not significant within
  // a parenthetical scope). Returns 'true' if we reached the end of
  // a statement.
  bool moveToEndOfStatement(bool inParens)
  {
     while (true)
     {
        // When we're in a parenthetical statement, newlines are no
        // longer significant. This means that, for example,
        //
        //    (someFunction
        //     (1, 2, 3))
        //
        // is actually a function call to `someFunction(1, 2, 3)`, while
        //
        //    someFunction
        //    (1, 2, 3)
        //
        // is actually two separate statements (the second being invalid)
        if (!inParens && nextToken().contentContains(L'\n'))
           return true;
        
        // Bail on semi-colons.
        if (isType(RToken::SEMI))
           return true;
        
        // Move over unary operators
        while (isValidAsUnaryOperator(*this))
           if (!moveToNextSignificantToken())
              return false;
        
        // Walk over binary operator pairs.
        //
        // This branch takes us as follows:
        //
        //    a + b
        //    ^->-^
        //
        while (isBinaryOp(nextSignificantToken()))
        {
           if (!moveToNextSignificantToken())
              return false;
           
           if (!moveToNextSignificantToken())
              return false;
           
           continue;
        }
        
        // Check for a parenthetical statement and move over it.
        //
        //    a + (...)
        //        ^~~~^
        //
        if (isLeftBracket(*this))
        {
           if (!fwdToMatchingToken())
              return false;
           
           if (!inParens && nextToken().contentContains(L'\n'))
              return true;
           
           // Bail on semi-colons.
           if (isType(RToken::SEMI))
              return true;
           
           continue;
        }
        
        // Check for a function call and move over it.
        //
        //    foo::bar(...)
        //         ^~~~>~~^
        if (isLeftBracket(nextSignificantToken()))
        {
           if (!moveToNextSignificantToken())
              return false;
           
           if (!fwdToMatchingToken())
              return false;
           
           if (!inParens && nextToken().contentContains(L'\n'))
              return true;
           
           continue;
        }
        
        return true;
     }
  }
  
private:
  
  template <bool ForwardDirection>
  bool findTokenImpl(const boost::function<bool(RTokenCursor)>& predicate,
                     std::size_t maxLookaround)
  {
     RTokenCursor cursor = clone();
     std::size_t count = 0;
     do
     {
        if (++count == maxLookaround)
           return false;
        
        if (predicate(cursor))
        {
           setOffset(cursor.offset());
           return true;
        }
     } while (cursor.moveInDirection(ForwardDirection));
     return false;
  }
  
public:
  
  bool findTokenFwd(const boost::function<bool(RTokenCursor)>& predicate,
                    std::size_t maxLookaround = 200)
  {
     return findTokenImpl<true>(predicate, maxLookaround);
  }
  
  bool findTokenBwd(const boost::function<bool(RTokenCursor)>& predicate,
                    std::size_t maxLookaround = 200)
  {
     return findTokenImpl<false>(predicate, maxLookaround);
  }
  
  // Move (backwards) to an opening paren, associated with
  // a function call. This name is terribly long because it's
  // surprisingly hard to express this concept.
  //
  //    my$call(a, b, c)
  //           ^<<<^
  //
  // Note that we are careful to differentiate between
  // opening parens associated with function calls (e.g. `foo()`)
  // and statements placed within parens (e.g. `(1 + 2)`)
  bool moveToOpeningParenAssociatedWithCurrentFunctionCall()
  {
     RTokenCursor cursor = clone();
     
     do
     {
        if (cursor.bwdToMatchingToken())
           continue;
        
        if (cursor.isType(RToken::LPAREN))
        {
           const RToken& prev = cursor.previousSignificantToken();
           if (isRightBracket(prev) || isValidAsIdentifier(prev))
           {
              setOffset(cursor.offset());
              return true;
           }
        }
        
     } while (cursor.moveToPreviousSignificantToken());
     
     return false;
  }
  
  // Get the head of a (magrittr) pipe chain.
  //
  // This function expects to be called with a pipe immediate
  // preceding the current cursor position; for example:
  //
  //    head %>% foo() %>% bar$baz(1, 2, 3)
  //                       ^^^
  //
  // In this example, the cursor should be on the 'bar' token.
  std::string getHeadOfPipeChain()
  {
     RTokenCursor cursor = clone();
     std::string onFailure;
     
PIPE_START:

     // mtcars %>% foo$bar(
     //        ???
     if (!isPipeOperator(cursor.previousSignificantToken()))
        return onFailure;

     // mtcars %>% foo$bar(
     //        ^<<<^
     if (!cursor.moveToPreviousSignificantToken())
        return onFailure;

     // mtcars %>% foo$bar(
     // ^<<<<<<^
     if (!cursor.moveToPreviousSignificantToken())
        return onFailure;

     RTokenCursor endCursor = cursor.clone();

     if (!cursor.moveToStartOfEvaluation())
        return onFailure;

     if (isPipeOperator(cursor.previousSignificantToken()))
        goto PIPE_START;

     return core::string_utils::wideToUtf8(
              std::wstring(cursor.begin(), endCursor.end()));
     
     return onFailure;
     
  }
  
  std::size_t length() const { return currentToken().length(); }
  
private:
   
   const core::r_util::RTokens& rTokens_;
   std::size_t offset_;
   std::size_t n_;
};

} // namespace token_cursor
} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // SESSION_MODULES_RTOKENCURSOR_HPP
