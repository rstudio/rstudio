/*
 * LocalStreamSocketUtils.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_HTTP_LOCAL_STREAM_SOCKET_UTILS_HPP
#define CORE_HTTP_LOCAL_STREAM_SOCKET_UTILS_HPP

#include <cerrno>

#include <fcntl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>

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
   acceptor.listen(boost::asio::socket_base::max_listen_connections, ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   return Success();
}

// Whether some process is accepting connections on the local stream at
// localStreamPath. A missing path is not listening, and neither is a socket
// left behind by a process that exited without removing it.
inline Error isLocalStreamListening(const FilePath& localStreamPath, bool* pListening)
{
   *pListening = false;

   boost::asio::local::stream_protocol::endpoint endpoint(localStreamPath.getAbsolutePath());

   int fd = ::socket(AF_UNIX, SOCK_STREAM, 0);
   if (fd == -1)
      return systemError(errno, ERROR_LOCATION);

   // a listener whose backlog is full is still listening; find that out
   // without waiting for it to accept
   int flags = ::fcntl(fd, F_GETFL, 0);
   if (flags == -1 || ::fcntl(fd, F_SETFL, flags | O_NONBLOCK) == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      ::close(fd);
      return error;
   }

   int result = ::connect(fd, endpoint.data(), static_cast<socklen_t>(endpoint.size()));
   int connectErrno = errno;
   ::close(fd);

   *pListening = result == 0 ||
                 connectErrno == EAGAIN ||
                 connectErrno == EWOULDBLOCK ||
                 connectErrno == EINPROGRESS;

   return Success();
}

// Identifies the file a local stream was bound to, so that its process can
// later tell whether the path still leads to its own socket, or to one that
// another process has bound there since.
struct LocalStreamIdentity
{
   dev_t device = 0;
   ino_t inode = 0;

   bool operator==(const LocalStreamIdentity& other) const
   {
      return device == other.device && inode == other.inode;
   }

   bool operator!=(const LocalStreamIdentity& other) const
   {
      return !(*this == other);
   }
};

inline Error getLocalStreamIdentity(const FilePath& localStreamPath,
                                    LocalStreamIdentity* pIdentity)
{
   struct stat info;
   if (::lstat(localStreamPath.getAbsolutePath().c_str(), &info) == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("stream", localStreamPath);
      return error;
   }

   pIdentity->device = info.st_dev;
   pIdentity->inode = info.st_ino;
   return Success();
}

// Binds and listens on the local stream at localStreamPath, unless another
// process is already listening there: binding over its socket would leave
// that process listening on a path that no longer leads to it. In that case
// this fails with address_in_use and leaves the path alone. A socket that
// nobody listens on any more is replaced. On success, *pIdentity identifies
// the socket bound here.
inline Error claimLocalStream(
   SocketAcceptorService<boost::asio::local::stream_protocol>& acceptorService,
   const core::FilePath& localStreamPath,
   core::FileMode fileMode,
   LocalStreamIdentity* pIdentity)
{
   using boost::asio::local::stream_protocol;
   stream_protocol::endpoint endpoint(localStreamPath.getAbsolutePath());
   stream_protocol::acceptor& acceptor = acceptorService.acceptor();

   // bind without removing the path first, so that of two processes racing
   // to claim a missing path, one fails rather than replacing the other
   boost::system::error_code ec;
   acceptor.open(endpoint.protocol(), ec);
   if (!ec)
      acceptor.bind(endpoint, ec);

   if (ec == boost::asio::error::address_in_use)
   {
      bool listening = false;
      Error error = isLocalStreamListening(localStreamPath, &listening);
      if (error)
         return error;

      if (listening)
      {
         error = Error(ec, ERROR_LOCATION);
         error.addProperty("description", "Another process is already listening on this stream");
         error.addProperty("stream", localStreamPath);
         return error;
      }

      error = localStreamPath.removeIfExists();
      if (error)
         return error;

      // a process that bound the path since our probe keeps it
      acceptor.bind(endpoint, ec);
   }

   if (ec)
   {
      Error error(ec, ERROR_LOCATION);
      error.addProperty("stream", localStreamPath);
      return error;
   }

   // record which socket is ours, then listen at once: until we do, another
   // process probing the path would take our socket for an abandoned one
   Error error = getLocalStreamIdentity(localStreamPath, pIdentity);
   if (error)
      return error;

   acceptor.listen(boost::asio::socket_base::max_listen_connections, ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);

   return localStreamPath.changeFileMode(fileMode);
}

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_LOCAL_STREAM_SOCKET_UTILS_HPP
