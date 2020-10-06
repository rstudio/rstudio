/*
 * RSourceIndex.cpp
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

#define RSTUDIO_DEBUG_LABEL "source_index"
// #define RSTUDIO_ENABLE_DEBUG_MACROS

#include <iostream>

#include <core/StringUtils.hpp>
#include <core/Macros.hpp>

#include <core/r_util/RSourceIndex.hpp>
#include <core/r_util/RTokenizer.hpp>
#include <core/r_util/RTokenCursor.hpp>

#include <boost/bind.hpp>
#include <boost/algorithm/string.hpp>

namespace rstudio {
namespace core {
namespace r_util {

using namespace token_utils;
using namespace token_cursor;

namespace {

bool isValidRPackageName(const std::string& pkgName)
{
   static const boost::regex rePkgName("[a-zA-Z][a-zA-Z0-9._]*");
   return regex_utils::match(pkgName, rePkgName);
}

std::wstring removeQuoteDelims(const std::wstring& input)
{
   // since we know this was parsed as a quoted string we can just remove
   // the first and last characters
   if (input.size() >= 2)
      return std::wstring(input, 1, input.size() - 2);
   else
      return std::wstring();
}

std::string contentAsUtf8(const RToken& token)
{
   if (token.type() == RToken::STRING)
      return string_utils::wideToUtf8(removeQuoteDelims(token.content()));
   else
      return string_utils::wideToUtf8(token.content());
}

bool isTokenType(RTokens::const_iterator begin,
                 RTokens::const_iterator end,
                 const wchar_t type)
{
   return begin != end && begin->type() == type;
}

bool advancePastNextToken(
         RTokens::const_iterator* pBegin,
         RTokens::const_iterator end,
         const boost::function<bool(const RToken&)>& tokenCondition)
{
   // alias and advance past current token
   RTokens::const_iterator& begin = *pBegin;
   begin++;

   // check for end
   if (begin == end)
   {
      return false;
   }

   // check for condition
   else if (!tokenCondition(*begin))
   {
      return false;
   }

   // advance and return true
   else
   {
      begin++;
      return true;
   }
}

bool advancePastNextToken(RTokens::const_iterator* pBegin,
                          RTokens::const_iterator end,
                          RToken::TokenType type)
{
   return advancePastNextToken(pBegin,
                               end,
                               boost::bind(&RToken::isType, _1, type));
}

bool advancePastNextOperatorToken(RTokens::const_iterator* pBegin,
                                  RTokens::const_iterator end,
                                  const std::wstring& op)
{
   return advancePastNextToken(pBegin,
                               end,
                               boost::bind(&RToken::isOperator, _1, op));
}

// statics for signature parsing comparisons
const std::wstring kOpEquals(L"=");
const std::wstring kSignatureSymbol(L"signature");
const std::wstring kCSymbol(L"c");

void parseSignatureFunction(RTokens::const_iterator begin,
                            RTokens::const_iterator end,
                            std::vector<RS4MethodParam>* pSignature)
{
   // advance to args
   if (!advancePastNextToken(&begin, end, RToken::LPAREN))
      return;

   // pick off args
   while (isTokenType(begin, end, RToken::ID))
   {
      // get the name
      std::string name = contentAsUtf8(*begin);

      // advance and check for equals
      if (!advancePastNextOperatorToken(&begin, end, kOpEquals))
         break;

      // check for string
      if (isTokenType(begin, end, RToken::STRING))
      {
         // get type and add to signature
         std::string type = contentAsUtf8(*begin);
         pSignature->push_back(RS4MethodParam(name, type));

         // advance past comma to next argument
         if (!advancePastNextToken(&begin, end, RToken::COMMA))
            break;
      }
      else
      {
         break;
      }
   }
}

void parseSignatureCharacterVector(RTokens::const_iterator begin,
                                   RTokens::const_iterator end,
                                   std::vector<RS4MethodParam>* pSignature)
{
   // advance to args
   if (!advancePastNextToken(&begin, end, RToken::LPAREN))
      return;

   // pick off args
   while (isTokenType(begin, end, RToken::STRING))
   {
      // get the type string
      pSignature->push_back(RS4MethodParam(contentAsUtf8(*begin)));

      // advance past comma to next argument
      if (!advancePastNextToken(&begin, end, RToken::COMMA))
         break;
   }
}

void parseSignature(RTokens::const_iterator begin,
                    RTokens::const_iterator end,
                    std::vector<RS4MethodParam>* pSignature)
{
   // the signature parameter of the setMethod function can take any
   // of the following forms
   //
   // setMethod("plot", signature(x="track", y="missing")
   // setMethod("plot", c("track", "missing")
   // setMethod("plot", "track"
   //

   if (isTokenType(begin, end, RToken::ID))
   {
      // call to signature function
      if (begin->contentEquals(kSignatureSymbol))
         parseSignatureFunction(begin, end, pSignature);

      // simple list of types
      else if (begin->contentEquals(kCSymbol))
         parseSignatureCharacterVector(begin, end, pSignature);
   }

   // a solitary quoted string (one element character vector)
   else if (isTokenType(begin, end, RToken::STRING))
   {
      pSignature->push_back(RS4MethodParam(contentAsUtf8(*begin)));
   }
}

bool isMethodOrClassDefinition(const RToken& token)
{
   return token.contentStartsWith(L"set") && (
            token.contentEquals(L"setGeneric") ||
            token.contentEquals(L"setMethod") ||
            token.contentEquals(L"setClass") ||
            token.contentEquals(L"setGroupGeneric") ||
            token.contentEquals(L"setClassUnion") ||
            token.contentEquals(L"setRefClass"));
}

class IndexStatus
{
   
public:
   
   IndexStatus(const RTokens& tokens)
      : tokens_(tokens)
   {
   }
   
   // The indexer maintains a vector of indices, recording
   // the indices at which brackets were discovered. Tokens
   // are popped off the stack as right brackets are discovered.
   void update(const RTokenCursor& cursor)
   {
      switch (cursor.type())
      {
      
      // Left brackets
      case RToken::LPAREN:
      case RToken::LBRACE:
      case RToken::LBRACKET:
      case RToken::LDBRACKET:
      {
         // push offset of bracket position
         stack_.push_back(cursor.offset());
         break;
      }
      
      // Right brackets
      case RToken::RBRACE:
      case RToken::RPAREN:
      case RToken::RBRACKET:
      case RToken::RDBRACKET:
      {
         // only pop if this is a matching bracket
         std::size_t n = stack_.size();
         if (n > 0)
         {
            // get the token at the recorded offset
            auto offset = stack_[n - 1];
            const RToken& token = tokens_.atUnsafe(offset);
            
            // check for matching types
            auto lhsType = token.type();
            auto rhsType = cursor.type();
            if (token_utils::typeComplement(lhsType) == rhsType)
               stack_.pop_back();
         }
         
         break;
      }
         
      // appease compiler
      default: break;
         
      }
   }
   
   bool isAtTopLevel() const
   {
      return stack_.empty();
   }
   
   int count(RToken::TokenType type) const
   {
      return std::count_if(stack_.begin(), stack_.end(), [&](std::size_t index)
      {
         const RToken& token = tokens_.at(index);
         return token.type() == type;
      });
   }
   
   const RTokens& tokens() const
   {
      return tokens_;
   }
   
   const std::vector<std::size_t> stack() const
   {
      return stack_;
   }
   
private:
   const RTokens& tokens_;
   std::vector<std::size_t> stack_;
   
};

void addSourceItem(RSourceItem::Type type,
                   const std::vector<RS4MethodParam>& signature,
                   const RToken& token,
                   const IndexStatus& status,
                   RSourceIndex* pIndex)
{
   pIndex->addSourceItem(RSourceItem(
                            type,
                            string_utils::strippedOfQuotes(token.contentAsUtf8()),
                            signature,
                            status.count(RToken::LBRACE),
                            token.row() + 1,
                            token.column() + 1));
}

void addSourceItem(RSourceItem::Type type,
                   const RToken& token,
                   const IndexStatus& status,
                   RSourceIndex* pIndex)
{
   addSourceItem(type,
                 std::vector<RS4MethodParam>(),
                 token,
                 status,
                 pIndex);
}

typedef boost::function<void(const RTokenCursor&, const IndexStatus&, RSourceIndex*)> Indexer;

void libraryCallIndexer(const RTokenCursor& cursor,
                        const IndexStatus& status,
                        RSourceIndex* pIndex)
{
   if (!cursor.isType(RToken::ID))
      return;
   
   if (!(cursor.contentEquals(L"library") || cursor.contentEquals(L"require")))
      return;
   
   RTokenCursor clone = cursor.clone();
   if (!clone.moveToNextToken())
      return;
   
   if (!clone.isType(RToken::LPAREN))
      return;
   
   if (!clone.moveToNextToken())
      return;
   
   // If the package name is supplied as a string, then we're done.
   if (clone.isType(RToken::STRING))
   {
      std::string pkgName = string_utils::strippedOfQuotes(clone.contentAsUtf8());
      if (isValidRPackageName(pkgName))
         pIndex->addInferredPackage(pkgName);
   }
   
   // If the package name is a symbol, then look forward and check for
   // the 'character.only' argument.
   else if (clone.isType(RToken::ID))
   {
      std::string pkgName = clone.contentAsUtf8();
      if (isValidRPackageName(pkgName))
         pIndex->addInferredPackage(pkgName);
   }
}

void s4MethodIndexer(const RTokenCursor& cursor,
                     const IndexStatus& status,
                     RSourceIndex* pIndex)
{
   if (isMethodOrClassDefinition(cursor))
   {
      bool isSetMethod = false;
      RSourceItem::Type setType = RSourceItem::None;

      if (cursor.contentEquals(L"setMethod"))
      {
         isSetMethod = true;
         setType = RSourceItem::Method;
      }
      else if (cursor.contentEquals(L"setGeneric") ||
               cursor.contentEquals(L"setGroupGeneric"))
      {
         setType = RSourceItem::Method;
      }
      else if (cursor.contentEquals(L"setClass") ||
               cursor.contentEquals(L"setClassUnion") ||
               cursor.contentEquals(L"setRefClass"))
      {
         setType = RSourceItem::Class;
      }
      else
      {
         return;
      }

      // make sure there are at least 4 more tokens
      const RTokens& rTokens = cursor.tokens();
      std::size_t i = cursor.offset();
      
      if (i + 3 >= rTokens.size())
         return;
      
      if ( (rTokens.at(i + 1).type() != RToken::LPAREN) ||
           (rTokens.at(i + 2).type() != RToken::STRING) ||
           (rTokens.at(i + 3).type() != RToken::COMMA))
         return;

      // if this was a setMethod then try to lookahead for the signature
      std::vector<RS4MethodParam> signature;
      if (isSetMethod)
      {
         parseSignature(rTokens.begin() + (i + 4),
                        rTokens.end(),
                        &signature);
      }
      
      addSourceItem(setType,
                    signature,
                    rTokens.at(i + 2),
                    status,
                    pIndex);
   }
}

bool isVariableIndexable(const RTokenCursor& cursor,
                         const IndexStatus& status,
                         RSourceIndex* pIndex)
{
   // can't index if this is the first token
   if (cursor.offset() == 0)
      return false;
   
   // check that cursor is currently on assignment operator
   if (!isLeftAssign(cursor))
      return false;
   
   // allow indexing at top level
   auto&& stack = status.stack();
   if (stack.empty())
      return true;
   
   // allow indexing within an R6Class definition
   for (auto&& index : stack)
   {
      // create token cursor and move to idnex
      RTokenCursor clone = cursor.clone();
      clone.setOffset(index);
      
      // try moving to previous token
      if (!clone.moveToPreviousSignificantToken())
         continue;
      
      // check that it's an R6Class
      if (clone.contentEquals(L"R6Class"))
         return true;
   }
   
   // disallow indexing otherwise
   return false;
   
}

void variableAssignmentIndexer(const RTokenCursor& cursor,
                               const IndexStatus& status,
                               RSourceIndex* pIndex)
{
   // check for indexable location
   bool canIndex = isVariableIndexable(cursor, status, pIndex);
   if (!canIndex)
      return;

   // validate that the previous token is a symbol / string
   // (valid target for assignment)
   const RToken& prevToken = cursor.previousToken();
   bool isExpectedType =
         prevToken.isType(RToken::ID) ||
         prevToken.isType(RToken::STRING);

   if (!isExpectedType)
      return;
 
   // validate that this isn't the assignment into
   // a sub-member of some object; e.g. 'foo$bar <- 1'
   if (cursor.offset() >= 2)
   {
      const RToken& prevPrevToken = cursor.previousToken(2);
      if (isBinaryOp(prevPrevToken))
         return;
   }
   
   // determine index type (function or variable?)
   const RToken& nextToken = cursor.nextToken();
   RSourceItem::Type type = nextToken.contentEquals(L"function")
         ? RSourceItem::Function
         : RSourceItem::Variable;
   
   // determine content for display in finder
   std::string text = string_utils::strippedOfQuotes(prevToken.contentAsUtf8());
   
   // add prefix for R6Class functions
   auto&& tokens = status.tokens();
   auto&& stack = status.stack();
   
   for (auto&& index : stack)
   {
      RTokenCursor cursor(tokens, index);
      
      // check for R6Class definition
      bool isR6Definition =
            cursor.previousSignificantToken().contentEquals(L"R6Class") &&
            cursor.nextSignificantToken().isType(RToken::STRING);
      
      if (!isR6Definition)
         continue;
      
      // don't index non-function things within an R6Class
      // (otherwise we can end up indexing the `public = list(...)`
      // defintiions, which are not useful)
      if (type != RSourceItem::Function)
         return;
    
      // okay, now construct the display text
      std::string className = cursor.nextSignificantToken().contentAsUtf8();
      text = string_utils::strippedOfQuotes(className) + "$" + text;
   }

   // all done -- add source item
   RSourceItem item(
            type,
            text,
            std::vector<RS4MethodParam>(),
            status.count(RToken::LBRACE),
            prevToken.row() + 1,
            prevToken.column() + 1);
   
   pIndex->addSourceItem(item);
   
}

std::vector<Indexer> makeIndexers()
{
   std::vector<Indexer> indexers;
   
   indexers.push_back(libraryCallIndexer);
   indexers.push_back(s4MethodIndexer);
   indexers.push_back(variableAssignmentIndexer);
   
   return indexers;
}

}  // anonymous namespace

RSourceIndex::RSourceIndex(const std::string& context, const std::string& code)
   : context_(context)
{
   static std::vector<Indexer> indexers = makeIndexers();
   
   // clear any (source-local) inferred packages
   inferredPkgNames_.clear();

   // tokenize and create token cursor
   std::wstring wCode = string_utils::utf8ToWide(code, context);
   RTokens rTokens(wCode, RTokens::StripWhitespace | RTokens::StripComments);
   if (rTokens.empty())
      return;
   
   RTokenCursor cursor(rTokens);
   
   // run over tokens and apply indexers
   IndexStatus status(rTokens);
   
   do
   {
      status.update(cursor);
      
      for (const Indexer& indexer : indexers)
      {
         indexer(cursor, status, this);
      }
   }
   while (cursor.moveToNextToken());
   
}

} // namespace r_util
} // namespace core 
} // namespace rstudio


