/*
 * SessionRSConnect.cpp
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

#include "SessionRSConnect.hpp"

#include <boost/algorithm/string.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/r_util/RProjectFile.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/ROptions.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>

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
   for (size_t i = 0; i < array.getSize(); i++)
   {
      // convert filenames to system encoding and escape quotes if quoted
      std::string filename = 
         string_utils::singleQuotedStrEscape(string_utils::utf8ToSystem(
                  array[i].getString()));

      // join into a single string
      joined += (quoted ? "'" : "") + 
                filename +
                (quoted ? "'" : "");
      if (i < array.getSize() - 1)
         joined += (quoted ? ", " : "|");
   }
   return joined;
}

// transforms a FilePath into an aliased json string
json::Value toJsonString(const core::FilePath& filePath)
{
   return json::Value(module_context::createAliasedPath(filePath));
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
         const std::string& appId, 
         const std::string& contentCategory,
         const std::string& websiteDir,
         const json::Array& additionalFilesList,
         const json::Array& ignoredFilesList,
         bool asMultiple,
         bool asStatic,
         boost::shared_ptr<RSConnectPublish>* pDeployOut)
   {
      boost::shared_ptr<RSConnectPublish> pDeploy(new RSConnectPublish(file));

      // lead command with download options and certificate check state
      std::string cmd("{ " + module_context::CRANDownloadOptions() + "; " 
                      "options(rsconnect.check.certificate = " +
                      (prefs::userPrefs().publishCheckCertificates() ? "TRUE" : "FALSE") + "); ");

      if (prefs::userPrefs().usePublishCaBundle() && 
          !prefs::userPrefs().publishCaBundle().empty())
      {
         FilePath caBundleFile = module_context::resolveAliasedPath(
               prefs::userPrefs().publishCaBundle());
         if (caBundleFile.exists())
         {
            // if a valid bundle path was specified, use it
            cmd += "options(rsconnect.ca.bundle = '" +
                   string_utils::utf8ToSystem(string_utils::singleQuotedStrEscape(
                      caBundleFile.getAbsolutePath())) +
                   "'); ";
         }
      }

      // create temporary file to host file manifest
      if (!fileList.isEmpty())
      {
         Error error = FilePath::tempFilePath(pDeploy->manifestPath_);
         if (error)
            return error;

         // write manifest to temporary file
         std::vector<std::string> deployFileList;
         fileList.toVectorString(deployFileList);
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
         std::string extension = docFile.getExtensionLowerCase();
         if (extension == ".rmd" || extension == ".html" || extension == ".r" ||
             extension == ".pdf" || extension == ".docx" || extension == ".rtf" || 
             extension == ".odt" || extension == ".pptx") 
         {
            primaryDoc = string_utils::utf8ToSystem(file);
         }
      }
      
      // for static website deployments, store the publish record in the 
      // website root instead of the appDir; this prevents record from being blown
      // away when the static content is cleaned and regenerated, thus permitting
      // iterative republishing of static content
      std::string recordDir;
      if (asStatic && contentCategory == "site" && !websiteDir.empty())
      {
         recordDir = string_utils::utf8ToSystem(websiteDir);
      }
      
      std::string appDir = string_utils::utf8ToSystem(dir);
      if (appDir == "~")
         appDir = "~/";

      // form the deploy command to hand off to the async deploy process
      cmd += "rsconnect::deployApp("
             "appDir = '" + string_utils::singleQuotedStrEscape(appDir) + "'," +
             (recordDir.empty() ? "" : "recordDir = '" + 
                string_utils::singleQuotedStrEscape(recordDir) + "',") +
             (pDeploy->manifestPath_.isEmpty() ? "" : "appFileManifest = '" +
                                                    string_utils::singleQuotedStrEscape(
                                                       pDeploy->manifestPath_.getAbsolutePath()) + "', ") +
             (primaryDoc.empty() ? "" : "appPrimaryDoc = '" + 
                string_utils::singleQuotedStrEscape(primaryDoc) + "', ") +
             (sourceDoc.empty() ? "" : "appSourceDoc = '" + 
                string_utils::singleQuotedStrEscape(sourceDoc) + "', ") +
             "account = '" + string_utils::singleQuotedStrEscape(account) + "',"
             "server = '" + string_utils::singleQuotedStrEscape(server) + "', "
             "appName = '" + string_utils::singleQuotedStrEscape(appName) + "', " + 
             (appTitle.empty() ? "" : "appTitle = '" + 
                string_utils::singleQuotedStrEscape(appTitle) + "', ") + 
             (appId.empty() ? "" : "appId = " + appId + ", ") + 
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
             (prefs::userPrefs().showPublishDiagnostics() ? ", logLevel = 'verbose'" : "") + 
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
      for (std::string& line : lines)
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

Error cancelPublish(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   if (s_pRSConnectPublish_ &&
       s_pRSConnectPublish_->isRunning())
   {
      // There is a running publish operation; end it.
      s_pRSConnectPublish_->terminate();
      pResponse->setResult(true);
   }
   else
   {
      // No running publish operation.
      pResponse->setResult(false);
   }

   return Success();
}

Error rsconnectPublish(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   json::Object source, settings;
   std::string account, server, appName, appTitle, appId;
   Error error = json::readParams(request.params, &source, &settings,
                                   &account, &server, 
                                   &appName, &appTitle, &appId);
   if (error)
      return error;

   // read publish source information
   std::string sourceDir, sourceDoc, sourceFile, contentCategory, websiteDir;
   error = json::readObject(source, "deploy_dir",       sourceDir,
                                    "deploy_file",      sourceFile,
                                    "source_file",      sourceDoc,
                                    "content_category", contentCategory,
                                    "website_dir",      websiteDir);
   if (error)
      return error;

   // read publish settings
   bool asMultiple = false, asStatic = false;
   json::Array deployFiles, additionalFiles, ignoredFiles;
   error = json::readObject(settings, "deploy_files",     deployFiles,
                                      "additional_files", additionalFiles,
                                      "ignored_files",    ignoredFiles,
                                      "as_multiple",      asMultiple,
                                      "as_static",        asStatic);
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
                                       account, server, appName, appTitle, appId, 
                                       contentCategory,
                                       websiteDir,
                                       additionalFiles,
                                       ignoredFiles, asMultiple, asStatic,
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
   if (result.getType() != json::Type::ARRAY)
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
      shinyPaths.push_back(appPath.completeChildPath("app.R"));
      shinyPaths.push_back(appPath.completeChildPath("ui.R"));
      shinyPaths.push_back(appPath.completeChildPath("server.R"));
      shinyPaths.push_back(appPath.completeChildPath("www/index.html"));
      for (const FilePath& filePath : shinyPaths)
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
   if (resultVal.getType() != json::Type::OBJECT)
      return Error(json::errc::ParseError, ERROR_LOCATION);
   json::Object result = resultVal.getValue<json::Object>();

   // augment with website project information
   FilePath path = module_context::resolveAliasedPath(target);
   std::string websiteDir;
   std::string websiteOutputDir;
   if (path.exists() && (path.hasExtensionLowerCase(".rmd") || 
                         path.hasExtensionLowerCase(".md")))
   {
      FilePath webPath = session::projects::projectContext().fileUnderWebsitePath(path);
      if (!webPath.isEmpty())
      {
         websiteDir = webPath.getAbsolutePath();
         
         // also get build output dir
         if (!module_context::websiteOutputDir().empty())
         {
            FilePath websiteOutputPath = 
                  module_context::resolveAliasedPath(module_context::websiteOutputDir());
            websiteOutputDir = websiteOutputPath.getAbsolutePath();
         }
      }
   }
   result["website_dir"] = websiteDir;
   result["website_output_dir"] = websiteOutputDir;

   pResponse->setResult(result);
      
   return Success();
}

void applyPreferences()
{
   // push preference changes into rsconnect package options immediately, so that it's possible to
   // use them without restarting R
   r::options::setOption("rsconnect.check.certificate", prefs::userPrefs().publishCheckCertificates());
   if (prefs::userPrefs().usePublishCaBundle())
      r::options::setOption("rsconnect.ca.bundle", prefs::userPrefs().publishCaBundle());
   else
      r::options::setOption("rsconnect.ca.bundle", R_NilValue);
}

Error initializeOptions()
{
   SEXP checkSEXP = r::options::getOption("rsconnect.check.certificate");
   if (checkSEXP == R_NilValue)
   {
      // no user defined setting for certificate checks; disable if requested for the session
      if (!prefs::userPrefs().publishCheckCertificates())
         r::options::setOption("rsconnect.check.certificate", false);
   }
   else
   {
      // the user has a setting defined; mirror it if it differs. this allows us to reflect the
      // the current value of the option in our preferences, but also means that if you've set the
      // option in e.g. .Rprofile then it wins over RStudio's setting in the end.
      bool check = r::sexp::asLogical(checkSEXP);
      if (prefs::userPrefs().publishCheckCertificates() != check)
      {
         prefs::userPrefs().setPublishCheckCertificates(check);
      }
   }

   std::string caBundle = r::sexp::safeAsString(r::options::getOption("rsconnect.ca.bundle"));
   if (caBundle.empty() && prefs::userPrefs().usePublishCaBundle())
   {
      // no user defined setting for CA bundle; inject a bundle if the user asked for one
      r::options::setOption("rsconnect.ca.bundle", prefs::userPrefs().publishCaBundle());
   }
   else if (!caBundle.empty())
   {
      // promote user setting as above
      prefs::userPrefs().setUsePublishCaBundle(true);
      prefs::userPrefs().setPublishCaBundle(caBundle);
   }
   
   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   module_context::events().onPreferencesSaved.connect(applyPreferences);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_rsconnect_deployments", rsconnectDeployments))
      (bind(registerRpcMethod, "rsconnect_publish", rsconnectPublish))
      (bind(registerRpcMethod, "cancel_publish", cancelPublish))
      (bind(registerRpcMethod, "get_edit_published_docs", getEditPublishedDocs))
      (bind(registerRpcMethod, "get_rmd_publish_details", getRmdPublishDetails))
      (bind(sourceModuleRFile, "SessionRSConnect.R"))
      (bind(initializeOptions));

   return initBlock.execute();
}

} // namespace rsconnect
} // namespace modules
} // namespace session
} // namespace rstudio

