/*
 * MathJax.hpp
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

#include <string>
#include <map>

#include <boost/utility.hpp>
#include <boost/regex.hpp>

namespace core {
namespace markdown {

class MathJaxFilter : boost::noncopyable
{
public:
   MathJaxFilter(std::string* pInput, std::string* pHTMLOutput);
   ~MathJaxFilter();

private:
   void filter(const boost::regex& re,
               std::string* pInput,
               std::map<std::string,std::string>* pMathBlocks);

   std::string substitute(
               boost::match_results<std::string::const_iterator> match,
               std::map<std::string,std::string>* pMathBlocks);

   void restore(const std::map<std::string,std::string>::value_type& block,
                const std::string& beginDelim,
                const std::string& endDelim);

private:
   std::string* pHTMLOutput_;
   std::map<std::string,std::string> displayMathBlocks_;
   std::map<std::string,std::string> inlineMathBlocks_;
};

bool requiresMathjax(const std::string& htmlOutput);


} // namespace markdown
} // namespace core
   



