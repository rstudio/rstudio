/*
 * LocalStreamAsyncServer.hpp
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

#ifndef CORE_HTTP_LOCAL_STREAM_ASYNC_SERVER_HPP
#define CORE_HTTP_LOCAL_STREAM_ASYNC_SERVER_HPP

#include <core/http/LocalStreamSocketUtils.hpp>
#include <core/http/AsyncServerImpl.hpp>

#include <core/system/PosixUser.hpp>

namespace core {
namespace http {

class LocalStreamAsyncServer
   : public AsyncServerImpl<boost::asio::local::stream_protocol>
{
public:
   LocalStreamAsyncServer(const std::string& serverName,
                          const std::string& baseUri = std::string())
      : AsyncServerImpl<boost::asio::local::stream_protocol>(serverName, baseUri)
   {
   }
   
   virtual ~LocalStreamAsyncServer()
   {
      try
      {
         Error error = removeLocalStream();
         if (error)
            LOG_ERROR(error);
      }
      catch(...)
      {
      }
   }
   
   
public:
   Error init(const core::FilePath& localStreamPath)
   {
      // set stream path
      localStreamPath_ = localStreamPath;
      
      // remove any existing stream
      Error error = removeLocalStream();
      if (error)
         return error ;
      
      // initialize acceptor
      return initLocalStreamAcceptor(acceptorService(),
                                     localStreamPath_,
                                     core::system::EveryoneReadWriteMode);
   }
   
private:
   virtual void onRequest(boost::asio::local::stream_protocol::socket* pSocket,
                          http::Request* pRequest)
   {
      // get peer identity
      core::system::user::UserIdentity peerIdentity;
      Error error = core::system::user::socketPeerIdentity(pSocket->native(), 
                                                           &peerIdentity);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
      
      // set it
      pRequest->remoteUid_ = peerIdentity.userId;
   }
   
   
   
   Error removeLocalStream()
   {
      if (localStreamPath_.exists())
      {
         return localStreamPath_.remove();
      }
      else
      {
         return Success();
      }
   }
   
private:
   core::FilePath localStreamPath_;

};

} // namespace http
} // namespace core

#endif // CORE_HTTP_LOCAL_STREAM_ASYNC_SERVER_HPP


