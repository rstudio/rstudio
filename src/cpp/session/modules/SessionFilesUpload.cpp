/*
 * SessionFilesUpload.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionFilesUpload.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace files {

namespace {

// a directory path is small; this only stops a malformed body from making us
// buffer it without bound
constexpr std::size_t kMaxTargetDirectoryBytes = 64 * 1024;

Error formError(const std::string& description, const ErrorLocation& location)
{
   return systemError(boost::system::errc::protocol_error,
                      "Invalid form data received - " + description,
                      location);
}

} // anonymous namespace

UploadFormParser::UploadFormParser(const std::string& boundary, const FileSink& fileSink)
   : fileSink_(fileSink),
     hasFile_(false),
     hasTargetDirectory_(false),
     parser_(boundary, [this](const http::Headers& headers, FileSink* pSink)
     {
        return beginPart(headers, pSink);
     })
{
}

Error UploadFormParser::beginPart(const http::Headers& headers, FileSink* pSink)
{
   // RFC 7578 4.2: every part has a form-data Content-Disposition with a name
   http::util::FormDataDisposition disposition;
   if (!http::util::parseFormDataDisposition(headers, false, &disposition))
      return formError("part is not named form-data", ERROR_LOCATION);

   // the first file and the first targetDirectory win
   if (disposition.isFile)
   {
      if (!hasFile_)
      {
         hasFile_ = true;
         fileName_ = disposition.fileName;
         *pSink = fileSink_;
      }
   }
   else if (disposition.name == "targetDirectory" && !hasTargetDirectory_)
   {
      hasTargetDirectory_ = true;
      *pSink = [this](const char* data, std::size_t size) -> Error
      {
         if (targetDirectory_.size() + size > kMaxTargetDirectoryBytes)
            return formError("targetDirectory too long", ERROR_LOCATION);

         targetDirectory_.append(data, size);
         return Success();
      };
   }

   return Success();
}

} // namespace files
} // namespace modules
} // namespace session
} // namespace rstudio
