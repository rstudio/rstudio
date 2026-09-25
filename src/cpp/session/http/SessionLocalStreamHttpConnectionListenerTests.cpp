/*
 * SessionLocalStreamHttpConnectionListenerTests.cpp
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

#ifndef _WIN32

#include "SessionLocalStreamHttpConnectionListener.hpp"

#include <gtest/gtest.h>

#include <boost/asio/error.hpp>

#include <core/FileSerializer.hpp>

namespace rstudio {
namespace session {
namespace tests {

namespace {

using namespace rstudio::core;
using boost::asio::local::stream_protocol;

const char* const kForeignPid = "12345";

class LocalStreamHttpConnectionListenerTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(streamPath_));
      ASSERT_FALSE(streamPath_.removeIfExists());
      pidPath_ = FilePath(streamPath_.getAbsolutePath() + ".pid");
   }

   void TearDown() override
   {
      streamPath_.removeIfExists();
      pidPath_.removeIfExists();
   }

   std::unique_ptr<LocalStreamHttpConnectionListener> newListener()
   {
      return std::unique_ptr<LocalStreamHttpConnectionListener>(
               new LocalStreamHttpConnectionListener(streamPath_,
                                                     FileMode::USER_READ_WRITE,
                                                     "",
                                                     -1));
   }

   bool inUse()
   {
      bool inUse = true;
      Error error = http::isLocalStreamInUse(streamPath_, &inUse);
      EXPECT_FALSE(error) << error.asString();
      return inUse;
   }

   http::LocalStreamIdentity identity()
   {
      http::LocalStreamIdentity identity;
      Error error = http::getLocalStreamIdentity(streamPath_, &identity);
      EXPECT_FALSE(error) << error.asString();
      return identity;
   }

   // binds a listener at the stream path the way a session that knows
   // nothing of this one would: by unlinking whatever is there first
   void bindForeignListener(http::SocketAcceptorService<stream_protocol>* pService)
   {
      ASSERT_FALSE(streamPath_.removeIfExists());
      ASSERT_FALSE(http::initLocalStreamAcceptor(*pService, streamPath_, FileMode::USER_READ_WRITE));
      ASSERT_FALSE(writeStringToFile(pidPath_, kForeignPid));
   }

   FilePath streamPath_;
   FilePath pidPath_;
};

} // anonymous namespace

TEST_F(LocalStreamHttpConnectionListenerTest, StartBindsAndStopReleasesTheStream)
{
   auto listener = newListener();
   ASSERT_FALSE(listener->start());
   EXPECT_TRUE(inUse());

   std::string pid;
   ASSERT_FALSE(readStringFromFile(pidPath_, &pid));
   EXPECT_EQ(pid, std::to_string(core::system::currentProcessId()));

   listener->stop();
   EXPECT_FALSE(streamPath_.exists());
   EXPECT_FALSE(pidPath_.exists());
}

TEST_F(LocalStreamHttpConnectionListenerTest, ReplacesStaleStream)
{
   // a socket file left behind by a listener that went away without unlinking
   http::SocketAcceptorService<stream_protocol> stale;
   bindForeignListener(&stale);
   boost::system::error_code ec;
   stale.closeAcceptor(ec);
   ASSERT_FALSE(ec);
   ASSERT_TRUE(streamPath_.exists());
   ASSERT_FALSE(inUse());

   auto listener = newListener();
   ASSERT_FALSE(listener->start());
   EXPECT_TRUE(inUse());

   std::string pid;
   ASSERT_FALSE(readStringFromFile(pidPath_, &pid));
   EXPECT_EQ(pid, std::to_string(core::system::currentProcessId()));

   listener->stop();
   EXPECT_FALSE(streamPath_.exists());
   EXPECT_FALSE(pidPath_.exists());
}

TEST_F(LocalStreamHttpConnectionListenerTest, RefusesToSupplantLiveListener)
{
   auto first = newListener();
   ASSERT_FALSE(first->start());
   http::LocalStreamIdentity firstIdentity = identity();

   // a duplicate session reports the stream as in use, so that its startup
   // retries and then gives up, instead of taking the stream over
   auto second = newListener();
   Error error = second->start();
   ASSERT_TRUE(error);
   EXPECT_EQ(error, boost::asio::error::make_error_code(boost::asio::error::address_in_use))
         << error.asString();

   // the first listener is untouched, and its pid file still names this process
   EXPECT_TRUE(inUse());
   EXPECT_EQ(identity(), firstIdentity);
   std::string pid;
   ASSERT_FALSE(readStringFromFile(pidPath_, &pid));
   EXPECT_EQ(pid, std::to_string(core::system::currentProcessId()));

   // the duplicate bound nothing, so the first listener still owns the stream
   first->stop();
   EXPECT_FALSE(streamPath_.exists());
   EXPECT_FALSE(pidPath_.exists());
}

TEST_F(LocalStreamHttpConnectionListenerTest, SupplantedListenerLeavesSuccessorInPlace)
{
   auto listener = newListener();
   ASSERT_FALSE(listener->start());

   // a session unaware of this one takes the path over (as sessions did
   // before this check existed); this listener now serves an unlinked socket
   http::SocketAcceptorService<stream_protocol> successor;
   bindForeignListener(&successor);
   http::LocalStreamIdentity successorIdentity = identity();

   // exiting must not take the successor's stream and pid file with it
   listener->stop();
   ASSERT_TRUE(streamPath_.exists());
   EXPECT_EQ(identity(), successorIdentity);
   EXPECT_TRUE(inUse());

   std::string pid;
   ASSERT_FALSE(readStringFromFile(pidPath_, &pid));
   EXPECT_EQ(pid, kForeignPid);
}

TEST_F(LocalStreamHttpConnectionListenerTest, StopToleratesStreamAlreadyRemoved)
{
   auto listener = newListener();
   ASSERT_FALSE(listener->start());

   ASSERT_FALSE(streamPath_.remove());
   listener->stop();
   EXPECT_FALSE(streamPath_.exists());
}

} // namespace tests
} // namespace session
} // namespace rstudio

#endif // !_WIN32
