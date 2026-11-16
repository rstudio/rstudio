/*
 * UriHandlerTests.cpp
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

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/UriHandler.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

TEST(UriHandlerTest, UploadHandlerVisitedWithAFullRequestGetsItsWholeBody)
{
   // an upload whose handler wasn't registered when its headers arrived is
   // read in full and dispatched like any other request
   int calls = 0;
   bool complete = false;
   std::string received;
   UriAsyncUploadHandlerFunction uploadHandler =
         [&](const Request&, const std::string& formData, bool isComplete, const UriHandlerFunctionContinuation&)
   {
      ++calls;
      complete = isComplete;
      received = formData;
      return true;
   };

   Request request;
   request.setBody("--boundary\r\nform data\r\n--boundary--\r\n");
   visitHandler(UriAsyncHandlerFunctionVariant(uploadHandler), request, [](Response*) {});

   EXPECT_EQ(1, calls);
   EXPECT_TRUE(complete);
   EXPECT_EQ(request.body(), received);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
