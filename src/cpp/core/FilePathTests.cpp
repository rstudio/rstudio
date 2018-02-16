/*
 * FilePathTests.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace tests {

TEST_CASE("file paths")
{
   SECTION("relative path construction")
   {
      FilePath rootPath("/");
      FilePath pPath("/path/to");
      FilePath aPath("/path/to/a");
      FilePath bPath("/path/to/b");

      CHECK(aPath.isWithin(pPath));
      CHECK(bPath.isWithin(pPath));
      CHECK(!aPath.isWithin(bPath));

      CHECK(aPath.relativePath(pPath) == "a");
   }
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
