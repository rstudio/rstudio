/*
 * SessionRequest.hpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#ifndef SESSION_REQUEST_HPP
#define SESSION_REQUEST_HPP

#include <core/Error.hpp>
#include <core/system/Environment.hpp>

#include <session/http/SessionRequest.hpp>

#include <core/http/TcpIpBlockingClient.hpp>
#include <core/http/ConnectionRetryProfile.hpp>

#ifndef _WIN32
# include <core/http/LocalStreamBlockingClient.hpp>
# include <session/SessionLocalStreams.hpp>
#endif

#include <session/SessionConstants.hpp>

namespace rstudio {
namespace session {
namespace http {
   
// this function sends an request directly to the session; it's inlined so that
// it can be used both from the postback executable (which does not link with
// rsession) and the session itself
inline core::Error sendSessionRequest(const std::string& uri, 
                                      const std::string& body,
                                      core::http::Response* pResponse)
{
   // build request
   core::http::Request request;
   request.setMethod("POST");
   request.setUri(uri);
   request.setHeader("Accept", "*/*");
   request.setHeader("Connection", "close");
   request.setBody(body);

   std::string tcpipPort = core::system::getenv(kRSessionStandalonePortNumber);

   // first, attempt to send a plain old http request
   if (tcpipPort.empty())
   {
      // if no standalone port, make an authenticated session request
      tcpipPort = core::system::getenv(kRSessionPortNumber);
      if (!tcpipPort.empty())
      {
         request.setHeader("X-Shared-Secret", core::system::getenv("RS_SHARED_SECRET"));
         return core::http::sendRequest("127.0.0.1", tcpipPort, request,  pResponse);
      }
   }

#ifndef _WIN32
   // otherwise, attempt communicating over a local stream (unix domain socket)
   // determine stream path -- check server environment variable first
   core::FilePath streamPath;
   std::string stream = core::system::getenv(kRStudioSessionStream);
   if (stream.empty())
   {
      // if no server environment variable, check desktop variant
      streamPath = core::FilePath(core::system::getenv("RS_LOCAL_PEER"));
      request.setHeader("X-Shared-Secret",
                        core::system::getenv("RS_SHARED_SECRET"));
   }
   else
   {
      streamPath = session::local_streams::streamPath(stream);
   }
   return core::http::sendRequest(streamPath, request, pResponse);
#endif

   return core::Success();
}

} // namespace http
} // namespace session
} // namespace rstudio

#endif
