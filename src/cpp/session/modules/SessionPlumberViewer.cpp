/*
 * SessionPlumberViewer.cpp
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

#include "SessionPlumberViewer.hpp"

#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>
#include <r/ROptions.hpp>

#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUrlPorts.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "plumber/SessionPlumber.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace plumber_viewer {

namespace {

void enqueueStartEvent(const std::string& url, const std::string& path,
                       const std::string& viewerType, int options)
{
   FilePath plumberPath(path);

   // enque the event
   json::Object dataJson;
   dataJson["url"] = url_ports::mapUrlPorts(url);
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
      std::string viewertype = r::sexp::safeAsString(viewerSEXP, kPlumberViewerTypeWindow);

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

void setPlumberViewerType(const std::string& viewerType)
{
   Error error =
      r::exec::RFunction(".rs.setPlumberViewerType",
                         viewerType).call();
   if (error)
      LOG_ERROR(error);
}

void onUserSettingsChanged(const std::string& pref, 
      boost::shared_ptr<std::string> pPlumberViewerType)
{
   if (pref != kPlumberViewerType)
      return;

   std::string plumberViewerType = prefs::userPrefs().plumberViewerType();
   setPlumberViewerType(plumberViewerType);
   *pPlumberViewerType = plumberViewerType;
}

Error getPlumberRunCmd(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   std::string targetPath;
   Error error = json::readParams(request.params, &targetPath);
   if (error)
      return error;

   FilePath plumberPath = module_context::resolveAliasedPath(targetPath);
   
   // Existence of "entrypoint.R" requires a different form of Plumber run command
   bool hasEntrypointFile = false;
   FilePath searchFolder = plumberPath.isDirectory() ? plumberPath : plumberPath.getParent();
   std::vector<FilePath> children;
   error = searchFolder.getChildren(children);
   if (error)
      return error;

   for (const auto& child : children)
   {
      if (!child.isDirectory() && string_utils::toLower(child.getFilename()) == "entrypoint.r")
      {
         hasEntrypointFile = true;
         break;
      }
   }

   if (hasEntrypointFile)
   {
      // entrypoint.R mode operates on the folder
      if (!plumberPath.isDirectory())
         plumberPath = plumberPath.getParent();
   }

   std::string plumberRunPath = module_context::pathRelativeTo(
            module_context::safeCurrentPath(),
            plumberPath);

   // check to see if Plumber is attached to the search path
   bool isPlumberAttached = r::util::isPackageAttached("plumber");
   
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

Error initPlumberViewerPref(boost::shared_ptr<std::string> pPlumberViewerType)
{
   SEXP plumberBrowser = r::options::getOption("plumber.docs.callback");
   *pPlumberViewerType = prefs::userPrefs().plumberViewerType();
   if (plumberBrowser == R_NilValue)
   {
      setPlumberViewerType(*pPlumberViewerType);
   }

   return Success();
}
 
} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   boost::shared_ptr<std::string> pPlumberViewerType = boost::make_shared<std::string>(
         kPlumberViewerTypeNone);

   RS_REGISTER_CALL_METHOD(rs_plumberviewer);

   prefs::userPrefs().onChanged.connect(bind(
            onUserSettingsChanged, _2, pPlumberViewerType));

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPlumberViewer.R"))
      (bind(registerRpcMethod, "get_plumber_run_cmd", getPlumberRunCmd))
      (bind(initPlumberViewerPref, pPlumberViewerType));

   return initBlock.execute();
}

} // namespace plumber_viewer
} // namespace modules
} // namespace session
} // namespace rstudio

