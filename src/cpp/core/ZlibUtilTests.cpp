/*
* zlibTests.cpp
*
* Copyright (C) 2022 by Posit Software, PBC
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

#include <core/ZlibUtil.hpp>

#include <gtest/gtest.h>

#include <algorithm>
#include <iterator>
#include <sstream>

namespace rstudio {
namespace core {
namespace zlib {

namespace {

const std::string kGzipFixtureContents = "The quick brown fox jumps over the lazy dog.\n";

// 'gzip -n' output for kGzipFixtureContents
const unsigned char kGzipFixture[] = {
   31, 139, 8, 0, 0, 0, 0, 0, 0, 3, 11, 201, 72, 85, 40, 44,
   205, 76, 206, 86, 72, 42, 202, 47, 207, 83, 72, 203, 175, 80, 200, 42,
   205, 45, 40, 86, 200, 47, 75, 45, 82, 40, 1, 74, 231, 36, 86, 85,
   42, 164, 228, 167, 235, 113, 1, 0, 106, 204, 80, 235, 45, 0, 0, 0
};

std::string gzipFixture(std::size_t length)
{
   return std::string(reinterpret_cast<const char*>(kGzipFixture), length);
}

std::string readAll(std::istream& is)
{
   char buffer[64];
   std::string contents;
   while (is.read(buffer, sizeof(buffer)))
   {
      contents.append(buffer, static_cast<std::size_t>(is.gcount()));
   }
   contents.append(buffer, static_cast<std::size_t>(is.gcount()));

   return contents;
}

} // anonymous namespace

TEST(ZlibTest, CanCompressAndDecompressDifficultStrings)
{
   const std::string hardToCompress = "The quick brown fox jumps over the lazy dog.";

   std::vector<unsigned char> compressed;
   std::string uncompressed;
   Error error = compressString(hardToCompress, &compressed);
   ASSERT_FALSE(error);

   error = decompressString(compressed, &uncompressed);
   ASSERT_FALSE(error);

   EXPECT_EQ(hardToCompress, uncompressed);
}

TEST(ZlibTest, CanCompressAndDecompressEasyStrings)
{
   const std::string easyToCompress = "easy easy easy easy easy easy easy easy easy easy";

   std::vector<unsigned char> compressed;
   std::string uncompressed;
   Error error = compressString(easyToCompress, &compressed);
   ASSERT_FALSE(error);

   error = decompressString(compressed, &uncompressed);
   ASSERT_FALSE(error);

   EXPECT_EQ(easyToCompress, uncompressed);
}

TEST(ZlibTest, CanCompressAndDecompressNormalStrings)
{
   const std::string launcherJobName = "rsl-RStudio s12345678904321 (slurmUser1) - postman test-command=cat-args=-E-stdin=test\nsubmit\njob-us=e53ccc2ab4d74c8595596a90f3d2831a-tags=s12345678904321,rstudio-ide,s12345,rstudio-r-session,rstudio-r-session-name:postman test,rstudio-r-session-id:s12345678904321";

   std::vector<unsigned char> compressed;
   std::string uncompressed;
   Error error = compressString(launcherJobName, &compressed);
   ASSERT_FALSE(error);

   error = decompressString(compressed, &uncompressed);
   ASSERT_FALSE(error);

   EXPECT_EQ(launcherJobName, uncompressed);
}

TEST(ZlibTest, CanCompressAndDecompressEmptyString)
{
   const std::string empty = "";

   std::vector<unsigned char> compressed;
   std::string uncompressed;
   Error error = compressString(empty, &compressed);
   ASSERT_FALSE(error);

   error = decompressString(compressed, &uncompressed);
   ASSERT_FALSE(error);

   EXPECT_EQ(empty, uncompressed);
}

TEST(ZlibTest, InvalidCompressedStringFailsToDecompress)
{
   const std::string invalidCompressed = "H\r:ßÓø";

   std::vector<unsigned char> compressed;
   std::copy(invalidCompressed.begin(), invalidCompressed.end(), std::back_inserter(compressed));

   std::string uncompressed;
   Error error = decompressString(compressed, &uncompressed);
   ASSERT_TRUE(error);
}

TEST(ZlibTest, GzipStreambufDecompressesGzipStream)
{
   std::istringstream source(gzipFixture(sizeof(kGzipFixture)));
   GzipDecompressingStreambuf streambuf(source);
   std::istream is(&streambuf);

   std::string contents = readAll(is);

   EXPECT_FALSE(is.bad());
   EXPECT_EQ(kGzipFixtureContents, contents);
}

TEST(ZlibTest, GzipStreambufDecompressesConcatenatedMembers)
{
   // a gzip stream is a sequence of members; all of them are decompressed
   std::string twoMembers = gzipFixture(sizeof(kGzipFixture)) + gzipFixture(sizeof(kGzipFixture));
   std::istringstream source(twoMembers);
   GzipDecompressingStreambuf streambuf(source);
   std::istream is(&streambuf);

   std::string contents = readAll(is);

   EXPECT_FALSE(is.bad());
   EXPECT_EQ(kGzipFixtureContents + kGzipFixtureContents, contents);
}

TEST(ZlibTest, GzipStreambufSetsBadbitOnCorruptStream)
{
   std::istringstream source("not gzip data");
   GzipDecompressingStreambuf streambuf(source);
   std::istream is(&streambuf);

   readAll(is);

   EXPECT_TRUE(is.bad());
}

TEST(ZlibTest, GzipStreambufSetsBadbitOnTruncatedStream)
{
   // cut mid-deflate, before the trailer
   std::istringstream source(gzipFixture(sizeof(kGzipFixture) - 20));
   GzipDecompressingStreambuf streambuf(source);
   std::istream is(&streambuf);

   readAll(is);

   EXPECT_TRUE(is.bad());
}

TEST(ZlibTest, GzipStreambufSetsBadbitOnMissingTrailer)
{
   // the deflate data is intact, but the 8-byte trailer (CRC + size) is
   // missing: the content decodes, but the stream must still fail
   std::istringstream source(gzipFixture(sizeof(kGzipFixture) - 8));
   GzipDecompressingStreambuf streambuf(source);
   std::istream is(&streambuf);

   readAll(is);

   EXPECT_TRUE(is.bad());
}

} // namespace zlib
} // namespace core
} // namespace rstudio
