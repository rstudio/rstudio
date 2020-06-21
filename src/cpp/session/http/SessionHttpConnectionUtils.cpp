/*
 * SessionHttpConnectionUtils.cpp
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


#include "SessionHttpConnectionUtils.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/FilePath.hpp>
#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/FileSerializer.hpp>


#include <core/http/Response.hpp>
#include <core/http/Request.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/r_util/RSessionContext.hpp>

#include <core/system/Interrupts.hpp>

#include <r/RExec.hpp>

#include <session/SessionMain.hpp>
#include <session/SessionOptions.hpp>
#include <session/projects/ProjectsSettings.hpp>

namespace rstudio {
namespace session {

void HttpConnection::sendJsonRpcError(const core::Error& error)
{
   core::json::JsonRpcResponse jsonRpcResponse;
   jsonRpcResponse.setError(error);
   sendJsonRpcResponse(jsonRpcResponse);
}

void HttpConnection::sendJsonRpcResponse()
{
   core::json::JsonRpcResponse jsonRpcResponse;
   sendJsonRpcResponse(jsonRpcResponse);
}

void HttpConnection::sendJsonRpcResponse(
                     const core::json::JsonRpcResponse& jsonRpcResponse)
{
   // setup response
   core::http::Response response;

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
         core::FilePath userScratch = session::options().userScratchPath();
         projects::ProjectsSettings settings(userScratch);
         settings.setNextSessionProject(nextProj);
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

      // kill child processes before going down
      terminateAllChildProcesses();

      // abort the process
      // we no longer do this with ::abort because it generated unwanted exceptions
      // ::_Exit should perform the same functionality (not running destructors and exiting process)
      // without generating an exception
      std::_Exit(EXIT_SUCCESS);
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
   using namespace rstudio::core::json;
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
         using namespace rstudio::core::system;
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

bool checkForInterrupt(boost::shared_ptr<HttpConnection> ptrConnection)
{
   using namespace rstudio::core;
   using namespace rstudio::core::json;
   
   if (!isMethod(ptrConnection, "interrupt"))
      return false;
   
   JsonRpcRequest request;
   Error error = parseJsonRpcRequest(
            ptrConnection->request().body(),
            &request);
   if (error)
   {
      ptrConnection->sendJsonRpcError(error);
   }
   else
   {
      // interrupt the session. note that we call core::system::interrupt()
      // as this will signal the interrupt to all processes in the same
      // process group, which implies that processes launched through e.g.
      // system() in R can be successfully interrupted. however, in some
      // cases, a running application in R might install their own interrupt
      // handler, thereby preventing us from receiving the signal we're now
      // broadcasting. (was observed with Shiny applications on Windows)
      //
      // to ensure that the R session always receives an interrupt, we explicitly
      // set the interrupt flag even though the normal interrupt handler would do
      // the same.
      r::exec::setInterruptsPending(true);
      core::system::interrupt();

      // acknowledge request
      ptrConnection->sendJsonRpcResponse();
   }

   return true;
}

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
} // namespace rstudio


