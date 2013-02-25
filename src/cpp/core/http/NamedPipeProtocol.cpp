/*
 * NamedPipeProtocol.cpp
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

#include <core/http/NamedPipeProtocol.hpp>

namespace core {
namespace http {


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
      // disconnect named pipe
      if (!::DisconnectNamedPipe(socket.native_handle()))
         LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
   }

   // close handle
   return closeSocket(socket);
}

// specialization of handleWrite (note: this isn't currently any
// different from the generic form but we have it here in case
// we need to do a read prior to disconnecting from the named pipe
// (as per the microsoft named pipes with overlapped io example)
template<> void AsyncConnectionImpl<NamedPipeProtocol>::handleWrite(
                          const boost::system::error_code& e)
{
   try
   {
      if (e)
      {
         // log the error if it wasn't connection terminated
         Error error(e, ERROR_LOCATION);
         if (!http::isConnectionTerminatedError(error))
            LOG_ERROR(error);
      }

      // close the socket
      Error error = closeServerSocket(socket_);
      if (error)
         LOG_ERROR(error);

      //
      // no more async operations are initiated here so the shared_ptr to
      // this connection no more references and is automatically destroyed
      //
   }
   CATCH_UNEXPECTED_EXCEPTION
}

   
} // namespace http
} // namespace core

