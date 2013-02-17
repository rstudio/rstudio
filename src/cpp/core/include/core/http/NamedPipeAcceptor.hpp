/*
 * NamedPipeAcceptor.hpp
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

#ifndef CORE_HTTP_NAMED_PIPE_ACCEPTOR_HPP
#define CORE_HTTP_NAMED_PIPE_ACCEPTOR_HPP

#include <boost/asio/io_service.hpp>
#include <boost/asio/windows/stream_handle.hpp>
#include <boost/asio/windows/overlapped_ptr.hpp>

#include <core/Error.hpp>

namespace core {
namespace http {

class NamedPipeAcceptor
{
public:
   explicit NamedPipeAcceptor(boost::asio::io_service& ioService)
      : ioService_(ioService)
   {
   }

   Error init(const std::string& pipeName)
   {
      pipeName_ = pipeName;
      return Success();
   }

   bool is_open()
   {
      return !pipeName_.empty();
   }

   void close(boost::system::error_code& ec)
   {
      // nothing to close
   }

   // standard asio handler signature
   typedef boost::function<void(const boost::system::error_code& ec)>
                                                               AcceptHandler;

   void async_accept(boost::asio::windows::stream_handle& socket,
                     AcceptHandler acceptHandler)
   {

      // create the named pipe.
      // TODO: security attributes for local authenticated pipe only
      // TODO: should we add this flag to pipe-mode?
      const DWORD PIPE_REJECT_REMOTE_CLIENTS = 0x00000008;
      HANDLE hPipe = ::CreateNamedPipeA(
            pipeName_.c_str(),            // pipe name
            PIPE_ACCESS_DUPLEX |          // support reading and writing
            FILE_FLAG_OVERLAPPED,         // enable asynchronous i/o
            PIPE_TYPE_BYTE |              // byte writes
            PIPE_READMODE_BYTE |          // byte reads
            PIPE_WAIT,                    // no lan manager no-blocking mode
            PIPE_UNLIMITED_INSTANCES,     // no limit on instances
            4096,                         // output buffer size
            4096,                         // input buffer size
            0,                            // use default client timeout (50ms)
            NULL);                        // no security attributes
      if (hPipe == INVALID_HANDLE_VALUE)
      {
         acceptHandler(lastSystemError());
         return;
      }

      // use an overlapped_ptr to map the handler to an overlapped ptr
      boost::asio::windows::overlapped_ptr overlapped(ioService_,
          boost::bind(&NamedPipeAcceptor::handleAccept, this,
                      boost::ref(socket), hPipe, acceptHandler, _1));

      // wait for the connection asynchronously using the overlapped ptr
      BOOL success = ::ConnectNamedPipe(hPipe, overlapped.get());
      DWORD lastError = ::GetLastError();

      // failure with either ERROR_IO_PENDING or ERROR_PIPE_CONNECTED is
      // actually success so check those along with the success flag
      if (!success &&
          (lastError != ERROR_IO_PENDING) &&
          (lastError != ERROR_PIPE_CONNECTED))
      {
          // error so complete immediately
          overlapped.complete(lastSystemError(), 0);
      }
      else
      {
         // success - release ownership of the overlapped ptr to the io service
         overlapped.release();
      }
   }

private:
   void handleAccept(boost::asio::windows::stream_handle& stream,
                     HANDLE hPipe,
                     AcceptHandler acceptHandler,
                     const boost::system::error_code& ec)
   {
      stream.assign(hPipe);
      acceptHandler(ec);
   }

   boost::system::error_code lastSystemError() const
   {
      boost::system::error_code ec(
                                ::GetLastError(),
                                boost::asio::error::get_system_category());
      return ec;
   }

private:
   boost::asio::io_service& ioService_;
   std::string pipeName_;
};

   
} // namespace http
} // namespace core

#endif // CORE_HTTP_NAMED_PIPE_ACCEPTOR_HPP
