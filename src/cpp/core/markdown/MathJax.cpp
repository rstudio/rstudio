/*
 * MathJax.cpp
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

#include "MathJax.hpp"

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/foreach.hpp>
#include <boost/algorithm/string.hpp>

#include <core/RegexUtils.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {

using namespace html_utils;

namespace markdown {

namespace {


bool hasLessThanThreeNewlines(const std::string& str)
{
   return std::count(str.begin(), str.end(), '\n') < 3;
}

}

MathJaxFilter::MathJaxFilter(const std::vector<html_utils::ExcludePattern>& excludePatterns,
                             std::string* pInput,
                             std::string* pHTMLOutput)
   : pHTMLOutput_(pHTMLOutput)
{
   // divide the document into ranges (some of which will be processed
   // and some of which will not -- we don't process some regions so that
   // we don't need to worry about mathjax ambiguity within code regions)
   std::vector<TextRange> ranges;
   std::string::const_iterator pos = pInput->begin();
   std::string::const_iterator inputEnd = pInput->end();
   while (pos != inputEnd)
   {
      // try all of the exclude patterns
      std::vector<TextRange> matchedRanges;
      BOOST_FOREACH(const html_utils::ExcludePattern& pattern, excludePatterns)
      {
         boost::smatch m;
         if (regex_utils::search(pos, inputEnd, m, pattern.begin))
         {
            // set begin and end (may change if there is an end pattern)
            std::string::const_iterator begin = m[0].first;
            std::string::const_iterator end = m[0].second;

            // check for a second match
            if (!pattern.end.empty())
            {
               if (regex_utils::search(end, inputEnd, m, pattern.end))
               {
                  // update end to be the end of the match
                  end = m[0].second;
               }
               else
               {
                  // didn't find a matching end pattern so set the end to the
                  // end of the document -- this will cause us to exclude the
                  // rest of the document from processing
                  end = inputEnd;
               }
            }

            // add the matched range to our list
            matchedRanges.push_back(TextRange(false, begin, end));
         }
      }

      // if we found at least one matched range then find the closest one,
      // add it to our list, and continue
      if (!matchedRanges.empty())
      {
         // find the closest range
         TextRange range = findClosestRange(pos, matchedRanges);

         // mark everything before the match as requiring processing
         ranges.push_back(TextRange(true, pos, range.begin));

         // add the range
         ranges.push_back(range);

         // update the position
         pos = range.end;
      }

      // no match -- consume remaining input and tag it for processing
      else
      {
         ranges.push_back(TextRange(true, pos, pInput->end()));
         pos = pInput->end();
      }
   }

   // now iterate through the ranges and substitute a guid for math blocks
   std::string filteredInput;
   BOOST_FOREACH(const TextRange& range, ranges)
   {
      std::string rangeText(range.begin, range.end);

      if (range.process)
      {
         // native mathjax display equations
         filter(boost::regex("\\\\\\[([\\s\\S]+?)\\\\\\]"),
                             &rangeText,
                             &displayMathBlocks_);

         // latex display equations (latex designator optional, used for
         // syntactic compatiblity w/ wordpress-style inline equations)
         filter(boost::regex("\\${2}(?:latex\\s)?([\\s\\S]+?)\\${2}"),
                             &rangeText,
                             &displayMathBlocks_);

         // native mathjax inline equations
         filter(boost::regex("\\\\\\(([\\s\\S]+?)\\\\\\)"),
                             &rangeText,
                             &inlineMathBlocks_);

         // wordpress style inline equations
         filter(boost::regex("\\$latex\\s([\\s\\S]+?)\\$"),
                             &rangeText,
                             &inlineMathBlocks_);

         // Org-mode style inline equations
         filter(boost::regex("\\$((?!\\s)[^$]*[^$\\s])\\$(?![\\w\\d`])"),
                             &hasLessThanThreeNewlines,
                             &rangeText,
                             &inlineMathBlocks_);
      }

      filteredInput.append(rangeText);
   }

   *pInput = filteredInput;
}

MathJaxFilter::~MathJaxFilter()
{
   try
   {
      std::for_each(
         displayMathBlocks_.begin(),
         displayMathBlocks_.end(),
         boost::bind(&MathJaxFilter::restore, this, _1, "\\[", "\\]"));

      std::for_each(
         inlineMathBlocks_.begin(),
         inlineMathBlocks_.end(),
         boost::bind(&MathJaxFilter::restore, this, _1, "\\(", "\\)"));
   }
   catch(...)
   {
   }
}

void MathJaxFilter::filter(const boost::regex& re,
                           const boost::function<bool(const std::string&)>& condition,
                           std::string* pInput,
                           std::map<std::string,MathBlock>* pMathBlocks)
{
   // explicit function type required because the Formatter functor
   // supports 3 distinct signatures
   boost::function<std::string(
       boost::match_results<std::string::const_iterator>)> formatter =
                              boost::bind(&MathJaxFilter::substitute,
                                          this, condition, _1, pMathBlocks);

   *pInput = boost::regex_replace(*pInput, re, formatter);
}

std::string MathJaxFilter::substitute(
               const boost::function<bool(const std::string&)>& condition,
               boost::match_results<std::string::const_iterator> match,
               std::map<std::string,MathBlock>* pMathBlocks)
{
   // get the equation
   std::string equation = match[1];

   // apply additional condition if available
   if (condition && !condition(equation))
   {
      // don't perform any substitution
      return match[0];
   }
   else
   {
      std::string guid = core::system::generateUuid(false);
      std::string suffix = (match.size() > 2) ? std::string(match[2]) : "";
      pMathBlocks->insert(std::make_pair(guid, MathBlock(equation,suffix)));
      return guid;
   }
}

void MathJaxFilter::restore(
               const std::map<std::string,MathBlock>::value_type& block,
               const std::string& beginDelim,
               const std::string& endDelim)
{
   boost::algorithm::replace_first(
     *pHTMLOutput_,
     block.first,
     beginDelim + " " + block.second.equation + " " + endDelim +
       block.second.suffix);
}

bool requiresMathjax(const std::string& htmlOutput)
{
   boost::regex inlineMathRegex("\\\\\\(([\\s\\S]+?)\\\\\\)");
   if (regex_utils::search(htmlOutput, inlineMathRegex))
      return true;

   boost::regex displayMathRegex("\\\\\\[([\\s\\S]+?)\\\\\\]");
   if (regex_utils::search(htmlOutput, displayMathRegex))
      return true;

   boost::regex mathmlRegex("<math[>\\s](?s).*?</math>");
   if (regex_utils::search(htmlOutput, mathmlRegex))
      return true;

   return false;
}

} // namespace markdown
} // namespace core
} // namespace rstudio
   



