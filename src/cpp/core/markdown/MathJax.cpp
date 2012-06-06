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

typedef boost::iterator_range<std::string::const_iterator> ExcludedRange;

std::vector<ExcludedRange> findExcludedRanges(const std::string& input,
                                              const ExcludePattern& pattern)
{
   std::string::const_iterator pos = input.begin();
   std::string::const_iterator end = input.end();

   std::vector<ExcludedRange> ranges;
   while(true)
   {
      // if the pattern isn't found then break
      boost::smatch match;
      if (!boost::regex_search(pos, end, match, pattern.beginPattern))
         break;

      // capture begin location and update pos
      std::string::const_iterator begin = match[1].first;
      pos = match[1].second;

      // if there is an end pattern then look for it -- if it's not
      // found then break
      if (!pattern.endPattern.empty() &&
          !boost::regex_search(pos, end, match, pattern.endPattern))
      {
         break;
      }

      // add to excluded ranges (match.end() will either be the end
      // of the beginPattern match or the endPattern match)
      ranges.push_back(ExcludedRange(begin, match[1].second));
    }

    return ranges;
}

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
   ranges.push_back(TextRange(true, pInput->begin(), pInput->end()));


   // now iterate through the ranges and substitute a guid for math blocks
   std::string filteredInput;
   BOOST_FOREACH(const TextRange& range, ranges)
   {
      std::string rangeText(range.begin, range.end);

      if (range.process)
      {
         filter(boost::regex("\\${2}latex(\\s[\\s\\S]+?)\\${2}"),
                             &rangeText,
                             &displayMathBlocks_);

         filter(boost::regex("\\$latex(\\s[\\s\\S]+?)\\$"),
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
                           std::map<std::string,std::string>* pMathBlocks)
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
               std::map<std::string,std::string>* pMathBlocks)
{
   // insert a guid
   std::string guid = core::system::generateUuid(false);
   pMathBlocks->insert(std::make_pair(guid, match[1]));
   return guid;
}

void MathJaxFilter::restore(
               const std::map<std::string,std::string>::value_type& block,
               const std::string& beginDelim,
               const std::string& endDelim)
{
   boost::algorithm::replace_first(
                       *pHTMLOutput_,
                       block.first,
                       beginDelim + " " + block.second + " " + endDelim);
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
   



