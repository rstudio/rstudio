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

   void accept(boost::asio::windows::stream_handle& socket,
               boost::system::error_code& ec)
   {

   }

   void async_accept(
      boost::asio::windows::stream_handle& socket,
      boost::function<void(const boost::system::error_code& ec)> acceptHandler)
   {
      // NOTE: socket is not yet initialied so we can assign
      // it after we accept (verify this)

      // NOTE: after we create the pipe we can probably use
      // boost::overlapped_ptr to allow a handler to get called
      // back from ConnectNamedPipe (we pass the overlapped_ptr
      // to ConnectNamedPipe and it calls the handle back once
      // the overlapped event is signaled

      // ....BUT....does this handle ERROR_PIPE_CONNECTED. I
      // guess we could check for that in the ec and
      // act accordingly

      // Check for example uses of overlapped_ptr, especially
      // with ConnectNamedPipe
   }

   void cancel(boost::system::error_code& ec)
   {

   }

private:
   boost::asio::io_service& ioService_;
   std::string pipeName_;
};

   
} // namespace http
} // namespace core

#endif // CORE_HTTP_NAMED_PIPE_ACCEPTOR_HPP
