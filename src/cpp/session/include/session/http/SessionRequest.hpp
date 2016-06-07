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
   std::cerr << "request: " << request << std::endl;

#ifdef _WIN32
   // get local peer
   std::string pipeName = core::system::getenv("RS_LOCAL_PEER");
   pRequest->setHeader("X-Shared-Secret",
                       core::system::getenv("RS_SHARED_SECRET"));
   return core::http::sendRequest(pipeName,
                            *pRequest,
                            ConnectionRetryProfile(
                                  boost::posix_time::seconds(10),
                                  boost::posix_time::milliseconds(50)),
                            pResponse);
#else
   std::string tcpipPort = core::system::getenv(kRSessionStandalonePortNumber);
   if (!tcpipPort.empty())
   {
      return core::http::sendRequest("127.0.0.1", tcpipPort, request, 
            pResponse);
   }
   else
   {
      // determine stream path
      std::string stream = core::system::getenv(kRStudioSessionStream);
      core::FilePath streamPath = session::local_streams::streamPath(stream);
      return core::http::sendRequest(streamPath, request, pResponse);
   }
#endif

   return core::Success();
}

} // namespace http
} // namespace session
} // namespace rstudio

#endif
