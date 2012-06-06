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
#include <boost/algorithm/string.hpp>

#include <core/system/System.hpp>

namespace core {
namespace markdown {

MathJaxFilter::MathJaxFilter(std::string* pInput, std::string* pHTMLOutput)
   : pHTMLOutput_(pHTMLOutput)
{
   filter(boost::regex("\\${2}latex(\\s[\\s\\S]+?)\\${2}"),
                       pInput,
                       &displayMathBlocks_);

   filter(boost::regex("\\$latex(\\s[\\s\\S]+?)\\$"),
                       pInput,
                       &inlineMathBlocks_);
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
   



