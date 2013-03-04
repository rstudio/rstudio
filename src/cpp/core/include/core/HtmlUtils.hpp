/*
 * HtmlUtils.hpp
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

#ifndef CORE_HTML_UTILS_HPP
#define CORE_HTML_UTILS_HPP

#include <string>

#include <boost/regex.hpp>
#include <boost/iostreams/filter/regex.hpp>

#include <core/FilePath.hpp>

namespace core {
namespace html_utils {
   
std::string defaultTitle(const std::string& htmlContent);

// convert images to base64
class Base64ImageFilter : public boost::iostreams::regex_filter
{
public:
   explicit Base64ImageFilter(const FilePath& basePath);

private:
   std::string toBase64Image(const boost::cmatch& match);

private:
   FilePath basePath_;
};

// convert fonts to base64
class CssUrlFilter : public boost::iostreams::regex_filter
{
public:
   explicit CssUrlFilter(const FilePath& basePath);

private:
   std::string toBase64Url(const boost::cmatch& match);

private:
   FilePath basePath_;
};


} // namespace regex_utils
} // namespace core 


#endif // CORE_HTML_UTILS_HPP

