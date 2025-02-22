/*
 * LocalStreamAsyncClient.hpp
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

#ifndef CORE_HTTP_LOCAL_STREAM_ASYNC_CLIENT_HPP
#define CORE_HTTP_LOCAL_STREAM_ASYNC_CLIENT_HPP

#include <sys/stat.h>

#include <boost/function.hpp>
#include <boost/optional.hpp>

#include <boost/asio/local/stream_protocol.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/system/PosixUser.hpp>

#include <core/http/AsyncClient.hpp>
#include <core/http/LocalStreamSocketUtils.hpp>

namespace rstudio {
namespace core {
namespace http {  

class LocalStreamAsyncClient
   : public AsyncClient<boost::asio::local::stream_protocol::socket>
{
public:
   LocalStreamAsyncClient(boost::asio::io_context& ioContext,
                          const FilePath localStreamPath,
                          bool logToStderr = false,
                          boost::optional<UidType> validateUid = boost::none,
                          const http::ConnectionRetryProfile& retryProfile =
                                                http::ConnectionRetryProfile())
     : AsyncClient<boost::asio::local::stream_protocol::socket>(ioContext,
                                                                logToStderr),
       socket_(ioContext),
       localStreamPath_(localStreamPath),
       validateUid_(validateUid),
       retriedPermDenied_(false)
   {
      setConnectionRetryProfile(retryProfile);
   }

protected:

   virtual boost::asio::local::stream_protocol::socket& socket()
   {
      return socket_;
   }

private:

   virtual void connectAndWriteRequest()
   {
      // validate if requested
      if (validateUid_.is_initialized() && localStreamPath_.exists())
      {
         struct stat st;
         if (::stat(localStreamPath_.getAbsolutePath().c_str(), &st) == 0)
         {
            if (st.st_uid != validateUid_.get())
            {
                Error error = systemError(boost::system::errc::permission_denied,
                                          ERROR_LOCATION);
                error.addProperty("path", localStreamPath_);
                error.addProperty("user-id", validateUid_.get());
                error.addProperty("stream-user-id", st.st_uid);
                handleConnectionError(error);
                return;
            }
         }
         else
         {
            Error error = systemError(boost::system::errc::permission_denied, ERROR_LOCATION);
            error.addProperty("errno", errno);
            error.addProperty("path", localStreamPath_);
            handleConnectionError(error);
            return;
         }

      }

      // establish endpoint
      using boost::asio::local::stream_protocol;
      stream_protocol::endpoint endpoint(localStreamPath_.getAbsolutePath());

      // connect
      socket().async_connect(
         endpoint,
         boost::bind(&LocalStreamAsyncClient::handleConnect,
                     sharedFromThis(),
                     boost::asio::placeholders::error));
   }

   virtual std::string getDefaultHostHeader()
   {
      return "localhost";
   }

   void handleConnect(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // the connection was successful call base to write the request
            writeRequest();
         }
         else
         {
            handleConnectionError(Error(ec, ERROR_LOCATION));
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   virtual void addErrorProperties(Error& error)
   {
      AsyncClient::addErrorProperties(error);

      error.addProperty("path", localStreamPath_);
      if (validateUid_.is_initialized())
         error.addProperty("user-id", validateUid_.get());
   }

   const boost::shared_ptr<LocalStreamAsyncClient> sharedFromThis()
   {
      boost::shared_ptr<AsyncClient<boost::asio::local::stream_protocol::socket> >
                                    ptrShared = shared_from_this();

      return boost::static_pointer_cast<LocalStreamAsyncClient>(ptrShared);
   }

   virtual bool recentConnectionError(const Error& connectionError)
   {
      bool permDeniedError = connectionError.getCode() == boost::system::errc::permission_denied;
      if (!permDeniedError)
         return false;
      if (retriedPermDenied_) // Already been here once so it is probably a real perm denied error
      {
         LOG_DEBUG_MESSAGE("Not retrying local stream connection: " + localStreamPath_.getAbsolutePath() + " - 2nd permission denied error for same connection");
         return false;
      }

      // Seen once in 5000 sessions during load tests. Possibly the local stream socket is in a weird state
      // for an instant as it's being created by the remote side. So the fix here is to retry on this error just
      // once. Don't wait a full timeout interval (possibly 10-45s) if it's a real permission denied error just in case
      // that happens regularly.
      LOG_DEBUG_MESSAGE("Retrying local stream connection: " + localStreamPath_.getAbsolutePath() + " due to permission denied error");

      retriedPermDenied_ = true;
      return true;
   }

private:
   boost::asio::local::stream_protocol::socket socket_;
   core::FilePath localStreamPath_;
   boost::optional<UidType> validateUid_;
   bool retriedPermDenied_;
};
   
   
} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_LOCAL_STREAM_ASYNC_CLIENT_HPP
