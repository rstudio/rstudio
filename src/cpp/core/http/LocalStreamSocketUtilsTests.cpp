/*
 * LocalStreamSocketUtilsTests.cpp
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

#include <core/http/LocalStreamSocketUtils.hpp>

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

using boost::asio::local::stream_protocol;

class LocalStreamSocketUtilsTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(streamPath_));
      ASSERT_FALSE(streamPath_.removeIfExists());
   }

   void TearDown() override
   {
      streamPath_.removeIfExists();
   }

   bool listening()
   {
      bool listening = true;
      Error error = isLocalStreamListening(streamPath_, &listening);
      EXPECT_FALSE(error) << error.asString();
      return listening;
   }

   FilePath streamPath_;
};

} // anonymous namespace

TEST_F(LocalStreamSocketUtilsTest, MissingPathIsNotListening)
{
   EXPECT_FALSE(listening());
}

TEST_F(LocalStreamSocketUtilsTest, RegularFileIsNotListening)
{
   ASSERT_FALSE(writeStringToFile(streamPath_, "not a socket"));
   EXPECT_FALSE(listening());
}

TEST_F(LocalStreamSocketUtilsTest, BoundSocketIsListening)
{
   SocketAcceptorService<stream_protocol> service;
   ASSERT_FALSE(initLocalStreamAcceptor(service, streamPath_, FileMode::USER_READ_WRITE));
   EXPECT_TRUE(listening());
}

TEST_F(LocalStreamSocketUtilsTest, SocketLeftByClosedListenerIsNotListening)
{
   SocketAcceptorService<stream_protocol> service;
   ASSERT_FALSE(initLocalStreamAcceptor(service, streamPath_, FileMode::USER_READ_WRITE));

   // closing the acceptor leaves the socket file behind, as a crashed or
   // killed listener would
   boost::system::error_code ec;
   service.closeAcceptor(ec);
   ASSERT_FALSE(ec);
   ASSERT_TRUE(streamPath_.exists());

   EXPECT_FALSE(listening());
}

TEST_F(LocalStreamSocketUtilsTest, IdentityDistinguishesReboundSockets)
{
   SocketAcceptorService<stream_protocol> first;
   ASSERT_FALSE(initLocalStreamAcceptor(first, streamPath_, FileMode::USER_READ_WRITE));

   LocalStreamIdentity firstIdentity;
   ASSERT_FALSE(getLocalStreamIdentity(streamPath_, &firstIdentity));

   // the identity survives the listener closing
   boost::system::error_code ec;
   first.closeAcceptor(ec);
   ASSERT_FALSE(ec);

   LocalStreamIdentity closedIdentity;
   ASSERT_FALSE(getLocalStreamIdentity(streamPath_, &closedIdentity));
   EXPECT_EQ(firstIdentity, closedIdentity);

   // a socket bound at the same path by someone else is a different object
   ASSERT_FALSE(streamPath_.remove());
   SocketAcceptorService<stream_protocol> second;
   ASSERT_FALSE(initLocalStreamAcceptor(second, streamPath_, FileMode::USER_READ_WRITE));

   LocalStreamIdentity secondIdentity;
   ASSERT_FALSE(getLocalStreamIdentity(streamPath_, &secondIdentity));
   EXPECT_NE(firstIdentity, secondIdentity);
}

TEST_F(LocalStreamSocketUtilsTest, ClaimBindsMissingPath)
{
   SocketAcceptorService<stream_protocol> service;
   LocalStreamIdentity identity;
   ASSERT_FALSE(claimLocalStream(service, streamPath_, FileMode::USER_READ_WRITE, &identity));

   EXPECT_TRUE(listening());
   LocalStreamIdentity current;
   ASSERT_FALSE(getLocalStreamIdentity(streamPath_, &current));
   EXPECT_EQ(current, identity);
}

TEST_F(LocalStreamSocketUtilsTest, ClaimRefusesLiveListener)
{
   SocketAcceptorService<stream_protocol> live;
   LocalStreamIdentity liveIdentity;
   ASSERT_FALSE(claimLocalStream(live, streamPath_, FileMode::USER_READ_WRITE, &liveIdentity));

   SocketAcceptorService<stream_protocol> duplicate;
   LocalStreamIdentity duplicateIdentity;
   Error error = claimLocalStream(duplicate, streamPath_, FileMode::USER_READ_WRITE, &duplicateIdentity);
   ASSERT_TRUE(error);
   EXPECT_EQ(error, boost::asio::error::make_error_code(boost::asio::error::address_in_use))
         << error.asString();

   // the live listener's socket is untouched
   EXPECT_TRUE(listening());
   LocalStreamIdentity current;
   ASSERT_FALSE(getLocalStreamIdentity(streamPath_, &current));
   EXPECT_EQ(current, liveIdentity);
}

TEST_F(LocalStreamSocketUtilsTest, ClaimReplacesStaleSocket)
{
   SocketAcceptorService<stream_protocol> stale;
   LocalStreamIdentity staleIdentity;
   ASSERT_FALSE(claimLocalStream(stale, streamPath_, FileMode::USER_READ_WRITE, &staleIdentity));
   boost::system::error_code ec;
   stale.closeAcceptor(ec);
   ASSERT_FALSE(ec);
   ASSERT_TRUE(streamPath_.exists());

   SocketAcceptorService<stream_protocol> service;
   LocalStreamIdentity identity;
   ASSERT_FALSE(claimLocalStream(service, streamPath_, FileMode::USER_READ_WRITE, &identity));
   EXPECT_TRUE(listening());
   EXPECT_NE(identity, staleIdentity);
}

TEST_F(LocalStreamSocketUtilsTest, IdentityOfMissingPathIsAnError)
{
   LocalStreamIdentity identity;
   Error error = getLocalStreamIdentity(streamPath_, &identity);
   ASSERT_TRUE(error);
   EXPECT_EQ(error, systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()));
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio

#endif // !_WIN32
