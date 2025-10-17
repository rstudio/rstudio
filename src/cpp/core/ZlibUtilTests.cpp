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

namespace rstudio {
namespace core {
namespace zlib {

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

} // namespace zlib
} // namespace core
} // namespace rstudio
