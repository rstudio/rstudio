/*
 * NamedPipeAsyncClient.hpp
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

#ifndef CORE_HTTP_NAMED_PIPE_ASYNC_CLIENT_HPP
#define CORE_HTTP_NAMED_PIPE_ASYNC_CLIENT_HPP

#include <boost/function.hpp>

#include <boost/asio/io_service.hpp>
#include <boost/asio/windows/stream_handle.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/http/AsyncClient.hpp>

#include <core/http/NamedPipeProtocol.hpp>

namespace core {
namespace http {

class NamedPipeAsyncClient
   : public AsyncClient<boost::asio::windows::stream_handle>
{
public:
   // create a named pipe client -- note that the connectionRetryProfile is
   // required because named pipes typically require a retry loop (due to
   // servers either not having a pipe available or being between calls
   // to ConnectNamedPipe). rather than create yet another timer-based
   // retry mechanism for CreateFile on the named pipe client handle we
   // require that clients use a connection retry profile
   NamedPipeAsyncClient(boost::asio::io_service& ioService,
                        const std::string& pipeName,
                        const http::ConnectionRetryProfile& retryProfile)
     : AsyncClient<boost::asio::windows::stream_handle>(ioService),
       handle_(ioService),
       pipeName_(pipeName)
   {  
      setConnectionRetryProfile(retryProfile);
   }

protected:

   virtual boost::asio::windows::stream_handle& socket()
   {
      return handle_;
   }

private:

   virtual void connectAndWriteRequest()
   {
      try
      {
         // connect to named pipe
         HANDLE hPipe = ::CreateFileA(
                  pipeName_.c_str(),		// pipe name
                  GENERIC_READ |          // allow reading
                  GENERIC_WRITE,          // allow writing
                  0,                      // no sharing
                  NULL,                   // default security attributes
                  OPEN_EXISTING,          // opens existing
                  FILE_FLAG_OVERLAPPED |  // allow overlapped io
                  SECURITY_SQOS_PRESENT | // custom security attribs
                  SECURITY_IDENTIFICATION,// impersonate identity only
                  NULL);               	// no template file

         // handle connection error if necessary)
         if (hPipe == INVALID_HANDLE_VALUE)
         {
            handleConnectionError(systemError(::GetLastError(),ERROR_LOCATION));
            return;
         }

         // assign the pipe to our handle
         handle_.assign(hPipe);

         // write the request
         writeRequest();
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   // detect when we've got the whole response and force a
   // response + close of the socket
   virtual bool stopReadingAndRespond()
   {
      return response_.body().length() >= response_.contentLength();
   }

   virtual bool isShutdownError(const boost::system::error_code& ec)
   {
      if (ec.category() == boost::system::get_system_category() &&
          (ec.value() == ERROR_PIPE_NOT_CONNECTED) )
      {
         return true;
      }
      else
      {
         return false;
      }

   }

   const boost::shared_ptr<NamedPipeAsyncClient> sharedFromThis()
   {
      boost::shared_ptr<AsyncClient<boost::asio::windows::stream_handle> >
                                    ptrShared = shared_from_this();

      return boost::shared_static_cast<NamedPipeAsyncClient>(ptrShared);
   }

private:
   std::string pipeName_;
   boost::asio::windows::stream_handle handle_;
};
   
   
} // namespace http
} // namespace core

#endif // CORE_HTTP_LOCAL_STREAM_ASYNC_CLIENT_HPP
