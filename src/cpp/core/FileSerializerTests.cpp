/*
 * FileSerializerTests.cpp
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

#include <gtest/gtest.h>

#include <ctime>
#include <istream>
#include <memory>
#include <string>
#include <vector>

#include <core/FileSerializer.hpp>
#include <core/tests/ScratchDir.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#ifdef _WIN32
# include <windows.h>
#else
# include <cerrno>
# include <sys/stat.h>
# include <unistd.h>
#endif

namespace rstudio {
namespace core {

using tests::scratchDir;

TEST(FileSerializerTest, WriteStringRoundTrips)
{
   FilePath filePath;
   ASSERT_FALSE(FilePath::tempFilePath(filePath));

   std::string contents = "hello\nworld\n";
   Error error = writeStringToFile(filePath, contents);
   EXPECT_FALSE(error);

   std::string readback;
   error = readStringFromFile(filePath, &readback);
   EXPECT_FALSE(error);
   EXPECT_EQ(contents, readback);

   filePath.removeIfExists();
}

// An empty write is a real code path (e.g. AdvisoryFileLock) and must both
// succeed and truncate any prior contents.
TEST(FileSerializerTest, WriteStringEmptyRoundTrips)
{
   FilePath filePath;
   ASSERT_FALSE(FilePath::tempFilePath(filePath));

   Error error = writeStringToFile(filePath, "initial contents");
   EXPECT_FALSE(error);

   error = writeStringToFile(filePath, "");
   EXPECT_FALSE(error);

   std::string readback = "not empty";
   error = readStringFromFile(filePath, &readback);
   EXPECT_FALSE(error);
   EXPECT_EQ("", readback);

   filePath.removeIfExists();
}

// A payload larger than any internal buffer, to exercise a big write and
// confirm nothing is truncated or duplicated by the write loop.
TEST(FileSerializerTest, WriteStringLargeRoundTrips)
{
   FilePath filePath;
   ASSERT_FALSE(FilePath::tempFilePath(filePath));

   std::string contents;
   contents.reserve(4 * 1024 * 1024);
   for (std::size_t i = 0; i < 4 * 1024 * 1024; ++i)
      contents.push_back(static_cast<char>('a' + (i % 26)));

   Error error = writeStringToFile(filePath, contents);
   EXPECT_FALSE(error);

   std::string readback;
   error = readStringFromFile(filePath, &readback);
   EXPECT_FALSE(error);
   EXPECT_EQ(contents, readback);

   filePath.removeIfExists();
}

// Content with embedded NUL bytes and high bytes must round-trip exactly: the
// write is length-based, not NUL-terminated, and we must not convert line
// endings when passing them through.
TEST(FileSerializerTest, WriteStringBinaryRoundTrips)
{
   FilePath filePath;
   ASSERT_FALSE(FilePath::tempFilePath(filePath));

   std::string contents;
   contents.push_back('a');
   contents.push_back('\0');
   contents.push_back('b');
   contents.push_back('\xFF');
   contents.push_back('\0');
   contents.push_back('c');

   Error error = writeStringToFile(filePath, contents);
   EXPECT_FALSE(error);

   std::string readback;
   error = readStringFromFile(filePath, &readback);
   EXPECT_FALSE(error);
   EXPECT_EQ(contents, readback);

   filePath.removeIfExists();
}

// A truncating write over a longer file must leave only the new (shorter)
// contents behind, with no trailing remnants of the original.
TEST(FileSerializerTest, WriteStringTruncatesExisting)
{
   FilePath filePath;
   ASSERT_FALSE(FilePath::tempFilePath(filePath));

   Error error = writeStringToFile(filePath, "a much longer set of contents");
   EXPECT_FALSE(error);

   error = writeStringToFile(filePath, "short");
   EXPECT_FALSE(error);

   std::string readback;
   error = readStringFromFile(filePath, &readback);
   EXPECT_FALSE(error);
   EXPECT_EQ("short", readback);

   filePath.removeIfExists();
}

// A non-truncating write appends to the existing contents.
TEST(FileSerializerTest, WriteStringAppends)
{
   FilePath filePath;
   ASSERT_FALSE(FilePath::tempFilePath(filePath));

   Error error = writeStringToFile(filePath, "hello");
   EXPECT_FALSE(error);

   error = writeStringToFile(filePath, " world", string_utils::LineEndingPassthrough,
                             /* truncate */ false);
   EXPECT_FALSE(error);

   std::string readback;
   error = readStringFromFile(filePath, &readback);
   EXPECT_FALSE(error);
   EXPECT_EQ("hello world", readback);

   filePath.removeIfExists();
}

// The durable write path additionally flushes to physical storage; confirm a
// normal round-trip still works through it.
TEST(FileSerializerTest, WriteStringDurableRoundTrips)
{
   FilePath filePath;
   ASSERT_FALSE(FilePath::tempFilePath(filePath));

   std::string contents = "hello\nworld\n";
   Error error = writeStringToFile(filePath,
                                   contents,
                                   string_utils::LineEndingPassthrough,
                                   true /* truncate */,
                                   0 /* maxOpenRetrySeconds */,
                                   true /* logError */,
                                   true /* durable */);
   EXPECT_FALSE(error);

   std::string readback;
   error = readStringFromFile(filePath, &readback);
   EXPECT_FALSE(error);
   EXPECT_EQ(contents, readback);

   filePath.removeIfExists();
}

TEST(FileSerializerTest, WriteStringAtomicRoundTrips)
{
   FilePath filePath;
   ASSERT_FALSE(FilePath::tempFilePath(filePath));

   std::string contents = "hello\nworld\n";
   Error error = writeStringToFileAtomic(filePath, contents);
   EXPECT_FALSE(error);

   std::string readback;
   error = readStringFromFile(filePath, &readback);
   EXPECT_FALSE(error);
   EXPECT_EQ(contents, readback);

   filePath.removeIfExists();
}

namespace {

int countAtomicWriteTempFiles(const FilePath& dir)
{
   std::vector<FilePath> children;
   EXPECT_FALSE(dir.getChildren(children));

   int count = 0;
   for (const FilePath& child : children)
   {
      if (isAtomicWriteTempFile(child))
         count++;
   }
   return count;
}

} // anonymous namespace

TEST(FileSerializerTest, WriteStringAtomicReplacesContents)
{
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("state.json");

   ASSERT_FALSE(writeStringToFileAtomic(filePath, "a much longer original value\n"));
   ASSERT_FALSE(writeStringToFileAtomic(filePath, "new\n"));

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("new\n", readback);

   // the temporary file was renamed into place, not left behind
   EXPECT_EQ(0, countAtomicWriteTempFiles(dir));

   dir.remove();
}

TEST(FileSerializerTest, WriteStringAtomicDurableRoundTrips)
{
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("state.json");

   AtomicWriteOptions options;
   options.durable = true;
   ASSERT_FALSE(writeStringToFileAtomic(filePath, "old\n", string_utils::LineEndingPassthrough, options));
   ASSERT_FALSE(writeStringToFileAtomic(filePath, "new\n", string_utils::LineEndingPassthrough, options));

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("new\n", readback);

   dir.remove();
}

TEST(FileSerializerTest, WriteCollectionInPlaceAndAtomic)
{
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("list");
   std::vector<std::string> lines = { "one", "two" };

   ASSERT_FALSE(writeCollectionToFile<std::vector<std::string>>(filePath, lines, stringifyString));

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("one\ntwo\n", readback);

   lines.push_back("three");
   ASSERT_FALSE(writeCollectionToFile<std::vector<std::string>>(filePath, lines, stringifyString, false /* atomic */));

   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("one\ntwo\nthree\n", readback);

   dir.remove();
}

TEST(FileSerializerTest, IdentifiesAtomicWriteTempFiles)
{
   EXPECT_TRUE(isAtomicWriteTempFile(FilePath("/tmp/.rstudio-tmp-3f2a-9c1b-77d0-a1e4")));
   EXPECT_FALSE(isAtomicWriteTempFile(FilePath("/tmp/.rstudio-tmp")));
   EXPECT_FALSE(isAtomicWriteTempFile(FilePath("/tmp/rstudio-prefs.json")));
   EXPECT_FALSE(isAtomicWriteTempFile(FilePath("/tmp/.rstudio-tmp-dir/state.json")));
}

// Only temporary files old enough to have been abandoned are removed; a recent
// one may belong to a write that another process is still making.
TEST(FileSerializerTest, RemovesStaleAtomicWriteTempFiles)
{
   FilePath dir = scratchDir();

   FilePath stale = dir.completePath(".rstudio-tmp-stale");
   FilePath recent = dir.completePath(".rstudio-tmp-recent");
   FilePath data = dir.completePath("data");
   ASSERT_FALSE(writeStringToFile(stale, "stale"));
   ASSERT_FALSE(writeStringToFile(recent, "recent"));
   ASSERT_FALSE(writeStringToFile(data, "data"));

   std::time_t twoHoursAgo = std::time(nullptr) - 2 * 60 * 60;
   stale.setLastWriteTime(twoHoursAgo);
   data.setLastWriteTime(twoHoursAgo);

   removeStaleAtomicWriteTempFiles(dir, 60 * 60);

   EXPECT_FALSE(stale.exists());
   EXPECT_TRUE(recent.exists());
   EXPECT_TRUE(data.exists());

   // a missing directory is not an error
   removeStaleAtomicWriteTempFiles(dir.completePath("missing"));

   dir.remove();
}

// The first write into a directory cleans up after an earlier write that a
// crash interrupted, without the writer having to know about it.
TEST(FileSerializerTest, WriteStringAtomicRemovesStaleTempFiles)
{
   FilePath dir = scratchDir();

   FilePath stale = dir.completePath(".rstudio-tmp-stale");
   FilePath recent = dir.completePath(".rstudio-tmp-recent");
   ASSERT_FALSE(writeStringToFile(stale, "stale"));
   ASSERT_FALSE(writeStringToFile(recent, "recent"));
   stale.setLastWriteTime(std::time(nullptr) - 2 * 60 * 60);

   ASSERT_FALSE(writeStringToFileAtomic(dir.completePath("state.json"), "{}"));

   EXPECT_FALSE(stale.exists());
   EXPECT_TRUE(recent.exists());

   dir.remove();
}

// A write into a directory that doesn't exist yet must not count as having
// swept it: the sweep happens once the directory is there.
TEST(FileSerializerTest, WriteStringAtomicSweepsDirectoryCreatedLater)
{
   FilePath dir = scratchDir().completePath("later");
   FilePath filePath = dir.completePath("state.json");
   ASSERT_TRUE(writeStringToFileAtomic(filePath, "{}"));

   ASSERT_FALSE(dir.ensureDirectory());
   FilePath stale = dir.completePath(".rstudio-tmp-stale");
   ASSERT_FALSE(writeStringToFile(stale, "stale"));
   stale.setLastWriteTime(std::time(nullptr) - 2 * 60 * 60);

   ASSERT_FALSE(writeStringToFileAtomic(filePath, "{}"));

   EXPECT_FALSE(stale.exists());
   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("{}", readback);

   dir.getParent().remove();
}

// A failure names the file being written, not the temporary file, since that
// is what the caller reports to the user.
TEST(FileSerializerTest, WriteStringAtomicErrorNamesTarget)
{
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("missing").completePath("state.json");

   Error error = writeStringToFileAtomic(filePath, "{}");
   ASSERT_TRUE(error);
   EXPECT_EQ(filePath.getAbsolutePath(), error.getProperty("path"));
   EXPECT_EQ(0, countAtomicWriteTempFiles(dir));

   dir.remove();
}

#ifndef _WIN32

namespace {

mode_t fileMode(const FilePath& filePath)
{
   struct stat st;
   EXPECT_EQ(0, ::stat(filePath.getAbsolutePath().c_str(), &st));
   return st.st_mode & 07777;
}

} // anonymous namespace

// Replacing a file must not reset its mode to the temporary file's default.
TEST(FileSerializerTest, WriteStringAtomicPreservesPermissions)
{
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("state.json");

   ASSERT_FALSE(writeStringToFile(filePath, "original\n"));
   ASSERT_EQ(0, ::chmod(filePath.getAbsolutePath().c_str(), 0640));

   ASSERT_FALSE(writeStringToFileAtomic(filePath, "replaced\n"));

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("replaced\n", readback);
   EXPECT_EQ(0640u, fileMode(filePath));

   dir.remove();
}

TEST(FileSerializerTest, WriteStringAtomicOwnerOnly)
{
   FilePath dir = scratchDir();
   FilePath newFile = dir.completePath("new.json");
   FilePath existingFile = dir.completePath("existing.json");

   AtomicWriteOptions options;
   options.ownerOnly = true;

   ASSERT_FALSE(writeStringToFileAtomic(newFile, "{}", string_utils::LineEndingPassthrough, options));
   EXPECT_EQ(0600u, fileMode(newFile));

   ASSERT_FALSE(writeStringToFile(existingFile, "{}"));
   ASSERT_EQ(0, ::chmod(existingFile.getAbsolutePath().c_str(), 0644));
   ASSERT_FALSE(writeStringToFileAtomic(existingFile, "{}", string_utils::LineEndingPassthrough, options));
   EXPECT_EQ(0600u, fileMode(existingFile));

   dir.remove();
}

// A file we may not write to can still be replaced when its directory is
// writable; e.g. a state file left owned by root by 'sudo rstudio'.
TEST(FileSerializerTest, WriteStringAtomicReplacesReadOnlyFile)
{
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("state.json");

   ASSERT_FALSE(writeStringToFile(filePath, "original\n"));
   ASSERT_EQ(0, ::chmod(filePath.getAbsolutePath().c_str(), 0400));

   ASSERT_FALSE(writeStringToFileAtomic(filePath, "replaced\n"));

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("replaced\n", readback);
   EXPECT_EQ(0400u, fileMode(filePath));

   dir.remove();
}

// When no temporary file can be created in the directory, the file is
// rewritten in place instead.
TEST(FileSerializerTest, WriteStringAtomicFallsBackToInPlace)
{
   if (::geteuid() == 0)
      GTEST_SKIP() << "root ignores directory permissions";

   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("state.json");
   ASSERT_FALSE(writeStringToFile(filePath, "original\n"));
   ASSERT_EQ(0, ::chmod(dir.getAbsolutePath().c_str(), 0500));

   Error error = writeStringToFileAtomic(filePath, "replaced\n");
   ASSERT_EQ(0, ::chmod(dir.getAbsolutePath().c_str(), 0700));
   EXPECT_FALSE(error);

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("replaced\n", readback);

   dir.remove();
}

// When neither the temporary file nor the target can be created, the error is
// the one from creating the temporary file, not a misleading ENOENT from
// trying to prepare a target that isn't there.
TEST(FileSerializerTest, WriteStringAtomicMissingTargetInUnwritableDirReportsPermission)
{
   if (::geteuid() == 0)
      GTEST_SKIP() << "root ignores directory permissions";

   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("state.json");
   ASSERT_EQ(0, ::chmod(dir.getAbsolutePath().c_str(), 0500));

   AtomicWriteOptions options;
   options.ownerOnly = true;
   Error error = writeStringToFileAtomic(filePath, "{}", string_utils::LineEndingPassthrough, options);
   ASSERT_EQ(0, ::chmod(dir.getAbsolutePath().c_str(), 0700));
   ASSERT_TRUE(error);
   EXPECT_EQ(EACCES, error.getCode());
   EXPECT_EQ(filePath.getAbsolutePath(), error.getProperty("path"));
   EXPECT_FALSE(filePath.exists());

   dir.remove();
}

// A caller that needs the original to survive a failed write (Replace All)
// turns the in-place fallback off: the write fails and nothing is truncated.
TEST(FileSerializerTest, WriteStringAtomicWithoutFallbackKeepsOriginal)
{
   if (::geteuid() == 0)
      GTEST_SKIP() << "root ignores directory permissions";

   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("source.R");
   ASSERT_FALSE(writeStringToFile(filePath, "original\n"));
   ASSERT_EQ(0, ::chmod(dir.getAbsolutePath().c_str(), 0500));

   AtomicWriteOptions options;
   options.allowInPlaceFallback = false;
   Error error = writeStringToFileAtomic(filePath, "replaced\n", string_utils::LineEndingPassthrough, options);
   ASSERT_EQ(0, ::chmod(dir.getAbsolutePath().c_str(), 0700));
   ASSERT_TRUE(error);
   EXPECT_EQ(EACCES, error.getCode());
   EXPECT_EQ(filePath.getAbsolutePath(), error.getProperty("path"));

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("original\n", readback);
   EXPECT_EQ(0, countAtomicWriteTempFiles(dir));

   dir.remove();
}

#ifdef RSTUDIO_UNIT_TESTS_ENABLED

namespace {

// Turns the injected failures off again however the test ends.
struct AtomicWriteFailureReset
{
   ~AtomicWriteFailureReset()
   {
      setAtomicWriteChmodFailureForTesting(0);
      setAtomicWriteRenameFailureForTesting(0);
   }
};

} // anonymous namespace

// Once the temporary file exists, a failure (here setting its mode) is
// reported rather than falling back to truncating the target in place.
TEST(FileSerializerTest, WriteStringAtomicFailureAfterTempCreationKeepsOriginal)
{
   AtomicWriteFailureReset reset;
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("state.json");
   ASSERT_FALSE(writeStringToFile(filePath, "original\n"));

   setAtomicWriteChmodFailureForTesting(EPERM);
   Error error = writeStringToFileAtomic(filePath, "replaced\n");
   ASSERT_TRUE(error);
   EXPECT_EQ(EPERM, error.getCode());
   EXPECT_EQ(filePath.getAbsolutePath(), error.getProperty("path"));

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("original\n", readback);
   EXPECT_EQ(0, countAtomicWriteTempFiles(dir));

   dir.remove();
}

// A target that can be written but not renamed over (a bind-mounted file) is
// written in place, unless the caller has turned the fallback off.
TEST(FileSerializerTest, WriteStringAtomicRenameFailureFallsBackToInPlace)
{
   AtomicWriteFailureReset reset;
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("rstudio-prefs.json");
   ASSERT_FALSE(writeStringToFile(filePath, "original\n"));

   setAtomicWriteRenameFailureForTesting(EBUSY);
   ASSERT_FALSE(writeStringToFileAtomic(filePath, "replaced\n"));

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("replaced\n", readback);
   EXPECT_EQ(0, countAtomicWriteTempFiles(dir));

   AtomicWriteOptions options;
   options.allowInPlaceFallback = false;
   Error error = writeStringToFileAtomic(filePath, "again\n", string_utils::LineEndingPassthrough, options);
   ASSERT_TRUE(error);
   EXPECT_EQ(EBUSY, error.getCode());
   EXPECT_EQ(filePath.getAbsolutePath(), error.getProperty("path"));

   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("replaced\n", readback);
   EXPECT_EQ(0, countAtomicWriteTempFiles(dir));

   dir.remove();
}

#endif // RSTUDIO_UNIT_TESTS_ENABLED

// Writing through a symlink (e.g. a dotfile manager's link to
// rstudio-prefs.json) replaces the file it points to and keeps the link.
TEST(FileSerializerTest, WriteStringAtomicFollowsSymlinks)
{
   FilePath dir = scratchDir();
   FilePath targetDir = dir.completePath("dotfiles");
   FilePath linkDir = dir.completePath("config");
   ASSERT_FALSE(targetDir.ensureDirectory());
   ASSERT_FALSE(linkDir.ensureDirectory());

   FilePath target = targetDir.completePath("rstudio-prefs.json");
   FilePath link = linkDir.completePath("rstudio-prefs.json");
   ASSERT_FALSE(writeStringToFile(target, "original\n"));

   // a relative link, which resolves against the link's own directory
   ASSERT_EQ(0, ::symlink("../dotfiles/rstudio-prefs.json", link.getAbsolutePath().c_str()));

   ASSERT_FALSE(writeStringToFileAtomic(link, "replaced\n"));

   EXPECT_TRUE(link.isSymlink());
   std::string readback;
   EXPECT_FALSE(readStringFromFile(target, &readback));
   EXPECT_EQ("replaced\n", readback);
   EXPECT_EQ(0, countAtomicWriteTempFiles(targetDir));
   EXPECT_EQ(0, countAtomicWriteTempFiles(linkDir));

   dir.remove();
}

TEST(FileSerializerTest, WriteStringAtomicCreatesDanglingSymlinkTarget)
{
   FilePath dir = scratchDir();
   FilePath target = dir.completePath("target.json");
   FilePath link = dir.completePath("link.json");
   ASSERT_EQ(0, ::symlink(target.getAbsolutePath().c_str(), link.getAbsolutePath().c_str()));

   ASSERT_FALSE(writeStringToFileAtomic(link, "created\n"));

   EXPECT_TRUE(link.isSymlink());
   std::string readback;
   EXPECT_FALSE(readStringFromFile(target, &readback));
   EXPECT_EQ("created\n", readback);

   dir.remove();
}

TEST(FileSerializerTest, WriteStringAtomicRejectsSymlinkLoop)
{
   FilePath dir = scratchDir();
   FilePath a = dir.completePath("a");
   FilePath b = dir.completePath("b");
   ASSERT_EQ(0, ::symlink(b.getAbsolutePath().c_str(), a.getAbsolutePath().c_str()));
   ASSERT_EQ(0, ::symlink(a.getAbsolutePath().c_str(), b.getAbsolutePath().c_str()));

   EXPECT_TRUE(writeStringToFileAtomic(a, "contents"));
   EXPECT_TRUE(a.isSymlink());
   EXPECT_TRUE(b.isSymlink());

   dir.remove();
}

// .Rhistory and similar user files are rewritten in place, which keeps other
// links to the file pointing at the new contents.
TEST(FileSerializerTest, WriteCollectionInPlaceKeepsHardLinks)
{
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath(".Rhistory");
   FilePath hardLink = dir.completePath("history-link");
   ASSERT_FALSE(writeStringToFile(filePath, "old\n"));
   ASSERT_EQ(0, ::link(filePath.getAbsolutePath().c_str(), hardLink.getAbsolutePath().c_str()));

   std::vector<std::string> lines = { "new" };
   ASSERT_FALSE(writeCollectionToFile<std::vector<std::string>>(filePath, lines, stringifyString, false /* atomic */));

   std::string readback;
   EXPECT_FALSE(readStringFromFile(hardLink, &readback));
   EXPECT_EQ("new\n", readback);

   dir.remove();
}

#endif

// Regression test for #17833: a write that fails (e.g. a full disk or an
// exceeded disk quota) must be reported as an error rather than silently
// appearing to succeed. We use /dev/full, which always fails writes with
// ENOSPC, to simulate the condition. The device is only available on Linux,
// so the test is skipped elsewhere. We exercise the durable path (matching the
// document-save call site) and disable error logging so the expected failure
// does not pollute the test output.
TEST(FileSerializerTest, WriteStringReportsFailedWrite)
{
   FilePath devFull("/dev/full");
   if (!devFull.exists())
      GTEST_SKIP() << "/dev/full not available on this platform";

   Error error = writeStringToFile(devFull,
                                   "this write should fail",
                                   string_utils::LineEndingPassthrough,
                                   true /* truncate */,
                                   0 /* maxOpenRetrySeconds */,
                                   false /* logError */,
                                   true /* durable */);
   EXPECT_TRUE(error);
}

#ifdef _WIN32

// Our readers open with FILE_SHARE_DELETE, and the replacement uses the
// POSIX-semantics rename, so a file another part of RStudio is reading can
// still be replaced; that reader keeps seeing the old contents.
TEST(FileSerializerTest, WriteStringAtomicReplacesFileHeldOpenForRead)
{
   FilePath dir = scratchDir();
   FilePath filePath = dir.completePath("state.json");
   ASSERT_FALSE(writeStringToFileAtomic(filePath, "old\n"));

   std::shared_ptr<std::istream> pOldReader;
   ASSERT_FALSE(filePath.openForRead(pOldReader));

   Error error = writeStringToFileAtomic(filePath, "new\n");
   EXPECT_FALSE(error) << error.asString();

   std::string oldContents;
   std::getline(*pOldReader, oldContents);
   EXPECT_EQ("old", oldContents);
   pOldReader.reset();

   std::string readback;
   EXPECT_FALSE(readStringFromFile(filePath, &readback));
   EXPECT_EQ("new\n", readback);
   EXPECT_EQ(0, countAtomicWriteTempFiles(dir));

   dir.remove();
}

#endif

// isDiskSpaceError must recognize the full-disk / over-quota error codes (so a
// raw write failure can be turned into a recovery-oriented message) and must
// not misclassify unrelated errors or success.
TEST(FileSerializerTest, IsDiskSpaceErrorClassifies)
{
   EXPECT_FALSE(isDiskSpaceError(Success()));

#ifdef _WIN32
   EXPECT_TRUE(isDiskSpaceError(systemError(ERROR_DISK_FULL, ERROR_LOCATION)));
   EXPECT_TRUE(isDiskSpaceError(systemError(ERROR_HANDLE_DISK_FULL, ERROR_LOCATION)));
#else
   EXPECT_TRUE(isDiskSpaceError(systemError(ENOSPC, ERROR_LOCATION)));
# ifdef EDQUOT
   EXPECT_TRUE(isDiskSpaceError(systemError(EDQUOT, ERROR_LOCATION)));
# endif
   // an unrelated system error is not a disk-space error
   EXPECT_FALSE(isDiskSpaceError(systemError(ENOENT, ERROR_LOCATION)));
#endif
}

} // namespace core
} // namespace rstudio
