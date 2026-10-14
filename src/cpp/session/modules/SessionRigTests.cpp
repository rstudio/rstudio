/*
 * SessionRigTests.cpp
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

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>

#include "SessionRig.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rig {

namespace {

// SHA-256 of "hello"
const char* const kHelloDigest = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

class VerifyArchiveDigestTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(".tar.gz", archive_));
      ASSERT_FALSE(writeStringToFile(archive_, "hello"));
   }

   void TearDown() override
   {
      archive_.removeIfExists();
   }

   FilePath archive_;
};

} // anonymous namespace

TEST_F(VerifyArchiveDigestTest, AcceptsAMatchingDigestInEitherCase)
{
   EXPECT_FALSE(verifyArchiveDigest(archive_, kHelloDigest));

   std::string upper(kHelloDigest);
   for (char& ch : upper)
      ch = static_cast<char>(toupper(ch));
   EXPECT_FALSE(verifyArchiveDigest(archive_, upper));
}

TEST_F(VerifyArchiveDigestTest, RejectsAMismatchNamingBothDigests)
{
   std::string tampered(kHelloDigest);
   tampered[0] = tampered[0] == '0' ? '1' : '0';

   Error error = verifyArchiveDigest(archive_, tampered);
   ASSERT_TRUE(error);
   EXPECT_EQ(error.getProperty("expected"), tampered);
   EXPECT_EQ(error.getProperty("actual"), kHelloDigest);
}

TEST_F(VerifyArchiveDigestTest, RejectsAnArchiveWithNoKnownDigest)
{
   // nothing can be verified against an empty digest, even a sound archive
   EXPECT_TRUE(verifyArchiveDigest(archive_, ""));
}

TEST_F(VerifyArchiveDigestTest, RejectsAnArchiveThatCannotBeRead)
{
   ASSERT_FALSE(archive_.remove());
   EXPECT_TRUE(verifyArchiveDigest(archive_, kHelloDigest));
}

} // namespace rig
} // namespace modules
} // namespace session
} // namespace rstudio
