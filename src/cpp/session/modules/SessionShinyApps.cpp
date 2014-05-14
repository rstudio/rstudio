/*
 * SessionShinyApps.cpp
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

#include "SessionShinyApps.hpp"

#include <boost/algorithm/string.hpp>
#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>

#define kFinishedMarker "ShinyApps deployment completed: "

using namespace core;

namespace session {
namespace modules { 
namespace shiny_apps {

namespace {

class ShinyAppDeploy : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<ShinyAppDeploy> create(
         const std::string& dir,
         const std::string& account,
         const std::string& app)
   {
      boost::shared_ptr<ShinyAppDeploy> pDeploy(new ShinyAppDeploy());
      std::string cmd("shinyapps::deployApp("
            "appDir = '" + dir + "'," 
            "account = '" + account + "',"
            "appName = '" + app + "', " 
            "launch.browser = function (url) { "
            "   message('" kFinishedMarker "', url) "
            "})");
      pDeploy->start(cmd.c_str(), FilePath());
      return pDeploy;
   }

private:
   void onStderr(const std::string& output)
   {
      onOutput(module_context::kCompileOutputNormal, output);
   }

   void onStdout(const std::string& output)
   {
      onOutput(module_context::kCompileOutputError, output);
   }

   void onOutput(int type, const std::string& output)
   {
      r::sexp::Protect protect;
      Error error;

      // look on each line of emitted output to see whether it contains the
      // finished marker
      std::vector<std::string> lines;
      boost::algorithm::split(lines, output,
                              boost::algorithm::is_any_of("\n\r"));
      int ncharMarker = sizeof(kFinishedMarker) - 1;
      BOOST_FOREACH(std::string& line, lines)
      {
         if (line.substr(0, ncharMarker) == kFinishedMarker)
         {
            deployedUrl_ = line.substr(ncharMarker, line.size() - ncharMarker);
         }
      }

      // emit the output to the client for display
      module_context::CompileOutput deployOutput(type, output);
      ClientEvent event(client_events::kRmdShinyAppsDeploymentOutput, 
                        module_context::compileOutputAsJson(deployOutput));
      module_context::enqueClientEvent(event);
   }

   void onCompleted(int exitStatus)
   {
      // when the process completes, emit the discovered URL, if any
      ClientEvent event(client_events::kRmdShinyAppsDeploymentCompleted, 
                        deployedUrl_);
      module_context::enqueClientEvent(event);
   }

   std::string deployedUrl_;
};

boost::shared_ptr<ShinyAppDeploy> s_pShinyAppDeploy_;

Error deployShinyApp(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string sourceDir, account, appName;
   Error error = json::readParams(request.params, &sourceDir, &account,
                                  &appName);
   if (error)
      return error;

   if (s_pShinyAppDeploy_ &&
       s_pShinyAppDeploy_->isRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      s_pShinyAppDeploy_ = ShinyAppDeploy::create(sourceDir, account, appName);
      pResponse->setResult(true);
   }

   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "deploy_shiny_app", deployShinyApp))
      (bind(sourceModuleRFile, "SessionShinyApps.R"));

   return initBlock.execute();
}

} // namespace shiny_apps
} // namespace modules
} // namespace session

