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

#include <core/HtmlUtils.hpp>

#include <core/system/System.hpp>

#include <boost/format.hpp>
#include <boost/foreach.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Base64.hpp>
#include <core/FileSerializer.hpp>
#include <core/RegexUtils.hpp>
#include <core/StringUtils.hpp>

#include <core/http/Util.hpp>

namespace rstudio {
namespace core {
namespace html_utils {


HTML::HTML(const std::string& text, bool isHTML)
{
   if (!isHTML)
      text_ = string_utils::htmlEscape(text);
   else
      text_ = text;
}


std::string defaultTitle(const std::string& htmlContent)
{
   boost::regex re("<[Hh]([1-6]).*?>(.*?)</[Hh]\\1>");
   boost::smatch match;
   if (regex_utils::search(htmlContent, match, re))
      return match[2];
   else
      return "";
}


Base64ImageFilter::Base64ImageFilter(const FilePath& basePath)
   : boost::iostreams::regex_filter(
       boost::regex(
        "(<\\s*[Ii][Mm][Gg] [^\\>]*[Ss][Rr][Cc]\\s*=\\s*)([\"'])(.*?)(\\2)"),
        boost::bind(&Base64ImageFilter::toBase64Image, this, _1)),
     basePath_(basePath)
{
}


std::string Base64ImageFilter::toBase64Image(const boost::cmatch& match)
{
   // extract image reference
   std::string imgRef = match[3];

   // url decode it
   imgRef = http::util::urlDecode(imgRef);

   // see if this is an image within the base directory. if it is then
   // base64 encode it
   FilePath imagePath = basePath_.childPath(imgRef);
   if (imagePath.exists() &&
       boost::algorithm::starts_with(imagePath.mimeContentType(), "image/"))
   {     
      std::string imageBase64;
      Error error = core::base64::encode(imagePath, &imageBase64);
      if (!error)
      {
         imgRef = "data:" + imagePath.mimeContentType() + ";base64,";
         imgRef.append(imageBase64);
      }
      else
      {
         LOG_ERROR(error);
      }
   }

   // return the filtered result
   return match[1] + match[2] + imgRef + match[4];
}

// convert fonts to base64

CssUrlFilter::CssUrlFilter(const FilePath& basePath)
   : boost::iostreams::regex_filter(
        boost::regex("url\\('([^'']+)'\\)"),
        boost::bind(&CssUrlFilter::toBase64Url, this, _1)),
     basePath_(basePath)
{
}

std::string CssUrlFilter::toBase64Url(const boost::cmatch& match)
{
   // is this a local file?
   std::string urlRef = match[1];
   FilePath urlPath = basePath_.childPath(urlRef);
   std::string ext = urlPath.extensionLowerCase();
   if (urlPath.exists() && (ext == ".ttf" || ext == ".otf"))
   {
      std::string fontBase64;
      Error error = core::base64::encode(urlPath, &fontBase64);
      if (!error)
      {
         // return base64 encoded font
         std::string type = (ext == ".ttf") ? "truetype" : "opentype";
         boost::format fmt("url(data:font/%1%;base64,%2%)");
         return boost::str(fmt % type % fontBase64);
      }
      else
      {
         LOG_ERROR(error);
         return match[0];
      }
   }
   else
   {
      return match[0];
   }
}

TextRange findClosestRange(std::string::const_iterator pos,
                           const std::vector<TextRange>& ranges)
{
   TextRange closestRange = ranges.front();

   BOOST_FOREACH(const TextRange& range, ranges)
   {
      if (std::abs(range.begin - pos) < std::abs(closestRange.begin - pos))
         closestRange = range;
   }

   return closestRange;
}

void HtmlPreserver::preserve(std::string* pInput)
{
   // begin and end regexes
   boost::regex beginPreserve("<!--html_preserve-->");
   boost::regex endPreserve("<!--\\/html_preserve-->");

   // discover ranges we need to preserve
   std::vector<TextRange> ranges;
   std::string::const_iterator pos = pInput->begin();
   std::string::const_iterator inputEnd = pInput->end();
   while (pos != inputEnd)
   {
      // look for begin marker
      boost::smatch m;
      if (regex_utils::search(pos, inputEnd, m, beginPreserve))
      {
         // set begin iterator
         std::string::const_iterator begin = m[0].first;
         std::string::const_iterator end = m[0].second;

         // look for end marker
         if (regex_utils::search(end, inputEnd, m, endPreserve))
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

         // mark everything before the match as requiring processing
         ranges.push_back(TextRange(true, pos, begin));

         // add the matched range to our list
         ranges.push_back(TextRange(false, begin, end));

         // update the position
         pos = end + 1;

      }
      else
      {
         // no more preserve regions, set termination condition
         ranges.push_back(TextRange(true, pos, pInput->end()));
         pos = inputEnd;
      }
   }

   // substitute guids for all of the matched ranges
   std::string modifiedInput;
   for (std::vector<TextRange>::iterator it = ranges.begin();
        it != ranges.end(); it++)
   {
      if (it->process)
      {
         modifiedInput += std::string(it->begin, it->end);
      }
      else
      {
         std::string guid = core::system::generateUuid();
         std::string html = std::string(it->begin, it->end);
         preserved_[guid] = html;
         modifiedInput += guid;
      }
   }

   // return the modified input
   *pInput = modifiedInput;

}

void HtmlPreserver::restore(std::string* pOutput)
{
   typedef std::pair<std::string,std::string> StringPair;
   BOOST_FOREACH(const StringPair& preserve, preserved_)
   {
      boost::algorithm::replace_first(*pOutput,
                                      preserve.first,
                                      preserve.second);
   }
}



} // namespace html_utils
} // namespace core 
} // namespace rstudio



