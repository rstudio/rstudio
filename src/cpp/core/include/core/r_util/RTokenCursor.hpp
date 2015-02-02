/*
 * RTokenCursor.hpp
 *
 * Copyright (C) 2009-2015 by RStudio, Inc.
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

#ifndef CORE_R_UTIL_RTOKENCURSOR_HPP
#define CORE_R_UTIL_RTOKENCURSOR_HPP

#include "RTokenizer.hpp"
#include "RParser.hpp"

#include <core/collection/Position.hpp>

#include <core/Macros.hpp>

namespace rstudio {
namespace core {
namespace r_util {
namespace token_utils {

using namespace collection;
using core::r_util::ParseItem;

class TokenCursor
{
private:
   
   TokenCursor(const AnnotatedRTokens &rTokens,
               std::size_t offset,
               std::size_t n)
      : rTokens_(rTokens), offset_(offset), n_(n) {}
   
public:
   
   explicit TokenCursor(const AnnotatedRTokens& rTokens)
      : rTokens_(rTokens), offset_(0), n_(rTokens.size()) {}
   
   TokenCursor(const AnnotatedRTokens &rTokens,
               std::size_t offset)
      : rTokens_(rTokens), offset_(offset), n_(rTokens.size()) {}
   
   TokenCursor clone() const
   {
      return TokenCursor(rTokens_, offset_, n_);
   }
   
   const AnnotatedRTokens& tokens() const
   {
      return rTokens_;
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
   
   const AnnotatedRToken& currentToken() const
   {
      return rTokens_.at(offset_);
   }
   
   const Position currentPosition() const
   {
      const AnnotatedRToken& token = currentToken();
      return Position(token.row(), token.column());
   }
   
   const AnnotatedRToken& nextToken() const
   {
      return rTokens_.at(offset_ + 1);
   }
   
   const AnnotatedRToken& previousToken() const
   {
      return rTokens_.at(offset_ - 1);
   }
   
   const AnnotatedRToken& nextSignificantToken(std::size_t times = 1) const
   {
      int offset = 0;
      while (times != 0)
      {
         ++offset;
         while (isWhitespace(rTokens_.at(offset_ + offset)))
            ++offset;
         
         --times;
      }
      
      return rTokens_.at(offset_ + offset);
   }
   
   const AnnotatedRToken& previousSignificantToken(std::size_t times = 1) const
   {
      int offset = 0;
      while (times != 0)
      {
         ++offset;
         while (isWhitespace(rTokens_.at(offset_ - offset)))
            ++offset;
         
         --times;
      }
      
      return rTokens_.at(offset_ - offset);
   }
   
   operator const AnnotatedRToken&() const
   {
      return rTokens_.at(offset_);
   }
   
   operator const RToken&() const
   {
      return rTokens_.at(offset_).token();
   }
   
   operator ParseItem() const
   {
      return ParseItem(contentAsUtf8(),
                       currentPosition(),
                       NULL);
   }
   
   std::string contentAsUtf8() const
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
   
   char type() const
   {
      return currentToken().type();
   }
   
   bool isType(char type) const
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
      return !clone().moveToNextSignificantToken();
   }
   
   bool finishesExpression()
   {
      const AnnotatedRToken& token = currentToken();
      bool isSemi = token.isType(RToken::SEMI);
      bool isComma = token.isType(RToken::COMMA);
      bool hasNewline = token.isType(RToken::WHITESPACE) &&
            token.contentContains(L'\n');
      bool isRightParen = isRightBrace(token);
      bool isFinalToken = offset_ == n_ - 1;

      return isSemi || hasNewline || isFinalToken || isRightParen || isComma;
   }
   
   friend std::ostream& operator <<(std::ostream& os,
                                    const TokenCursor& cursor)
   {
      os << cursor.currentToken().asString();
      return os;
   }
   
private:
   
   bool doFwdToMatchingToken(char leftTokenType,
                             char rightTokenType)
   {
      if (!isType(leftTokenType))
         return false;
      
      TokenCursor cursor = clone();
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
   
   bool doBwdToMatchingToken(char leftTokenType,
                             char rightTokenType)
   {
      if (!isType(rightTokenType))
         return false;
      
      TokenCursor cursor = clone();
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
   
   static std::map<char, char> makeComplementMap()
   {
      std::map<char, char> map;

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
   
   static std::map<char, char> complements()
   {
      static std::map<char, char> map = makeComplementMap();
      return map;
   }

public:
  bool fwdToMatchingToken()
  {
     return doFwdToMatchingToken(type(),
                                 complements()[type()]);
  }

  bool bwdToMatchingToken()
  {
     return doBwdToMatchingToken(type(),
                                 complements()[type()]);
  }
  
public:
  
  bool isAtEndOfExpression() const
  {
     return isWhitespace(nextToken()) ||
            nextSignificantToken().isType(RToken::SEMI) ||
            nextSignificantToken().isType(RToken::COMMA);
  }
  
  bool endsExpression() const
  {
     return isWhitespace(currentToken()) ||
            isType(RToken::SEMI) ||
            isType(RToken::COMMA);
  }
  
  bool appearsToBeBinaryOperator() const
  {
     return isBinaryOp(currentToken()) &&
            isValidAsIdentifier(previousSignificantToken()) &&
            isValidAsIdentifier(nextSignificantToken());
  }
  
private:
   
   const AnnotatedRTokens& rTokens_;
   std::size_t offset_;
   std::size_t n_;
};

} // namespace token_utils
} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_RTOKENCURSOR_HPP
