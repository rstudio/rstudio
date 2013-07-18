/*
 * SessionHttpConnectionUtils.cpp
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


#include "SessionHttpConnectionUtils.hpp"


#include <boost/algorithm/string/predicate.hpp>

#include <core/FilePath.hpp>
#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FileSerializer.hpp>


#include <core/http/Response.hpp>
#include <core/http/Request.hpp>

#include <core/json/JsonRpc.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionConstants.hpp>



namespace session {

void HttpConnection::sendJsonRpcError(const core::Error& error)
{
   core::json::JsonRpcResponse jsonRpcResponse;
   jsonRpcResponse.setError(error);
   sendJsonRpcResponse(jsonRpcResponse);
}

void HttpConnection::sendJsonRpcResponse()
{
   core::json::JsonRpcResponse jsonRpcResponse ;
   sendJsonRpcResponse(jsonRpcResponse);
}

void HttpConnection::sendJsonRpcResponse(
                     const core::json::JsonRpcResponse& jsonRpcResponse)
{
   // setup response
   core::http::Response response ;

   // automagic gzip support
   if (request().acceptsEncoding(core::http::kGzipEncoding))
      response.setContentEncoding(core::http::kGzipEncoding);

   // set response
   core::json::setJsonRpcResponse(jsonRpcResponse, &response);

   // send the response
   sendResponse(response);
}



namespace connection {

std::string rstudioRequestIdFromRequest(const core::http::Request& request)
{
   return request.headerValue("X-RS-RID");
}


bool isMethod(boost::shared_ptr<HttpConnection> ptrConnection,
                     const std::string& method)
{
   return boost::algorithm::ends_with(ptrConnection->request().uri(),
                                      "rpc/" + method);
}

bool isGetEvents(boost::shared_ptr<HttpConnection> ptrConnection)
{
   return boost::algorithm::ends_with(ptrConnection->request().uri(),
                                      "events/get_events");
}

void handleAbortNextProjParam(
               boost::shared_ptr<HttpConnection> ptrConnection)
{
   std::string nextProj;
   core::json::JsonRpcRequest jsonRpcRequest;
   core::Error error = core::json::parseJsonRpcRequest(
                                         ptrConnection->request().body(),
                                         &jsonRpcRequest);
   if (!error)
   {
      error = core::json::readParam(jsonRpcRequest.params, 0, &nextProj);
      if (error)
         LOG_ERROR(error);

      if (!nextProj.empty())
      {
         // NOTE: this must be synchronized with the implementation of
         // ProjectContext::setNextSessionProject -- we do this using
         // constants rather than code so that this code (which runs in
         // a background thread) don't call into the projects module (which
         // is designed to be foreground and single-threaded)
         core::FilePath userScratch = session::options().userScratchPath();
         core::FilePath settings = userScratch.complete(kProjectsSettings);
         error = settings.ensureDirectory();
         if (error)
            LOG_ERROR(error);
         core::FilePath writePath = settings.complete(kNextSessionProject);
         core::Error error = core::writeStringToFile(writePath, nextProj);
         if (error)
            LOG_ERROR(error);
      }
   }
   else
   {
      LOG_ERROR(error);
   }
}

bool checkForAbort(boost::shared_ptr<HttpConnection> ptrConnection,
                   const boost::function<void()> cleanupHandler)
{
   if (isMethod(ptrConnection, "abort"))
   {
      // respond and log (try/catch so we are ALWAYS guaranteed to abort)
      try
      {
         // handle the nextProj param if it's specified
         handleAbortNextProjParam(ptrConnection);

         // respond
         ptrConnection->sendJsonRpcResponse();

         // log
         LOG_WARNING_MESSAGE("Abort requested");
      }
      catch(...)
      {
      }

      // cleanup (if we don't do this then the user may be locked out of
      // future requests). note that this should occur in the normal
      // course of a graceful shutdown but we do it here anyway just
      // to be paranoid
      try
      {
         if (cleanupHandler)
            cleanupHandler();
      }
      catch(...)
      {
      }

      // abort
      ::abort();
      return true;
   }
   else
   {
      return false;
   }
}

// on windows we allow suspend_session to be handled on the foreground
// thread since we don't have a way to ::kill on that that platform
#ifdef _WIN32

bool checkForSuspend(boost::shared_ptr<HttpConnection> ptrConnection)
{
   return false;
}

#else

bool checkForSuspend(boost::shared_ptr<HttpConnection> ptrConnection)
{
   using namespace core::json;
   if (isMethod(ptrConnection, "suspend_session"))
   {
      bool force = false;
      JsonRpcRequest jsonRpcRequest;
      core::Error error = parseJsonRpcRequest(ptrConnection->request().body(),
                                              &jsonRpcRequest);
      if (error)
      {
         ptrConnection->sendJsonRpcError(error);
      }
      else if ((error = readParam(jsonRpcRequest.params, 0, &force)))
      {
         ptrConnection->sendJsonRpcError(error);
      }
      else
      {
         // send a signal to this process to suspend
         using namespace core::system;
         sendSignalToSelf(force ? SigUsr2 : SigUsr1);

         // send response
         ptrConnection->sendJsonRpcResponse();
      }

      return true;
   }
   else
   {
      return false;
   }
}
#endif

bool authenticate(boost::shared_ptr<HttpConnection> ptrConnection,
                  const std::string& secret)
{
   // allow all requests if no secret
   if (secret.empty())
      return true;

   // validate against shared secret
   return secret == ptrConnection->request().headerValue("X-Shared-Secret");
}

} // namespace connection
} // namespace session


