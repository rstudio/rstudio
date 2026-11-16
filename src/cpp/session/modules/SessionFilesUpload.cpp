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
#include <map>
#include <vector>

#include <boost/algorithm/string/predicate.hpp>

#include <core/http/Header.hpp>
#include <core/http/Util.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace files {

namespace {

// Part headers and the target directory are small; this only stops a
// malformed body from making us buffer it without bound.
constexpr std::size_t kMaxBufferedBytes = 64 * 1024;

Error formError(const std::string& description, const ErrorLocation& location)
{
   return systemError(boost::system::errc::protocol_error,
                      "Invalid form data received - " + description,
                      location);
}

} // anonymous namespace

Error multipartBoundary(const std::string& contentType, std::string* pBoundary)
{
   std::vector<std::string> values;
   std::map<std::string, std::string> params;
   http::util::parseHeaderParams(contentType, &values, &params);

   *pBoundary = params["boundary"];
   if (pBoundary->empty())
      return formError("no boundary", ERROR_LOCATION);

   return Success();
}

UploadFormParser::UploadFormParser(const std::string& boundary, const FileSink& fileSink)
   : delimiter_("\r\n--" + boundary),
     fileSink_(fileSink),
     // the preamble is skipped like the body of an ignored part; the CRLF
     // lets the first delimiter be found when the body opens with it
     state_(State::PartBody),
     partKind_(PartKind::Ignored),
     buffer_("\r\n"),
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
   case State::PartBody:
   {
      // pass on everything before the next delimiter, holding back what
      // could be the start of one split across chunks
      std::size_t pos = buffer_.find(delimiter_);
      std::size_t size = pos;
      if (pos == std::string::npos)
         size = buffer_.size() - std::min(buffer_.size(), delimiter_.size() - 1);

      Error error = emitPartData(size);
      if (error)
         return error;

      if (pos == std::string::npos)
      {
         buffer_.erase(0, size);
         return Success();
      }

      buffer_.erase(0, pos + delimiter_.size());
      state_ = State::AfterDelimiter;
      break;
   }

   case State::AfterDelimiter:
   {
      // "--" closes the body; otherwise the delimiter line ends with
      // optional transport padding and a CRLF
      if (buffer_.compare(0, 2, "--") == 0)
      {
         state_ = State::Epilogue;
         break;
      }

      std::size_t pos = buffer_.find_first_not_of(" \t");
      if (pos == std::string::npos || pos + 2 > buffer_.size())
      {
         if (buffer_.size() > kMaxBufferedBytes)
            return formError("malformed boundary", ERROR_LOCATION);

         return Success();
      }

      if (buffer_.compare(pos, 2, "\r\n") != 0)
         return formError("malformed boundary", ERROR_LOCATION);

      // keep the CRLF, so that even an empty header block ends at "\r\n\r\n"
      buffer_.erase(0, pos);
      state_ = State::PartHeaders;
      break;
   }

   case State::PartHeaders:
   {
      std::size_t pos = buffer_.find("\r\n\r\n");
      if (pos == std::string::npos)
      {
         if (buffer_.size() > kMaxBufferedBytes)
            return formError("part headers too long", ERROR_LOCATION);

         return Success();
      }

      Error error = beginPart(std::string_view(buffer_).substr(2, pos));
      if (error)
         return error;

      buffer_.erase(0, pos + 4);
      state_ = State::PartBody;
      break;
   }

   case State::Epilogue:
      buffer_.clear();
      return Success();
   }

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
      std::size_t lineEnd = std::min(headerBlock.find("\r\n", lineStart), headerBlock.size());

      http::Header header;
      if (http::parseHeader(std::string(headerBlock.substr(lineStart, lineEnd - lineStart)), &header))
         headers.push_back(header);

      lineStart = lineEnd + 2;
   }

   // RFC 7578 4.2: every part has a form-data Content-Disposition with a name
   std::vector<std::string> values;
   std::map<std::string, std::string> params;
   http::util::parseHeaderParams(http::headerValue(headers, "Content-Disposition"), &values, &params);
   if (values.empty() || !boost::algorithm::iequals(values.front(), "form-data") || !params.count("name"))
      return formError("part is not named form-data", ERROR_LOCATION);

   // the first file and the first targetDirectory win
   partKind_ = PartKind::Ignored;
   if (params.count("filename"))
   {
      if (!hasFile_)
      {
         hasFile_ = true;
         fileName_ = params["filename"];
         partKind_ = PartKind::File;
      }
   }
   else if (params["name"] == "targetDirectory" && !hasTargetDirectory_)
   {
      hasTargetDirectory_ = true;
      partKind_ = PartKind::TargetDirectory;
   }

   return Success();
}

Error UploadFormParser::emitPartData(std::size_t size)
{
   if (size == 0 || partKind_ == PartKind::Ignored)
      return Success();

   if (partKind_ == PartKind::File)
      return fileSink_(buffer_.data(), size);

   if (targetDirectory_.size() + size > kMaxBufferedBytes)
      return formError("targetDirectory too long", ERROR_LOCATION);

   targetDirectory_.append(buffer_.data(), size);
   return Success();
}

} // namespace files
} // namespace modules
} // namespace session
} // namespace rstudio
