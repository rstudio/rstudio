/*
 * HttpChunkFormatTests.cpp
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

// Coverage for util::formatMessageAsHttpChunk() and its arithmetic companion
// util::httpChunkSize().
//
// The two exist as a pair so FixedBufferProxy can decide whether an enveloped
// body piece fits in its write buffer without paying to build the envelope for
// pieces it then declines and has redelivered. That only works if the
// prediction is exact: FixedBufferProxy charges httpChunkSize() against
// currentBufferSize_ and later credits back the formatted string's real
// size(), so any disagreement makes the accounting drift -- upward until
// backpressure never releases and the download stalls, or downward until the
// buffer grows past its configured bound.

#include <cstddef>
#include <string>

#include <core/http/Util.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

TEST(HttpChunkFormat, EnvelopeShape)
{
   // <chunk size (hex)>CRLF<chunk data>CRLF, size in lowercase hex
   EXPECT_EQ(util::formatMessageAsHttpChunk("hello"), "5\r\nhello\r\n");
   EXPECT_EQ(util::formatMessageAsHttpChunk(std::string(255, 'x')),
             "ff\r\n" + std::string(255, 'x') + "\r\n");
}

TEST(HttpChunkFormat, EmptyMessageIsTheTerminator)
{
   // the zero-length chunk is chunked framing's end-of-body marker
   EXPECT_EQ(util::formatMessageAsHttpChunk(""), "0\r\n\r\n");
   EXPECT_EQ(util::httpChunkSize(0), 5u);
}

TEST(HttpChunkFormat, MessageWithEmbeddedNulsIsNotTruncated)
{
   // the envelope is length-delimited, not NUL-terminated: body bytes are
   // arbitrary, and a snprintf'd size line must not tempt anyone into
   // treating the payload as a C string
   std::string message("a\0b", 3);
   EXPECT_EQ(util::formatMessageAsHttpChunk(message), std::string("3\r\na\0b\r\n", 8));
}

TEST(HttpChunkFormat, PredictedSizeMatchesFormattedSize)
{
   // The invariant FixedBufferProxy's buffer accounting rests on. Walks every
   // size up to a few hex-digit rollovers, plus each boundary itself, since a
   // miscounted digit is exactly how the prediction would go wrong.
   for (std::size_t size = 0; size <= 300; ++size)
   {
      EXPECT_EQ(util::httpChunkSize(size),
                util::formatMessageAsHttpChunk(std::string(size, 'y')).size())
         << "size " << size;
   }

   const std::size_t boundaries[] = { 15, 16, 255, 256, 4095, 4096, 65535, 65536 };
   for (std::size_t size : boundaries)
   {
      EXPECT_EQ(util::httpChunkSize(size),
                util::formatMessageAsHttpChunk(std::string(size, 'y')).size())
         << "size " << size;
   }
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
