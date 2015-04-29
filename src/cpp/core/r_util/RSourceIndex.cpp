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

#include <core/r_util/RSourceIndex.hpp>

#include <boost/algorithm/string.hpp>

#include <core/StringUtils.hpp>

#include <core/r_util/RTokenizer.hpp>
#include <core/Macros.hpp>

namespace rstudio {
namespace core {
namespace r_util {

using namespace token_utils;

// static members
std::set<std::string> RSourceIndex::s_allInferredPkgNames_;
std::set<std::string> RSourceIndex::s_importedPackages_;
RSourceIndex::ImportFromMap RSourceIndex::s_importFromDirectives_;
std::map<std::string, PackageInformation> RSourceIndex::s_packageInformation_;
FunctionInformation RSourceIndex::s_noSuchFunction_;

namespace {

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

}  // anonymous namespace

RSourceIndex::RSourceIndex(const std::string& context,
                           const std::string& code)
   : context_(context)
{
   // clear any (source-local) inferred packages
   inferredPkgNames_.clear();

   // convert code to wide
   std::wstring wCode = string_utils::utf8ToWide(code, context);

   // determine where the linebreaks are and initialize an iterator
   // used for scanning them
   std::vector<std::size_t> newlineLocs;
   std::size_t nextNL = 0;
   while ( (nextNL = wCode.find(L'\n', nextNL)) != std::string::npos )
      newlineLocs.push_back(nextNL++);
   std::vector<std::size_t>::const_iterator newlineIter = newlineLocs.begin();
   std::vector<std::size_t>::const_iterator endNewlines = newlineLocs.end();

   // tokenize
   RTokens rTokens(wCode, RTokens::StripWhitespace | RTokens::StripComments);
   
   // track nest level
   int braceLevel = 0;
   int parenLevel = 0;
   int bracketLevel = 0;
   int doubleBracketLevel = 0;
   
   // scan for function, method, and class definitions
   std::wstring function(L"function");
   std::wstring set(L"set");
   std::wstring setGeneric(L"setGeneric");
   std::wstring setGroupGeneric(L"setGroupGeneric");
   std::wstring setMethod(L"setMethod");
   std::wstring setClass(L"setClass");
   std::wstring setClassUnion(L"setClassUnion");
   std::wstring setRefClass(L"setRefClass");
   
   // operator tokens
   std::wstring eqOp(L"=");
   std::wstring assignOp(L"<-");
   std::wstring parentAssignOp(L"<<-");
   
   // library, require (requireNamespace?)
   std::wstring package(L"package");
   std::wstring library(L"library");
   std::wstring require(L"require");
   
   std::size_t n = rTokens.size();
   for (std::size_t i = 0; i < n; ++i)
   {
      // initial name, qualifer, and type are nil
      RSourceItem::Type type = RSourceItem::None;
      std::wstring name;
      std::size_t tokenOffset = -1;
      bool isSetMethod = false;
      std::vector<RS4MethodParam> signature;

      // alias the token
      const RToken& token = rTokens.at(i);
      DEBUG("Current token: " << token);
      
      // update brace nesting levels
      braceLevel += token.isType(RToken::LBRACE);
      braceLevel -= token.isType(RToken::RBRACE);
      
      parenLevel += token.isType(RToken::LPAREN);
      parenLevel -= token.isType(RToken::RPAREN);
      
      bracketLevel += token.isType(RToken::LBRACKET);
      bracketLevel -= token.isType(RToken::RBRACKET);
      
      doubleBracketLevel += token.isType(RToken::LDBRACKET);
      doubleBracketLevel -= token.isType(RToken::RDBRACKET);

      // is this a potential method or class definition?
      if (isMethodOrClassDefinition(token))
      {
         RSourceItem::Type setType = RSourceItem::None;

         if (token.contentEquals(setMethod))
         {
            isSetMethod = true;
            setType = RSourceItem::Method;
         }
         else if (token.contentEquals(setGeneric) ||
                  token.contentEquals(setGroupGeneric))
         {
            setType = RSourceItem::Method;
         }
         else if (token.contentEquals(setClass) ||
                  token.contentEquals(setClassUnion) ||
                  token.contentEquals(setRefClass))
         {
            setType = RSourceItem::Class;
         }
         else
         {
            continue;
         }

         // make sure there are at least 4 more tokens
         if ( (i + 3) >= rTokens.size())
            continue;

         // check for the rest of the token sequene for a valid call to set*
         if ( (rTokens.at(i+1).type() != RToken::LPAREN) ||
              (rTokens.at(i+2).type() != RToken::STRING) ||
              (rTokens.at(i+3).type() != RToken::COMMA))
            continue;

         // found a class or method definition (will find location below)
         type = setType;
         name = removeQuoteDelims(rTokens.at(i+2).content());
         tokenOffset = token.offset();

         // if this was a setMethod then try to lookahead for the signature
         if (isSetMethod)
         {
            parseSignature(rTokens.begin() + (i+4),
                           rTokens.end(),
                           &signature);
         }
      }

      // is this a function?
      else if (token.contentEquals(function))
      {
         // if there is no room for an operator and identifier prior
         // to the function then bail
         if (i < 2)
            continue;

         // check for an assignment operator
         const RToken& opToken = rTokens.at(i-1);
         if ( opToken.type() != RToken::OPER)
            continue;
         if (!opToken.isOperator(eqOp) &&
             !opToken.isOperator(assignOp) &&
             !opToken.isOperator(parentAssignOp))
            continue;

         // check for an identifier or string
         const RToken& idToken = rTokens.at(i-2);
         if (!(idToken.type() == RToken::ID ||
               idToken.type() == RToken::STRING))
            continue;

         // if there is another previous token make sure it isn't a
         // comma or an open paren
         if ( i > 2 )
         {
            const RToken& prevToken = rTokens.at(i-3);
            if (prevToken.type() == RToken::LPAREN ||
                prevToken.type() == RToken::COMMA)
               continue;
         }

         // if we got this far then this is a function definition
         type = RSourceItem::Function;
         name = idToken.content();
         tokenOffset = idToken.offset();
      }
      
      // is this a library / require call?
      else if (token.contentEquals(library) ||
               token.contentEquals(require))
      {
         DEBUG("** Checking library token!");
         
         // make sure we have enough tokens available
         if (i + 3 >= rTokens.size())
            continue;
         
         // check for an open paren following
         const RToken& lParenToken = rTokens.at(i + 1);
         if (lParenToken.type() != RToken::LPAREN)
            continue;
         
         // ensure that the following token is not a closing paren
         if (static_cast<const RToken&>(rTokens.at(i + 2)).type() == RToken::RPAREN)
            continue;
         
         // walk forward until we find an associated closing paren -- we'll have to
         // look ahead a bit, and count parens as we go. also attempt to figure out
         // what the 'package' and 'character.only' arguments resolve to
         bool characterOnly = false;
         
         // assume that the package token is the first argument (but change our minds later
         // if necessary)
         RToken packageToken = rTokens.at(i + 2);
         
         std::size_t endParenIndex = i + 2;
         bool foundRightParen = false;
         int parenCount = 0;
         for (int count = 0; count < 30; ++count)
         {
            if (endParenIndex + count >= rTokens.size())
               break;
            
            const RToken& currentToken = rTokens.at(endParenIndex + count);
            DEBUG("- Current token: '" << string_utils::wideToUtf8(currentToken.content()) << "'");
            
            if (currentToken.type() == RToken::LPAREN)
               ++parenCount;
            
            if (currentToken.type() == RToken::RPAREN)
            {
               if (parenCount == 0)
               {
                  DEBUG("- Found closing right paren");
                  foundRightParen = true;
                  break;
               }
               --parenCount;
            }
            
            // if we encounter 'package =', update the package setting
            if (currentToken.contentEquals(package) &&
                endParenIndex + count + 2 < rTokens.size())
            {
               const RToken& nextToken = rTokens.at(endParenIndex + count + 1);
               if (nextToken.contentEquals(eqOp))
               {
                  // note -- validate later whether this is appropriate as a package name
                  packageToken = rTokens.at(endParenIndex + count + 2);
               }
            }
            
            // if we encounter the token 'character.only', assume that it's
            // set to TRUE, ie,
            //
            //    library("foo", character.only = TRUE)
            //
            // it would be unlikely someone would set character.only = FALSE
            // which is the default
            if (currentToken.contentEquals(L"character.only"))
            {
               DEBUG("- - Setting 'characterOnly' to true");
               characterOnly = true;
            }
            
         }
         
         if (!foundRightParen)
            continue;
         
         // if 'character.only' is TRUE and 'package' is not a string, bail
         if (characterOnly && packageToken.type() != RToken::STRING)
         {
            DEBUG("* characterOnly is true but package token'"
                  << string_utils::wideToUtf8(packageToken.content())
                  << "' is not a string");
            continue;
         }
         
         // otherwise, take the name of the package and cache it
         std::wstring packageName = packageToken.type() == RToken::STRING ?
                  removeQuoteDelims(packageToken.content()) :
                  packageToken.content();
         
         DEBUG("** Adding package '" << string_utils::wideToUtf8(packageName) << "'");
         DEBUG("");

         addInferredPackage(string_utils::wideToUtf8(packageName));

         continue;
      }
      
      // is this some other kind of assignment?
      // we want to add e.g.
      //
      //    foo <- 1
      //
      // to the source index here
      else if (i >= 1 && isLeftAssign(rTokens.at(i - 1)))
      {
         // bail if we're not at the top level
         if (braceLevel || parenLevel || bracketLevel || doubleBracketLevel)
         {
            DEBUG("Bailing: [" << braceLevel << ", " << parenLevel << ", " <<
                  bracketLevel << ", " << doubleBracketLevel << "]");
            continue;
         }
         
         // ensure the token previous to the left assign is
         // an identifier or string
         const RToken& idToken = rTokens.at(i - 2);
         DEBUG("- Token: " << idToken);
         
         if (!(isId(idToken) || isString(idToken)))
         {
            DEBUG("* Not an id or string");
            continue;
         }
         
         // ensure the token previous to the assignment token
         // is not a binary operator
         if (i > 2)
         {
            const RToken& beforeId = rTokens.at(i - 3);
            if (isBinaryOp(beforeId))
            {
               DEBUG("* Has binary op before; skipping");
               continue;
            }
         }
            
         // if we get this far then it's a variable def'n
         type = RSourceItem::Variable;
         name = idToken.content();
         tokenOffset = idToken.offset();
      }
      
      else
      {
         continue;
      }

      // compute the line by starting at the current line index and
      // finding the first newline which is after the idToken offset
      newlineIter = std::upper_bound(newlineIter,
                                     endNewlines,
                                     tokenOffset);
      std::size_t line = newlineIter - newlineLocs.begin() + 1;

      // compute column by comparing the offset to the PREVIOUS newline
      // (guard against no previous newline)
      std::size_t column;
      if (line > 1)
         column = tokenOffset - *(newlineIter - 1);
      else
         column = tokenOffset;

      // add to index
      items_.push_back(RSourceItem(type,
                                   string_utils::wideToUtf8(name),
                                   signature,
                                   braceLevel,
                                   line,
                                   column));

   }
}

} // namespace r_util
} // namespace core 
} // namespace rstudio


