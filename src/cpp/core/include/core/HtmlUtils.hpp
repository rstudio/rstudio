/*
 * HtmlUtils.hpp
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

#ifndef CORE_HTML_UTILS_HPP
#define CORE_HTML_UTILS_HPP

#include <string>

#include <boost/regex.hpp>
#include <boost/iostreams/filter/regex.hpp>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace html_utils {
   

class HTML
{
public:
   HTML() {}
   explicit HTML(const std::string& text, bool isHTML = false);

   const std::string& text() const { return text_; }

 private:
   std::string text_;
};


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

struct ExcludePattern
{
   ExcludePattern(const boost::regex& pattern)
      : begin(pattern)
   {
   }

   ExcludePattern(const boost::regex& beginPattern,
                  const boost::regex& endPattern)
      : begin(beginPattern), end(endPattern)
   {
   }

   boost::regex begin;
   boost::regex end;
};

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


TextRange findClosestRange(std::string::const_iterator pos,
                           const std::vector<TextRange>& ranges);


class HtmlPreserver : boost::noncopyable
{
public:
   void preserve(std::string* pInput);
   void restore(std::string* pOutput);

private:
   std::map<std::string,std::string> preserved_;
};

} // namespace regex_utils
} // namespace core 
} // namespace rstudio


#endif // CORE_HTML_UTILS_HPP

