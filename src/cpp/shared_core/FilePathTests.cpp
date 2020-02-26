/*
 * FilePath.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include <tests/TestThat.hpp>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {

TEST_CASE("Empty File Path tests")
{
   SECTION("Construction")
   {
      FilePath f;
      CHECK(f.getAbsolutePath().empty());
   }

   SECTION("Comparison (equal, true)")
   {
      FilePath f1, f2;

      CHECK(f1 == f2);
      CHECK(f2 == f1);
      CHECK(f1 == f1);
   }

   SECTION("Comparison (equal, false)")
   {
      FilePath f1, f2("/a/different/path");

      CHECK_FALSE(f1 == f2);
      CHECK_FALSE(f2 == f1);
   }

   SECTION("Comparison (inequal, false)")
   {
      FilePath f1, f2;

      CHECK_FALSE(f1 != f2);
      CHECK_FALSE(f2 != f1);
      CHECK_FALSE(f1 != f1);
   }

   SECTION("Comparison (inequal, true)")
   {
      FilePath f1, f2("/a/different/path");

      CHECK(f1 != f2);
      CHECK(f2 != f1);
   }

   SECTION("Comparison (lt)")
   {
      FilePath f1, f2("/a/different/path");

      CHECK(f1 < f2);
      CHECK_FALSE(f2 < f1);
   }

   SECTION("Retrieval methods")
   {
      FilePath f;
      std::vector<FilePath> children;

      CHECK_FALSE(f.exists());
      CHECK(f.getAbsolutePath().empty());
      CHECK(f.getAbsolutePathNative().empty());
#ifdef _WIN32
      CHECK(f.getAbsolutePathW().empty());
#endif
      CHECK(f.getCanonicalPath().empty());
      CHECK(f.getChildren(children)); // Returns error.
      CHECK(children.empty());
      CHECK(f.getExtension().empty());
      CHECK(f.getExtensionLowerCase().empty());
      CHECK(f.getFilename().empty());
      CHECK(f.getLastWriteTime() == 0);
      CHECK(f.getLexicallyNormalPath().empty());
      CHECK(f.getMimeContentType() == "text/plain"); // text/plain is the default.
      CHECK(f.getParent() == f); // Error on getting the parent, so self should be returned.
      CHECK(f.getRelativePath(FilePath("/a/parent/path")).empty());
      CHECK(f.getSize() == 0);
      CHECK(f.getSizeRecursive() == 0);
      CHECK(f.getStem().empty());
      CHECK_FALSE(f.hasExtension("ext"));
      CHECK_FALSE(f.hasTextMimeType()); // has text mime type sets the default mime type as "application/octet-stream"
      CHECK_FALSE(f.isDirectory());
      CHECK(f.isEmpty());
      CHECK_FALSE(f.isHidden());
      CHECK_FALSE(f.isJunction());
      CHECK_FALSE(f.isRegularFile());
      CHECK_FALSE(f.isSymlink());
      CHECK(f.isWithin(f));
      CHECK_FALSE(f.isWithin(FilePath("/some/path")));
   }

   SECTION("Complete path methods")
   {
      FilePath f1, f2;
      FilePath fExpected = FilePath::safeCurrentPath(
         FilePath("/this/shouldn't/be/used")).completeChildPath("some/path");

      CHECK(f1.completeChildPath("some/path") == fExpected);
      CHECK_FALSE(f1.completeChildPath("some/path", f2));
      CHECK(f2 == fExpected);
      CHECK(f1.completePath("some/path") == fExpected);
      CHECK(f1.completeChildPath("/some/absolute/path") == f1);
      CHECK(f1.completeChildPath("/some/absolute/path", f2)); // Error here.
      CHECK(f2 == f1); // f2 should have been set to f1.
   }
}

TEST_CASE("Copy FilePath Tests")
{
   FilePath f1("/a/path");

   CHECK(f1.getAbsolutePath() == "/a/path");

   FilePath f2 = f1;

   CHECK(f1.getAbsolutePath() == "/a/path");
   CHECK(f2 == f1);
   CHECK(f2.getAbsolutePath() == "/a/path");
}

} // namespace core
} // namespace rstudio
