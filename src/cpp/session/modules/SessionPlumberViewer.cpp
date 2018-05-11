/*
 * SessionPlumberViewer.cpp
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

#include "SessionPlumberViewer.hpp"

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

#include "plumber/SessionPlumber.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace plumber_viewer {

namespace {

// track a pending Plumber path launch
FilePath s_pendingPlumberPath;

void enqueueStartEvent(const std::string& url, const std::string& path,
                       int viewerType, int options)
{
   FilePath plumberPath(path);
   if (module_context::safeCurrentPath() == plumberPath &&
       !s_pendingPlumberPath.empty())
   {
      // when Plumber starts an app from a anonymous expr (e.g. plumberApp(foo)),
      // it reports the working directory as the app's "path". We sometimes
      // know which file on disk this app corresponds to, so inject that now.
      plumberPath = s_pendingPlumberPath;
   }
   s_pendingPlumberPath = FilePath();

   // enque the event
   json::Object dataJson;
   dataJson["url"] = module_context::mapUrlPorts(url);
   dataJson["path"] = module_context::createAliasedPath(plumberPath);
   dataJson["state"] = "started";
   dataJson["viewer"] = viewerType;
   dataJson["options"] = options;
   ClientEvent event(client_events::kPlumberViewer, dataJson);
   module_context::enqueClientEvent(event);
}

SEXP rs_plumberviewer(SEXP urlSEXP, SEXP pathSEXP, SEXP viewerSEXP)
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
            module_context::consoleWriteError("\nWARNING: To view Plumber "
              "APIs inside RStudio, you need to "
              "install the latest version of the httpuv package from "
              "CRAN (version 1.2 or higher is required).\n\n");
         }
      }

      std::string path = r::sexp::safeAsString(pathSEXP);
      enqueueStartEvent(r::sexp::safeAsString(urlSEXP),
                        path,
                        viewertype,
                        PLUMBER_VIEWER_OPTIONS_NONE);
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   return R_NilValue;
}

void setPlumberViewerType(int viewerType)
{
   Error error =
      r::exec::RFunction(".rs.setPlumberViewerType",
                         viewerType).call();
   if (error)
      LOG_ERROR(error);
}

void onUserSettingsChanged(boost::shared_ptr<int> pPlumberViewerType)
{
   int plumberViewerType = userSettings().plumberViewerType();
   if (plumberViewerType != *pPlumberViewerType)
   {
      setPlumberViewerType(plumberViewerType);
      *pPlumberViewerType = plumberViewerType;
   }
}

Error setPlumberViewer(boost::shared_ptr<int> pPlumberViewerType,
                       const json::JsonRpcRequest& request,
                       json::JsonRpcResponse*)
{
   int viewerType = 0;
   Error error = json::readParams(request.params, &viewerType);
   if (error)
      return error;

   if (viewerType != *pPlumberViewerType)
   {
      setPlumberViewerType(viewerType);
      *pPlumberViewerType = viewerType;
   }

   return Success();
}

Error getPlumberRunCmd(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   std::string targetPath, extendedType;
   Error error = json::readParams(request.params, &targetPath, &extendedType);
   if (error)
      return error;

   FilePath plumberPath = module_context::resolveAliasedPath(targetPath);
   
   // filename "entrypoint.R" has special meaning when running locally or publishing to rsConnect;
   // won't necessarily have any annotations
   bool hasEntrypointFile = false;
   if (plumberPath.stem() == "entrypoint")
   {
      hasEntrypointFile = true;
   }
   else
   {
      // if the folder contains entrypoint.R, use it, even if not the currently loaded file
      FilePath searchFolder = plumberPath.isDirectory() ? plumberPath : plumberPath.parent();
      FilePath entryPointPath = searchFolder.complete("entrypoint.R");
      if (entryPointPath.exists() && !entryPointPath.isDirectory())
         hasEntrypointFile = true;
   }
   
   if (hasEntrypointFile)
   {
      // entrypoint.R mode operates on the folder
      if (!plumberPath.isDirectory())
         plumberPath = plumberPath.parent();
   }

   std::string plumberRunPath = module_context::pathRelativeTo(
            module_context::safeCurrentPath(),
            plumberPath);

   // check to see if Plumber is attached to the search path
   bool isPlumberAttached = false;
   SEXP namespaces = R_NilValue;
   r::sexp::Protect protect;
   error = r::exec::RFunction("search").call(&namespaces, &protect);
   if (error)
   {
      // not fatal; we'll just presume Plumber is not on the path
      LOG_ERROR(error);
   }
   else
   {
      int len = r::sexp::length(namespaces);
      for (int i = 0; i < len; i++)
      {
         std::string ns = r::sexp::safeAsString(STRING_ELT(namespaces, i), "");
         if (ns == "package:plumber") 
         {
            isPlumberAttached = true;
            break;
         }
      }
   }

   std::string runCmd;
   if (!isPlumberAttached)
      runCmd = "plumber::";
   
   if (!hasEntrypointFile)
   {
      runCmd.append(R"RCODE(plumb(file=')RCODE");
      runCmd.append(plumberRunPath);
      runCmd.append(R"RCODE(')$run())RCODE");
   } 
   else
   {
      runCmd.append(R"RCODE(plumb(dir=')RCODE");
      runCmd.append(plumberRunPath);
      runCmd.append(R"RCODE(')$run())RCODE");
   }

   json::Object dataJson;
   dataJson["run_cmd"] = runCmd;
   pResponse->setResult(dataJson);
   return Success();
}

Error initPlumberViewerPref(boost::shared_ptr<int> pPlumberViewerType)
{
   SEXP plumberBrowser = r::options::getOption("plumber.launch.browser");
   *pPlumberViewerType = userSettings().plumberViewerType();
   if (plumberBrowser == R_NilValue)
   {
      setPlumberViewerType(*pPlumberViewerType);
   }

   return Success();
}
 
void onConsoleInput(const std::string& input)
{
   boost::smatch match;

   // capture source commands -- note that this doesn't handle quotes in file
   // names or attempt to balance quote styles, evaluate expressions, etc.
   // (it will primarily detect the output of getPlumberRunCmd but we also want
   // to catch most user-entered source() expressions)
   if (regex_utils::search(input, match,
                 boost::regex("source\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)")))
   {
      // source commands can result in the execution of Plumber API objects, 
      // which Plumber doesn't map back to the original file; keep track of 
      // executions so we can do the mapping ourselves (see comments in 
      // enqueueStartEvent)
      s_pendingPlumberPath = FilePath(
            module_context::resolveAliasedPath(match[1]));
   }
   else
   {
      // not a source command
      s_pendingPlumberPath = FilePath();
   }
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   boost::shared_ptr<int> pPlumberViewerType = boost::make_shared<int>(PLUMBER_VIEWER_NONE);
   json::JsonRpcFunction setPlumberViewerTypeRpc = boost::bind(setPlumberViewer, pPlumberViewerType, _1, _2);

   R_CallMethodDef methodDefViewer;
   methodDefViewer.name = "rs_plumberviewer";
   methodDefViewer.fun = (DL_FUNC) rs_plumberviewer;
   methodDefViewer.numArgs = 3;
   r::routines::addCallMethod(methodDefViewer);

   events().onConsoleInput.connect(onConsoleInput);
   userSettings().onChanged.connect(bind(onUserSettingsChanged,
                                         pPlumberViewerType));

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPlumberViewer.R"))
      (bind(registerRpcMethod, "get_plumber_run_cmd", getPlumberRunCmd))
      (bind(registerRpcMethod, "set_plumber_viewer_type", setPlumberViewerTypeRpc))
      (bind(initPlumberViewerPref, pPlumberViewerType));

   return initBlock.execute();
}

} // namespace plumber_viewer
} // namespace modules
} // namespace session
} // namespace rstudio

