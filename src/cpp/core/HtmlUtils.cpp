/*
 * HtmlUtils.cpp
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

#include <core/RegexUtils.hpp>


#include <boost/regex.hpp>



namespace core {
namespace html_utils {

std::string defaultTitle(const std::string& htmlContent)
{
   boost::regex re("<[Hh]([1-6]).*?>(.*?)</[Hh]\\1>");
   boost::smatch match;
   if (boost::regex_search(htmlContent, match, re))
      return match[2];
   else
      return "";
}


} // namespace html_utils
} // namespace core 



