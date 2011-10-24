/*
 * RSourceIndex.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/r_util/RSourceIndex.hpp>

#include <boost/algorithm/string.hpp>

#include <core/StringUtils.hpp>

#include <core/r_util/RTokenizer.hpp>

namespace core {
namespace r_util {

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
   return string_utils::wideToUtf8(removeQuoteDelims(token.content()));
}

void parseSignatureFunction(RTokens::const_iterator begin,
                            RTokens::const_iterator end,
                            std::vector<RS4MethodParam>* pSignature)
{
   while (begin != end && begin->type() == RToken::ID)
   {


   }
}

void parseSignatureCharacterVector(RTokens::const_iterator begin,
                                   RTokens::const_iterator end,
                                   std::vector<RS4MethodParam>* pSignature)
{
   while (begin != end && begin->type() == RToken::STRING)
   {
      // get the type string
      pSignature->push_back(RS4MethodParam(contentAsUtf8(*begin)));

      // advance
      begin++;

      // if end then terminate
      if (begin == end)
         break;

      // if not a comma then terminate
      else if (begin->type() != RToken::COMMA)
         break;

      // otherwise advance
      else
         begin++;
   }
}

void parseSignature(RTokens::const_iterator begin,
                    RTokens::const_iterator end,
                    std::vector<RS4MethodParam>* pSignature)
{
   // this code isn't live yet
   return;

   /*
   // the signature parameter of the setMethod function can take any
   // of the following forms
   //
   // setMethod("plot", signature(x="track", y="missing")
   // setMethod("plot", "track"
   // setMethod("plot", c("track", "missing")
   //

   if (begin == end)
      return;

   if (begin->type() == RToken::ID)
   {
      // call to signature function
      if (begin->contentEquals(std::wstring(L"signature")))
         parseSignatureFunction(begin+1, end, pSignature);

      // simple list of types
      else if (begin->contentEquals(std::wstring(L"c")))
         parseSignatureCharacterVector(begin+1, end, pSignature);
   }

   // single quoted string
   else if (begin->type() == RToken::STRING)
   {
      pSignature->push_back(RS4MethodParam(contentAsUtf8(*begin)));
   }
   */
}


}  // anonymous namespace

RSourceIndex::RSourceIndex(const std::string& context,
                           const std::string& code)
   : context_(context)
{
   // convert code to wide
   std::wstring wCode = string_utils::utf8ToWide(code);

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

   // scan for function, method, and class definitions (track indent level)
   int braceLevel = 0;
   std::wstring function(L"function");
   std::wstring set(L"set");
   std::wstring setGeneric(L"setGeneric");
   std::wstring setGroupGeneric(L"setGroupGeneric");
   std::wstring setMethod(L"setMethod");
   std::wstring setClass(L"setClass");
   std::wstring setClassUnion(L"setClassUnion");
   std::wstring eqOp(L"=");
   std::wstring assignOp(L"<-");
   std::wstring parentAssignOp(L"<<-");
   for (std::size_t i=0; i<rTokens.size(); i++)
   {
      // initial name, qualifer, and type are nil
      RSourceItem::Type type = RSourceItem::None;
      std::wstring name;
      std::size_t tokenOffset = -1;
      bool isSetMethod = false;
      std::vector<RS4MethodParam> signature;

      // alias the token
      const RToken& token = rTokens.at(i);

      // see if this is a begin or end brace and update the level
      if (token.type() == RToken::LBRACE)
      {
         braceLevel++;
         continue;
      }

      else if (token.type() == RToken::RBRACE)
      {
         braceLevel--;
         continue;
      }
      // bail for non-identifiers
      else if (token.type() != RToken::ID)
      {
         continue;
      }

      // is this a potential method or class definition?
      if (token.contentStartsWith(set))
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
                  token.contentEquals(setClassUnion))
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

         // check for an identifier
         const RToken& idToken = rTokens.at(i-2);
         if ( idToken.type() != RToken::ID )
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


