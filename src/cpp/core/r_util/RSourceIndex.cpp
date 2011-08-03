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

   // scan for functions
   std::wstring function(L"function");
   std::wstring eqOp(L"=");
   std::wstring assignOp(L"<-");
   std::wstring parentAssignOp(L"<<-");
   for (std::size_t i=0; i<rTokens.size(); i++)
   {
      // is this a function?
      if (rTokens.at(i).isIdentifier(function))
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
         // (next step is likely json so convert to utf8 here)
         std::string name = string_utils::wideToUtf8(idToken.content());

         // compute the line by starting at the current line index and
         // finding the first newline which is after the idToken offset
         newlineIter = std::upper_bound(newlineIter,
                                        endNewlines,
                                        idToken.offset());
         std::size_t line = newlineIter - newlineLocs.begin() + 1;

         // compute column by comparing the offset to the PREVIOUS newline
         // (guard against no previous newline)
         std::size_t column;
         if (line > 1)
            column = idToken.offset() - *(newlineIter - 1);
         else
            column = idToken.offset();

         // add to our list of indexed functions
         functions_.push_back(RFunctionInfo(name, line, column));
      }
   }
}


boost::regex RSourceIndex::patternToRegex(const std::string& pattern)
{
   // split into componenents
   using namespace boost::algorithm;
   std::vector<std::string> components;
   split(components, pattern, is_any_of("*"), token_compress_on);

   // build and return regex
   std::string regex;
   for (std::size_t i=0; i<components.size(); i++)
   {
      if (i > 0)
         regex.append(".*");
      regex.append("\\Q");
      regex.append(components.at(i));
      regex.append("\\E");
   }
   return boost::regex(regex);
}


} // namespace r_util
} // namespace core 


