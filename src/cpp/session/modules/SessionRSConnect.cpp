/*
 * SessionRSConnect.cpp
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

#include "SessionRSConnect.hpp"

#include <boost/algorithm/string.hpp>
#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>

#define kFinishedMarker "Deployment completed: "
#define kRSConnectFolder "rsconnect/"
#define kPackratFolder "packrat/"

#define kMaxDeploymentSize 104857600

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace rsconnect {

namespace {

class ShinyAppDeploy : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<ShinyAppDeploy> create(
         const std::string& dir,
         const json::Array& fileList, 
         const std::string& file, 
         const std::string& account,
         const std::string& server,
         const std::string& app)
   {
      boost::shared_ptr<ShinyAppDeploy> pDeploy(new ShinyAppDeploy(file));

      std::string cmd("{ options(repos = c(CRAN='" +
                       module_context::CRANReposURL() + "')); ");

      // join and quote incoming filenames to deploy
      std::string files;
      for (size_t i = 0; i < fileList.size(); i++) 
      {
         files += "'" + fileList[i].get_str() + "'";
         if (i < fileList.size() - 1) 
            files += ", ";
      }

      // if a R Markdown document is being deployed, mark it as the primary
      // file 
      std::string primaryRmd;
      if (!file.empty())
      {
         FilePath sourceFile = module_context::resolveAliasedPath(file);
         if (sourceFile.extensionLowerCase() == ".rmd") 
         {
            primaryRmd = file;
         }
      }

      // form the deploy command to hand off to the async deploy process
      cmd += "rsconnect::deployApp("
             "appDir = '" + dir + "'," +
             (files.empty() ? "" : "appFiles = c(" + files + "), ") +
             (primaryRmd.empty() ? "" : "appPrimaryRmd = '" + primaryRmd + "', ") + 
             "account = '" + account + "',"
             "server = '" + server + "', "
             "appName = '" + app + "', "
             "launch.browser = function (url) { "
             "   message('" kFinishedMarker "', url) "
             "}, "
             "lint = FALSE)}";

      pDeploy->start(cmd.c_str(), FilePath(), async_r::R_PROCESS_VANILLA);
      return pDeploy;
   }

private:
   ShinyAppDeploy(const std::string& file)
   {
      sourceFile_ = file;
   }

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

            // check to see if a source file was specified; if so return a URL
            // with the source file appended
            if (!sourceFile_.empty() &&
                !boost::algorithm::iends_with(deployedUrl_, ".rmd") &&
                (string_utils::toLower(sourceFile_) != "index.rmd"))
            {
               // append / to the URL if it doesn't already have one
               if (deployedUrl_[deployedUrl_.length() - 1] != '/')
                  deployedUrl_.append("/");
               deployedUrl_.append(sourceFile_);
            }
         }
      }

      // emit the output to the client for display
      module_context::CompileOutput deployOutput(type, output);
      ClientEvent event(client_events::kRmdRSConnectDeploymentOutput,
                        module_context::compileOutputAsJson(deployOutput));
      module_context::enqueClientEvent(event);
   }

   void onCompleted(int exitStatus)
   {
      // when the process completes, emit the discovered URL, if any
      ClientEvent event(client_events::kRmdRSConnectDeploymentCompleted,
                        deployedUrl_);
      module_context::enqueClientEvent(event);
   }

   std::string deployedUrl_;
   std::string sourceFile_;
};

boost::shared_ptr<ShinyAppDeploy> s_pShinyAppDeploy_;

Error deployShinyApp(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   json::Array sourceFiles;
   std::string sourceDir, sourceFile, account, server, appName;
   Error error = json::readParams(request.params, &sourceDir, &sourceFiles,
                                   &sourceFile, &account, &server, &appName);
   if (error)
      return error;

   if (s_pShinyAppDeploy_ &&
       s_pShinyAppDeploy_->isRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      s_pShinyAppDeploy_ = ShinyAppDeploy::create(sourceDir, sourceFiles, 
                                                  sourceFile, account, server, 
                                                  appName);
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
      (bind(sourceModuleRFile, "SessionRSConnect.R"));

   return initBlock.execute();
}

} // namespace rsconnect
} // namespace modules
} // namespace session
} // namespace rstudio

