/*
 * LocalStreamSocketUtils.hpp
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

#ifndef CORE_HTTP_LOCAL_STREAM_SOCKET_UTILS_HPP
#define CORE_HTTP_LOCAL_STREAM_SOCKET_UTILS_HPP

#include <boost/asio/local/stream_protocol.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/system/System.hpp>

#include <core/http/SocketAcceptorService.hpp>

namespace rstudio {
namespace core {
namespace http {  

inline Error initializeStreamDir(const FilePath& streamDir)
{
   if (!streamDir.exists())
   {
      Error error = streamDir.ensureDirectory();
      if (error)
         return error;
      
      return streamDir.changeFileMode(FileMode::ALL_READ_WRITE_EXECUTE, true);
   }
   else
   {
      return Success();
   }
}
   
inline Error initLocalStreamAcceptor(
   SocketAcceptorService<boost::asio::local::stream_protocol>& acceptorService,
   const core::FilePath& localStreamPath,
   core::FileMode fileMode)
{
   // initialize endpoint
   using boost::asio::local::stream_protocol;
   stream_protocol::endpoint endpoint(localStreamPath.getAbsolutePath());
   
   // get acceptor
   stream_protocol::acceptor& acceptor = acceptorService.acceptor();
   
   // open
   boost::system::error_code ec;
   acceptor.open(endpoint.protocol(), ec);
   if (ec)
   {
      Error error(ec, ERROR_LOCATION);
      error.addProperty("stream", localStreamPath);
      return error;
   }
   
   // bind
   acceptor.bind(endpoint, ec);
   if (ec)
   {
      Error error(ec, ERROR_LOCATION);
      error.addProperty("stream", localStreamPath);
      return error;
   }
   
   // chmod on the stream file
   Error error = localStreamPath.changeFileMode(fileMode);
   if (error)
      return error;
   
   // listen
   acceptor.listen(boost::asio::socket_base::max_connections, ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   return Success();
}

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_LOCAL_STREAM_SOCKET_UTILS_HPP
