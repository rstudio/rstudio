/*
 * SessionLocalStreamHttpConnectionListener.hpp
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


#include <vector>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/system/System.hpp>
#include <core/system/PosixUser.hpp>

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
                                     int limitRpcClientUid)
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
         permittedClients_.push_back(limitRpcClientUid);
      }
   }

private:

   virtual Error initializeAcceptor(
      http::SocketAcceptorService<boost::asio::local::stream_protocol>*
                                                                  pAcceptor)
   {
      Error error = writePidFile();
      if (error)
         return error;

      return http::initLocalStreamAcceptor(*pAcceptor,
                                           localStreamPath_,
                                           streamFileMode_);
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
      Error error = cleanupPidFile();
      if (error)
         LOG_ERROR(error);

      return localStreamPath_.removeIfExists();
   }

protected:

   virtual bool authenticate(boost::shared_ptr<HttpConnection> ptrConnection)
   {
      return connection::authenticate(ptrConnection, secret_);
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

   // desktop shared secret
   std::string secret_;

   // user-ids we will accept connections from
   std::vector<uid_t> permittedClients_;
};

} // namespace session
} // namespace rstudio
