/*
 * SessionShinyViewer.cpp
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
#include <session/SessionUrlPorts.hpp>
#include <session/SessionUserSettings.hpp>

#include "shiny/SessionShiny.hpp"
#include "session-config.h"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace shiny_viewer {

namespace {

// track a pending Shiny path launch
FilePath s_pendingShinyPath;

void enqueueStartEvent(const std::string& url, const std::string& path,
                     int viewerType, int options)
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
   dataJson["url"] = url_ports::mapUrlPorts(url);
   dataJson["path"] = module_context::createAliasedPath(shinyPath);
   dataJson["state"] = "started";
   dataJson["viewer"] = viewerType;
   dataJson["options"] = options;
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

      int options = SHINY_VIEWER_OPTIONS_NONE;

      std::string path = r::sexp::safeAsString(pathSEXP);
      if (path.find("/shinytest/recorder") != std::string::npos ||
          path.find("/shinytest/diffviewerapp") != std::string::npos)
      {
          viewertype = SHINY_VIEWER_WINDOW;
          options = SHINY_VIEWER_OPTIONS_NOTOOLS;
      }
      if (path.find("/shinytest/recorder") != std::string::npos)
      {
          options |= SHINY_VIEWER_OPTIONS_WIDE;
      }

      enqueueStartEvent(r::sexp::safeAsString(urlSEXP),
                        path,
                        viewertype,
                        options);
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
   bool isShinyAttached = r::util::isPackageAttached("shiny");
   
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
   if (shinyBrowser == R_NilValue)
   {
      setShinyViewerType(*pShinyViewerType);
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

   RS_REGISTER_CALL_METHOD(rs_shinyviewer);

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

