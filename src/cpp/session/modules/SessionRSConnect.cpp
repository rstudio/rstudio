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
#include <r/RJson.hpp>

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

// transforms a JSON array of file names into a single string. If 'quoted',
// then the input strings are quoted and comma-delimited; otherwise, file names
// are pipe-delimited.
std::string quotedFilesFromArray(json::Array array, bool quoted) 
{
   std::string joined;
   for (size_t i = 0; i < array.size(); i++) 
   {
      joined += (quoted ? "'" : "") + 
                string_utils::utf8ToSystem(array[i].get_str()) +
                (quoted ? "'" : "");
      if (i < array.size() - 1) 
         joined += (quoted ? ", " : "|");
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
         const std::string& sourceDoc,
         const std::string& account,
         const std::string& server,
         const std::string& app,
         const json::Array& additionalFilesList,
         const json::Array& ignoredFilesList,
         bool asMultiple,
         bool asStatic)
   {
      boost::shared_ptr<RSConnectPublish> pDeploy(new RSConnectPublish(file));

      std::string cmd("{ options(repos = c(CRAN='" +
                       module_context::CRANReposURL() + "')); ");

      // join and quote incoming filenames to deploy
      std::string deployFiles = quotedFilesFromArray(fileList, true);
      std::string additionalFiles = quotedFilesFromArray(additionalFilesList,
            false);
      std::string ignoredFiles = quotedFilesFromArray(ignoredFilesList,
            false);

      // if an R Markdown document or HTML document is being deployed, mark it
      // as the primary file 
      std::string primaryDoc;
      if (!file.empty())
      {
         FilePath docFile = module_context::resolveAliasedPath(file);
         std::string extension = docFile.extensionLowerCase();
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
             (sourceDoc.empty() ? "" : "appSourceDoc = '" + 
                sourceDoc + "', ") + 
             "account = '" + account + "',"
             "server = '" + server + "', "
             "appName = '" + app + "', "
             "launch.browser = function (url) { "
             "   message('" kFinishedMarker "', url) "
             "}, "
             "lint = FALSE,"
             "metadata = list(" 
             "   asMultiple = " + (asMultiple ? "TRUE" : "FALSE") + ", "
             "   asStatic = " + (asStatic ? "TRUE" : "FALSE") + 
                 (additionalFiles.empty() ? "" : ", additionalFiles = '" + 
                    additionalFiles + "'") + 
                 (ignoredFiles.empty() ? "" : ", ignoredFiles = '" + 
                    ignoredFiles + "'") + 
             "))}";

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
            deployedUrl_ = line.substr(ncharMarker, line.size() - ncharMarker);
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
   std::string sourceDir, sourceFile, sourceDoc, account, server, appName;
   bool asMultiple = false, asStatic = false;
   Error error = json::readParams(request.params, &sourceDir, &sourceFiles,
                                   &sourceFile, &sourceDoc, &account, &server, 
                                   &appName, &additionalFiles, &ignoredFiles, 
                                   &asMultiple, &asStatic);
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
                                                  sourceFile, sourceDoc, 
                                                  account, server, appName, 
                                                  additionalFiles,
                                                  ignoredFiles, asMultiple,
                                                  asStatic);
      pResponse->setResult(true);
   }

   return Success();
}


Error rsconnectDeployments(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{

   std::string sourcePath, outputPath;
   Error error = json::readParams(request.params, &sourcePath, &outputPath);
   if (error)
      return error;

   // get prior RPubs upload IDs, if any are known
   std::string rpubsUploadId;
   if (!outputPath.empty())
   {
     rpubsUploadId = module_context::previousRpubsUploadId(
         module_context::resolveAliasedPath(outputPath));
   }

   // blend with known deployments from the rsconnect package
   r::sexp::Protect protect;
   SEXP sexpDeployments;
   error = r::exec::RFunction(".rs.getRSConnectDeployments", sourcePath, 
         rpubsUploadId).call(&sexpDeployments, &protect);
   if (error)
      return error;
   
   // convert result to JSON and return
   json::Value result;
   error = r::json::jsonValueFromObject(sexpDeployments, &result);
   if (error)
      return error;

   // we want to always return an array, even if it's just one element long, so
   // wrap the result in an array if it isn't one already
   if (result.type() != json::ArrayType) 
   {
      json::Array singleEle;
      singleEle.push_back(result);
      result = singleEle;
   }

   pResponse->setResult(result);

   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_rsconnect_deployments", rsconnectDeployments))
      (bind(registerRpcMethod, "rsconnect_publish", rsconnectPublish))
      (bind(sourceModuleRFile, "SessionRSConnect.R"));

   return initBlock.execute();
}

} // namespace rsconnect
} // namespace modules
} // namespace session
} // namespace rstudio

