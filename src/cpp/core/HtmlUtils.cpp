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

#include <boost/algorithm/string/predicate.hpp>

#include <core/Base64.hpp>
#include <core/FileSerializer.hpp>

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

   // see if this is an image within the base directory. if it is then
   // base64 encode it
   FilePath imagePath = basePath_.childPath(imgRef);
   if (imagePath.exists() &&
       boost::algorithm::starts_with(imagePath.mimeContentType(), "image/"))
   {
      std::string imageContents;
      Error error = core::readStringFromFile(imagePath, &imageContents);
      if (!error)
      {
         std::string imageBase64;
         Error error = core::base64::encode(imageContents, &imageBase64);
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
      else
      {
         LOG_ERROR(error);
      }
   }

   // return the filtered result
   return match[1] + match[2] + imgRef + match[4];
}

} // namespace html_utils
} // namespace core 



