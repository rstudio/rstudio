/*
 * SocketUtils.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef CORE_HTTP_SOCKET_UTILS_HPP
#define CORE_HTTP_SOCKET_UTILS_HPP

#include <boost/asio/error.hpp>
#include <boost/asio/socket_base.hpp>

#ifdef _WIN32
#include <boost/system/windows_error.hpp>
#endif

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {

inline bool isShutdownError(const boost::system::error_code& ec)
{
   
#ifdef _WIN32
   if (ec.value() == WSAENOTSOCK)
      return true;
#endif
   
   return
         ec == boost::asio::error::operation_aborted ||
         ec == boost::asio::error::invalid_argument ||
         ec == boost::system::errc::bad_file_descriptor;
}

namespace http {  

template <typename SocketService>
Error closeSocket(SocketService& socket)
{
   if (socket.is_open())
   {
      // shutdown, but don't allow shutdown errors to prevent us from closing
      // (shutdown errors often occur b/c the other end of the socket has
      // already been closed)
      boost::system::error_code ec;
      socket.shutdown(boost::asio::socket_base::shutdown_both, ec);
      
      socket.close(ec);
      if (ec)
         return Error(ec, ERROR_LOCATION);
   }
   
   return Success();
}

inline bool isWrongProtocolTypeError(const core::Error& error)
{
   return error == systemError(boost::system::errc::wrong_protocol_type, ErrorLocation());
}

inline bool isConnectionTerminatedError(const core::Error& error)
{
   // look for errors that indicate the client closing the connection
   bool timedOut = error == boost::asio::error::timed_out;
   bool eof = error == boost::asio::error::eof;
   bool reset = error == boost::asio::error::connection_reset;
   bool badFile = error == boost::asio::error::bad_descriptor;
   bool brokenPipe = error == boost::asio::error::broken_pipe;
   bool noFile = error == systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation());

#ifdef _WIN32
   int ec = error.getCode();
   bool noData = (error.getName() == boost::system::system_category().name()) && (ec == ERROR_NO_DATA);
#else
   bool noData = false;
#endif
   
   return timedOut || eof || reset || badFile || brokenPipe || noFile || noData;
}

inline bool isConnectionUnavailableError(const Error& error)
{
   // determine whether the error connecting was caused by the session process 
   // not currently existing (and thus remediable by launching a new one)
   
   return (
         // for unix domain sockets
         error == systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()) ||

         // for tcp-ip and unix domain sockets
         error == boost::asio::error::connection_refused

         // for windows named pipes
 #ifdef _WIN32
         || error == boost::system::windows_error::file_not_found
         || error == boost::system::windows_error::broken_pipe
         || error == boost::system::error_code(ERROR_PIPE_BUSY, boost::system::system_category())
 #endif
   );
}

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_SOCKET_UTILS_HPP
