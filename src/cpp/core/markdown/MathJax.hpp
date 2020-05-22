/*
 * MathJax.hpp
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

#include <string>
#include <vector>
#include <map>

#include <boost/utility.hpp>
#include <boost/regex.hpp>
#include <boost/function.hpp>

#include <core/HtmlUtils.hpp>

namespace rstudio {
namespace core {
namespace markdown {

struct MathBlock
{
   MathBlock(const std::string& equation,
             const std::string& suffix)
      : equation(equation), suffix(suffix)
   {
   }

   std::string equation;
   std::string suffix;
};

class MathJaxFilter : boost::noncopyable
{
public:
   MathJaxFilter(const std::vector<html_utils::ExcludePattern>& excludePatterns,
                 std::string* pInput,
                 std::string* pHTMLOutput);
   ~MathJaxFilter();

private:
   void filter(const boost::regex& re,
               std::string* pInput,
               std::map<std::string,MathBlock>* pMathBlocks)
   {
      filter(re,
             boost::function<bool(const std::string&)>(),
             pInput,
             pMathBlocks);
   }

   void filter(const boost::regex& re,
               const boost::function<bool(const std::string&)>& condition,
               std::string* pInput,
               std::map<std::string,MathBlock>* pMathBlocks);

   std::string substitute(
               const boost::function<bool(const std::string&)>& condition,
               boost::match_results<std::string::const_iterator> match,
               std::map<std::string,MathBlock>* pMathBlocks);

   void restore(const std::map<std::string,MathBlock>::value_type& block,
                const std::string& beginDelim,
                const std::string& endDelim);

private:
   std::string* pHTMLOutput_;
   std::map<std::string,MathBlock> displayMathBlocks_;
   std::map<std::string,MathBlock> inlineMathBlocks_;
};

bool requiresMathjax(const std::string& htmlOutput);


} // namespace markdown
} // namespace core
} // namespace rstudio
   



