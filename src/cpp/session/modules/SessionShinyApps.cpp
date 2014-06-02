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
#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>

#define kFinishedMarker "ShinyApps deployment completed: "
#define kShinyAppsFolder "shinyapps/"

#define kMaxDeploymentSize 104857600

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
         const std::string& file, 
         const std::string& account,
         const std::string& app)
   {
      boost::shared_ptr<ShinyAppDeploy> pDeploy(new ShinyAppDeploy(file));
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
            if (!sourceFile_.empty())
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
   std::string sourceFile_;
};

boost::shared_ptr<ShinyAppDeploy> s_pShinyAppDeploy_;

Error deployShinyApp(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string sourceDir, sourceFile, account, appName;
   Error error = json::readParams(request.params, &sourceDir, &sourceFile, 
                                  &account, &appName);
   if (error)
      return error;

   if (s_pShinyAppDeploy_ &&
       s_pShinyAppDeploy_->isRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      s_pShinyAppDeploy_ = ShinyAppDeploy::create(sourceDir, sourceFile, 
                                                  account, appName);
      pResponse->setResult(true);
   }

   return Success();
}

// sums the sizes of files in a directory, stopping when the sizes exceed
// maxSize
bool directorySummer(int level, const FilePath& file, const FilePath& root, 
                     uintmax_t maxSize, std::vector<std::string>* pFileNames, 
                     uintmax_t* pRunningSum)
{
   // ignore directory nodes
   if (file.isDirectory())
      return true;

   // ignore hidden files (note that this leads to a slightly incorrect result
   // since some hidden files are in fact pushed to the server by shinyapps)
   std::string relPath = file.relativePath(root);
   if (relPath.substr(0, 1) == ".")
      return true;

   // ignore shinyapps folder 
   if (relPath.substr(0, sizeof(kShinyAppsFolder) - 1) == kShinyAppsFolder)
      return true;

   // ignore the R project file
   if (file.hasExtensionLowerCase(".rproj"))
      return true;

   // add the file to the list and update the running sum
   pFileNames->push_back(relPath);
   *pRunningSum += file.size();
   return *pRunningSum < maxSize;
}

// returns the list of files to be deployed, given a source directory
Error getDeploymentFiles(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   std::string sourceDir;
   Error error = json::readParams(request.params, &sourceDir);
   FilePath sourcePath = module_context::resolveAliasedPath(sourceDir);
   json::Object result;

   // sum the sizes of the files in the source directory
   uintmax_t dirSize = 0;
   std::vector<std::string> fileNames;
   sourcePath.childrenRecursive(
         boost::bind(directorySummer, _1, _2, sourcePath, 
                     kMaxDeploymentSize, &fileNames, &dirSize));
   
   if (dirSize < kMaxDeploymentSize)
   {
      // the result is reasonably sized; return it
      result["dir_list"] = json::toJsonArray(fileNames);
   }
   else
   {
      // the result is too big; don't return it 
      result["dir_list"] = json::Value();
   }
   result["max_size"] = kMaxDeploymentSize;
   result["dir_size"] = static_cast<int>(dirSize);
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
      (bind(registerRpcMethod, "deploy_shiny_app", deployShinyApp))
      (bind(registerRpcMethod, "get_deployment_files", getDeploymentFiles))
      (bind(sourceModuleRFile, "SessionShinyApps.R"));

   return initBlock.execute();
}

} // namespace shiny_apps
} // namespace modules
} // namespace session

