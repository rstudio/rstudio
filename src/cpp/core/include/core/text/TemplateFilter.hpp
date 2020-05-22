/*
 * TemplateFilter.hpp
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

#ifndef CORE_TEXT_TEMPLATE_FILTER_HPP
#define CORE_TEXT_TEMPLATE_FILTER_HPP

#include <iostream>

#include <string>
#include <map>

#include <boost/bind.hpp>
#include <boost/function.hpp>

#include <boost/iostreams/filter/regex.hpp>

#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {

class FilePath;
namespace http {
   class Request;
   class Response;
}

namespace text {

// Add variables to templates using #foo# syntax. All values will be
// HTML-escaped automatically unless prepended with !, e.g. #!foo# will
// use the raw value of foo. Alternatively, you can prepend with ' and JS
// literal escaping will be used instead of HTML escaping (i.e. the value
// will be prepared for inserting into a JavaScript string literal).
class TemplateFilter : public boost::iostreams::regex_filter
{
public:
   TemplateFilter(const std::map<std::string, std::string>& variables)
      : boost::iostreams::regex_filter(
            boost::regex("#([!\\']?)([A-Za-z0-9_-]+)#"),
            boost::bind(&TemplateFilter::substitute, this, _1)),
   
        variables_(variables)
   {
   }
   
private:
   std::string substitute(const boost::cmatch& match)
   {
      std::map<std::string, std::string>::const_iterator valPos = 
                                                   variables_.find(match[2]);
      if (valPos != variables_.end())
      {
         if (match[1] == "!")
            return valPos->second;
         else if (match[1] == "'")
            return string_utils::jsLiteralEscape(valPos->second);
         else
            return string_utils::htmlEscape(valPos->second, true);
         return valPos->second;
      }
      else
         return "MISSING VALUE";
   }

private:
   std::map<std::string, std::string> variables_;
};



void handleTemplateRequest(const FilePath& templatePath,
                           const http::Request& request,
                           http::Response* pResponse);

void handleSecureTemplateRequest(const std::string& username,
                                 const FilePath& progressPagePath,
                                 const http::Request& request,
                                 http::Response* pResponse);

core::Error renderTemplate(const core::FilePath& templateFile,
                           const std::map<std::string, std::string> &vars,
                           std::ostream& os);


} // namespace text
} // namespace core
} // namespace rstudio


#endif // CORE_TEXT_TEMPLATE_FILTER_HPP
