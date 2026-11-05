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

#include <array>
#include <thread>

#include <poll.h>
#include <signal.h>
#include <sys/wait.h>
#include <unistd.h>

#include <boost/thread/barrier.hpp>

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
      pinPath_ = FilePath(streamPath_.getAbsolutePath() + ".pin");
      lockPath_ = FilePath(streamPath_.getAbsolutePath() + ".lock");
   }

   void TearDown() override
   {
      streamPath_.removeIfExists();
      pinPath_.removeIfExists();
      lockPath_.removeIfExists();
   }

   // Linux hands a freed inode number straight to the next file created, so
   // a socket bound after the old one is unlinked can get the old identity.
   // A hard link keeps the old socket's inode allocated, as a live listener's
   // open socket does in practice.
   void pinInode()
   {
      int result = ::link(streamPath_.getAbsolutePath().c_str(), pinPath_.getAbsolutePath().c_str());
      ASSERT_EQ(result, 0) << "link() failed: errno " << errno;
   }

   bool listening()
   {
      bool listening = true;
      Error error = isLocalStreamListening(streamPath_, &listening);
      EXPECT_FALSE(error) << error.asString();
      return listening;
   }

   void checkConcurrentClaims(bool stale)
   {
      for (int attempt = 0; attempt < 25; ++attempt)
      {
         SCOPED_TRACE(attempt);
         ASSERT_FALSE(streamPath_.removeIfExists());
         if (stale)
         {
            SocketAcceptorService<stream_protocol> service;
            ASSERT_FALSE(initLocalStreamAcceptor(service, streamPath_, FileMode::USER_READ_WRITE));
         }

         constexpr int count = 8;
         boost::barrier start(count);
         std::array<SocketAcceptorService<stream_protocol>, count> services;
         std::array<LocalStreamIdentity, count> identities;
         std::array<Error, count> errors;
         std::array<std::thread, count> threads;
         for (int i = 0; i < count; ++i)
         {
            threads[i] = std::thread([&, i]()
            {
               start.wait();
               errors[i] = claimLocalStream(
                  services[i],
                  streamPath_,
                  FileMode::USER_READ_WRITE,
                  &identities[i]);
            });
         }
         for (auto& thread : threads)
            thread.join();

         // Keep all acceptors open until every claim finishes. Exactly one
         // must succeed, and the path must still identify that listener.
         int claimed = 0;
         LocalStreamIdentity current;
         ASSERT_FALSE(getLocalStreamIdentity(streamPath_, &current));
         for (int i = 0; i < count; ++i)
         {
            if (!errors[i])
            {
               ++claimed;
               EXPECT_EQ(identities[i], current);
            }
            else
            {
               EXPECT_EQ(errors[i], boost::asio::error::make_error_code(boost::asio::error::address_in_use))
                     << errors[i].asString();
            }
         }
         ASSERT_EQ(claimed, 1);
         EXPECT_TRUE(listening());
      }
   }

   FilePath streamPath_;
   FilePath pinPath_;
   FilePath lockPath_;
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
   ASSERT_NO_FATAL_FAILURE(pinInode());
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
   ASSERT_NO_FATAL_FAILURE(pinInode());

   SocketAcceptorService<stream_protocol> service;
   LocalStreamIdentity identity;
   ASSERT_FALSE(claimLocalStream(service, streamPath_, FileMode::USER_READ_WRITE, &identity));
   EXPECT_TRUE(listening());
   EXPECT_NE(identity, staleIdentity);
}

TEST_F(LocalStreamSocketUtilsTest, ClaimedSocketIsNotInherited)
{
   SocketAcceptorService<stream_protocol> service;
   LocalStreamIdentity identity;
   ASSERT_FALSE(claimLocalStream(service, streamPath_, FileMode::USER_READ_WRITE, &identity));

   int flags = ::fcntl(service.acceptor().native_handle(), F_GETFD);
   ASSERT_NE(flags, -1);
   EXPECT_TRUE(flags & FD_CLOEXEC);
}

TEST_F(LocalStreamSocketUtilsTest, ConcurrentClaimsOfMissingPathHaveOneOwner)
{
   checkConcurrentClaims(false);
}

TEST_F(LocalStreamSocketUtilsTest, ConcurrentClaimsOfStaleSocketHaveOneOwner)
{
   checkConcurrentClaims(true);
}

TEST_F(LocalStreamSocketUtilsTest, ClaimWaitsForAnotherProcess)
{
   int lockFd = ::open(lockPath_.getAbsolutePath().c_str(), O_CREAT | O_RDWR, 0600);
   ASSERT_NE(lockFd, -1);
   BOOST_SCOPE_EXIT(lockFd)
   {
      ::close(lockFd);
   }
   BOOST_SCOPE_EXIT_END
   ASSERT_EQ(::flock(lockFd, LOCK_EX), 0);

   int ready[2];
   ASSERT_EQ(::pipe(ready), 0);
   BOOST_SCOPE_EXIT(&ready)
   {
      ::close(ready[0]);
      if (ready[1] != -1)
         ::close(ready[1]);
   }
   BOOST_SCOPE_EXIT_END

   pid_t child = ::fork();
   ASSERT_NE(child, -1);
   if (child == 0)
   {
      ::close(ready[0]);
      ::close(lockFd);
      // Bound the test even if a regression leaves the child waiting.
      ::signal(SIGALRM, SIG_DFL);
      ::alarm(5);
      if (::write(ready[1], "r", 1) != 1)
         ::_exit(2);

      SocketAcceptorService<stream_protocol> service;
      LocalStreamIdentity identity;
      Error error = claimLocalStream(service, streamPath_, FileMode::USER_READ_WRITE, &identity);
      ::_exit(error ? 1 : 0);
   }

   ::close(ready[1]);
   ready[1] = -1;
   char byte;
   EXPECT_EQ(::read(ready[0], &byte, 1), 1);

   // The child's pipe reaches EOF when its claim finishes. It must not
   // finish or touch the socket while this process holds the claim lock.
   struct pollfd finished = {ready[0], POLLIN, 0};
   EXPECT_EQ(::poll(&finished, 1, 200), 0);
   EXPECT_FALSE(streamPath_.exists());
   EXPECT_EQ(::flock(lockFd, LOCK_UN), 0);

   int status = 0;
   EXPECT_EQ(::waitpid(child, &status, 0), child);
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(WEXITSTATUS(status), 0);
}

TEST_F(LocalStreamSocketUtilsTest, ClaimsLeaveTheSameUnlockedFile)
{
   SocketAcceptorService<stream_protocol> live;
   LocalStreamIdentity identity;
   ASSERT_FALSE(claimLocalStream(live, streamPath_, FileMode::USER_READ_WRITE, &identity));

   LocalStreamIdentity lockIdentity;
   ASSERT_FALSE(getLocalStreamIdentity(lockPath_, &lockIdentity));
   int lockFd = ::open(lockPath_.getAbsolutePath().c_str(), O_RDWR);
   ASSERT_NE(lockFd, -1);
   BOOST_SCOPE_EXIT(lockFd)
   {
      ::close(lockFd);
   }
   BOOST_SCOPE_EXIT_END
   ASSERT_EQ(::flock(lockFd, LOCK_EX | LOCK_NB), 0);
   ASSERT_EQ(::flock(lockFd, LOCK_UN), 0);

   SocketAcceptorService<stream_protocol> duplicate;
   EXPECT_TRUE(claimLocalStream(duplicate, streamPath_, FileMode::USER_READ_WRITE, &identity));
   EXPECT_EQ(::flock(lockFd, LOCK_EX | LOCK_NB), 0);
   LocalStreamIdentity current;
   ASSERT_FALSE(getLocalStreamIdentity(lockPath_, &current));
   EXPECT_EQ(lockIdentity, current);
}

TEST_F(LocalStreamSocketUtilsTest, ClaimRefusesSymlinkLockFile)
{
   ASSERT_FALSE(writeStringToFile(pinPath_, "untouched"));
   ASSERT_EQ(::symlink(pinPath_.getAbsolutePath().c_str(), lockPath_.getAbsolutePath().c_str()), 0);

   SocketAcceptorService<stream_protocol> service;
   LocalStreamIdentity identity;
   EXPECT_TRUE(claimLocalStream(service, streamPath_, FileMode::USER_READ_WRITE, &identity));
   EXPECT_FALSE(streamPath_.exists());
   std::string contents;
   ASSERT_FALSE(readStringFromFile(pinPath_, &contents));
   EXPECT_EQ(contents, "untouched");
}

TEST_F(LocalStreamSocketUtilsTest, ClaimRefusesNonRegularLockFile)
{
   ASSERT_EQ(::mkfifo(lockPath_.getAbsolutePath().c_str(), 0600), 0);

   SocketAcceptorService<stream_protocol> service;
   LocalStreamIdentity identity;
   EXPECT_TRUE(claimLocalStream(service, streamPath_, FileMode::USER_READ_WRITE, &identity));
   EXPECT_FALSE(streamPath_.exists());
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
