/*
 * SessionLocalStreamHttpConnectionListener.hpp
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

#ifndef SESSION_LOCAL_STREAM_HTTP_CONNECTION_LISTENER_HPP
#define SESSION_LOCAL_STREAM_HTTP_CONNECTION_LISTENER_HPP

#include <vector>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/system/System.hpp>
#include <core/system/PosixUser.hpp>

#include <boost/asio/error.hpp>
#include <boost/optional.hpp>

#include <core/http/LocalStreamSocketUtils.hpp>

#include "SessionHttpConnectionListenerImpl.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {

// implementation of local stream http connection listener
class LocalStreamHttpConnectionListener :
    public HttpConnectionListenerImpl<boost::asio::local::stream_protocol>
{
public:
   LocalStreamHttpConnectionListener(const FilePath& streamPath,
                                     core::FileMode streamFileMode,
                                     const std::string& secret,
                                     int64_t limitRpcClientUid)
      : localStreamPath_(streamPath),
        streamFileMode_(streamFileMode),
        secret_(secret)
   {
      if (limitRpcClientUid != -1)
      {
         // always add current user
         using namespace rstudio::core::system::user;
         permittedClients_.push_back(currentUserIdentity().userId);

         // also add rpc client
         permittedClients_.push_back(static_cast<UidType>(limitRpcClientUid));
      }
   }

private:

   virtual Error initializeAcceptor(
      http::SocketAcceptorService<boost::asio::local::stream_protocol>*
                                                                  pAcceptor)
   {
      // A live session may already be serving this path (a duplicate launch
      // of the same session; see #18572). Binding over its socket would make
      // that session unreachable, and removing the path on the way out would
      // then lock the user out of it (#18941). Report the path as in use
      // instead, so startup retries briefly and then exits.
      bool inUse = false;
      Error error = http::isLocalStreamInUse(localStreamPath_, &inUse);
      if (error)
         return error;

      if (inUse)
      {
         error = Error(boost::asio::error::make_error_code(boost::asio::error::address_in_use),
                       ERROR_LOCATION);
         error.addProperty("description", "Another session is listening on this stream");
         error.addProperty("stream", localStreamPath_);
         return error;
      }

      // remove a stale stream left behind by a session that did not exit cleanly
      error = localStreamPath_.removeIfExists();
      if (error)
         return error;

      error = http::initLocalStreamAcceptor(*pAcceptor,
                                            localStreamPath_,
                                            streamFileMode_);
      if (error)
         return error;

      // remember which socket is ours (see cleanup)
      http::LocalStreamIdentity identity;
      error = http::getLocalStreamIdentity(localStreamPath_, &identity);
      if (error)
         return error;
      boundStream_ = identity;

      return writePidFile();
   }

   virtual bool validateConnection(
      boost::shared_ptr<HttpConnectionImpl<boost::asio::local::stream_protocol> > ptrConnection)
   {
      // only validate if we have a set of permitted clients
      if (permittedClients_.size() > 0)
      {
         // get socket
         int socket = ptrConnection->socket().native_handle();

         // get client identity
         core::system::user::UserIdentity userIdentity;
         core::Error error = socketPeerIdentity(socket,&userIdentity);
         if (error)
         {
            LOG_ERROR(error);
            return false;
         }

         // got it
         uid_t clientUid = userIdentity.userId;

         // check against list
         for (std::vector<uid_t>::const_iterator it = permittedClients_.begin();
              it != permittedClients_.end();
              ++it)
         {
            if (clientUid == *it)
               return true;
         }

         // didn't find it in the list
         LOG_WARNING_MESSAGE("Connection attempted by invalid user-id: " +
                             safe_convert::numberToString(clientUid));
         return false;
      }
      else
      {
         return true;
      }
   }


   virtual Error cleanup()
   {
      // only remove what this process bound: if the path now leads to a
      // socket that a later session bound in our place, that session's
      // socket and pid file must survive our exit (#18941)
      if (!boundStream_)
         return Success();
      http::LocalStreamIdentity bound = *boundStream_;
      boundStream_.reset();

      http::LocalStreamIdentity current;
      Error error = http::getLocalStreamIdentity(localStreamPath_, &current);
      if (error == systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()))
         return Success();
      else if (error)
         return error;

      if (current != bound)
      {
         LOG_DEBUG_MESSAGE("Leaving stream bound by another session in place: " +
                           localStreamPath_.getAbsolutePath());
         return Success();
      }

      error = cleanupPidFile();
      if (error)
         LOG_ERROR(error);

      return localStreamPath_.removeIfExists();
   }

protected:

   virtual bool authenticate(boost::shared_ptr<HttpConnection> ptrConnection)
   {
      if (!connection::authenticate(ptrConnection, secret_))
         return false;
      return HttpConnectionListenerImpl::authenticate(ptrConnection);
   }

private:
   Error writePidFile()
   {
      // get path to pid file
      FilePath pidFile = pidFilePath();

      // write pid to it
      std::ostringstream ostr;
      ostr << core::system::currentProcessId();
      Error error = core::writeStringToFile(pidFile, ostr.str());
      if (error)
         return error;

      // chmod to ensure other users can read the file
      return pidFile.changeFileMode(core::FileMode::USER_READ_WRITE_ALL_READ);
   }

   Error cleanupPidFile()
   {
      return pidFilePath().removeIfExists();
   }

   FilePath pidFilePath()
   {
      return FilePath(localStreamPath_.getAbsolutePath() + ".pid");
   }

private:
   core::FilePath localStreamPath_;
   core::FileMode streamFileMode_;

   // identity of the socket this listener bound, until cleanup releases it
   boost::optional<http::LocalStreamIdentity> boundStream_;

   // desktop shared secret
   std::string secret_;

   // user-ids we will accept connections from
   std::vector<uid_t> permittedClients_;
};

} // namespace session
} // namespace rstudio

#endif /* SESSION_LOCAL_STREAM_HTTP_CONNECTION_LISTENER_HPP */
