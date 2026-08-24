/*
 * HeaderTests.cpp
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

// Coverage for header field parsing (RFC 7230 3.2:
// header-field = field-name ":" OWS field-value OWS).
//
// parseHeader() used to require a literal ": ", so a field written without the
// optional whitespace was silently dropped from the parsed message. Silently is
// the dangerous part: this process and the sender then disagreed about which
// fields the message carried, and for a framing field like Transfer-Encoding
// that disagreement is a smuggling primitive -- see TransferEncodingTests.cpp
// and the chunked-framing checks in AsyncClient/FixedBufferProxy.
//
// This path parses *response* headers (via ResponseParser) and multipart part
// headers; inbound request headers go through RequestParser's own state
// machine, which rejects the whole request rather than dropping a field.

#include <sstream>
#include <string>

#include <core/http/Header.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

TEST(Header, ParsesFieldWithWhitespaceAfterColon)
{
   Header header;
   ASSERT_TRUE(parseHeader("Content-Type: text/plain", &header));
   EXPECT_EQ(header.name, "Content-Type");
   EXPECT_EQ(header.value, "text/plain");
}

TEST(Header, ParsesFieldWithNoWhitespaceAfterColon)
{
   // The regression: OWS is *optional* whitespace, so this is well-formed and
   // must not vanish from the message.
   Header header;
   ASSERT_TRUE(parseHeader("Transfer-Encoding:chunked", &header));
   EXPECT_EQ(header.name, "Transfer-Encoding");
   EXPECT_EQ(header.value, "chunked");
}

TEST(Header, ParsesFieldWithEmptyValue)
{
   // "Name:" is a field with an empty value, not a malformed line.
   Header header;
   ASSERT_TRUE(parseHeader("X-Empty:", &header));
   EXPECT_EQ(header.name, "X-Empty");
   EXPECT_TRUE(header.value.empty());
}

TEST(Header, KeepsColonsAppearingInsideTheValue)
{
   // Only the first colon delimits the field name.
   Header header;
   ASSERT_TRUE(parseHeader("Host: localhost:8787", &header));
   EXPECT_EQ(header.name, "Host");
   EXPECT_EQ(header.value, "localhost:8787");
}

TEST(Header, TrimsSurroundingWhitespaceAndTrailingCarriageReturn)
{
   // Lines arrive from std::getline still carrying the CR of their CRLF, and
   // RFC 7230 allows OWS on both sides of the field value.
   Header header;
   ASSERT_TRUE(parseHeader("Accept:   text/html   \r", &header));
   EXPECT_EQ(header.name, "Accept");
   EXPECT_EQ(header.value, "text/html");
}

TEST(Header, RejectsLineWithoutAColon)
{
   Header header;
   EXPECT_FALSE(parseHeader("this is not a header field", &header));
}

TEST(Header, ParseHeadersKeepsFieldsWrittenWithoutWhitespaceAfterTheColon)
{
   // The end-to-end shape of the bug: a response header block mixing both
   // spellings must yield both fields. Previously Transfer-Encoding was
   // dropped here while the body on the wire remained chunk-framed.
   std::istringstream headerBlock(
      "Content-Type: text/plain\r\n"
      "Transfer-Encoding:chunked\r\n"
      "\r\n");

   Headers headers;
   parseHeaders(headerBlock, &headers);

   EXPECT_EQ(headerValue(headers, "Content-Type"), "text/plain");
   EXPECT_EQ(headerValue(headers, "Transfer-Encoding"), "chunked");
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
