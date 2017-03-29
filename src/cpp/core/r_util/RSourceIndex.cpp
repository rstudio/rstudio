/*
 * RSourceIndex.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

// static members
std::set<std::string> RSourceIndex::s_allInferredPkgNames_;
std::set<std::string> RSourceIndex::s_importedPackages_;
RSourceIndex::ImportFromMap RSourceIndex::s_importFromDirectives_;
std::map<std::string, PackageInformation> RSourceIndex::s_packageInformation_;
FunctionInformation RSourceIndex::s_noSuchFunction_;

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
   
   IndexStatus()
      : braceLevel_(0),
        parenLevel_(0),
        bracketLevel_(0),
        doubleBracketLevel_(0)
   {}
   
   void update(const RTokenCursor& cursor)
   {
      switch (cursor.type())
      {
      
      case RToken::LBRACE: ++braceLevel_; break;
      case RToken::LPAREN: ++parenLevel_; break;
      case RToken::LBRACKET: ++bracketLevel_; break;
      case RToken::LDBRACKET: ++doubleBracketLevel_; break;
         
      case RToken::RBRACE: --braceLevel_; break;
      case RToken::RPAREN: --parenLevel_; break;
      case RToken::RBRACKET: --bracketLevel_; break;
      case RToken::RDBRACKET: --doubleBracketLevel_; break;
         
      default:
         ; // no-op
         
      }
   }
   
   bool isAtTopLevel() const
   {
      return braceLevel_ == 0 &&
             parenLevel_ == 0 &&
             bracketLevel_ == 0 &&
             doubleBracketLevel_ == 0;
   }
   
   int braceLevel() const { return braceLevel_; }
   int parenLevel() const { return parenLevel_; }
   int bracketLevel() const { return bracketLevel_; }
   int doubleBracketLevel() const { return doubleBracketLevel_; }
   
private:
   
   // NOTE: We use 'int' here to accomodate documents
   // where we might have more closing than opening parens.
   int braceLevel_;            // '{'
   int parenLevel_;            // '('
   int bracketLevel_;          // '['
   int doubleBracketLevel_;    // '[['
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
                            status.braceLevel(),
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

void libraryCallIndexer(const RTokenCursor& cursor, const IndexStatus& status, RSourceIndex* pIndex)
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

void s4MethodIndexer(const RTokenCursor& cursor, const IndexStatus& status, RSourceIndex* pIndex)
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

void variableAssignmentIndexer(const RTokenCursor& cursor, const IndexStatus& status, RSourceIndex* pIndex)
{
   if (status.isAtTopLevel() && isLeftAssign(cursor) && cursor.offset() >= 1)
   {
      const RToken& nextToken = cursor.nextToken();
      RSourceItem::Type type =
            nextToken.contentEquals(L"function") ?
            RSourceItem::Function :
            RSourceItem::Variable;
      
      const RToken& prevToken = cursor.previousToken();
      bool isExpectedType =
            prevToken.isType(RToken::ID) ||
            prevToken.isType(RToken::STRING);
      
      if (isExpectedType)
      {
         if (cursor.offset() >= 2)
         {
            const RToken& prevPrevToken = cursor.previousToken(2);
            if (isBinaryOp(prevPrevToken))
               return;
         }
         
         addSourceItem(type,
                       prevToken,
                       status,
                       pIndex);
      }
   }
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
   IndexStatus status;
   
   do
   {
      status.update(cursor);
      BOOST_FOREACH(const Indexer& indexer, indexers)
      {
         indexer(cursor, status, this);
      }
   } while (cursor.moveToNextToken());
   
}

} // namespace r_util
} // namespace core 
} // namespace rstudio


