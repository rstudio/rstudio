/*
 * MultipartRelated.cpp
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

#include <core/http/MultipartRelated.hpp>

#include <iostream>

#define kBoundary             "END_OF_PART";
#define kSectionBoundary      "--END_OF_PART"
#define kTerminatingBoundary  "--END_OF_PART--"
#define kContentType          "multipart/related; boundary=END_OF_PART"

namespace rstudio {
namespace core {
namespace http {

void MultipartRelated::addPart(const std::string& contentType,
                               const std::string& body)
{
   bodyStream_ << kSectionBoundary << std::endl;
   bodyStream_ << "Content-Type: " << contentType << std::endl << std::endl;
   bodyStream_ << body << std::endl;
}

void MultipartRelated::terminate()
{
   bodyStream_ << kTerminatingBoundary;
}

std::string MultipartRelated::contentType() const
{
   return kContentType;
}

std::string MultipartRelated::body() const
{
   return bodyStream_.str();
}

} // namespace http
} // namespace core
} // namespace rstudio
