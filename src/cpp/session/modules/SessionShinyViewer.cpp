/*
 * SessionShinyViewer.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include "SessionShinyViewer.hpp"

#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>
#include <r/ROptions.hpp>

#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace shiny_viewer {

namespace {

// track the current viewed url and path
std::string s_currentAppUrl;
std::string s_currentAppPath;

void enqueStartEvent(const std::string& url, const std::string& path,
                     int viewerType)
{
   // record the url and path
   s_currentAppUrl = module_context::mapUrlPorts(url);
   s_currentAppPath = path;

   // enque the event
   json::Object dataJson;
   dataJson["url"] = s_currentAppUrl;
   dataJson["path"] =
         module_context::createAliasedPath(FilePath(s_currentAppPath));
   dataJson["state"] = "started";
   dataJson["viewer"] = viewerType;
   ClientEvent event(client_events::kShinyViewer, dataJson);
   module_context::enqueClientEvent(event);
}

SEXP rs_shinyviewer(SEXP urlSEXP, SEXP pathSEXP, SEXP viewerSEXP)
{   
   try
   {
      if (!r::sexp::isString(urlSEXP) || (r::sexp::length(urlSEXP) != 1))
      {
         throw r::exec::RErrorException(
            "url must be a single element character vector.");
      }

      if (!r::sexp::isString(pathSEXP) || (r::sexp::length(pathSEXP) != 1))
      {
         throw r::exec::RErrorException(
            "path must be a single element character vector.");
      }
      int viewertype = r::sexp::asInteger(viewerSEXP);

      // in desktop mode make sure we have the right version of httpuv
      if (options().programMode() == kSessionProgramModeDesktop)
      {
         if (!module_context::isPackageVersionInstalled("httpuv", "1.2"))
         {
            module_context::consoleWriteError("\nWARNING: To view Shiny "
              "applications inside RStudio, you need to "
              "install the latest version of the httpuv package from "
              "CRAN (version 1.2 or higher is required).\n\n");
         }
      }

      enqueStartEvent(r::sexp::safeAsString(urlSEXP),
                      r::sexp::safeAsString(pathSEXP),
                      viewertype);
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   return R_NilValue;
}

void setShinyViewerType(int viewerType)
{
   Error error =
      r::exec::RFunction(".rs.setShinyViewerType",
                         viewerType).call();
   if (error)
      LOG_ERROR(error);
}

void onUserSettingsChanged(boost::shared_ptr<int> pShinyViewerType)
{
   int shinyViewerType = userSettings().shinyViewerType();
   if (shinyViewerType != *pShinyViewerType)
   {
      setShinyViewerType(shinyViewerType);
      *pShinyViewerType = shinyViewerType;
   }
}

Error getShinyRunCmd(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string targetPath;
   Error error = json::readParams(request.params, &targetPath);
   if (error)
      return error;

   // Consider: if the shiny namespace is attached to the search path, we
   // don't need to emit "shiny::".
   std::string runCmd = "shiny::runApp(";
   std::string dir = module_context::safeCurrentPath().pathRelativeTo(
            module_context::resolveAliasedPath(targetPath),
            module_context::userHomePath());
   if (dir != ".")
   {
      // runApp defaults to the current working directory, so don't specify
      // it unless we need to.
      runCmd.append("'");
      runCmd.append(dir);
      runCmd.append("'");
   }
   runCmd.append(")");

   json::Object dataJson;
   dataJson["run_cmd"] = runCmd;
   pResponse->setResult(dataJson);

   return Success();
}

Error initShinyViewerPref(boost::shared_ptr<int> pShinyViewerType)
{
   SEXP shinyBrowser = r::options::getOption("shiny.launch.browser");
   *pShinyViewerType = userSettings().shinyViewerType();

   // If the user hasn't specified a value for the shiny.launch.browser
   // preference, set it to the one specified in UI prefs. Note we only
   // do this for shiny >= 0.8 since that is the  version which supports
   // passing a function to shiny.launch.browser
   if (module_context::isPackageVersionInstalled("shiny", "0.8"))
   {
      if (shinyBrowser == R_NilValue)
      {
         setShinyViewerType(*pShinyViewerType);
      }
   }

   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   boost::shared_ptr<int> pShinyViewerType =
         boost::make_shared<int>(SHINY_VIEWER_NONE);

   R_CallMethodDef methodDefViewer;
   methodDefViewer.name = "rs_shinyviewer";
   methodDefViewer.fun = (DL_FUNC) rs_shinyviewer;
   methodDefViewer.numArgs = 3;
   r::routines::addCallMethod(methodDefViewer);

   userSettings().onChanged.connect(bind(onUserSettingsChanged,
                                         pShinyViewerType));

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionShinyViewer.R"))
      (bind(registerRpcMethod, "get_shiny_run_cmd", getShinyRunCmd))
      (bind(initShinyViewerPref, pShinyViewerType));

   return initBlock.execute();
}

} // namespace shiny_viewer
} // namespace modules
} // namesapce session

