/*
 * SessionShinyViewer.cpp
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

#include "SessionShinyViewer.hpp"

#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RJson.hpp>
#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>
#include <r/ROptions.hpp>

#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUrlPorts.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "shiny/SessionShiny.hpp"
#include "shiny/ShinyAsyncJob.hpp"
#include "jobs/AsyncRJobManager.hpp"
#include "session-config.h"

#define kForegroudAppId "foreground"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace shiny_viewer {

enum AppDestination
{
   ForegroundApp,
   BackgroundApp
};

namespace {


// track a pending Shiny path launch
FilePath s_pendingShinyPath;

void enqueueStartEvent(const std::string& url,
                       const std::string& path,
                       const std::string& viewerType,
                       const json::Value& meta,
                       int options)
{
   FilePath shinyPath(path);
   if (module_context::safeCurrentPath() == shinyPath &&
       !s_pendingShinyPath.isEmpty())
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
   dataJson["meta"] = meta;
   dataJson["options"] = options;
   dataJson["id"] = kForegroudAppId;
   ClientEvent event(client_events::kShinyViewer, dataJson);
   module_context::enqueClientEvent(event);
}

SEXP rs_shinyviewer(SEXP urlSEXP,
                    SEXP pathSEXP,
                    SEXP viewerSEXP,
                    SEXP metaSEXP)
{   
   try
   {
      if (!r::sexp::isString(urlSEXP) || (r::sexp::length(urlSEXP) != 1))
      {
         throw r::exec::RErrorException(
            "url must be a single element character vector.");
      }
      std::string url = r::sexp::safeAsString(urlSEXP);

      if (!r::sexp::isString(pathSEXP) || (r::sexp::length(pathSEXP) != 1))
      {
         throw r::exec::RErrorException(
            "path must be a single element character vector.");
      }
      std::string viewertype = r::sexp::safeAsString(viewerSEXP, kShinyViewerTypeWindow);

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
          viewertype = kShinyViewerTypeWindow;
          options = SHINY_VIEWER_OPTIONS_NOTOOLS;
      }
      if (path.find("/shinytest/recorder") != std::string::npos)
      {
          options |= SHINY_VIEWER_OPTIONS_WIDE;
      }
      
      json::Value meta;
      Error error = r::json::jsonValueFromObject(metaSEXP, &meta);
      if (error)
      {
         throw r::exec::RErrorException(
                  "meta must be NULL or a named R list");
      }

      enqueueStartEvent(
               url,
               path,
               viewertype,
               meta,
               options);
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   return R_NilValue;
}

void setShinyViewerType(const std::string& viewerType)
{
   Error error =
      r::exec::RFunction(".rs.setShinyViewerType",
                         viewerType).call();
   if (error)
      LOG_ERROR(error);
}

void onUserSettingsChanged(const std::string& pref,
      boost::shared_ptr<std::string> pShinyViewerType)
{
   if (pref != kShinyViewerType)
      return;

   std::string shinyViewerType = prefs::userPrefs().shinyViewerType();
   setShinyViewerType(shinyViewerType);
   *pShinyViewerType = shinyViewerType;
}

Error setShinyViewer(boost::shared_ptr<std::string> pShinyViewerType,
                     const json::JsonRpcRequest& request,
                     json::JsonRpcResponse*)
{
   std::string viewerType;
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

/**
 * Generates an R string to run the given Shiny application
 */
std::string shinyRunCmd(const std::string& targetPath,
      const FilePath& workingDir,
      const std::string& extendedType, 
      AppDestination dest)
{
   modules::shiny::ShinyFileType shinyType = 
      modules::shiny::shinyTypeFromExtendedType(extendedType);

   // resolve the file path we were passed; if this .R file belongs to a 
   // Shiny directory, use the parent
   FilePath shinyPath = module_context::resolveAliasedPath(targetPath);
   if (shinyType == modules::shiny::ShinyDirectory)
      shinyPath = shinyPath.getParent();

   // check to see if Shiny is attached to the search path, if we're running the app in the
   // foreground; in the background we start with a clean session so Shiny is never attached
   bool isShinyAttached = false;
   if (dest == AppDestination::ForegroundApp)
   {
      isShinyAttached = r::util::isPackageAttached("shiny");
   }

   // for brevity, specify the app path as relative to the working directory
   std::string runPath = module_context::pathRelativeTo(workingDir, shinyPath);

   std::string runCmd;

   if (shinyType == modules::shiny::ShinyDirectory)
   {
      if (!isShinyAttached)
         runCmd = "shiny::";
      runCmd.append("runApp(");
      if (runPath != ".")
      {
         // runApp defaults to the current working directory, so don't specify
         // it unless we need to.
         runCmd.append("'");
         runCmd.append(runPath);
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
         runCmd.append("runApp('" + runPath + "')");
      }
      else
      {
         if (shinyType == modules::shiny::ShinySingleFile)
            runCmd.append("print(");
         runCmd.append("source('");
         runCmd.append(runPath);
         runCmd.append("')");
         if (shinyType == modules::shiny::ShinySingleFile)
            runCmd.append("$value)");
      }
   }

   return runCmd;
}

Error runShinyBackgroundApp(boost::shared_ptr<std::string> pShinyViewerType,
                            const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   std::string targetPath, extendedType;
   Error error = json::readParams(request.params, &targetPath, &extendedType);
   if (error)
      return error;

   // always run the Shiny application from its own parent folder for consistency
   FilePath appPath = module_context::resolveAliasedPath(targetPath).getParent();

   // create the command that will be used to run the Shiny application in the background R session
   std::string cmd = shinyRunCmd(targetPath, appPath, extendedType, 
         AppDestination::BackgroundApp);

   // create the asynchronous R job that will be used to run the Shiny application
   boost::shared_ptr<shiny::ShinyAsyncJob> pJob = boost::make_shared<shiny::ShinyAsyncJob>(
         "Shiny: " + appPath.getFilename(),
         module_context::resolveAliasedPath(targetPath),
         *pShinyViewerType,
         cmd);

   // register it and create an ID
   std::string id;
   error = jobs::registerAsyncRJob(pJob, &id);
   if (error)
      return error;

   // start the job (actually creates the underlying R session)
   pJob->start();

   // return the ID we created
   pResponse->setResult(id);
   return Success();
}

Error getShinyRunCmd(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string targetPath, extendedType;
   Error error = json::readParams(request.params, &targetPath, &extendedType);
   if (error)
      return error;

   json::Object dataJson;
   dataJson["run_cmd"] = shinyRunCmd(targetPath, module_context::safeCurrentPath(),
         extendedType, AppDestination::ForegroundApp);
   pResponse->setResult(dataJson);

   return Success();
}

Error initShinyViewerPref(boost::shared_ptr<std::string> pShinyViewerType)
{
   SEXP shinyBrowser = r::options::getOption("shiny.launch.browser");
   *pShinyViewerType = prefs::userPrefs().shinyViewerType();
   if (shinyBrowser == R_NilValue)
   {
      setShinyViewerType(*pShinyViewerType);
   }
   else
   {
      // the functions used to invoke the Shiny viewer changed in v1.2 -> v1.3;
      // however, the older versions of these functions may be persisted as part
      // of the R options (for shiny.launch.browser). for that reason, we detect
      // if this option is set and ensure an up-to-date copy of the function is
      // used as appropriate
      Error error = r::exec::RFunction(".rs.refreshShinyLaunchBrowserOption").call();
      if (error)
         LOG_ERROR(error);
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

   boost::shared_ptr<std::string> pShinyViewerType =
         boost::make_shared<std::string>(kShinyViewerTypeNone);

   json::JsonRpcFunction setShinyViewerTypeRpc =
         boost::bind(setShinyViewer, pShinyViewerType, _1, _2);

   json::JsonRpcFunction runShinyBackground =
         boost::bind(runShinyBackgroundApp, pShinyViewerType, _1, _2);

   RS_REGISTER_CALL_METHOD(rs_shinyviewer);

   events().onConsoleInput.connect(onConsoleInput);
   prefs::userPrefs().onChanged.connect(bind(onUserSettingsChanged, _2,
                                         pShinyViewerType));

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionShinyViewer.R"))
      (bind(registerRpcMethod, "get_shiny_run_cmd", getShinyRunCmd))
      (bind(registerRpcMethod, "set_shiny_viewer_type", setShinyViewerTypeRpc))
      (bind(registerRpcMethod, "run_shiny_background_app", runShinyBackground))
      (bind(initShinyViewerPref, pShinyViewerType));

   return initBlock.execute();
}

} // namespace shiny_viewer
} // namespace modules
} // namespace session
} // namespace rstudio

