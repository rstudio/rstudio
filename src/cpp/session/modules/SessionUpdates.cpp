/*
 * SessionUpdates.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionUpdates.hpp"

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

#include <boost/bind/bind.hpp>

#include <session/SessionModuleContext.hpp>

#include <session/prefs/UserPrefs.hpp>

#include <string>

#include "session-config.h"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace updates {
namespace {

json::Object jsonFromProcessResult(const core::system::ProcessResult& result)
{
   json::Object obj;
   std::stringstream output(result.stdOut);
   // The output looks like:
   // key1=value1
   // key2=value2
   // ...
   for (std::string line; std::getline(output, line); )
   {
      size_t pos = line.find('=');
      if (pos > 0)
      {
         obj[line.substr(0, pos)] = line.substr(pos + 1,
                                                line.length() - (pos + 1));
      }
   }
   return obj;
}

void beginUpdateCheck(bool manual,
   const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   using namespace module_context;

   // Find the path to R 
   FilePath rProgramPath;
   Error error = module_context::rScriptPath(&rProgramPath);
   if (error)
   {
      return;
   }

   // Find the path to the script we need to source
   FilePath modulesPath = session::options().modulesRSourcePath();;
   std::string scriptPath = core::string_utils::utf8ToSystem(
      modulesPath.completePath("SessionUpdates.R").getAbsolutePath());

   // Arguments
   std::vector<std::string> args;
   args.push_back("--vanilla");
   args.push_back("-s");
   args.push_back("-e");
   
   // Build the command to send to R
   std::string cmd;
   cmd.append("source('");
   cmd.append(string_utils::jsLiteralEscape(scriptPath));
   cmd.append("'); downloadUpdateInfo('");
   
   // Testing/debugging feature: set the RStudio version you want to impersonate for the 
   // update check via the environment variable RSTUDIO_UPDATE_VERSION, e.g.
   // RSTUDIO_UPDATE_VERSION=2022.07.2+576 (~/.Renviron is a good place to set this)
   std::string versionOverride = core::system::getenv("RSTUDIO_UPDATE_VERSION");
   if (!versionOverride.empty())
      cmd.append(http::util::urlEncode(versionOverride));
   else
      cmd.append(http::util::urlEncode(RSTUDIO_VERSION));
   cmd.append("', '");
#if defined(_WIN32)
   cmd.append("windows");
#elif defined(__APPLE__)
   cmd.append("mac");
#else
   cmd.append("linux");
#endif
   cmd.append("', ");
   cmd.append(manual ? "TRUE" : "FALSE");
   cmd.append(", ");
   cmd.append(haveSecureDownloadFileMethod() ? "TRUE" : "FALSE");
   cmd.append(", '");
   cmd.append(downloadFileMethod("auto"));
   cmd.append("'");
   cmd.append(")");
   args.push_back(cmd);

   LOG_DEBUG_MESSAGE("Checking for updates with command: " + cmd);
   
   // Set options
   core::system::ProcessOptions options;
   options.terminateChildren = true;

   module_context::processSupervisor().runProgram(
      rProgramPath.getAbsolutePath(),
      args,
      std::string(),
      options,
      onCompleted);
}

void endRPCUpdateCheck(const json::JsonRpcFunctionContinuation& cont,
                       const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;
   response.setResult(jsonFromProcessResult(result));
   if (result.stdErr.empty())
   {
      cont(Success(), &response);
   }
   else
   {
      Error error(json::errc::ConnectionError, "Could not check for updates. Please check the network connection.", ERROR_LOCATION);
      error.addProperty("r-error", result.stdErr);
      LOG_ERROR(error);
      cont(error, &response);
   }
}
   
void checkForUpdates(const json::JsonRpcRequest& request,
                     const json::JsonRpcFunctionContinuation& cont)
{
   bool manual = false;
   Error error = json::readParam(request.params, 0, &manual);
   if (error)
   {
      json::JsonRpcResponse response;
      cont(error, &response);
      return;
   }
   beginUpdateCheck(manual, boost::bind(endRPCUpdateCheck, cont, _1));
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerAsyncRpcMethod, "check_for_updates", checkForUpdates))
   ;
   return initBlock.execute();
}

} // namespace updates
} // namespace modules
} // namespace session
} // namespace rstudio
