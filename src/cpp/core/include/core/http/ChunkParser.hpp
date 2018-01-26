/*
 * ChunkParser.hpp
 *
 * Copyright (C) 2017 by RStudio, Inc.
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

#ifndef CORE_HTTP_CHUNK_PARSER_HPP
#define CORE_HTTP_CHUNK_PARSER_HPP

#include <stdint.h>
#include <string>
#include <vector>

#include <boost/noncopyable.hpp>

namespace rstudio {
namespace core {
namespace http {

/// Parser for incoming chunks (TransferEncoding: chunked).
class ChunkParser : boost::noncopyable
{


public:
   /// Construct ready to parse chunks.
   ChunkParser() : state_(HeaderSize), chunkSize_(0) {}

   /// Parses the next buffer and outputs any processed complete chunks.
   /// Returns true if no more chunks will be received.
   bool parse(const char* buffer, size_t len, std::vector<std::string>* pOutChunks);

private:
   // state of the parser
   enum State
   {
      Invalid = -1,
      HeaderSize = 1,
      HeaderCRLF = 2,
      Chunk = 3,
      ChunkCRLF = 4,
      Complete = 5
   } state_;

   // size of the chunk to read
   size_t chunkSize_;

   // buffer for incoming chunk
   std::string chunkHeader_;
   std::string chunk_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_REQUEST_PARSER_HPP
