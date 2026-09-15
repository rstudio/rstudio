/*
 * SessionHttpConnectionListenerTests.cpp
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
#include "SessionHttpConnectionListenerImpl.hpp"

#include <cerrno>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace tests {

// Boost.Asio reports accept errors in the platform's native category: errno values on
// POSIX, Win32/Winsock values on Windows. Feed handleAccept the codes the OS would really
// report, so its errc comparisons are exercised the way they are in production.
#ifdef _WIN32
constexpr int kOutOfMemoryError = ERROR_NOT_ENOUGH_MEMORY;
constexpr int kTooManyFilesOpenError = WSAEMFILE;
constexpr int kTryAgainError = WSAEWOULDBLOCK;
#else
constexpr int kOutOfMemoryError = ENOMEM;
constexpr int kTooManyFilesOpenError = EMFILE;
constexpr int kTryAgainError = EAGAIN;
#endif

class HttpConnectionListenerTester
   : public HttpConnectionListenerImpl<boost::asio::ip::tcp>
{
public:
   using Connection = HttpConnectionImpl<boost::asio::ip::tcp>;
   using AcceptorService = core::http::SocketAcceptorService<boost::asio::ip::tcp>;

   core::Error loggedError;

   int testHandleAccept(int nativeErrorCode)
   {
      loggedError = core::Error();
      exitStatus = 0;

      handleAccept(boost::system::error_code(nativeErrorCode, boost::system::system_category()));

      return exitStatus;
   }

protected:
   int exitStatus = 0;

   virtual void exitEarly(int status) override
   {
      exitStatus = status;
   }

   virtual void acceptNextConnection() override
   {
      // do nothing
   }

private:
   virtual void logAcceptError(const core::Error& in_error, const core::ErrorLocation&) override
   {
      loggedError = in_error;
   }

   virtual core::Error initializeAcceptor(AcceptorService*) override
   {
      return core::Error();
   }

   virtual bool validateConnection(boost::shared_ptr<Connection>) override
   {
      return true;
   }

   virtual core::Error cleanup() override
   {
      return core::Error();
   }
};


TEST(HttpConnectionListenerTest, DetectsConsecutiveErrors) {
   HttpConnectionListenerTester tester;

   for (int i = 1; i <= 100; i++) {
      int status = tester.testHandleAccept(kTryAgainError);
      if (i <= 10 || i == 25 || i == 50 || i == 75 || i == 100) {
         EXPECT_TRUE(tester.loggedError);
      } else {
         EXPECT_FALSE(tester.loggedError);
      }
      EXPECT_EQ(status, 0);
   }
   int status = tester.testHandleAccept(kTryAgainError);
   EXPECT_TRUE(tester.loggedError);
   EXPECT_EQ(status, EXIT_FAILURE);
}

TEST(HttpConnectionListenerTest, DetectsOOMCondition) {
   HttpConnectionListenerTester tester;

   // First make sure that it runs the first hundred iterations without a problem
   int returnedErrorCount = 0;
   for (int i = 0; i < 100; i++) {
      int status = tester.testHandleAccept(kOutOfMemoryError);
      if (status) {
         ++returnedErrorCount;
      }
   }
   EXPECT_EQ(returnedErrorCount, 0);

   // Bail out on the 101st iteration with the correct error code
   int status = tester.testHandleAccept(kOutOfMemoryError);
   EXPECT_EQ(status, SESSION_EXIT_NOT_ENOUGH_MEMORY);
}

TEST(HttpConnectionListenerTest, DetectsFDExhaustion) {
   HttpConnectionListenerTester tester;

   // First make sure that it runs the first hundred iterations without a problem
   int returnedErrorCount = 0;
   for (int i = 0; i < 100; i++) {
      int status = tester.testHandleAccept(kTooManyFilesOpenError);
      if (status) {
         ++returnedErrorCount;
      }
   }
   EXPECT_EQ(returnedErrorCount, 0);

   // Bail out on the 101st iteration with the correct error code
   int status = tester.testHandleAccept(kTooManyFilesOpenError);
   EXPECT_EQ(status, SESSION_EXIT_TOO_MANY_OPEN_FILES);
}

} // end namespace tests
} // end namespace session
} // end namespace rstudio
