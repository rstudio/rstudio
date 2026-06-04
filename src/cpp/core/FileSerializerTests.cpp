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

#include <core/FileSerializer.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#ifdef _WIN32
# include <windows.h>
#else
# include <cerrno>
#endif

namespace rstudio {
namespace core {

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
