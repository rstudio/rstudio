/*
 * SessionUpdates.cpp
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
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

#include "SessionUpdates.hpp"

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/system/Process.hpp>

#include <boost/bind.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include <string>

using namespace core;

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

void endBootUpdateCheck(const core::system::ProcessResult& result)
{
   json::Object obj = jsonFromProcessResult(result);
   ClientEvent event (client_events::kUpdateCheck, obj);
   module_context::enqueClientEvent(event);
}
   
void beginUpdateCheck(bool manual, 
   const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
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
                     modulesPath.complete("SessionUpdates.R").absolutePath());

   // Arguments
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--vanilla");
#if defined(_WIN32)
   if (userSettings().useInternet2())
   {
      args.push_back("--internet2");
   }
#endif
   args.push_back("-e");
   
   // Build the command to send to R
   std::string cmd;
   cmd.append("source('");
   cmd.append(string_utils::jsLiteralEscape(scriptPath));
   cmd.append("'); downloadUpdateInfo('");
#if defined(_WIN32)
   cmd.append("windows");
#elif defined(__APPLE__)
   cmd.append("mac");
#else
   cmd.append("linux");
#endif
   cmd.append("', ");
   cmd.append(manual ? "TRUE" : "FALSE");
   cmd.append(")");
   args.push_back(cmd);
   
   // Set options
   core::system::ProcessOptions options;
   options.terminateChildren = true;

   module_context::processSupervisor().runProgram(rProgramPath.absolutePath(),
                                  args,
                                  std::string(),
                                  options,
                                  onCompleted);
}

void endRPCUpdateCheck(const json::JsonRpcFunctionContinuation& cont,
                       const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;
   response.setResponse(jsonFromProcessResult(result));
   cont(Success(), &response);
}
   
void checkForUpdates(const json::JsonRpcRequest& request,
                     const json::JsonRpcFunctionContinuation& cont)
{
   beginUpdateCheck(true, boost::bind(endRPCUpdateCheck, cont, _1));
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // Only check for updates in desktop mode
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      events().onDeferredInit.connect(boost::bind(beginUpdateCheck,
                                                  false,
                                                  endBootUpdateCheck));
   }

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerAsyncRpcMethod, "check_for_updates", checkForUpdates))
   ;
   return initBlock.execute();
}

} // namespace updates
} // namespace modules
} // namespace session
