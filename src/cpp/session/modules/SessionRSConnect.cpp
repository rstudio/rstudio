/*
 * SessionRSConnect.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
#include <core/FileSerializer.hpp>
#include <core/r_util/RProjectFile.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionSourceDatabase.hpp>

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
      // convert filenames to system encoding and escape quotes if quoted
      std::string filename = 
         string_utils::singleQuotedStrEscape(string_utils::utf8ToSystem(
                  array[i].get_str()));

      // join into a single string
      joined += (quoted ? "'" : "") + 
                filename +
                (quoted ? "'" : "");
      if (i < array.size() - 1) 
         joined += (quoted ? ", " : "|");
   }
   return joined;
}

// transforms a FilePath into an aliased json string
json::Value toJsonString(const core::FilePath& filePath)
{
   return module_context::createAliasedPath(filePath);
}

class RSConnectPublish : public async_r::AsyncRProcess
{
public:
   static Error create(
         const std::string& dir,
         const json::Array& fileList, 
         const std::string& file, 
         const std::string& sourceDoc,
         const std::string& account,
         const std::string& server,
         const std::string& appName,
         const std::string& appTitle,
         const std::string& contentCategory,
         const json::Array& additionalFilesList,
         const json::Array& ignoredFilesList,
         bool asMultiple,
         bool asStatic,
         bool diagnostics,
         boost::shared_ptr<RSConnectPublish>* pDeployOut)
   {
      boost::shared_ptr<RSConnectPublish> pDeploy(new RSConnectPublish(file));

      std::string cmd("{ " + module_context::CRANDownloadOptions() + "; ");

      // create temporary file to host file manifest
      if (!fileList.empty())
      {
         Error error = FilePath::tempFilePath(&pDeploy->manifestPath_);
         if (error)
            return error;

         // write manifest to temporary file
         std::vector<std::string> deployFileList;
         json::fillVectorString(fileList, &deployFileList);
         error = core::writeStringVectorToFile(pDeploy->manifestPath_, 
                                               deployFileList);
         if (error)
            return error;
      }

      // join and quote incoming filenames to deploy
      std::string additionalFiles = quotedFilesFromArray(additionalFilesList,
            false);
      std::string ignoredFiles = quotedFilesFromArray(ignoredFilesList,
            false);

      // if an R Markdown document or HTML document is being deployed, mark it
      // as the primary file, unless deploying a website
      std::string primaryDoc;
      if (!file.empty() && contentCategory != "site")
      {
         FilePath docFile = module_context::resolveAliasedPath(file);
         std::string extension = docFile.extensionLowerCase();
         if (extension == ".rmd" || extension == ".html" || extension == ".r") 
         {
            primaryDoc = string_utils::utf8ToSystem(file);
         }
      }
      
      std::string appDir = string_utils::utf8ToSystem(dir);
      if (appDir == "~")
         appDir = "~/";

      // form the deploy command to hand off to the async deploy process
      cmd += "rsconnect::deployApp("
             "appDir = '" + string_utils::singleQuotedStrEscape(appDir) + "'," +
             (pDeploy->manifestPath_.empty() ? "" : "appFileManifest = '" + 
                string_utils::singleQuotedStrEscape(
                   pDeploy->manifestPath_.absolutePath()) + "', ") +
             (primaryDoc.empty() ? "" : "appPrimaryDoc = '" + 
                string_utils::singleQuotedStrEscape(primaryDoc) + "', ") + 
             (sourceDoc.empty() ? "" : "appSourceDoc = '" + 
                string_utils::singleQuotedStrEscape(sourceDoc) + "', ") + 
             "account = '" + string_utils::singleQuotedStrEscape(account) + "',"
             "server = '" + string_utils::singleQuotedStrEscape(server) + "', "
             "appName = '" + string_utils::singleQuotedStrEscape(appName) + "', " + 
             (appTitle.empty() ? "" : "appTitle = '" + 
                string_utils::singleQuotedStrEscape(appTitle) + "', ") + 
             (contentCategory.empty() ? "" : "contentCategory = '" + 
                contentCategory + "', ") +
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
             ")" + 
             (diagnostics ? ", logLevel = 'verbose'" : "") + 
             ")}";

      pDeploy->start(cmd.c_str(), FilePath(), async_r::R_PROCESS_VANILLA);
      *pDeployOut = pDeploy;
      return Success();
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

      // check for HTTP errors
      boost::regex re("Error: HTTP (\\d{3})\\s+\\w+\\s+(\\S+)");
      boost::smatch match;
      if (regex_utils::search(output, match, re)) 
      {
         json::Object failure;
         failure["http_status"] = (int)safe_convert::stringTo(match[1], 0);
         failure["path"] = match[2].str();
         ClientEvent event(client_events::kRmdRSConnectDeploymentFailed,
                           failure);
         module_context::enqueClientEvent(event);
      }

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

      // clean up the manifest if we created it
      Error error = manifestPath_.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }

   std::string deployedUrl_;
   std::string sourceFile_;
   FilePath manifestPath_;
};

boost::shared_ptr<RSConnectPublish> s_pRSConnectPublish_;

Error rsconnectPublish(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   json::Object source, settings;
   std::string account, server, appName, appTitle;
   Error error = json::readParams(request.params, &source, &settings,
                                   &account, &server, 
                                   &appName, &appTitle);
   if (error)
      return error;

   // read publish source information
   std::string sourceDir, sourceDoc, sourceFile, contentCategory;
   error = json::readObject(source, "deploy_dir",       &sourceDir,
                                    "deploy_file",      &sourceFile,
                                    "source_file",      &sourceDoc,
                                    "content_category", &contentCategory);
   if (error)
      return error;

   // read publish settings
   bool asMultiple = false, asStatic = false, diagnostics = false;
   json::Array deployFiles, additionalFiles, ignoredFiles;
   error = json::readObject(settings, "deploy_files",     &deployFiles,
                                      "additional_files", &additionalFiles,
                                      "ignored_files",    &ignoredFiles,
                                      "as_multiple",      &asMultiple, 
                                      "as_static",        &asStatic,
                                      "show_diagnostics", &diagnostics);
   if (error)
      return error;

   if (s_pRSConnectPublish_ &&
       s_pRSConnectPublish_->isRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      error = RSConnectPublish::create(sourceDir, deployFiles, 
                                       sourceFile, sourceDoc, 
                                       account, server, appName, appTitle,
                                       contentCategory,
                                       additionalFiles,
                                       ignoredFiles, asMultiple,
                                       asStatic, diagnostics,
                                       &s_pRSConnectPublish_);
      if (error)
         return error;

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

Error getEditPublishedDocs(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string appPathParam;
   Error error = json::readParams(request.params, &appPathParam);
   if (error)
      return error;

   FilePath appPath = module_context::resolveAliasedPath(appPathParam);
   if (!appPath.exists())
      return pathNotFoundError(ERROR_LOCATION);

   // doc paths to return
   std::vector<FilePath> docPaths;

   // if it's a file then just return the file
   if (!appPath.isDirectory())
   {
      docPaths.push_back(appPath);
   }
   // otherwise look for shiny files
   else
   {
      std::vector<FilePath> shinyPaths;
      shinyPaths.push_back(appPath.childPath("app.R"));
      shinyPaths.push_back(appPath.childPath("ui.R"));
      shinyPaths.push_back(appPath.childPath("server.R"));
      shinyPaths.push_back(appPath.childPath("www/index.html"));
      BOOST_FOREACH(const FilePath& filePath, shinyPaths)
      {
         if (filePath.exists())
            docPaths.push_back(filePath);
      }
   }

   // return as json
   json::Array resultJson;
   std::transform(docPaths.begin(),
                  docPaths.end(),
                  std::back_inserter(resultJson),
                  toJsonString);
   pResponse->setResult(resultJson);
   return Success();
}

Error getRmdPublishDetails(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string target;
   Error error = json::readParams(request.params, &target);

   // look up the source document in the database to see if we know its
   // encoding
   std::string encoding("unknown");
   std::string id;
   source_database::getId(target, &id);
   if (!id.empty())
   {
      boost::shared_ptr<source_database::SourceDocument> pDoc(
               new source_database::SourceDocument());
      error = source_database::get(id, pDoc);
      if (error)
         LOG_ERROR(error);
      else
         encoding = pDoc->encoding();
   }

   // extract publish details we can discover with R
   r::sexp::Protect protect;
   SEXP sexpDetails;
   error = r::exec::RFunction(".rs.getRmdPublishDetails",
                              target, encoding).call(&sexpDetails, &protect);
   if (error)
      return error;

   // extract JSON object from result
   json::Value resultVal;
   error = r::json::jsonValueFromList(sexpDetails, &resultVal);
   if (resultVal.type() != json::ObjectType)
      return Error(json::errc::ParseError, ERROR_LOCATION);
   json::Object result = resultVal.get_obj();

   // augment with website project information
   FilePath path = module_context::resolveAliasedPath(target);
   std::string websiteDir;
   if (path.exists() && (path.hasExtensionLowerCase(".rmd") || 
                         path.hasExtensionLowerCase(".md")))
   {
      if (r_util::isWebsiteDirectory(path.parent()))
         websiteDir = path.parent().absolutePath();
   }
   result["website_dir"] = websiteDir;

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
      (bind(registerRpcMethod, "get_edit_published_docs", getEditPublishedDocs))
      (bind(registerRpcMethod, "get_rmd_publish_details", getRmdPublishDetails))
      (bind(sourceModuleRFile, "SessionRSConnect.R"));

   return initBlock.execute();
}

} // namespace rsconnect
} // namespace modules
} // namespace session
} // namespace rstudio

