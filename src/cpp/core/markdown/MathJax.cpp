/*
 * MathJax.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

#include <core/system/System.hpp>

namespace core {
namespace markdown {

namespace {

struct TextRange
{
   TextRange(bool process,
             const std::string::const_iterator& begin,
             const std::string::const_iterator& end)
      : process(process), begin(begin), end(end)
   {
   }

   bool process;
   std::string::const_iterator begin;
   std::string::const_iterator end;
};


}

MathJaxFilter::MathJaxFilter(const std::vector<ExcludePattern>& excludePatterns,
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
      bool foundPattern = false;
      BOOST_FOREACH(const ExcludePattern& pattern, excludePatterns)
      {
         boost::smatch m;
         if (boost::regex_search(pos, inputEnd, m, pattern.begin))
         {
            // set begin and end (may change if there is an end pattern)
            std::string::const_iterator begin = m[0].first;
            std::string::const_iterator end = m[0].second;

            // check for a second match
            if (!pattern.end.empty())
            {
               if (boost::regex_search(end, inputEnd, m, pattern.end))
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

            // mark everything before the match as requiring processing
            ranges.push_back(TextRange(true, pos, begin));

            // mark the match as excluded from processing
            ranges.push_back(TextRange(false, begin, end));

            // update the position
            pos = end;

            // mark us as finding a pattern and break out of pattern loop
            foundPattern = true;
            break;

         }
      }

      // if we didn't find a pattern then consume the rest of the input
      // and mark it as requiring processing
      if (!foundPattern)
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
         filter(boost::regex("\\\\\\\\\\[([\\s\\S]+?)\\\\\\\\\\]"),
                             &rangeText,
                             &displayMathBlocks_);

         // latex display equations (latex designator optional, used for
         // syntactic compatiblity w/ wordpress-style inline equations)
         filter(boost::regex("\\${2}(?:latex)?\\s([\\s\\S]+?)\\${2}"),
                             &rangeText,
                             &displayMathBlocks_);

         // native mathjax inline equations
         filter(boost::regex("\\\\\\\\\\(([\\s\\S]+?)\\\\\\\\\\)"),
                             &rangeText,
                             &inlineMathBlocks_);

         // wordpress style inline equations
         filter(boost::regex("\\$latex\\s([\\s\\S]+?)\\$"),
                             &rangeText,
                             &inlineMathBlocks_);

         // Org-mode style inline equations
         filter(boost::regex("\\$((?!\\s)[^$]*[^$\\s])\\$([\\s\\-\\.\\!\\?])"),
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
                           std::string* pInput,
                           std::map<std::string,MathBlock>* pMathBlocks)
{
   // explicit function type required because the Formatter functor
   // supports 3 distinct signatures
   boost::function<std::string(
       boost::match_results<std::string::const_iterator>)> formatter =
                              boost::bind(&MathJaxFilter::substitute,
                                          this, _1, pMathBlocks);

   *pInput = boost::regex_replace(*pInput, re, formatter);
}

std::string MathJaxFilter::substitute(
               boost::match_results<std::string::const_iterator> match,
               std::map<std::string,MathBlock>* pMathBlocks)
{
   // insert a guid
   std::string guid = core::system::generateUuid(false);
   std::string equation = match[1];
   std::string suffix = (match.size() > 2) ? std::string(match[2]) : "";
   pMathBlocks->insert(std::make_pair(guid, MathBlock(equation,suffix)));
   return guid;
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
   if (boost::regex_search(htmlOutput, inlineMathRegex))
      return true;

   boost::regex displayMathRegex("\\\\\\[([\\s\\S]+?)\\\\\\]");
   if (boost::regex_search(htmlOutput, displayMathRegex))
      return true;

   boost::regex mathmlRegex("<math[>\\s](?s).*?</math>");
   if (boost::regex_search(htmlOutput, mathmlRegex))
      return true;

   return false;
}

} // namespace markdown
} // namespace core
   



