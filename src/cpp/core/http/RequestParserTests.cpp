/*
 * RequestParserTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <cstdlib>

#include <boost/make_shared.hpp>

#include <core/http/RequestParser.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/system/Crypto.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

std::string simpleRequest(std::string* pBodyStr)
{
   std::string bodyStr =
         "--boundary\r\n"
         "Content-Disposition: form-data; name=\"field1\"\r\n\r\n"
         "value1\r\n"
         "--boundary\r\n"
         "Content-Disposition: form-data; name=\"field2\"; filename=\"example.txt\"\r\n"
         "Content-Type: text/plain\r\n\r\n"
         "This is a simple text file\r\n"
         "--boundary--\r\n";

   *pBodyStr = bodyStr;

   std::string bodySizeStr = safe_convert::numberToString(bodyStr.size());

   std::string requestStr =
         "POST /test HTTP/1.1\r\n"
         "Host: foo.example\r\n"
         "Content-Type: multipart/form-data; boundary=boundary\r\n"
         "Content-Length: " + bodySizeStr + "\r\n\r\n" + bodyStr;

   return requestStr;
}

std::string generateRandomBytes()
{
   // generate a large random payload
   uint32_t payloadSize = 1024*1024*2; // 2 MB
   std::vector<unsigned char> fileVector;
   REQUIRE_FALSE(core::system::crypto::random(payloadSize, fileVector));

   std::string fileBytes;
   std::copy(fileVector.begin(), fileVector.end(), std::back_inserter(fileBytes));

   return fileBytes;
}

std::string complexRequest(const std::string& fileBytes,
                           std::string* pBodyStr)
{
   std::string bodyStr =
         "--boundary\r\n"
         "Content-Disposition: form-data; name=\"field1\"\r\n\r\n"
         "value1\r\n"
         "--boundary\r\n"
         "Content-Disposition: form-data; name=\"field2\"; filename=\"example.txt\"\r\n"
         "Content-Type: application/octet-stream\r\n\r\n" +
         fileBytes + "\r\n"
         "--boundary--\r\n";

   *pBodyStr = bodyStr;

   std::string bodySizeStr = safe_convert::numberToString(bodyStr.size());

   std::string requestStr =
         "POST /test HTTP/1.1\r\n"
         "Host: foo.example\r\n"
         "Content-Type: multipart/form-data; boundary=boundary\r\n"
         "Content-Length: " + bodySizeStr + "\r\n\r\n" + bodyStr;

   return requestStr;
}

FormHandler formHandler(const std::string& expectedData)
{
   boost::shared_ptr<std::string> data = boost::make_shared<std::string>();

   auto formHandler = [=](const std::string& formData, bool complete) -> bool
   {
      (*data) += formData;

      if (complete)
      {
         REQUIRE(*data == expectedData);
      }
      return true;
   };

   return formHandler;
}

test_context("RequestParserTests")
{
   test_that("Simple form parsing works")
   {
      std::string bodyStr;
      std::string requestStr = simpleRequest(&bodyStr);
      Request request;

      FormHandler handler = formHandler(bodyStr);

      RequestParser parser;
      parser.setFormHandler(handler);

      RequestParser::status status = parser.parse(request, requestStr.c_str(), requestStr.c_str() + requestStr.size());
      REQUIRE(status == RequestParser::headers_parsed);

      status = parser.parse(request, requestStr.c_str(), requestStr.c_str() + requestStr.size());
      REQUIRE(status == RequestParser::form_complete);
   }

   test_that("Simple form parsing works, one byte at a time")
   {
      std::string bodyStr;
      std::string requestStr = simpleRequest(&bodyStr);
      Request request;

      FormHandler handler = formHandler(bodyStr);

      RequestParser parser;
      parser.setFormHandler(handler);

      RequestParser::status status;
      for (size_t i = 0; i < requestStr.size() - 1; ++i)
      {
         status = parser.parse(request, requestStr.c_str() + i, requestStr.c_str() + i + 1);
         REQUIRE((status == RequestParser::headers_parsed || status == RequestParser::incomplete));

         if (status == RequestParser::headers_parsed)
         {
            // need to pass the same buffer to resume
            i--;
         }
      }

      status = parser.parse(request, requestStr.c_str() + requestStr.size() - 1, requestStr.c_str() + requestStr.size());
      REQUIRE(status == RequestParser::form_complete);
   }

   test_that("Long, complicated form parsing works")
   {
      std::string fileBytes = generateRandomBytes();

      std::string bodyStr;
      std::string requestStr = complexRequest(fileBytes, &bodyStr);
      Request request;

      FormHandler handler = formHandler(bodyStr);

      RequestParser parser;
      parser.setFormHandler(handler);

      RequestParser::status status = parser.parse(request, requestStr.c_str(), requestStr.c_str() + requestStr.size());
      REQUIRE(status == RequestParser::headers_parsed);

      status = parser.parse(request, requestStr.c_str(), requestStr.c_str() + requestStr.size());
      REQUIRE(status == RequestParser::form_complete);
   }

   test_that("Long, complicated form parsing works, one byte at a time")
   {
      std::string fileBytes = generateRandomBytes();

      std::string bodyStr;
      std::string requestStr = complexRequest(fileBytes, &bodyStr);
      Request request;

      FormHandler handler = formHandler(bodyStr);

      RequestParser parser;
      parser.setFormHandler(handler);

      RequestParser::status status;
      for (size_t i = 0; i < requestStr.size() - 1; ++i)
      {
         status = parser.parse(request, requestStr.c_str() + i, requestStr.c_str() + i + 1);
         REQUIRE((status == RequestParser::headers_parsed || status == RequestParser::incomplete));

         if (status == RequestParser::headers_parsed)
         {
            // need to pass the same buffer to resume
            i--;
         }
      }

      status = parser.parse(request, requestStr.c_str() + requestStr.size() - 1, requestStr.c_str() + requestStr.size());
      REQUIRE(status == RequestParser::form_complete);
   }

   test_that("Long, complicated form parsing works, random byte boundaries")
   {
      std::string fileBytes = generateRandomBytes();

      std::string bodyStr;
      std::string requestStr = complexRequest(fileBytes, &bodyStr);
      Request request;

      FormHandler handler = formHandler(bodyStr);

      RequestParser parser;
      parser.setFormHandler(handler);

      RequestParser::status status;

      for (size_t i = 0; i < requestStr.size();)
      {
         size_t byteAmount = rand() % 8192 + 1;
         if (byteAmount > requestStr.size() - i)
            byteAmount = requestStr.size() - i;

         status = parser.parse(request, requestStr.c_str() + i, requestStr.c_str() + i + byteAmount);
         REQUIRE((status == RequestParser::headers_parsed || status == RequestParser::incomplete || status == RequestParser::form_complete));

         if (status == RequestParser::headers_parsed)
         {
            // need to pass the same buffer to resume
            status = parser.parse(request, requestStr.c_str() + i, requestStr.c_str() + i + byteAmount);
            REQUIRE(status == RequestParser::incomplete);
         }
         else if (status == RequestParser::form_complete)
         {
            break;
         }

         i += byteAmount;
      }
   }
}

} // end namespace tests
} // end namespace http
} // end namespace core
} // end namespace rstudio

