/*
 * LocalStreamAsyncClient.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_HTTP_LOCAL_STREAM_ASYNC_CLIENT_HPP
#define CORE_HTTP_LOCAL_STREAM_ASYNC_CLIENT_HPP

#include <boost/function.hpp>

#include <boost/asio/local/stream_protocol.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/http/AsyncClient.hpp>
#include <core/http/LocalStreamSocketUtils.hpp>

namespace core {
namespace http {  

class LocalStreamAsyncClient
   : public AsyncClient<boost::asio::local::stream_protocol::socket>
{
public:
   LocalStreamAsyncClient(boost::asio::io_service& ioService,
                          const FilePath localStreamPath,
                          const http::ConnectionRetryProfile& retryProfile =
                                                http::ConnectionRetryProfile())
     : AsyncClient<boost::asio::local::stream_protocol::socket>(ioService),
       socket_(ioService),
       localStreamPath_(localStreamPath)
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
      // establish endpoint
      using boost::asio::local::stream_protocol;
      stream_protocol::endpoint endpoint(localStreamPath_.absolutePath());

      // connect
      socket().async_connect(
         endpoint,
         boost::bind(&LocalStreamAsyncClient::handleConnect,
                     sharedFromThis(),
                     boost::asio::placeholders::error));
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


   const boost::shared_ptr<LocalStreamAsyncClient> sharedFromThis()
   {
      boost::shared_ptr<AsyncClient<boost::asio::local::stream_protocol::socket> >
                                    ptrShared = shared_from_this();

      return boost::static_pointer_cast<LocalStreamAsyncClient>(ptrShared);
   }

private:
   boost::asio::local::stream_protocol::socket socket_;
   core::FilePath localStreamPath_;
};
   
   
} // namespace http
} // namespace core

#endif // CORE_HTTP_LOCAL_STREAM_ASYNC_CLIENT_HPP
