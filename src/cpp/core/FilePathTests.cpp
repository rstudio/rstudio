/*
 * FilePathTests.cpp
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

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace tests {

namespace {

#ifdef _WIN32

// helper for creating a path with the system drive
// prefixed for Windows. we try to use the current
// drive if at all possible; if we cannot retrieve
// that for some reason then we just fall back to
// the default system drive (normally C:)
std::string getDrivePrefix()
{
   char buffer[MAX_PATH];
   DWORD n = GetCurrentDirectory(MAX_PATH, buffer);
   if (n < 2)
      return ::getenv("SYSTEMDRIVE");

   if (buffer[1] != ':')
      return ::getenv("SYSTEMDRIVE");

   return std::string(buffer, 2);
}

FilePath createPath(const std::string& path = "/")
{
   static const std::string prefix = getDrivePrefix();
   return FilePath(prefix + path);
}

#else

FilePath createPath(const std::string& path = "/")
{
   return FilePath(path);
}

#endif /* _WIN32 */

} // end anonymous namespace
TEST_CASE("file paths")
{
   SECTION("relative path construction")
   {
      FilePath rootPath = createPath();
      FilePath pPath = createPath("/path/to");
      FilePath aPath = createPath("/path/to/a");
      FilePath bPath = createPath("/path/to/b");

      CHECK(aPath.isWithin(pPath));
      CHECK(bPath.isWithin(pPath));
      CHECK(!aPath.isWithin(bPath));

      CHECK(aPath.getRelativePath(pPath) == "a");
   }

   SECTION("path containment pathology")
   {
      // isWithin should not be fooled by directory traversal; the first path is not inside the
      // second even though it appears to be lexically
      FilePath aPath = createPath("/path/to/a/../b");
      FilePath bPath = createPath("/path/to/a");
      CHECK(!aPath.isWithin(bPath));

      // isWithin should not be fooled by substrings
      FilePath cPath = createPath("/path/to/foo");
      FilePath dPath = createPath("/path/to/foobar");
      CHECK(!dPath.isWithin(cPath));
   }

   SECTION("child path completion")
   {
      // simple path completion should do what's expected
      FilePath aPath = createPath("/path/to/a");
      FilePath bPath = createPath("/path/to/a/b");
      CHECK(aPath.completeChildPath("b") == bPath);

      // trying to complete to a path outside should fail and return the original path
      FilePath cPath = createPath("/path/to/foo");
      CHECK(cPath.completeChildPath("../bar") == cPath);
      CHECK(cPath.completeChildPath("/path/to/quux") == cPath);

      // trailing slashes are okay
      FilePath dPath = createPath("/path/to/");
      FilePath ePath = createPath("/path/to/e");
      CHECK(dPath.completeChildPath("e") == ePath);
   }

   SECTION("general path completion")
   {
      // simple path completion should do what's expected
      FilePath aPath = createPath("/path/to/a");
      FilePath bPath = createPath("/path/to/a/b");
      CHECK(aPath.completePath("b") == bPath);

      // absolute paths are allowed
      FilePath cPath = createPath("/path/to/c");
      FilePath dPath = createPath("/path/to/d");
      CHECK(cPath.completePath("/path/to/d") == dPath);

      // directory traversal is allowed
      FilePath ePath = createPath("/path/to/e");
      FilePath fPath = createPath("/path/to/f");
      CHECK(ePath.completePath("../f").getLexicallyNormalPath() == fPath.getAbsolutePath());
   }

#ifdef _WIN32

   SECTION("relative paths for UNC shares")
   {
      // NOTE: need to be robust against mixed separators as these can
      // leak in depending on the API used to request the file path.
      //
      // https://github.com/rstudio/rstudio/issues/6587
      FilePath pPath(R"(//LOCALHOST/c$/p)");
      FilePath aPath(R"(\\LOCALHOST\c$\p\a)");
      CHECK(aPath.getRelativePath(pPath) == "a");
   }

#endif /* _WIN32 */

}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
