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

#include <algorithm>

#include <boost/algorithm/string/predicate.hpp>

#include <core/http/Header.hpp>
#include <core/http/Util.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace files {

namespace {

// Browsers send a few hundred bytes of headers per part, and the target
// directory is a single path; these only stop a malformed body from making
// us buffer without bound.
constexpr std::size_t kMaxPartHeaderBytes = 64 * 1024;
constexpr std::size_t kMaxFieldBytes = 64 * 1024;

// RFC 2046 5.1.1
constexpr std::size_t kMaxBoundaryLength = 70;

const char* const kCrLf = "\r\n";

Error formError(const std::string& description, const ErrorLocation& location)
{
   return systemError(boost::system::errc::protocol_error,
                      "Invalid form data received - " + description,
                      location);
}

bool isTransportPadding(char ch)
{
   return ch == ' ' || ch == '\t';
}

bool isIdentityTransferEncoding(const std::string& encoding)
{
   return boost::algorithm::iequals(encoding, "7bit") ||
          boost::algorithm::iequals(encoding, "8bit") ||
          boost::algorithm::iequals(encoding, "binary");
}

// RFC 7578 4.2: don't use directory information that may be present
std::string bareFileName(const std::string& fileName)
{
#ifdef _WIN32
   std::size_t pos = fileName.find_last_of("/\\");
#else
   std::size_t pos = fileName.find_last_of('/');
#endif

   if (pos == std::string::npos)
      return fileName;

   return fileName.substr(pos + 1);
}

} // anonymous namespace

Error multipartBoundary(const std::string& contentType, std::string* pBoundary)
{
   http::util::HeaderParams contentTypeParams = http::util::parseHeaderParams(contentType);
   if (contentTypeParams.values.empty() ||
       !boost::algorithm::iequals(contentTypeParams.values.front(), "multipart/form-data"))
   {
      return formError("not multipart/form-data", ERROR_LOCATION);
   }

   auto it = contentTypeParams.params.find("boundary");
   if (it == contentTypeParams.params.end() ||
       it->second.empty() ||
       it->second.size() > kMaxBoundaryLength)
   {
      return formError("missing or invalid boundary", ERROR_LOCATION);
   }

   *pBoundary = it->second;
   return Success();
}

UploadFormParser::UploadFormParser(const std::string& boundary, const FileSink& fileSink)
   : delimiter_(kCrLf + std::string("--") + boundary),
     fileSink_(fileSink),
     state_(State::Preamble),
     partKind_(PartKind::Ignored),
     // the body may open with the first delimiter and no CRLF before it;
     // seeding one lets every delimiter be found the same way
     buffer_(kCrLf),
     hasFile_(false),
     hasTargetDirectory_(false)
{
}

Error UploadFormParser::consume(std::string_view chunk, bool complete)
{
   buffer_.append(chunk.data(), chunk.size());

   bool progress = true;
   while (progress)
   {
      progress = false;
      Error error = step(&progress);
      if (error)
         return error;
   }

   if (complete && state_ != State::Epilogue)
      return formError("final boundary not found", ERROR_LOCATION);

   return Success();
}

Error UploadFormParser::step(bool* pProgress)
{
   switch (state_)
   {
   case State::Preamble:
   {
      std::size_t pos = buffer_.find(delimiter_);
      if (pos == std::string::npos)
      {
         // discard the preamble, keeping what could begin a delimiter
         std::size_t keep = std::min(buffer_.size(), delimiter_.size() - 1);
         buffer_.erase(0, buffer_.size() - keep);
         return Success();
      }

      buffer_.erase(0, pos + delimiter_.size());
      state_ = State::AfterDelimiter;
      *pProgress = true;
      return Success();
   }

   case State::AfterDelimiter:
      return stepAfterDelimiter(pProgress);

   case State::PartHeaders:
      return stepPartHeaders(pProgress);

   case State::PartBody:
      return stepPartBody(pProgress);

   case State::Epilogue:
      buffer_.clear();
      return Success();
   }

   return Success();
}

Error UploadFormParser::stepAfterDelimiter(bool* pProgress)
{
   // "--" straight after the boundary closes the body; anything after it is
   // epilogue, including the CRLF that usually follows
   if (!buffer_.empty() && buffer_[0] == '-')
   {
      if (buffer_.size() < 2)
         return Success();

      if (buffer_[1] != '-')
         return formError("malformed boundary", ERROR_LOCATION);

      buffer_.clear();
      state_ = State::Epilogue;
      *pProgress = true;
      return Success();
   }

   // otherwise optional transport padding, then the CRLF ending the line
   std::size_t pos = 0;
   while (pos < buffer_.size() && isTransportPadding(buffer_[pos]))
      ++pos;

   if (pos + 2 > buffer_.size())
   {
      if (buffer_.size() > kMaxPartHeaderBytes)
         return formError("boundary line too long", ERROR_LOCATION);

      return Success();
   }

   if (buffer_.compare(pos, 2, kCrLf) != 0)
      return formError("malformed boundary", ERROR_LOCATION);

   buffer_.erase(0, pos + 2);
   state_ = State::PartHeaders;
   *pProgress = true;
   return Success();
}

Error UploadFormParser::stepPartHeaders(bool* pProgress)
{
   // the headers end at an empty line, which comes first if there are none
   std::size_t headersSize = 0;
   std::size_t bodyStart = 0;
   if (buffer_.compare(0, 2, kCrLf) == 0)
   {
      bodyStart = 2;
   }
   else
   {
      std::size_t pos = buffer_.find("\r\n\r\n");
      if (pos == std::string::npos)
      {
         if (buffer_.size() > kMaxPartHeaderBytes)
            return formError("part headers too long", ERROR_LOCATION);

         return Success();
      }

      headersSize = pos + 2;
      bodyStart = pos + 4;
   }

   if (headersSize > kMaxPartHeaderBytes)
      return formError("part headers too long", ERROR_LOCATION);

   Error error = beginPart(std::string_view(buffer_).substr(0, headersSize));
   if (error)
      return error;

   buffer_.erase(0, bodyStart);
   state_ = State::PartBody;
   *pProgress = true;
   return Success();
}

Error UploadFormParser::stepPartBody(bool* pProgress)
{
   std::size_t pos = buffer_.find(delimiter_);
   if (pos == std::string::npos)
   {
      // hold back what could be the start of a delimiter split across chunks
      if (buffer_.size() >= delimiter_.size())
      {
         std::size_t size = buffer_.size() - (delimiter_.size() - 1);
         Error error = emitPartData(size);
         if (error)
            return error;

         buffer_.erase(0, size);
      }

      return Success();
   }

   Error error = emitPartData(pos);
   if (error)
      return error;

   buffer_.erase(0, pos + delimiter_.size());
   state_ = State::AfterDelimiter;
   *pProgress = true;
   return Success();
}

Error UploadFormParser::beginPart(std::string_view headerBlock)
{
   // each header line, including the last, ends with CRLF
   http::Headers headers;
   std::size_t lineStart = 0;
   while (lineStart < headerBlock.size())
   {
      std::size_t lineEnd = headerBlock.find(kCrLf, lineStart);
      if (lineEnd == std::string_view::npos)
         lineEnd = headerBlock.size();

      http::Header header;
      std::string line(headerBlock.substr(lineStart, lineEnd - lineStart));
      if (http::parseHeader(line, &header))
         headers.push_back(header);

      lineStart = lineEnd + 2;
   }

   // RFC 7578 4.2: every part has a form-data Content-Disposition with a name
   std::string disposition = http::headerValue(headers, "Content-Disposition");
   http::util::HeaderParams dispositionParams = http::util::parseHeaderParams(disposition);
   if (dispositionParams.values.empty() ||
       !boost::algorithm::iequals(dispositionParams.values.front(), "form-data"))
   {
      return formError("part is not form-data", ERROR_LOCATION);
   }

   auto nameIt = dispositionParams.params.find("name");
   if (nameIt == dispositionParams.params.end())
      return formError("part has no name", ERROR_LOCATION);

   auto fileNameIt = dispositionParams.params.find("filename");
   if (fileNameIt != dispositionParams.params.end())
   {
      // the upload dialog has a single file input
      if (hasFile_)
         return formError("more than one file", ERROR_LOCATION);

      hasFile_ = true;
      fileName_ = bareFileName(fileNameIt->second);
      partKind_ = PartKind::File;
   }
   else if (nameIt->second == "targetDirectory" && !hasTargetDirectory_)
   {
      hasTargetDirectory_ = true;
      partKind_ = PartKind::TargetDirectory;
   }
   else
   {
      partKind_ = PartKind::Ignored;
      return Success();
   }

   // RFC 7578 4.7 deprecates Content-Transfer-Encoding; the identity
   // encodings are harmless, but anything else would need decoding
   std::string transferEncoding = http::headerValue(headers, "Content-Transfer-Encoding");
   if (!transferEncoding.empty() && !isIdentityTransferEncoding(transferEncoding))
      return formError("unsupported Content-Transfer-Encoding " + transferEncoding, ERROR_LOCATION);

   return Success();
}

Error UploadFormParser::emitPartData(std::size_t size)
{
   if (size == 0)
      return Success();

   switch (partKind_)
   {
   case PartKind::File:
      return fileSink_(buffer_.data(), size);

   case PartKind::TargetDirectory:
      if (targetDirectory_.size() + size > kMaxFieldBytes)
         return formError("targetDirectory too long", ERROR_LOCATION);

      targetDirectory_.append(buffer_.data(), size);
      return Success();

   case PartKind::Ignored:
      return Success();
   }

   return Success();
}

} // namespace files
} // namespace modules
} // namespace session
} // namespace rstudio
