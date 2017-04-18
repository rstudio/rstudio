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

#include "shiny/SessionShiny.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace shiny_viewer {

namespace {

// track a pending Shiny path launch
FilePath s_pendingShinyPath;

void enqueueStartEvent(const std::string& url, const std::string& path,
                     int viewerType)
{
   FilePath shinyPath(path);
   if (module_context::safeCurrentPath() == shinyPath &&
       !s_pendingShinyPath.empty())
   {
      // when Shiny starts an app from a anonymous expr (e.g. shinyApp(foo)),
      // it reports the working directory as the app's "path". We sometimes
      // know which file on disk this app corresponds to, so inject that now.
      shinyPath = s_pendingShinyPath;
   }
   s_pendingShinyPath = FilePath();

   // enque the event
   json::Object dataJson;
   dataJson["url"] = module_context::mapUrlPorts(url);
   dataJson["path"] = module_context::createAliasedPath(shinyPath);
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

      enqueueStartEvent(r::sexp::safeAsString(urlSEXP),
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

Error setShinyViewer(boost::shared_ptr<int> pShinyViewerType,
                     const json::JsonRpcRequest& request,
                     json::JsonRpcResponse*)
{
   int viewerType = 0;
   Error error = json::readParams(request.params, &viewerType);
   if (error)
      return error;

   if (viewerType != *pShinyViewerType)
   {
      setShinyViewerType(viewerType);
      *pShinyViewerType = viewerType;
   }

   return Success();
}

Error getShinyRunCmd(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string targetPath, extendedType;
   Error error = json::readParams(request.params, &targetPath, &extendedType);
   if (error)
      return error;

   modules::shiny::ShinyFileType shinyType = 
      modules::shiny::shinyTypeFromExtendedType(extendedType);

   // resolve the file path we were passed; if this .R file belongs to a 
   // Shiny directory, use the parent
   FilePath shinyPath = module_context::resolveAliasedPath(targetPath); 
   if (shinyType == modules::shiny::ShinyDirectory)
      shinyPath = shinyPath.parent();

   std::string shinyRunPath = module_context::pathRelativeTo(
            module_context::safeCurrentPath(),
            shinyPath);

   // check to see if Shiny is attached to the search path
   bool isShinyAttached = false;
   SEXP namespaces = R_NilValue;
   r::sexp::Protect protect;
   error = r::exec::RFunction("search").call(&namespaces, &protect);
   if (error)
   {
      // not fatal; we'll just presume Shiny is not on the path
      LOG_ERROR(error);
   }
   else
   {
      int len = r::sexp::length(namespaces);
      for (int i = 0; i < len; i++)
      {
         std::string ns = r::sexp::safeAsString(STRING_ELT(namespaces, i), "");
         if (ns == "package:shiny") 
         {
            isShinyAttached = true;
            break;
         }
      }
   }

   std::string runCmd; 
   if (shinyType == modules::shiny::ShinyDirectory)
   {
      if (!isShinyAttached)
         runCmd = "shiny::";
      runCmd.append("runApp(");
      if (shinyRunPath != ".")
      {
         // runApp defaults to the current working directory, so don't specify
         // it unless we need to.
         runCmd.append("'");
         runCmd.append(shinyRunPath);
         runCmd.append("'");
      }
      runCmd.append(")");
   }
   else if (shinyType == modules::shiny::ShinySingleFile ||
            shinyType == modules::shiny::ShinySingleExecutable) 
   {
      if (!isShinyAttached)
         runCmd = "library(shiny); ";
      
      if (module_context::isPackageVersionInstalled("shiny", "0.13.0") &&
          shinyType == modules::shiny::ShinySingleFile)
      {
         runCmd.append("runApp('" + shinyRunPath + "')");
      }
      else
      {
         if (shinyType == modules::shiny::ShinySingleFile)
            runCmd.append("print(");
         runCmd.append("source('");
         runCmd.append(shinyRunPath);
         runCmd.append("')");
         if (shinyType == modules::shiny::ShinySingleFile)
            runCmd.append("$value)");
      }
   }

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
 
void onConsoleInput(const std::string& input)
{
   boost::smatch match;

   // capture source commands -- note that this doesn't handle quotes in file
   // names or attempt to balance quote styles, evaluate expressions, etc.
   // (it will primarily detect the output of getShinyRunCmd but we also want
   // to catch most user-entered source() expressions)
   if (regex_utils::search(input, match,
                 boost::regex("source\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)")))
   {
      // source commands can result in the execution of Shiny app objects, 
      // which Shiny doesn't map back to the original file; keep track of 
      // executions so we can do the mapping ourselves (see comments in 
      // enqueueStartEvent)
      s_pendingShinyPath = FilePath(
            module_context::resolveAliasedPath(match[1]));
   }
   else
   {
      // not a source command
      s_pendingShinyPath = FilePath();
   }
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   boost::shared_ptr<int> pShinyViewerType =
         boost::make_shared<int>(SHINY_VIEWER_NONE);

   json::JsonRpcFunction setShinyViewerTypeRpc =
         boost::bind(setShinyViewer, pShinyViewerType, _1, _2);

   R_CallMethodDef methodDefViewer;
   methodDefViewer.name = "rs_shinyviewer";
   methodDefViewer.fun = (DL_FUNC) rs_shinyviewer;
   methodDefViewer.numArgs = 3;
   r::routines::addCallMethod(methodDefViewer);

   events().onConsoleInput.connect(onConsoleInput);
   userSettings().onChanged.connect(bind(onUserSettingsChanged,
                                         pShinyViewerType));

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionShinyViewer.R"))
      (bind(registerRpcMethod, "get_shiny_run_cmd", getShinyRunCmd))
      (bind(registerRpcMethod, "set_shiny_viewer_type", setShinyViewerTypeRpc))
      (bind(initShinyViewerPref, pShinyViewerType));

   return initBlock.execute();
}

} // namespace shiny_viewer
} // namespace modules
} // namespace session
} // namespace rstudio

