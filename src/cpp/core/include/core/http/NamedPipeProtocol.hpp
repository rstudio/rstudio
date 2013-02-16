/*
 * NamedPipeProtocol.hpp
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

#ifndef CORE_HTTP_NAMED_PIPE_PROTOCOL_HPP
#define CORE_HTTP_NAMED_PIPE_PROTOCOL_HPP

#include <boost/asio/windows/stream_handle.hpp>

#include <core/Error.hpp>

#include <core/http/NamedPipeAcceptor.hpp>

namespace core {
namespace http {

class NamedPipeProtocol
{
public:
   typedef boost::asio::windows::stream_handle socket;
   typedef NamedPipeAcceptor acceptor;
};

// specialization of closeSocket for stream handle lowest level
template<> Error closeSocket(
              boost::asio::windows::stream_handle::lowest_layer_type& socket)
{
   if (socket.is_open())
   {
      boost::system::error_code ec;
      socket.close(ec);
      if (ec)
        return Error(ec, ERROR_LOCATION) ;
   }

   return Success();
}

// specialization of closeSocket for stream handles
template<> Error closeSocket(boost::asio::windows::stream_handle& socket)
{
   // delegate to lowest_layer (it's the same object)
   return closeSocket(socket.lowest_layer());
}

// specialization of closeServerSocket
template<> Error closeServerSocket(boost::asio::windows::stream_handle& socket)
{
   // TODO: call GetNamedPipeInfo to confirm that the handle
   // is to a named pipe

   // disconnect named pipe
   if (socket.native_handle() != INVALID_HANDLE_VALUE)
   {
      // flush buffers -- TODO: make sure this can never hang!!!!
      if (!::FlushFileBuffers(socket.native_handle()))
         LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));

      // disconnect named pipe
      if (!::DisconnectNamedPipe(socket.native_handle()))
         LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
   }

   // close handle
   return closeSocket(socket);
}

   
} // namespace http
} // namespace core

#endif // CORE_HTTP_NAMED_PIPE_PROTOCOL_HPP
