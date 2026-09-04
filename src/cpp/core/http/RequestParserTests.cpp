/*
 * RequestParserTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <cstdlib>
#include <algorithm>
#include <iterator>
#include <vector>

#include <boost/make_shared.hpp>
#include <boost/algorithm/string/replace.hpp>

#include <core/http/RequestParser.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/system/Crypto.hpp>
#include <shared_core/Error.hpp>
#include <core/Result.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

core::Result<std::string> generateRandomBytes()
{
   // generate a large random payload
   uint32_t payloadSize = 1024*1024*2; // 2 MB
   std::vector<unsigned char> fileVector;
   if (core::system::crypto::random(payloadSize, fileVector))
   {
      return tl::unexpected(systemError(boost::system::errc::io_error,
                                        "Failed to generate random bytes",
                                        ERROR_LOCATION));
   }

   std::string fileBytes;
   std::copy(fileVector.begin(), fileVector.end(), std::back_inserter(fileBytes));

   return fileBytes;
}

struct FormTester
{
   FormTester(const std::string& boundary = "boundary") : boundary(boundary)
   {
      parser.setFormHandler(
         [this](const std::string& formData, bool complete) -> bool
         {
            return handle(formData, complete);
         }
      );
   }

   bool handle(const std::string& formData, bool complete)
   {
      buffer += formData;
      if (complete)
      {
         request.setBody(buffer);
         if (buffer != expectedData)
         {
            validationError = systemError(
               boost::system::errc::invalid_argument,
               "Form handler validation failed",
               ERROR_LOCATION
            );
            return false;
         }
         buffer.clear();
      }
      return true;
   }

   std::string multipart(
      const std::string& name,
      const std::string& data,
      const std::string& contentType = std::string(),
      const std::string& filename = std::string(),
      bool quoteName = true
   )
   {
      std::ostringstream ss;
      ss << "\r\n--" << boundary << "\r\n";
      ss << "Content-Disposition: form-data; name=";
      if (quoteName)
        ss << "\"" << name << '"';
      else
        ss << name;
      if (!filename.empty())
         ss << "; filename=\"" << filename << '"';
      ss << "\r\n";
      if (!contentType.empty())
         ss << "Content-Type: " << contentType << "\r\n";
      ss << "\r\n";
      ss << data;
      return ss.str();
   }

   void simpleRequest()
   {
      complexRequest("This is a simple text file", "text/plain");
   }

   void complexRequest(const std::string& data, const std::string& contentType)
   {
      expectedData = multipart("field1", "value1") +
         multipart("field2", data, contentType, "example.txt") +
         "\r\n--" + boundary + "--";

      requestStr = "POST /test HTTP/1.1\r\n"
         "Host: example.com\r\n"
         "Content-Type: multipart/form-data; boundary=" + boundary + "\r\n"
         "Content-Length: " + std::to_string(expectedData.size()) +
         "\r\n\r\n" + expectedData;
   }

   RequestParser::status parse()
   {
      const char* begin = requestStr.c_str();
      const char* end = begin + requestStr.size();
      return parser.parse(request, begin, end);
   }

   RequestParser::status parseBytes(int count)
   {
      if (!parseIter)
      {
         parseIter = requestStr.c_str();
         endIter = parseIter + requestStr.size();
      }

      const char* stepEnd = parseIter + count;
      if (stepEnd > endIter)
         stepEnd = endIter;

      RequestParser::status status = parser.parse(request, parseIter, stepEnd);
      if (status != RequestParser::headers_parsed && status != RequestParser::form_complete)
         parseIter = stepEnd;

      return status;
   }

   bool eof() const
   {
      return !parseIter || parseIter >= endIter;
   }

   std::string boundary;
   std::string expectedData;
   std::string requestStr;
   std::string buffer;
   Error validationError;
   Request request;
   FormHandler handler;
   RequestParser parser;
   const char* parseIter = nullptr;
   const char* endIter = nullptr;
};

TEST(HttpTest, SimpleFormParsingWorks)
{
   FormTester form;
   form.simpleRequest();

   RequestParser::status status = form.parse();
   ASSERT_EQ(RequestParser::headers_parsed, status);

   status = form.parse();
   ASSERT_EQ(RequestParser::form_complete, status);

   EXPECT_FALSE(form.validationError) << form.validationError;

   EXPECT_EQ(form.request.formFieldValue("field1"), "value1");

   File file = form.request.uploadedFile("field2");
   EXPECT_FALSE(file.empty());
   EXPECT_EQ(file.name, "example.txt");
   EXPECT_EQ(file.contentType, "text/plain");
   EXPECT_EQ(file.contents, "This is a simple text file");
}

TEST(HttpTest, SimpleFormParsingWorksOneByteAtATime)
{
   FormTester form;
   form.simpleRequest();

   RequestParser::status status;
   do
   {
      status = form.parseBytes(1);
      if (status == RequestParser::form_complete)
         break;
      ASSERT_TRUE(status == RequestParser::headers_parsed || status == RequestParser::incomplete);
   }
   while (!form.eof());
   ASSERT_EQ(status, RequestParser::form_complete);

   EXPECT_FALSE(form.validationError) << form.validationError;

   EXPECT_EQ(form.request.formFieldValue("field1"), "value1");

   File file = form.request.uploadedFile("field2");
   EXPECT_FALSE(file.empty());
   EXPECT_EQ(file.name, "example.txt");
   EXPECT_EQ(file.contentType, "text/plain");
   EXPECT_EQ(file.contents, "This is a simple text file");
}

TEST(HttpTest, ComplicatedFormParsingWorks)
{
   auto result = generateRandomBytes();
   ASSERT_TRUE(result.has_value()) << result.error().getSummary();
   const std::string& fileBytes = *result;

   FormTester form;
   form.complexRequest(fileBytes, "application/octet-stream");

   RequestParser::status status = form.parse();
   ASSERT_EQ(RequestParser::headers_parsed, status);

   status = form.parse();
   ASSERT_EQ(RequestParser::form_complete, status);

   EXPECT_EQ(form.request.formFieldValue("field1"), "value1");

   File file = form.request.uploadedFile("field2");
   EXPECT_FALSE(file.empty());
   EXPECT_EQ(file.name, "example.txt");
   EXPECT_EQ(file.contentType, "application/octet-stream");
   EXPECT_TRUE(file.contents == fileBytes) << "uploaded file contents mismatch";
}

TEST(HttpTest, ComplicatedFormParsingWorksOneByteAtATime)
{
   auto result = generateRandomBytes();
   ASSERT_TRUE(result.has_value()) << result.error().getSummary();
   const std::string& fileBytes = *result;

   FormTester form;
   form.complexRequest(fileBytes, "application/octet-stream");

   RequestParser::status status;
   do
   {
      status = form.parseBytes(1);
      if (status == RequestParser::form_complete)
         break;
      ASSERT_TRUE(status == RequestParser::headers_parsed || status == RequestParser::incomplete);
   }
   while (!form.eof());
   ASSERT_EQ(status, RequestParser::form_complete);

   EXPECT_EQ(form.request.formFieldValue("field1"), "value1");

   File file = form.request.uploadedFile("field2");
   EXPECT_FALSE(file.empty());
   EXPECT_EQ(file.name, "example.txt");
   EXPECT_EQ(file.contentType, "application/octet-stream");
   EXPECT_TRUE(file.contents == fileBytes) << "uploaded file contents mismatch";
}

TEST(HttpTest, ComplicatedFormParsingWorksRandomByteBoundaries)
{
   auto result = generateRandomBytes();
   ASSERT_TRUE(result.has_value()) << result.error().getSummary();
   const std::string& fileBytes = *result;
   FormTester form;
   form.complexRequest(fileBytes, "application/octet-stream");

   RequestParser::status status;
   size_t blockSize = 0;
   do
   {
      blockSize = rand() % 8192 + 1;
      status = form.parseBytes(blockSize);
      if (status == RequestParser::form_complete)
         break;
      ASSERT_TRUE(status == RequestParser::headers_parsed || status == RequestParser::incomplete);
   }
   while (!form.eof());
   ASSERT_EQ(status, RequestParser::form_complete);

   EXPECT_EQ(form.request.formFieldValue("field1"), "value1");

   File file = form.request.uploadedFile("field2");
   EXPECT_FALSE(file.empty());
   EXPECT_EQ(file.name, "example.txt");
   EXPECT_EQ(file.contentType, "application/octet-stream");
   EXPECT_TRUE(file.contents == fileBytes) << "uploaded file contents mismatch";
}

TEST(HttpTest, FormParsingRejectsMalformedMultipart)
{
   FormTester form("");
   form.simpleRequest();

   RequestParser::status status = form.parse();
   ASSERT_EQ(RequestParser::headers_parsed, status);

   status = form.parse();
   ASSERT_EQ(RequestParser::form_complete, status);

   EXPECT_EQ(form.request.formFields().size(), 0);

   File file = form.request.uploadedFile("field2");
   EXPECT_TRUE(file.empty());
}

TEST(HttpTest, FormParsingToleratesMissingPreamble)
{
   FormTester form;
   form.simpleRequest();
   boost::algorithm::replace_first(form.requestStr, "\r\n\r\n\r\n", "\r\n\r\n");
   boost::algorithm::replace_first(form.requestStr,
      "Content-Length: " + std::to_string(form.expectedData.size()),
      "Content-Length: " + std::to_string(form.expectedData.size() - 2)
   );
   form.expectedData.erase(form.expectedData.begin(), form.expectedData.begin() + 2);

   RequestParser::status status = form.parse();
   ASSERT_EQ(RequestParser::headers_parsed, status);

   status = form.parse();
   EXPECT_EQ(RequestParser::form_complete, status);

   EXPECT_FALSE(form.validationError) << form.validationError;

   EXPECT_EQ(form.request.formFieldValue("field1"), "value1");

   File file = form.request.uploadedFile("field2");
   EXPECT_FALSE(file.empty());
   EXPECT_EQ(file.name, "example.txt");
   EXPECT_EQ(file.contentType, "text/plain");
   EXPECT_EQ(file.contents, "This is a simple text file");
}

TEST(HttpTest, FormParsingIgnoresEpilogue)
{
   FormTester form;
   form.simpleRequest();
   std::string extra = form.multipart("field3", "value3");
   extra = extra.substr(extra.find("Content-Disposition"));
   boost::algorithm::replace_first(form.requestStr,
      "Content-Length: " + std::to_string(form.expectedData.size()),
      "Content-Length: " + std::to_string(form.expectedData.size() + extra.size())
   );
   form.requestStr += extra;
   form.expectedData += extra;

   RequestParser::status status = form.parse();
   ASSERT_EQ(RequestParser::headers_parsed, status);

   status = form.parse();
   EXPECT_EQ(RequestParser::form_complete, status);

   EXPECT_FALSE(form.validationError) << form.validationError;

   EXPECT_EQ(form.request.formFieldValue("field1"), "value1");
   EXPECT_EQ(form.request.formFieldValue("field3"), std::string());
}

TEST(HttpTest, FormParsingEdgeCases)
{
   FormTester form;
   form.expectedData = form.multipart("field1", "value1") +
      form.multipart("field2", "test", "text/plain", "semicolon;and\\\"quote.txt") +
      "\r\n--boundary--";

   form.requestStr = "POST /test HTTP/1.1\r\n"
      "Host: example.com\r\n"
      "Content-Type: multipart/form-data; Boundary=\"boundary\" \r\n"
      "Content-Length: " + std::to_string(form.expectedData.size()) +
      "\r\n\r\n" + form.expectedData;

   RequestParser::status status = form.parse();
   ASSERT_EQ(RequestParser::headers_parsed, status);

   status = form.parse();
   EXPECT_EQ(RequestParser::form_complete, status);

   EXPECT_FALSE(form.validationError) << form.validationError;

   File file = form.request.uploadedFile("field2");
   EXPECT_EQ(file.name, "semicolon;and\"quote.txt");
}

TEST(HttpTest, FormParsingEdgeCases2)
{
   FormTester form;
   form.expectedData = form.multipart("field1", "value1") +
      "\r\n--boundary --" +
      form.multipart("fie\"ld2", "test", "text/plain", "filename.txt") +
      "\r\n--boundary--";

   form.requestStr = "POST /test HTTP/1.1\r\n"
      "Host: example.com\r\n"
      "Content-Type: multipart/form-data; Boundary=\"boundary\" \r\n"
      "Content-Length: " + std::to_string(form.expectedData.size()) +
      "\r\n\r\n" + form.expectedData;

   RequestParser::status status = form.parse();
   ASSERT_EQ(RequestParser::headers_parsed, status);

   status = form.parse();
   EXPECT_EQ(RequestParser::form_complete, status);

   EXPECT_FALSE(form.validationError) << form.validationError;

   EXPECT_EQ(form.request.formFieldValue("field1"), "value1\r\n--boundary --");

   File file = form.request.uploadedFile("fie\"ld2");
   EXPECT_EQ(file.name, "filename.txt");
   EXPECT_TRUE(file.contents == "test") << "uploaded file contents mismatch";
}

TEST(HttpTest, FormParsingBoundaryTrailingSpaces)
{
   FormTester form("boundary \t ");
   form.expectedData = form.multipart("field1", "value1") +
      form.multipart("field2", "test", "text/plain", "semi\\\\colon;and\\\"quote\\.txt") +
      "\r\n--boundary--";

   form.requestStr = "POST /test HTTP/1.1\r\n"
      "Host: example.com\r\n"
      "Content-Type: multipart/form-data; Boundary=\"boundary\" \r\n"
      "Content-Length: " + std::to_string(form.expectedData.size()) +
      "\r\n\r\n" + form.expectedData;

   RequestParser::status status = form.parse();
   ASSERT_EQ(RequestParser::headers_parsed, status);

   status = form.parse();
   EXPECT_EQ(RequestParser::form_complete, status);

   EXPECT_FALSE(form.validationError) << form.validationError;

   File file = form.request.uploadedFile("field2");
   EXPECT_EQ(file.name, "semi\\colon;and\"quote.txt");
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
