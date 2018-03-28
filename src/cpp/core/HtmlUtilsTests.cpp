/*
 * MiscellaneousTests.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <tests/TestThat.hpp>

#include <core/HtmlUtils.hpp>

namespace rstudio {
namespace core {
namespace html_utils {

context("data URIs")
{
   test_that("a non-base64 data URI can be parsed")
   {
      std::string uri = "data:image/png,abcdef";

      DataUri data;
      Error error = parseDataUri(uri, &data);
      
      expect_true(error == Success());
      expect_true(data.mediaType == "image/png");
      expect_true(data.base64 == false);
      expect_true(data.data == "abcdef");
   }
   
   test_that("a base64 data URI can be parsed")
   {
      std::string uri = "data:image/png;base64,abcdef";

      DataUri data;
      Error error = parseDataUri(uri, &data);
      
      expect_true(error == Success());
      expect_true(data.mediaType == "image/png");
      expect_true(data.base64 == true);
      expect_true(data.data == "abcdef");
   }
   
}

} // end namespace html_utils
} // end namespace core
} // end namespace rstudio
