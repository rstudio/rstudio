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
#include "SessionTcpIpHttpConnectionListener.hpp"

#include <atomic>
#include <cerrno>
#include <memory>
#include <string>

#include <boost/asio/read_until.hpp>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/write.hpp>

#include <gtest/gtest.h>

#include "../SessionUriHandlers.hpp"

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

namespace {

// handlers match by prefix, so this must not start with a registered one (e.g. "/upload")
const char* const kUploadTestUri = "/test-upload-auth";
const char* const kUploadTestSecret = "upload-auth-test-secret";

std::atomic<int> s_uploadHandlerCalls(0);

bool countingUploadHandler(const core::http::Request&,
                           const std::string&,
                           bool complete,
                           const core::http::UriHandlerFunctionContinuation& cont)
{
   ++s_uploadHandlerCalls;

   if (complete)
   {
      core::http::Response response;
      response.setStatusCode(core::http::status::Ok);
      cont(&response);
   }

   return true;
}

class UploadAuthenticationTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      static bool registered = false;
      if (!registered)
      {
         core::http::UriAsyncUploadHandlerFunction handler = countingUploadHandler;
         uri_handlers::handlers().add(core::http::UriHandler(kUploadTestUri, handler));
         registered = true;
      }

      s_uploadHandlerCalls = 0;

      listener_.reset(new TcpIpHttpConnectionListener("127.0.0.1", "0", kUploadTestSecret));
      ASSERT_FALSE(listener_->start());
   }

   void TearDown() override
   {
      if (listener_)
         listener_->stop();
   }

   // POSTs a small multipart form to the upload URI, sending the shared secret
   // if one is given, and returns the response's status line
   std::string postUpload(const std::string& secret)
   {
      std::string boundary = "UploadAuthTestBoundary";
      std::string body =
            "--" + boundary + "\r\n"
            "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n"
            "\r\n"
            "data\r\n"
            "--" + boundary + "--\r\n";

      std::string request =
            "POST " + std::string(kUploadTestUri) + " HTTP/1.1\r\n"
            "Host: 127.0.0.1\r\n"
            "Content-Type: multipart/form-data; boundary=" + boundary + "\r\n"
            "Content-Length: " + std::to_string(body.size()) + "\r\n"
            "Connection: close\r\n";
      if (!secret.empty())
         request += "X-Shared-Secret: " + secret + "\r\n";
      request += "\r\n" + body;

      boost::asio::io_context ioContext;
      boost::asio::ip::tcp::socket socket(ioContext);
      socket.connect(listener_->getLocalEndpoint());

      // fail rather than hang if the listener never answers
#ifdef _WIN32
      DWORD timeoutMs = 10000;
      setsockopt(socket.native_handle(), SOL_SOCKET, SO_RCVTIMEO,
                 reinterpret_cast<const char*>(&timeoutMs), sizeof(timeoutMs));
#else
      struct timeval timeout = { 10, 0 };
      setsockopt(socket.native_handle(), SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
#endif

      boost::asio::write(socket, boost::asio::buffer(request));

      boost::system::error_code ec;
      boost::asio::streambuf responseBuffer;
      boost::asio::read_until(socket, responseBuffer, "\r\n", ec);
      if (ec)
         return "read error: " + ec.message();

      std::istream responseStream(&responseBuffer);
      std::string statusLine;
      std::getline(responseStream, statusLine);
      return statusLine;
   }

   std::unique_ptr<TcpIpHttpConnectionListener> listener_;
};

} // anonymous namespace

TEST_F(UploadAuthenticationTest, RefusesUploadWithoutSharedSecret) {
   std::string statusLine = postUpload("");
   EXPECT_NE(statusLine.find(" 403 "), std::string::npos) << statusLine;
   EXPECT_EQ(s_uploadHandlerCalls, 0);
}

TEST_F(UploadAuthenticationTest, RefusesUploadWithWrongSharedSecret) {
   std::string statusLine = postUpload("not-the-secret");
   EXPECT_NE(statusLine.find(" 403 "), std::string::npos) << statusLine;
   EXPECT_EQ(s_uploadHandlerCalls, 0);
}

TEST_F(UploadAuthenticationTest, AcceptsUploadWithSharedSecret) {
   std::string statusLine = postUpload(kUploadTestSecret);
   EXPECT_NE(statusLine.find(" 200 "), std::string::npos) << statusLine;
   EXPECT_GT(s_uploadHandlerCalls, 0);
}

} // end namespace tests
} // end namespace session
} // end namespace rstudio
