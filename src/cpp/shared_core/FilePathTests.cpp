/*
 * FilePath.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the terms of a commercial license agreement
 * with Posit, then this program is licensed to you under the following terms:
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

#include <gtest/gtest.h>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
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

TEST(SharedCoreTest, EmptyFilePathTests)
{
   // Construction section
   {
      FilePath f;
      EXPECT_TRUE(f.isEmpty());
      EXPECT_TRUE(f.getAbsolutePath().empty());
   }

   // Empty string constructor section
   {
      std::string empty;
      FilePath filePath(empty);

      EXPECT_TRUE(filePath.isEmpty());
      EXPECT_TRUE(filePath.getAbsolutePath().empty());
   }

   // Empty string literal constructor section
   {
      FilePath filePath("");

      EXPECT_TRUE(filePath.isEmpty());
      EXPECT_TRUE(filePath.getAbsolutePath().empty());
   }

   // relative path construction section
   {
      FilePath rootPath = createPath();
      FilePath pPath = createPath("/path/to");
      FilePath aPath = createPath("/path/to/a");
      FilePath bPath = createPath("/path/to/b");

      EXPECT_TRUE(aPath.isWithin(pPath));
      EXPECT_TRUE(bPath.isWithin(pPath));
      EXPECT_FALSE(aPath.isWithin(bPath));

      EXPECT_EQ("a", aPath.getRelativePath(pPath));
   }

   // Raw string construction section
   {
      const char* path = "/a/path";
      FilePath f1(path);
      EXPECT_TRUE(f1.getAbsolutePath() == std::string(path));

      const char* empty = NULL;
      FilePath f2(empty);
      EXPECT_TRUE(f2.isEmpty());
   }

   // Comparison (equal, true) section
   {
      FilePath f1, f2;

      EXPECT_EQ(f2, f1);
      EXPECT_EQ(f1, f2);
      EXPECT_EQ(f1, f1);
   }

   // Comparison (equal, false) section
   {
      FilePath f1, f2("/a/different/path");

      EXPECT_NE(f1, f2);
      EXPECT_NE(f2, f1);
   }

   // Comparison (inequal, false) section
   {
      FilePath f1, f2;

      EXPECT_EQ(f1, f2);
      EXPECT_EQ(f2, f1);
      EXPECT_EQ(f1, f1);
   }

   // Comparison (inequal, true) section
   {
      FilePath f1, f2("/a/different/path");

      EXPECT_NE(f2, f1);
      EXPECT_NE(f1, f2);
   }

   // Comparison (lt) section
   {
      FilePath f1, f2("/a/different/path");

      EXPECT_LT(f1, f2);
      EXPECT_FALSE(f2 < f1);
   }

   // Retrieval methods section
   {
      FilePath f;
      std::vector<FilePath> children;

      EXPECT_FALSE(f.exists());
      EXPECT_TRUE(f.getAbsolutePath().empty());
      EXPECT_TRUE(f.getAbsolutePathNative().empty());
#ifdef _WIN32
      EXPECT_TRUE(f.getAbsolutePathW().empty());
#endif
      EXPECT_TRUE(f.getCanonicalPath().empty());
      EXPECT_TRUE(f.getChildren(children)); // Returns error.
      EXPECT_TRUE(children.empty());
      EXPECT_TRUE(f.getExtension().empty());
      EXPECT_TRUE(f.getExtensionLowerCase().empty());
      EXPECT_TRUE(f.getFilename().empty());
      EXPECT_TRUE(f.getLastWriteTime() == 0);
      EXPECT_TRUE(f.getLexicallyNormalPath().empty());
      EXPECT_TRUE(f.getMimeContentType() == "text/plain"); // text/plain is the default.
      EXPECT_TRUE(f.getParent() == f); // Error on getting the parent, so self should be returned.
      EXPECT_TRUE(f.getRelativePath(FilePath("/a/parent/path")).empty());
      EXPECT_TRUE(f.getSize() == 0);
      EXPECT_TRUE(f.getSizeRecursive() == 0);
      EXPECT_TRUE(f.getStem().empty());
      EXPECT_FALSE(f.hasExtension("ext"));
      EXPECT_FALSE(f.hasTextMimeType()); // has text mime type sets the default mime type as "application/octet-stream"
      EXPECT_FALSE(f.isDirectory());
      EXPECT_TRUE(f.isEmpty());
      EXPECT_FALSE(f.isHidden());
      EXPECT_FALSE(f.isJunction());
      EXPECT_FALSE(f.isRegularFile());
      EXPECT_FALSE(f.isSymlink());
      EXPECT_TRUE(f.isWithin(f));
      EXPECT_FALSE(f.isWithin(FilePath("/some/path")));
   }

   // Complete path methods section
   {
      FilePath f1, f2;
      FilePath fExpected = FilePath::safeCurrentPath(
         FilePath("/this/shouldn't/be/used")).completeChildPath("some/path");

      EXPECT_TRUE(f1.completeChildPath("some/path") == fExpected);
      EXPECT_FALSE(f1.completeChildPath("some/path", f2));
      EXPECT_TRUE(f2 == fExpected);
      EXPECT_TRUE(f1.completePath("some/path") == fExpected);
      EXPECT_TRUE(f1.completeChildPath("/some/absolute/path") == f1);
      EXPECT_TRUE(f1.completeChildPath("/some/absolute/path", f2)); // Error here.
      EXPECT_TRUE(f2 == f1); // f2 should have been set to f1.
   }

   // child path completion section
   {
      // simple path completion should do what's expected
      FilePath aPath = createPath("/path/to/a");
      FilePath bPath = createPath("/path/to/a/b");
      EXPECT_TRUE(aPath.completeChildPath("b") == bPath);

      // trying to complete to a path outside should fail and return the original path
      FilePath cPath = createPath("/path/to/foo");
      EXPECT_TRUE(cPath.completeChildPath("../bar") == cPath);
      EXPECT_TRUE(cPath.completeChildPath("/path/to/quux") == cPath);

      // trailing slashes are okay
      FilePath dPath = createPath("/path/to/");
      FilePath ePath = createPath("/path/to/e");
      EXPECT_TRUE(dPath.completeChildPath("e") == ePath);
   }

   // general path completion section
   {
      // simple path completion should do what's expected
      FilePath aPath = createPath("/path/to/a");
      FilePath bPath = createPath("/path/to/a/b");
      EXPECT_TRUE(aPath.completePath("b") == bPath);

      // absolute paths are allowed
      FilePath cPath = createPath("/path/to/c");
      FilePath dPath = createPath("/path/to/d");
      EXPECT_TRUE(cPath.completePath("/path/to/d") == dPath);

      // directory traversal is allowed
      FilePath ePath = createPath("/path/to/e");
      FilePath fPath = createPath("/path/to/f");
      EXPECT_TRUE(ePath.completePath("../f").getLexicallyNormalPath() == fPath.getAbsolutePath());
   }

#ifdef _WIN32

   // relative paths for UNC shares section
   {
      // NOTE: need to be robust against mixed separators as these can
      // leak in depending on the API used to request the file path.
      //
      // https://github.com/rstudio/rstudio/issues/6587
      FilePath pPath(R"(//LOCALHOST/c$/p)");
      FilePath aPath(R"(\\LOCALHOST\c$\p\a)");
      EXPECT_TRUE(aPath.getRelativePath(pPath) == "a");
   }

   // directory write testing for Win32 section
   {
      // create temporary directory
      FilePath tempDir;
      Error error = FilePath::tempFilePath(tempDir);
      EXPECT_TRUE(!error);

      error = tempDir.ensureDirectory();
      EXPECT_TRUE(!error);

      // it should now report as writeable
      bool writeable = false;
      error = tempDir.isWriteable(writeable);
      EXPECT_TRUE(!error);
      EXPECT_TRUE(writeable);

      // clean up
      tempDir.remove();
   }

#else

   // Non-Windows tests

   // directory write testing section
   {
      // create temporary directory
      FilePath tempDir;
      Error error = FilePath::tempFilePath(tempDir);
      EXPECT_TRUE(!error);

      error = tempDir.ensureDirectory();
      EXPECT_TRUE(!error);

      // ensure it's writeable
      error = tempDir.changeFileMode(FileMode::USER_READ_WRITE_ALL_READ);
      EXPECT_TRUE(!error);

      // it should now report as writeable
      bool writeable = false;
      error = tempDir.isWriteable(writeable);
      EXPECT_TRUE(!error);
      EXPECT_TRUE(writeable);

      // however, it should be an error to test write permissions (that's only for files)
      error = tempDir.testWritePermissions();
      EXPECT_TRUE(error);

      // clean up
      tempDir.remove();
   }
#endif /* !_WIN32 */

}

#ifndef _WIN32

TEST(SharedCoreTest, GetFileOwnerTest)
{
   // create temporary file
   FilePath tempFile;
   Error error = FilePath::tempFilePath(tempFile);
   EXPECT_TRUE(!error);

   // ensure it exists
   error = tempFile.ensureFile();
   EXPECT_TRUE(!error);

   // get the current user
   system::User currentUser;
   error = system::User::getCurrentUser(currentUser);
   EXPECT_TRUE(!error);

   // ensure we can get the owner
   std::string username;
   error = tempFile.getFileOwner(&username);
   EXPECT_TRUE(!error);
   //EXPECT_TRUE(username == currentUser.getUsername());
   EXPECT_TRUE(username == currentUser.getUsername());

   // clean up
   tempFile.remove();
}

#endif

TEST(SharedCoreTest, CopyFilePathTests)
{
   FilePath f1("/a/path");

   EXPECT_TRUE(f1.getAbsolutePath() == "/a/path");

   FilePath f2 = f1;

   EXPECT_TRUE(f1.getAbsolutePath() == "/a/path");
   EXPECT_TRUE(f2 == f1);
   EXPECT_TRUE(f2.getAbsolutePath() == "/a/path");
}

#ifdef _WIN32

TEST(SharedCoreTest, WindowsFilePathIsWithin)
{
   EXPECT_TRUE(FilePath("C:/tmp/dir").isWithin(FilePath("C:/tmp")));
   EXPECT_TRUE(FilePath("C:\\tmp\\dir").isWithin(FilePath("C:/tmp")));
   EXPECT_TRUE(FilePath("C:/tmp/dir").isWithin(FilePath("C:\\tmp")));
   EXPECT_TRUE(FilePath("C:\\tmp\\dir").isWithin(FilePath("C:\\tmp")));
}

#endif

#ifndef _WIN32

TEST(SharedCoreTest, SymlinksTests)
{
   // Directories section
   {
      FilePath src("/tmp/rstudio-symlinks-a");
      FilePath dst("/tmp/rstudio-symlinks-b");

      src.ensureDirectory();
      EXPECT_TRUE(src.isDirectory());

      int status = ::symlink(src.getAbsolutePath().c_str(), dst.getAbsolutePath().c_str());
      EXPECT_TRUE(status == 0);
      EXPECT_TRUE(dst.isSymlink());
      EXPECT_TRUE(dst.isDirectory());

      dst.remove();
      EXPECT_TRUE(!dst.exists());

      src.remove();
      EXPECT_TRUE(!src.exists());
   }
}

#endif

} // namespace core
} // namespace rstudio
