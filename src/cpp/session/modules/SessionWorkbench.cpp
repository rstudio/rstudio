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
#include <session/SessionUserSettings.hpp>

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
   pResponse->setSuppressDetectChanges(true);

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

CRANMirror toCRANMirror(const json::Object& cranMirrorJson)
{
   CRANMirror cranMirror;
   json::readObject(cranMirrorJson,
                    "name", &cranMirror.name,
                    "host", &cranMirror.host,
                    "url", &cranMirror.url,
                    "country", &cranMirror.country);
   return cranMirror;
}

void setCRANReposOption(const std::string& url)
{
   if (!url.empty())
   {
      Error error = r::exec::RFunction(".rs.setCRANRepos", url).call();
      if (error)
         LOG_ERROR(error);
   }
}

Error setPrefs(const json::JsonRpcRequest& request, json::JsonRpcResponse*)
{
   // read params
   json::Object uiPrefs, generalPrefs, historyPrefs;
   Error error = json::readObjectParam(request.params, 0,
                                       "general_prefs", &generalPrefs,
                                       "history_prefs", &historyPrefs);
   if (error)
      return error;
   error = json::readParam(request.params, 1, &uiPrefs);
   if (error)
      return error;


   // read and set general prefs
   int saveAction;
   bool loadRData;
   std::string initialWorkingDir;
   json::Object cranMirrorJson;
   error = json::readObject(generalPrefs,
                            "save_action", &saveAction,
                            "load_rdata", &loadRData,
                            "initial_working_dir", &initialWorkingDir,
                            "cran_mirror", &cranMirrorJson);
   if (error)
      return error;
   CRANMirror cranMirror = toCRANMirror(cranMirrorJson);
   userSettings().beginUpdate();
   userSettings().setSaveAction(saveAction);
   userSettings().setLoadRData(loadRData);
   userSettings().setInitialWorkingDirectory(FilePath(initialWorkingDir));
   userSettings().setCRANMirror(cranMirror);
   userSettings().endUpdate();

   // NOTE: must be outside of userSettings update block because it has its
   // own block and nested blocks are not supported
   setCRANReposOption(cranMirror.url);


   // read and set history prefs
   bool alwaysSave, useGlobal;
   error = json::readObject(historyPrefs,
                            "always_save", &alwaysSave,
                            "use_global", &useGlobal);
   if (error)
      return error;
   userSettings().beginUpdate();
   userSettings().setAlwaysSaveHistory(alwaysSave);
   userSettings().setUseGlobalHistory(useGlobal);
   userSettings().endUpdate();


   // set ui prefs
   userSettings().setUiPrefs(uiPrefs);

   return Success();
}


Error setUiPrefs(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   json::Object uiPrefs;
   Error error = json::readParams(request.params, &uiPrefs);
   if (error)
      return error;

   userSettings().setUiPrefs(uiPrefs);

   return Success();
}


json::Object toCRANMirrorJson(const CRANMirror& cranMirror)
{
   json::Object cranMirrorJson;
   cranMirrorJson["name"] = cranMirror.name;
   cranMirrorJson["host"] = cranMirror.host;
   cranMirrorJson["url"] = cranMirror.url;
   cranMirrorJson["country"] = cranMirror.country;
   return cranMirrorJson;
}

Error getRPrefs(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   // get general prefs
   json::Object generalPrefs;
   generalPrefs["save_action"] = userSettings().saveAction();
   generalPrefs["load_rdata"] = userSettings().loadRData();
   generalPrefs["initial_working_dir"] = module_context::createAliasedPath(
         userSettings().initialWorkingDirectory());
   generalPrefs["cran_mirror"] = toCRANMirrorJson(userSettings().cranMirror());

   // get history prefs
   json::Object historyPrefs;
   historyPrefs["always_save"] = userSettings().alwaysSaveHistory();
   historyPrefs["use_global"] = userSettings().useGlobalHistory();

   // initialize and set result object
   json::Object result;
   result["general_prefs"] = generalPrefs;
   result["history_prefs"] = historyPrefs;

   pResponse->setResult(result);

   return Success();
}

Error setCRANMirror(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   json::Object cranMirrorJson;
   Error error = json::readParam(request.params, 0, &cranMirrorJson);
   if (error)
      return error;
   CRANMirror cranMirror = toCRANMirror(cranMirrorJson);

   userSettings().beginUpdate();
   userSettings().setCRANMirror(cranMirror);
   userSettings().endUpdate();

   setCRANReposOption(cranMirror.url);

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
      (bind(registerRpcMethod, "set_prefs", setPrefs))
      (bind(registerRpcMethod, "set_ui_prefs", setUiPrefs))
      (bind(registerRpcMethod, "get_r_prefs", getRPrefs))
      (bind(registerRpcMethod, "set_cran_mirror", setCRANMirror));
   return initBlock.execute();
}


} // namepsace workbench
} // namespace modules
} // namesapce session

