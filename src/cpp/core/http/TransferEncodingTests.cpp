/*
 * TransferEncodingTests.cpp
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

// Coverage for util::parseTransferEncoding(), the single answer to "is this
// body chunk-framed" shared by AsyncClient (which de-chunks), FixedBufferProxy
// (which re-frames toward the client) and the buffered localhost proxy path.
//
// Those three used to compare the raw header field to the string "chunked"
// independently. Any input where the comparisons disagreed with each other --
// or agreed but were both wrong about the bytes on the wire -- produced a body
// that got chunked twice, which RFC 7230 3.3.1 forbids outright ("A sender
// MUST NOT apply chunked more than once to a message body").

#include <string>

#include <core/http/Header.hpp>
#include <core/http/Message.hpp>
#include <core/http/Util.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

Headers transferEncoding(const std::string& value)
{
   Headers headers;
   headers.push_back(Header("Content-Type", "text/plain"));
   headers.push_back(Header(kTransferEncoding, value));
   return headers;
}

} // anonymous namespace

TEST(TransferEncoding, AbsentFieldIsNotChunked)
{
   Headers headers;
   headers.push_back(Header("Content-Length", "5"));

   util::TransferEncoding te = util::parseTransferEncoding(headers);
   EXPECT_FALSE(te.present);
   EXPECT_FALSE(te.chunkedIsFinal);
   EXPECT_FALSE(te.hasOtherCodings);
}

TEST(TransferEncoding, PlainChunkedIsChunked)
{
   util::TransferEncoding te = util::parseTransferEncoding(transferEncoding("chunked"));
   EXPECT_TRUE(te.present);
   EXPECT_TRUE(te.chunkedIsFinal);
   EXPECT_FALSE(te.hasOtherCodings);
}

TEST(TransferEncoding, CodingNamesAreCaseInsensitive)
{
   // RFC 7230 4: transfer-coding names are tokens, matched case-insensitively.
   // A string compare against "chunked" said "not chunked" here, so the body
   // was read as unencoded while chunk-framed on the wire.
   util::TransferEncoding te = util::parseTransferEncoding(transferEncoding("Chunked"));
   EXPECT_TRUE(te.chunkedIsFinal);
   EXPECT_FALSE(te.hasOtherCodings);
}

TEST(TransferEncoding, TolerantOfSurroundingWhitespace)
{
   util::TransferEncoding te = util::parseTransferEncoding(transferEncoding("  chunked  "));
   EXPECT_TRUE(te.chunkedIsFinal);
}

TEST(TransferEncoding, ChunkedAppliedLastAfterAnotherCodingIsStillChunkFramed)
{
   // "gzip, chunked" means gzip was applied first and chunked last, so the
   // wire body *is* chunk-framed -- but de-chunking it leaves gzip-encoded
   // bytes, not the payload. Both facts have to be reported.
   util::TransferEncoding te = util::parseTransferEncoding(transferEncoding("gzip, chunked"));
   EXPECT_TRUE(te.present);
   EXPECT_TRUE(te.chunkedIsFinal);
   EXPECT_TRUE(te.hasOtherCodings);
}

TEST(TransferEncoding, ChunkedNotAppliedLastIsNotChunkFramed)
{
   // The reverse order: the outermost coding is gzip, so the bytes on the wire
   // are not chunk-framed at all and nothing here can decode them.
   util::TransferEncoding te = util::parseTransferEncoding(transferEncoding("chunked, gzip"));
   EXPECT_TRUE(te.present);
   EXPECT_FALSE(te.chunkedIsFinal);
   EXPECT_TRUE(te.hasOtherCodings);
}

TEST(TransferEncoding, SplitAcrossRepeatedHeaderFieldsIsOneOrderedList)
{
   // 1#transfer-coding may be split across repeated fields; only the final
   // coding of the combined list decides whether the body is chunk-framed.
   // headerValue() would have seen only the first field.
   Headers headers;
   headers.push_back(Header(kTransferEncoding, "gzip"));
   headers.push_back(Header(kTransferEncoding, "chunked"));

   util::TransferEncoding te = util::parseTransferEncoding(headers);
   EXPECT_TRUE(te.chunkedIsFinal);
   EXPECT_TRUE(te.hasOtherCodings);
}

TEST(TransferEncoding, IgnoresCodingParameters)
{
   util::TransferEncoding te =
      util::parseTransferEncoding(transferEncoding("chunked;q=1.0"));
   EXPECT_TRUE(te.chunkedIsFinal);
   EXPECT_FALSE(te.hasOtherCodings);
}

TEST(TransferEncoding, IdentityIsTreatedAsNoCodingAtAll)
{
   // "identity" was dropped as a transfer coding in RFC 7230 4 and means
   // nothing was applied, so it must not be reported as a coding we would
   // have to refuse for being undecodable.
   util::TransferEncoding te = util::parseTransferEncoding(transferEncoding("identity"));
   EXPECT_FALSE(te.present);
   EXPECT_FALSE(te.hasOtherCodings);

   util::TransferEncoding withChunked =
      util::parseTransferEncoding(transferEncoding("identity, chunked"));
   EXPECT_TRUE(withChunked.chunkedIsFinal);
   EXPECT_FALSE(withChunked.hasOtherCodings);
}

TEST(TransferEncoding, EmptyFieldValueIsNotAcoding)
{
   util::TransferEncoding te = util::parseTransferEncoding(transferEncoding(""));
   EXPECT_FALSE(te.present);
   EXPECT_FALSE(te.chunkedIsFinal);
}

TEST(TransferEncoding, UnknownCodingIsReportedAsOther)
{
   util::TransferEncoding te = util::parseTransferEncoding(transferEncoding("br"));
   EXPECT_TRUE(te.present);
   EXPECT_FALSE(te.chunkedIsFinal);
   EXPECT_TRUE(te.hasOtherCodings);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
