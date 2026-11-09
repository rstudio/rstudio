/*
 * ServerPathsTests.cpp
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

#include <core/http/LocalStreamSocketUtils.hpp>
#include <server/ServerPaths.hpp>

namespace rstudio {
namespace server {
namespace tests {

TEST(ServerPathsTest, OwnershipPassPreservesSessionClaimLock)
{
   FilePath serverData;
   ASSERT_FALSE(FilePath::tempFilePath(serverData));
   ASSERT_FALSE(serverData.removeIfExists());
   ASSERT_FALSE(serverData.ensureDirectory());
   BOOST_SCOPE_EXIT(&serverData)
   {
      serverData.removeIfExists();
   }
   BOOST_SCOPE_EXIT_END

   FilePath streamDir = serverData.completeChildPath(kSessionTmpDir);
   ASSERT_FALSE(streamDir.ensureDirectory());
   FilePath streamPath = streamDir.completeChildPath("user-d");
   FilePath lockPath(streamPath.getAbsolutePath() + ".lock");
   using boost::asio::local::stream_protocol;
   core::http::LocalStreamIdentity identity;
   {
      core::http::SocketAcceptorService<stream_protocol> service;
      ASSERT_FALSE(core::http::claimLocalStream(
         service,
         streamPath,
         FileMode::USER_READ_WRITE,
         &identity));
   }

   core::system::User user;
   ASSERT_FALSE(core::system::User::getCurrentUser(user));
   bool sawLock = false;
   bool chownLock = false;
   ASSERT_FALSE(serverData.changeOwnership(
      user,
      true,
      [&](int depth, const FilePath& file)
      {
         bool shouldChown = shouldChownServerDataFile(depth, file);
         if (file == lockPath)
         {
            sawLock = true;
            chownLock = shouldChown;
         }
         return shouldChown;
      }));
   EXPECT_TRUE(sawLock);
   EXPECT_FALSE(chownLock);

   // The ownership pass used on restart must leave the persistent lock
   // available to the next launch of the same user's session.
   core::http::SocketAcceptorService<stream_protocol> next;
   EXPECT_FALSE(core::http::claimLocalStream(
      next,
      streamPath,
      FileMode::USER_READ_WRITE,
      &identity));
}

} // namespace tests
} // namespace server
} // namespace rstudio
