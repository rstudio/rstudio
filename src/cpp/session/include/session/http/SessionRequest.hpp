/*
 * SessionRequest.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#if !defined(_WIN32)
#include <core/http/TcpIpBlockingClient.hpp>
#include <core/http/LocalStreamBlockingClient.hpp>
#include <core/http/ConnectionRetryProfile.hpp>
#else
#include <core/http/NamedPipeBlockingClient.hpp>
#endif

#if !defined(_WIN32)
#include <session/SessionLocalStreams.hpp>
#endif

#include <session/SessionConstants.hpp>

namespace rstudio {
namespace session {
namespace http {
   
// this function sends an request directly to the session; it's inlined so that
// it can be used both from the postback executable (which does not link with
// rsession) and the session itself
inline core::Error sendSessionRequest(const std::string& uri, 
      const std::string& body, core::http::Response* pResponse)
{
   // build request
   core::http::Request request;
   request.setMethod("POST");
   request.setUri(uri);
   request.setHeader("Accept", "*/*");
   request.setHeader("Connection", "close");
   request.setBody(body);

#ifdef _WIN32
   // get local peer
   std::string pipeName = core::system::getenv("RS_LOCAL_PEER");
   request.setHeader("X-Shared-Secret",
                       core::system::getenv("RS_SHARED_SECRET"));
   return core::http::sendRequest(pipeName,
                            request,
                            core::http::ConnectionRetryProfile(
                                  boost::posix_time::seconds(10),
                                  boost::posix_time::milliseconds(50)),
                            pResponse);
#else
   std::string tcpipPort = core::system::getenv(kRSessionStandalonePortNumber);

#ifdef __APPLE__
   // on OS X desktop, we use HTTP
   if (tcpipPort.empty())
   {
      // if no standalone port, make an authenticated session request
      tcpipPort = core::system::getenv(kRSessionPortNumber);
      request.setHeader("X-Shared-Secret",
                          core::system::getenv("RS_SHARED_SECRET"));
   }
#endif

   if (!tcpipPort.empty())
   {
      return core::http::sendRequest("127.0.0.1", tcpipPort, request, 
            pResponse);
   }
   else
   {
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
   }
#endif

   return core::Success();
}

} // namespace http
} // namespace session
} // namespace rstudio

#endif
