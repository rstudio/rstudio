/*
 * SessionRequest.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_REQUEST_HPP
#define SESSION_REQUEST_HPP

#include <shared_core/Error.hpp>
#include <core/system/Environment.hpp>

#include <session/http/SessionRequest.hpp>

#include <core/http/TcpIpBlockingClient.hpp>
#include <core/http/TcpIpBlockingClientSsl.hpp>
#include <core/http/ConnectionRetryProfile.hpp>
#include <core/http/CSRFToken.hpp>

#ifndef _WIN32
#include <core/http/LocalStreamBlockingClient.hpp>
#include <session/SessionLocalStreams.hpp>
#endif

#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <server_core/sessions/SessionSignature.hpp>
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
                                      const bool ssl,
                                      core::http::Response* pResponse)
{
   // build request
   core::http::Request request;
   request.setMethod("POST");
   request.setUri(uri);
   request.setHeader("Accept", "*/*");
   request.setHeader("Connection", "close");
   request.setHeader("X-Session-Postback", "1");
   request.setHeader(kRStudioUserIdentityDisplay, core::system::username());

   // generate random CSRF token for request
   std::string token = core::system::generateUuid();
   request.setHeader(kCSRFTokenHeader, token);
   request.addCookie(kCSRFTokenCookie, token);

   // NOTE: Remove block when Ghost Orchid 2021.09 is not supported ======================================
   // Set the old cookie and header as well, for backward compatibility
   request.setHeader(kOldCSRFTokenHeader, token);
   request.addCookie(kOldCSRFTokenCookie, token);
   // ====================================================================================================
   
   request.setBody(body);

   // first, attempt to send a plain old http request
   std::string tcpipPort = core::system::getenv(kRSessionStandalonePortNumber);
   if (tcpipPort.empty())
   {
      // if no standalone port, grab regular port number (desktop mode)
      tcpipPort = core::system::getenv(kRSessionPortNumber);
   }

   if (!tcpipPort.empty())
   {
      std::string sharedSecret = core::system::getenv("RS_SHARED_SECRET");
      if (!sharedSecret.empty())
      {
         // if we have a shared secret to use (like in desktop mode), simply stamp it on the request
         request.setHeader("X-Shared-Secret", sharedSecret);
      }
#ifdef RSTUDIO_SERVER
      else
      {
         // otherwise, we need to authenticate via message signing with RSA crypto
         core::Error error = server_core::sessions::signRequest(
                  core::system::getenv("RSTUDIO_SESSION_RSA_PRIVATE_KEY"),
                  request);
         if (error)
            return error;

      }
#endif
      // Need to turn verify off here because we don't know the address used in the cert. Because we are listening on 127.0.0.1, 
      // and the tcp stack forces that to be a local connection, there's no ambiguity about who we are connecting to so I don't
      // think verification adds anything here.
      if (ssl)
         return core::http::sendSslRequest("127.0.0.1", tcpipPort, false /* verify cert */, request,  pResponse);
      else
         return core::http::sendRequest("127.0.0.1", tcpipPort, request,  pResponse);
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
      streamPath = local_streams::streamPath(stream);
   }
   return core::http::sendRequest(streamPath, request, pResponse);
#endif

   return core::Success();
}

} // namespace http
} // namespace session
} // namespace rstudio

#endif
