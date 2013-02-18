/*
 * NamedPipeAsyncServer.hpp
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

#ifndef CORE_HTTP_NAMED_PIPE_ASYNC_SERVER_HPP
#define CORE_HTTP_NAMED_PIPE_ASYNC_SERVER_HPP

#include <core/http/NamedPipeProtocol.hpp>
#include <core/http/AsyncServer.hpp>

namespace core {
namespace http {

class NamedPipeAsyncServer : public AsyncServer<NamedPipeProtocol>
{
public:
   NamedPipeAsyncServer(const std::string& serverName,
                        const std::string& baseUri = std::string())
      : AsyncServer<NamedPipeProtocol>(serverName, baseUri)
   {
   }
   
   virtual ~NamedPipeAsyncServer()
   {
      try
      {
      }
      catch(...)
      {
      }
   }
   
public:
   // TODO: some assistance with constructing correct pipe names:
   // http://msdn.microsoft.com/en-us/library/aa365783(VS.85).aspx
   Error init(const std::string& pipeName)
   {
      return acceptorService().acceptor().init(pipeName);
   }
   
private:
   virtual void onRequest(NamedPipeProtocol::socket* pSocket,
                          http::Request* pRequest)
   {

   }
   
private:


};

} // namespace http
} // namespace core

#endif // CORE_HTTP_NAMED_PIPE_ASYNC_SERVER_HPP


