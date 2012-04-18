/*
 * SocketUtils.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_HTTP_SOCKET_UTILS_HPP
#define CORE_HTTP_SOCKET_UTILS_HPP

#include <boost/asio/error.hpp>
#include <boost/asio/socket_base.hpp>

#include <core/Error.hpp>

namespace core {
namespace http {  

template <typename SocketService>
Error closeSocket(SocketService& socket)
{
   if (socket.is_open())
   {
      // shutdown, but don't allow shutdown errors to prevent us from closing
      // (shutdown errors often occur b/c the other end of the socket has
      // already been closed)
      boost::system::error_code ec ;
      socket.shutdown(boost::asio::socket_base::shutdown_both, ec) ;
      
      socket.close(ec) ;
      if (ec)
         return Error(ec, ERROR_LOCATION) ;
   }
   
   return Success() ; 
}

inline bool isConnectionTerminatedError(const core::Error& error)
{
   // look for errors that indicate the client closing the connection
   bool timedOut = error.code() == boost::asio::error::timed_out;
   
   bool sysTimedOut = error.code() == boost::system::errc::timed_out;
   
   bool eof = error.code() == boost::asio::error::eof;
   
   bool reset = error.code() == boost::system::errc::connection_reset;
   
   bool brokenPipe = error.code() == boost::system::errc::broken_pipe;

   bool noFile = boost::system::errc::no_such_file_or_directory;
   
   return timedOut || sysTimedOut || eof || reset || brokenPipe || noFile;
}

inline bool isConnectionUnavailableError(const Error& error)
{
   // determine whether the error connecting was caused by the session process 
   // not currently existing (and thus remediable by launching a new one)
   
   return (
      // for unix domain sockets
      error.code() == boost::system::errc::no_such_file_or_directory ||

      // for tcp-ip and unix domain sockets
      error.code() == boost::system::errc::connection_refused
   );
}

} // namespace http
} // namespace core

#endif // CORE_HTTP_SOCKET_UTILS_HPP
