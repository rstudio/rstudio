/*
 * SessionLocalStreamHttpConnectionListener.hpp
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


#include <vector>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/system/PosixUser.hpp>

#include <core/http/LocalStreamSocketUtils.hpp>

#include "SessionHttpConnectionListenerImpl.hpp"

using namespace core ;

namespace session {

// implementation of local stream http connection listener
class LocalStreamHttpConnectionListener :
    public HttpConnectionListenerImpl<boost::asio::local::stream_protocol>
{
public:
   LocalStreamHttpConnectionListener(const FilePath& streamPath,
                                     int limitRpcClientUid)
      : localStreamPath_(streamPath)
   {
      if (limitRpcClientUid != -1)
      {
         // always add current user
         using namespace core::system::user;
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
      return http::initLocalStreamAcceptor(*pAcceptor, localStreamPath_);
   }

   virtual bool validateConnection(
      boost::shared_ptr<HttpConnectionImpl<boost::asio::local::stream_protocol> > ptrConnection)
   {
      // only validate if we have a set of permitted clients
      if (permittedClients_.size() > 0)
      {
         // get socket
         int socket = ptrConnection->socket().native();

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
      return localStreamPath_.removeIfExists();
   }


private:
   core::FilePath localStreamPath_;

   // user-ids we will accept connections from
   std::vector<uid_t> permittedClients_;
};

} // namespace session
