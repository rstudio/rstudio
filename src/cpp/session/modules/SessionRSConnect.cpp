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

std::string quotedFilesFromArray(json::Array array) 
{
   std::string joined;
   for (size_t i = 0; i < array.size(); i++) 
   {
      joined += "'" + string_utils::utf8ToSystem(array[i].get_str()) + "'";
      if (i < array.size() - 1) 
         joined += ", ";
   }
   return joined;
}

class RSConnectPublish : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<RSConnectPublish> create(
         const std::string& dir,
         const json::Array& fileList, 
         const std::string& file, 
         const std::string& account,
         const std::string& server,
         const std::string& app,
         const json::Array& additionalFilesList,
         const json::Array& ignoredFilesList,
         bool asMultiple)
   {
      boost::shared_ptr<RSConnectPublish> pDeploy(new RSConnectPublish(file));

      std::string cmd("{ options(repos = c(CRAN='" +
                       module_context::CRANReposURL() + "')); ");

      // join and quote incoming filenames to deploy
      std::string deployFiles = quotedFilesFromArray(fileList);
      std::string additionalFiles = quotedFilesFromArray(additionalFilesList);
      std::string ignoredFiles = quotedFilesFromArray(ignoredFilesList);

      // if an R Markdown document or HTML document is being deployed, mark it
      // as the primary file 
      std::string primaryDoc;
      if (!file.empty())
      {
         FilePath sourceFile = module_context::resolveAliasedPath(file);
         std::string extension = sourceFile.extensionLowerCase();
         if (extension == ".rmd" || extension == ".html") 
         {
            primaryDoc = string_utils::utf8ToSystem(file);
         }
      }

      // form the deploy command to hand off to the async deploy process
      cmd += "rsconnect::deployApp("
             "appDir = '" + string_utils::utf8ToSystem(dir) + "'," +
             (deployFiles.empty() ? "" : "appFiles = c(" + 
                deployFiles + "), ") +
             (primaryDoc.empty() ? "" : "appPrimaryDoc = '" + 
                primaryDoc + "', ") + 
             "account = '" + account + "',"
             "server = '" + server + "', "
             "appName = '" + app + "', "
             "launch.browser = function (url) { "
             "   message('" kFinishedMarker "', url) "
             "}, "
             "lint = FALSE,"
             "metadata = list(" 
             "   asMultiple = " + (asMultiple ? "TRUE" : "FALSE") + 
                 (additionalFiles.empty() ? "" : ", additionalFiles = c(" + 
                    additionalFiles + ")") + 
                 (ignoredFiles.empty() ? "" : ", ignoredFiles = c(" + 
                    ignoredFiles + ")") + 
             "))}";

      std::cerr << cmd << std::endl;
      pDeploy->start(cmd.c_str(), FilePath(), async_r::R_PROCESS_VANILLA);
      return pDeploy;
   }

private:
   RSConnectPublish(const std::string& file)
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

boost::shared_ptr<RSConnectPublish> s_pRSConnectPublish_;

Error rsconnectPublish(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   json::Array sourceFiles, additionalFiles, ignoredFiles;
   std::string sourceDir, sourceFile, account, server, appName;
   bool asMultiple = false;
   Error error = json::readParams(request.params, &sourceDir, &sourceFiles,
                                   &sourceFile, &account, &server, &appName,
                                   &additionalFiles, &ignoredFiles, 
                                   &asMultiple);
   if (error)
      return error;

   if (s_pRSConnectPublish_ &&
       s_pRSConnectPublish_->isRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      s_pRSConnectPublish_ = RSConnectPublish::create(sourceDir, sourceFiles, 
                                                  sourceFile, account, server, 
                                                  appName, additionalFiles,
                                                  ignoredFiles, asMultiple);
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
      (bind(registerRpcMethod, "rsconnect_publish", rsconnectPublish))
      (bind(sourceModuleRFile, "SessionRSConnect.R"));

   return initBlock.execute();
}

} // namespace rsconnect
} // namespace modules
} // namespace session
} // namespace rstudio

