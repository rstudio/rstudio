/*
 * MultipartFormTests.cpp
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

#include <string>
#include <utility>
#include <vector>

#include <shared_core/Error.hpp>

#include <core/http/Header.hpp>
#include <core/http/Util.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

typedef std::vector<std::pair<std::string, std::string>> Parts;

struct Parsed
{
   Error error;

   // each part's Content-Disposition and data
   Parts parts;
};

// parse `body` in chunks that end at each offset in `splits`
Parsed parseChunks(const std::string& boundary,
                   const std::string& body,
                   const std::vector<std::size_t>& splits)
{
   Parsed parsed;
   util::MultipartFormParser parser(boundary, [&](const Headers& headers, util::MultipartFormParser::DataSink* pSink)
   {
      parsed.parts.emplace_back(headerValue(headers, "Content-Disposition"), std::string());
      *pSink = [&](const char* data, std::size_t size)
      {
         parsed.parts.back().second.append(data, size);
         return Success();
      };

      return Success();
   });

   std::size_t start = 0;
   for (std::size_t i = 0; i <= splits.size(); i++)
   {
      std::size_t end = (i < splits.size()) ? splits[i] : body.size();
      bool complete = (i == splits.size());
      parsed.error = parser.consume(std::string_view(body).substr(start, end - start), complete);
      if (parsed.error)
         break;

      start = end;
   }

   return parsed;
}

Parsed parse(const std::string& boundary, const std::string& body)
{
   return parseChunks(boundary, body, {});
}

// Parse `body` split into two chunks at every offset, then one byte at a
// time, and describe the first result that differs from parsing it whole.
std::string firstSplitMismatch(const std::string& boundary, const std::string& body)
{
   Parsed whole = parse(boundary, body);
   auto differs = [&](const Parsed& parsed)
   {
      return static_cast<bool>(parsed.error) != static_cast<bool>(whole.error) || parsed.parts != whole.parts;
   };

   for (std::size_t split = 0; split <= body.size(); split++)
   {
      if (differs(parseChunks(boundary, body, { split })))
         return "split at offset " + std::to_string(split);
   }

   std::vector<std::size_t> everyByte;
   for (std::size_t offset = 1; offset < body.size(); offset++)
      everyByte.push_back(offset);

   if (differs(parseChunks(boundary, body, everyByte)))
      return "one byte at a time";

   return std::string();
}

} // anonymous namespace

TEST(MultipartFormTest, FindsTheBoundaryInTheContentType)
{
   std::vector<std::pair<std::string, std::string>> cases = {
      { "multipart/form-data; boundary=abc", "abc" },
      { "multipart/form-data; boundary=\"abc def\"", "abc def" },
      { "multipart/form-data; boundary=abc; charset=utf-8", "abc" },
      { "Multipart/Form-Data; charset=utf-8; BOUNDARY=abc", "abc" },
      { "multipart/form-data; boundary=" + std::string(70, 'b'), std::string(70, 'b') }
   };

   for (const auto& testCase : cases)
   {
      std::string boundary;
      ASSERT_FALSE(util::multipartFormBoundary(testCase.first, &boundary)) << testCase.first;
      EXPECT_EQ(testCase.second, boundary) << testCase.first;
   }

   std::vector<std::string> invalid = {
      "multipart/form-data",
      "multipart/form-data; boundary=",
      "multipart/form-data; boundary=\"\"",
      "text/plain; boundary=abc"
   };

   for (const std::string& contentType : invalid)
   {
      std::string boundary;
      EXPECT_TRUE(util::multipartFormBoundary(contentType, &boundary)) << contentType;
   }
}

TEST(MultipartFormTest, ReadsTheFormDataDisposition)
{
   Headers headers;
   util::FormDataDisposition disposition;

   headers = { Header("Content-Disposition", "form-data; name=\"field\"") };
   ASSERT_TRUE(util::parseFormDataDisposition(headers, true, &disposition));
   EXPECT_EQ("field", disposition.name);
   EXPECT_FALSE(disposition.isFile);

   // an empty file name is still a file part: no file was chosen
   headers = { Header("Content-Disposition", "Form-Data; FileName=\"\"; Name=upload") };
   ASSERT_TRUE(util::parseFormDataDisposition(headers, true, &disposition));
   EXPECT_EQ("upload", disposition.name);
   EXPECT_TRUE(disposition.isFile);
   EXPECT_EQ("", disposition.fileName);

   headers = { Header("Content-Disposition", "form-data; name=\"upload\"; filename=\"a\\\\b\\\".txt\"") };
   ASSERT_TRUE(util::parseFormDataDisposition(headers, true, &disposition));
   EXPECT_EQ("a\\b\".txt", disposition.fileName);
   ASSERT_TRUE(util::parseFormDataDisposition(headers, false, &disposition));
   EXPECT_EQ("a\\\\b\\\".txt", disposition.fileName);

   std::vector<Headers> invalid = {
      {},
      { Header("Content-Type", "text/plain") },
      { Header("Content-Disposition", "attachment; name=\"field\"") },
      { Header("Content-Disposition", "form-data; filename=\"a.txt\"") }
   };

   for (const Headers& invalidHeaders : invalid)
      EXPECT_FALSE(util::parseFormDataDisposition(invalidHeaders, true, &disposition));
}

TEST(MultipartFormTest, ReadsEachPartWhereverTheChunksSplit)
{
   // The first part holds a nested multipart whose boundary has ours as a
   // prefix, and lines that start like a delimiter but go on with something
   // other than "--" or padding and a CRLF; all of that is part data. The
   // second part has no headers, and the epilogue looks like a third.
   std::string first =
         "one\r\n--outerinner\r\nnested\r\n--outer x\r\n--outer-x\r\n--outer \t\rx\r\n--outerinner--";

   std::string body =
         "preamble\r\n"
         "--outer \t\r\n"
         "Content-Disposition: form-data; name=\"a\"\r\n"
         "\r\n" +
         first +
         "\r\n--outer\r\n"
         "\r\n"
         "two"
         "\r\n--outer--"
         "\r\nepilogue\r\n--outer\r\n"
         "Content-Disposition: form-data; name=\"late\"\r\n"
         "\r\n"
         "late";

   Parsed parsed = parse("outer", body);
   ASSERT_FALSE(parsed.error);

   Parts expected = { { "form-data; name=\"a\"", first }, { "", "two" } };
   EXPECT_EQ(expected, parsed.parts);

   EXPECT_EQ("", firstSplitMismatch("outer", body));
}

TEST(MultipartFormTest, PassesOnTheRestOfAnUnclosedBody)
{
   // what looks like the start of the close delimiter can't be one once the
   // body has ended
   std::string body = "--b\r\nContent-Disposition: form-data; name=\"a\"\r\n\r\nvalue\r\n--b ";

   Parsed parsed = parse("b", body);
   EXPECT_TRUE(parsed.error);

   Parts expected = { { "form-data; name=\"a\"", "value\r\n--b " } };
   EXPECT_EQ(expected, parsed.parts);

   EXPECT_EQ("", firstSplitMismatch("b", body));

   // which a Request's form fields keep, trimmed like any other field
   Fields fields;
   Files files;
   util::parseMultipartForm("multipart/form-data; boundary=b", body, &fields, &files);
   EXPECT_EQ(Fields({ { "a", "value\r\n--b" } }), fields);
   EXPECT_TRUE(files.empty());
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
