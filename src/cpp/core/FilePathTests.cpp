/*
 * FilePathTests.cpp
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

// All of these tests presume Unix-style paths
#ifndef _WIN32

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

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

      CHECK(aPath.getRelativePath(pPath) == "a");
   }

   SECTION("path containment pathology")
   {
      // isWithin should not be fooled by directory traversal; the first path is not inside the
      // second even though it appears to be lexically
      FilePath aPath("/path/to/a/../b");
      FilePath bPath("/path/to/a");
      CHECK(!aPath.isWithin(bPath));

      // isWithin should not be fooled by substrings
      FilePath cPath("/path/to/foo");
      FilePath dPath("/path/to/foobar");
      CHECK(!dPath.isWithin(cPath));
   }

   SECTION("child path completion")
   {
      // simple path completion should do what's expected
      FilePath aPath("/path/to/a");
      FilePath bPath("/path/to/a/b");
      CHECK(aPath.completeChildPath("b") == bPath);

      // trying to complete to a path outside should fail and return the original path
      FilePath cPath("/path/to/foo");
      CHECK(cPath.completeChildPath("../bar") == cPath);
      CHECK(cPath.completeChildPath("/path/to/quux") == cPath);

      // trailing slashes are okay
      FilePath dPath("/path/to/");
      FilePath ePath("/path/to/e");
      CHECK(dPath.completeChildPath("e") == ePath);
   }

   SECTION("general path completion")
   {
      // simple path completion should do what's expected
      FilePath aPath("/path/to/a");
      FilePath bPath("/path/to/a/b");
      CHECK(aPath.completePath("b") == bPath);

      // absolute paths are allowed
      FilePath cPath("/path/to/c");
      FilePath dPath("/path/to/d");
      CHECK(cPath.completePath("/path/to/d") == dPath);

      // directory traversal is allowed
      FilePath ePath("/path/to/e");
      FilePath fPath("/path/to/f");
      CHECK(ePath.completePath("../f").getLexicallyNormalPath() == fPath.getAbsolutePath());
   }
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio

#endif  // _WIN32

