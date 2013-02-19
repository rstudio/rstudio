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

#include <boost/assert.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/asio/windows/stream_handle.hpp>
#include <boost/asio/windows/object_handle.hpp>
#include <boost/asio/windows/overlapped_ptr.hpp>

#include <core/Error.hpp>

namespace core {
namespace http {

class NamedPipeAcceptor : boost::noncopyable
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


      // login sid:
      //    http://msdn.microsoft.com/en-us/library/windows/desktop/aa365600(v=vs.85).aspx
      //    http://msdn.microsoft.com/en-us/library/windows/desktop/aa446670(v=vs.85).aspx

      // http://us.generation-nt.com/answer/named-pipes-intra-session-ipc-help-27898332.html


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

      // create OVERLAPPED structure to wait with
      OVERLAPPED overlapped;
      ZeroMemory(&overlapped, sizeof(overlapped));
      overlapped.hEvent = ::CreateEvent(NULL, TRUE, FALSE, NULL);
      if (!overlapped.hEvent)
      {
         acceptHandler(lastSystemError());
         return;
      }

      // assign the event to an object handle we can async_wait on
      boost::shared_ptr<boost::asio::windows::object_handle> pAcceptEvent(
               new boost::asio::windows::object_handle(ioService_));
      boost::system::error_code ec;
      pAcceptEvent->assign(overlapped.hEvent, ec);
      if (ec)
      {
         acceptHandler(ec);
         return;
      }

      // wait for the connection
      BOOL success = ::ConnectNamedPipe(hPipe, &overlapped);
      DWORD lastError = ::GetLastError();
      if (success || (lastError == ERROR_PIPE_CONNECTED))
      {
         closeAcceptEvent(pAcceptEvent, ERROR_LOCATION);

         acceptHandler(boost::system::error_code());
      }
      else if (lastError == ERROR_IO_PENDING)
      {
         // need to wait asynchronously on the event
         pAcceptEvent->async_wait(
            boost::bind(&NamedPipeAcceptor::handleAccept, this,
                        pAcceptEvent,
                        boost::ref(socket),
                        hPipe,
                        acceptHandler,
                        _1));
      }
      else
      {
         closeAcceptEvent(pAcceptEvent, ERROR_LOCATION);

         boost::system::error_code ec(lastError,
                                      boost::system::get_system_category());
         acceptHandler(ec);
      }
   }

private:
   void handleAccept(
          boost::shared_ptr<boost::asio::windows::object_handle> pEvent,
          boost::asio::windows::stream_handle& stream,
          HANDLE hPipe,
          AcceptHandler acceptHandler,
          const boost::system::error_code& ec)
   {
      closeAcceptEvent(pEvent, ERROR_LOCATION);

      boost::system::error_code assignEc;
      stream.assign(hPipe, assignEc);
      if (!assignEc)
      {
         acceptHandler(ec);
      }
      else if (!ec)
      {
         acceptHandler(assignEc);
      }
      else
      {
         LOG_ERROR(Error(assignEc, ERROR_LOCATION));
         acceptHandler(ec);
      }
   }

   void closeAcceptEvent(
       boost::shared_ptr<boost::asio::windows::object_handle> pAcceptEvent,
       const core::ErrorLocation& location)
   {
      boost::system::error_code ec;
      pAcceptEvent->close(ec);
      if (ec)
         LOG_ERROR(Error(ec, location));
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
