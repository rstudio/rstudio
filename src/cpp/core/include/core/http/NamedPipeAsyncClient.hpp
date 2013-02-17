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
   NamedPipeAsyncClient(boost::asio::io_service& ioService,
                        const std::string& pipeName)
     : AsyncClient<boost::asio::windows::stream_handle>(ioService),
       handle_(ioService),
       pipeName_(pipeName)
   {  
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
         // connect to named pipe -- note for our purposes we can
         // probably do this synchronously, but if we wanted to
         // do it async we'd have to use an ioService timer


         HANDLE hPipe = INVALID_HANDLE_VALUE;

         // we can either:
         //   - try once then call handleConnectionError which assumes
         //     there is a retry profile established
         //   - try in a loop (avoiding common failure modes associated
         //     with intermitent availability of server pipe)
         //   - setup a timer and retry in the timer (then also do
         //     handleConnectionError

         // if we fail then need to call handleConnectionError (to get
         // automatic retrying)
         //
         // handleConnectionError(Error(ec, ERROR_LOCATION));
         //

         // assign the pipe to our handle
         handle_.assign(hPipe);


         // write the request
         writeRequest();
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
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
