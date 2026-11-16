/*
 * SessionFilesUpload.hpp
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

#ifndef SESSION_SESSION_FILES_UPLOAD_HPP
#define SESSION_SESSION_FILES_UPLOAD_HPP

#include <cstddef>
#include <functional>
#include <string>
#include <string_view>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace files {

// The boundary parameter of a multipart/form-data Content-Type, which may be
// quoted.
core::Error multipartBoundary(const std::string& contentType, std::string* pBoundary);

// Streaming parser for the Files pane upload form (RFC 7578): the file part
// goes to the sink as it arrives, and the "targetDirectory" field is kept.
// Parts may come in any order, other fields are ignored, and the body may be
// split into chunks at any byte.
class UploadFormParser
{
public:
   // never called with empty data
   typedef std::function<core::Error(const char* data, std::size_t size)> FileSink;

   UploadFormParser(const std::string& boundary, const FileSink& fileSink);

   // `complete` marks the final chunk, which must end the body
   core::Error consume(std::string_view chunk, bool complete);

   bool hasFile() const { return hasFile_; }
   const std::string& fileName() const { return fileName_; }
   const std::string& targetDirectory() const { return targetDirectory_; }

private:
   enum class State { AfterDelimiter, PartHeaders, PartBody, Epilogue };
   enum class PartKind { File, TargetDirectory, Ignored };

   core::Error step(bool* pProgress);
   core::Error beginPart(std::string_view headerBlock);
   core::Error emitPartData(std::size_t size);

   std::string delimiter_;
   FileSink fileSink_;

   State state_;
   PartKind partKind_;
   std::string buffer_;

   bool hasFile_;
   bool hasTargetDirectory_;
   std::string fileName_;
   std::string targetDirectory_;
};

} // namespace files
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_FILES_UPLOAD_HPP
