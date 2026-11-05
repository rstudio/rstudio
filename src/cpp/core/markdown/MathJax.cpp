/*
 * MathJax.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "MathJax.hpp"

#include <algorithm>

#include <boost/bind/bind.hpp>
#include <boost/function.hpp>
#include <boost/algorithm/string.hpp>

#include <core/HtmlUtils.hpp>
#include <core/RegexUtils.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>

using namespace boost::placeholders;

namespace rstudio {
namespace core {

using namespace html_utils;

namespace markdown {

namespace {


bool hasLessThanThreeNewlines(const std::string& str)
{
   return std::count(str.begin(), str.end(), '\n') < 3;
}

struct CodePattern
{
   CodePattern(const std::string& pattern, bool fenced = false)
      : begin(pattern), fenced(fenced)
   {
   }

   boost::regex begin;
   bool fenced;
};

}

MathJaxFilter::MathJaxFilter(std::string* pInput,
                             std::string* pHTMLOutput,
                             bool escapeHtml)
   : pHTMLOutput_(pHTMLOutput), escapeHtml_(escapeHtml)
{
   // Retain the legacy backtick-only, column-zero fence rules. The filter
   // does not implement full CommonMark context or tilde-fence exclusions.
   // The legacy filter cannot determine
   // where an enclosing list ends. Match only the opener until it is selected,
   // so a fence nested in another code block never scans for its own closer.
   const std::vector<CodePattern> patterns = {
      CodePattern(R"(^(`{3,})(?!`)[^`\r\n]*(?:\r?\n|\z))", true),
      // Match the pinned inline parser's MAXBACKTICKS limit. No fallback may
      // split a longer run into shorter delimiters and hide ordinary math.
#ifdef RSTUDIO_USE_CMARK_GFM
      CodePattern(R"((?<!`)(`{1,80})(?!`)[^\r\n]*?(?<!`)\1(?!`))"),
#else
      CodePattern(R"((?<!`)(`+)(?!`)[^\r\n]*?(?<!`)\1(?!`))"),
      CodePattern(R"(`[^\n]+?`)"),
#endif
      CodePattern(R"((\A|\A\s*\n|\n\s*\n)(( {4}|\t)[^\n]*\n)*(( {4}|\t)[^\n]*))")
   };

   // divide the document into ranges (some of which will be processed
   // and some of which will not -- we don't process some regions so that
   // we don't need to worry about mathjax ambiguity within code regions)
   std::vector<TextRange> ranges;
   std::string::const_iterator pos = pInput->begin();
   std::string::const_iterator inputEnd = pInput->end();
   // Retain future matches (including exhaustion) instead of searching the
   // entire suffix for every pattern after each inline code span.
   std::vector<boost::smatch> nextMatches(patterns.size());
   std::vector<bool> searched(patterns.size(), false);
   bool afterFence = false;
   while (pos != inputEnd)
   {
      std::size_t closest = patterns.size();
      for (std::size_t i = 0; i < patterns.size(); ++i)
      {
         boost::smatch& next = nextMatches[i];
         auto flags = pos == pInput->begin() ? boost::match_default : boost::match_prev_avail;
         // The indented-code pattern's \A also applies just after a fence,
         // where a new block can start without a blank line. Inline spans do
         // not create this boundary: following spaces are still paragraph text.
         boost::smatch atBoundary;
         if (afterFence && regex_utils::search(pos, inputEnd, atBoundary, patterns[i].begin,
                                               flags | boost::match_continuous))
         {
            next = atBoundary;
         }
         else if (!searched[i] || (!next.empty() && next[0].first < pos))
         {
            next = boost::smatch();
            if (pos != pInput->begin())
               flags |= boost::match_not_bob;
            if (!regex_utils::search(pos, inputEnd, next, patterns[i].begin, flags))
               next = boost::smatch();
         }
         searched[i] = true;
         if (!next.empty() &&
             (closest == patterns.size() || next[0].first < nextMatches[closest][0].first))
            closest = i;
      }

      // if we found at least one matched range then find the closest one,
      // add it to our list, and continue
      if (closest != patterns.size())
      {
         const boost::smatch& match = nextMatches[closest];
         TextRange range(false, match[0].first, match[0].second);
         if (patterns[closest].fenced)
         {
            // A closer must use the same character and at least as many
            // delimiters as its opener. Search only for the chosen block.
            std::string delimiter = match[1];
            std::size_t length = delimiter.size();
#ifdef RSTUDIO_USE_CMARK_GFM
            // The pinned parser stores fence_length in a byte and caps it at
            // 255 in blocks.c. Exclusions must end where that parser closes.
            length = std::min(length, std::size_t(255));
#endif
            boost::regex closer("^ {0,3}" + delimiter.substr(0, 1) + "{" +
                                 std::to_string(length) + ",}[ \\t]*\\r?$");
            boost::smatch end;
            range.end = regex_utils::search(range.end, inputEnd, end, closer,
                                            boost::match_prev_avail | boost::match_not_bob)
               ? end[0].second : inputEnd;
         }

         // mark everything before the match as requiring processing
         ranges.push_back(TextRange(true, pos, range.begin));

         // add the range
         ranges.push_back(range);

         // update the position
         pos = range.end;
         afterFence = patterns[closest].fenced;
      }

      // no match -- consume remaining input and tag it for processing
      else
      {
         break;
      }
   }
   if (pos != inputEnd)
      ranges.push_back(TextRange(true, pos, inputEnd));

   // now iterate through the ranges and substitute a guid for math blocks
   std::string filteredInput;
   for (const TextRange& range : ranges)
   {
      std::string rangeText(range.begin, range.end);

      if (range.process)
      {
         // native mathjax display equations
         filter(boost::regex("\\\\\\[([\\s\\S]+?)\\\\\\]"),
                             &rangeText,
                             &displayMathBlocks_);

         // latex display equations (latex designator optional, used for
         // syntactic compatibility w/ wordpress-style inline equations)
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
      // A fixed digit-and-letter prefix keeps math placeholders from being
      // interpreted as HTML tag names or ordered-list markers.
      std::string guid = "0x" + core::system::generateUuid(false);
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
   // Restoration happens after the Markdown renderer's escaping pass.
   std::string equation = escapeHtml_ ? string_utils::htmlEscape(block.second.equation)
                                     : block.second.equation;
   boost::algorithm::replace_first(
     *pHTMLOutput_,
     block.first,
     beginDelim + " " + equation + " " + endDelim +
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
   



