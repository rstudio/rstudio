/*
 * SessionWorkbench.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionWorkbench.hpp"

#include <algorithm>

#include <boost/function.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/ROptions.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RClientState.hpp> 
#include <r/RFunctionHook.hpp>

#include <session/SessionModuleContext.hpp>

#include <R_ext/RStartup.h>
extern "C" SA_TYPE SaveAction;

using namespace core;

namespace session {
namespace modules { 
namespace workbench {

namespace {   
      
Error setClientState(const json::JsonRpcRequest& request, 
                     json::JsonRpcResponse* pResponse)
{   
   // extract params
   json::Object temporaryState, persistentState;
   Error error = json::readParams(request.params, 
                                  &temporaryState,
                                  &persistentState);
   if (error)
      return error ;
   
   // set state
   r::session::ClientState& clientState = r::session::clientState();
   clientState.putTemporary(temporaryState);
   clientState.putPersistent(persistentState);
   
   return Success();
}
   
     
// IN: WorkbenchMetrics object
// OUT: Void
Error setWorkbenchMetrics(const json::JsonRpcRequest& request, 
                          json::JsonRpcResponse* pResponse)
{
   // extract fields
   r::session::RClientMetrics metrics ;
   Error error = json::readObjectParam(request.params, 0,
                                 "consoleWidth", &(metrics.consoleWidth),
                                 "graphicsWidth", &(metrics.graphicsWidth),
                                 "graphicsHeight", &(metrics.graphicsHeight));
   if (error)
      return error;
   
   // set the metrics
   r::session::setClientMetrics(metrics);
   
   return Success();
}
   
// options("pdfviewer")
void viewPdfPostback(const std::string& pdfPath)
{
   module_context::showFile(FilePath(pdfPath));
}


void handleFileShow(const http::Request& request, http::Response* pResponse)
{
   // get the file path
   FilePath filePath(request.queryParamValue("path"));
   if (!filePath.exists())
   {
      pResponse->setError(http::status::NotFound, "File not found");
      return;
   }

   // send it back
   pResponse->setCacheableFile(filePath, request);
}

Error setSaveAction(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // read params
   std::string saveAction;
   Error error = json::readParams(request.params, &saveAction);
   if (error)
      return error;

   if (saveAction == "yes")
      SaveAction = SA_SAVE;
   else if (saveAction == "no")
      SaveAction = SA_NOSAVE;
   else if (saveAction == "ask")
      SaveAction = SA_SAVEASK;
   else
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   return Success();
}

SEXP capabilitiesX11Hook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   r::sexp::Protect rProtect;
   return r::sexp::create(false, &rProtect);
}

}
   
Error initialize()
{
   // register postback handler for viewPDF (server-only)
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      std::string pdfShellCommand ;
      Error error = module_context::registerPostbackHandler("pdfviewer",
                                                            viewPdfPostback,
                                                            &pdfShellCommand);
      if (error)
         return error ;

      // set pdfviewer option
      error = r::options::setOption("pdfviewer", pdfShellCommand);
      if (error)
         return error ;

      // ensure that capabilitiesX11 always returns false
      error = r::function_hook::registerReplaceHook("capabilitiesX11",
                                                    capabilitiesX11Hook,
                                                    (CCODE*)NULL);
      if (error)
         return error;
   }
   
   // complete initialization
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerUriHandler, "/file_show", handleFileShow))
      (bind(registerRpcMethod, "set_client_state", setClientState))
      (bind(registerRpcMethod, "set_workbench_metrics", setWorkbenchMetrics))
      (bind(registerRpcMethod, "set_save_action", setSaveAction));
   return initBlock.execute();
}


} // namepsace workbench
} // namespace modules
} // namesapce session

