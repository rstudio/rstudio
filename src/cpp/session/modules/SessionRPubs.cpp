/*
 * SessionRPubs.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionRPubs.hpp"

#include <boost/bind.hpp>
#include <boost/utility.hpp>
#include <boost/format.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/Log.hpp>
#include <core/Settings.hpp>
#include <core/FileSerializer.hpp>

#include <core/text/CsvParser.hpp>
#include <core/http/Util.hpp>
#include <core/system/Process.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace session {

namespace {

// we get a fresh settings object for each read or write so multiple
// processes can all read and write without cache issues
void getUploadIdSettings(Settings* pSettings)
{
   FilePath rpubsUploadIds =
         module_context::scopedScratchPath().complete("rpubs_upload_ids");
   Error error = pSettings->initialize(rpubsUploadIds);
   if (error)
      LOG_ERROR(error);
}

std::string pathIdentifier(const FilePath& filePath)
{
   // use a relative path if we are in a project
   std::string path;
   projects::ProjectContext& projectContext = projects::projectContext();
   if (projectContext.hasProject() &&
       filePath.isWithin(projectContext.directory()))
   {
      path = filePath.relativePath(projectContext.directory());
   }
   else
   {
      path = filePath.absolutePath();
   }

   // urlencode so we can use it as a key
   return http::util::urlEncode(path);
}


} // anonymous namespace


namespace modules { 
namespace rpubs {

namespace {

class RPubsUpload : boost::noncopyable,
                    public boost::enable_shared_from_this<RPubsUpload>
{
public:
   static boost::shared_ptr<RPubsUpload> create(const std::string& contextId,
                                                const std::string& title,
                                                const FilePath& originalRmd,
                                                const FilePath& htmlFile,
                                                const std::string& uploadId,
                                                bool allowUpdate)
   {
      boost::shared_ptr<RPubsUpload> pUpload(new RPubsUpload(contextId));
      pUpload->start(title, originalRmd, htmlFile, uploadId, allowUpdate);
      return pUpload;
   }

   virtual ~RPubsUpload()
   {
   }

   bool isRunning() const { return isRunning_; }

   void terminate()
   {
      terminationRequested_ = true;
   }

private:
   explicit RPubsUpload(const std::string& contextId)
      : contextId_(contextId), terminationRequested_(false), isRunning_(false)
   {
   }

   void start(const std::string& title, const FilePath& originalRmd, 
              const FilePath& htmlFile, const std::string& uploadId, 
              bool allowUpdate)
   {
      using namespace rstudio::core::string_utils;
      using namespace module_context;

      htmlFile_ = htmlFile;
      csvOutputFile_ = module_context::tempFile("rpubsupload", "csv");
      isRunning_ = true;

      // if we don't already have an ID for this file, and updates are allowed,
      // check for a previous upload ID
      std::string id = allowUpdate && uploadId.empty() ? 
         previousRpubsUploadId(htmlFile_) : uploadId;
      boost::algorithm::trim(id);

      // R binary
      FilePath rProgramPath;
      Error error = rScriptPath(&rProgramPath);
      if (error)
      {
         terminateWithError(error);
         return;
      }

      // args
      std::vector<std::string> args;
      args.push_back("--slave");
      args.push_back("--no-save");
      args.push_back("--no-restore");
      args.push_back("-e");

      boost::format fmt(
               "result <- rsconnect::rpubsUpload('%1%', '%2%', '%3%', %4%); "
               "utils::write.csv(as.data.frame(result), "
                               " file='%5%', "
                               " row.names=FALSE);");

      std::string htmlPath = utf8ToSystem(htmlFile.absolutePath());
      std::string outputPath = utf8ToSystem(csvOutputFile_.absolutePath());

      // we may not have an original R Markdown document for this publish
      // event (and that's fine)
      std::string rmdPath = originalRmd == FilePath() ? "" :
         utf8ToSystem(originalRmd.absolutePath());

      std::string escapedTitle = string_utils::jsLiteralEscape(title);
      std::string escapedHtmlPath = string_utils::jsLiteralEscape(htmlPath);
      std::string escapedRmdPath = string_utils::jsLiteralEscape(rmdPath);
      std::string escapedId = string_utils::jsLiteralEscape(id);
      std::string escapedOutputPath = string_utils::jsLiteralEscape(outputPath);

      std::string cmd = boost::str(fmt %
                    escapedTitle % escapedHtmlPath % escapedRmdPath %
                    (!escapedId.empty() ? "'" + escapedId + "'" : "NULL") %
                    escapedOutputPath);
      args.push_back(cmd);

      // options
      core::system::ProcessOptions options;
      options.terminateChildren = true;
      options.workingDir = htmlFile.parent();

      // callbacks
      core::system::ProcessCallbacks cb;
      cb.onContinue = boost::bind(&RPubsUpload::onContinue,
                                  RPubsUpload::shared_from_this());
      cb.onStdout = boost::bind(&RPubsUpload::onStdOut,
                                RPubsUpload::shared_from_this(), _2);
      cb.onStderr = boost::bind(&RPubsUpload::onStdErr,
                                RPubsUpload::shared_from_this(), _2);
      cb.onExit =  boost::bind(&RPubsUpload::onCompleted,
                                RPubsUpload::shared_from_this(), _1);

      // execute
      processSupervisor().runProgram(rProgramPath.absolutePath(),
                                     args,
                                     options,
                                     cb);
   }

   bool onContinue()
   {
      return !terminationRequested_;
   }

   void onStdOut(const std::string& output)
   {
      output_.append(output);
   }

   void onStdErr(const std::string& error)
   {
      error_.append(error);
   }

   void onCompleted(int exitStatus)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         if(csvOutputFile_.exists())
         {
            std::string csvOutput;
            Error error = core::readStringFromFile(
                                             csvOutputFile_,
                                             &csvOutput,
                                             string_utils::LineEndingPosix);
            if (error)
            {
               terminateWithError(error);
            }
            else
            {
               // parse output
               Result result = parseOutput(csvOutput);
               if (!result.empty())
                  terminateWithResult(result);
               else
                  terminateWithError(
                           "Unexpected output from upload: " + csvOutput);
            }
         }
         else
         {
            terminateWithError("Unexpected output from upload: " + output_);
         }
      }
      else
      {
         terminateWithError(error_);
      }
   }

   void terminateWithError(const Error& error)
   {
      terminateWithError(error.summary());
   }

   void terminateWithError(const std::string& error)
   {
      terminateWithResult(Result(error));
   }

   struct Result
   {
      Result()
      {
      }

      Result(const std::string& error)
         : error(error)
      {
      }

      Result(const std::string& id, const std::string& continueUrl)
         : id(id), continueUrl(continueUrl)
      {
      }

      bool empty() const { return id.empty() && error.empty();  }

      std::string id;
      std::string continueUrl;
      std::string error;
   };

   void terminateWithResult(const Result& result)
   {
      isRunning_ = false;
      json::Object statusJson;
      statusJson["contextId"] = contextId_;
      statusJson["id"] = result.id;
      statusJson["continueUrl"] = result.continueUrl;
      statusJson["error"] = result.error;
      ClientEvent event(client_events::kRPubsUploadStatus, statusJson);
      module_context::enqueClientEvent(event);
   }

   Result parseOutput(const std::string& output)
   {
      std::pair<std::vector<std::string>, std::string::const_iterator>
                  line = text::parseCsvLine(output.begin(), output.end());
      if (!line.first.empty())
      {
         std::vector<std::string> headers = line.first;

         line = text::parseCsvLine(line.second, output.end());
         if (!line.first.empty())
         {
            std::vector<std::string> data = line.first;

            if (headers.size() == 1 &&
                data.size() == 1 &&
                headers[0] == "error")
            {
               return Result(data[0]);
            }
            else if (headers.size() == 2 &&
                     data.size() == 2 &&
                     headers[0] == "id" &&
                     headers[1] == "continueUrl")
            {
               return Result(data[0], data[1]);
            }
         }
      }

      return Result();
   }

private:
   std::string contextId_;
   FilePath htmlFile_;
   bool terminationRequested_;
   bool isRunning_;
   std::string output_;
   std::string error_;
   FilePath csvOutputFile_;
};

std::map<std::string, boost::shared_ptr<RPubsUpload> > s_pCurrentUploads;

bool isUploadRunning(const std::string& contextId)
{
   boost::shared_ptr<RPubsUpload> pCurrentUpload = s_pCurrentUploads[contextId];
   return pCurrentUpload && pCurrentUpload->isRunning();
}

Error rpubsIsPublished(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
    std::string htmlFile;
    Error error = json::readParams(request.params, &htmlFile);
    if (error)
       return error;

    FilePath filePath = module_context::resolveAliasedPath(htmlFile);

    pResponse->setResult(
          !module_context::previousRpubsUploadId(filePath).empty());

    return Success();
}

Error rpubsUpload(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   std::string contextId, title, originalRmd, htmlFile, uploadId;
   bool isUpdate;
   Error error = json::readParams(request.params,
                                  &contextId,
                                  &title,
                                  &originalRmd, 
                                  &htmlFile,
                                  &uploadId,
                                  &isUpdate);
   if (error)
      return error;

   if (isUploadRunning(contextId))
   {
      pResponse->setResult(false);
   }
   else
   {
      // provide a default title if necessary
      if (title.empty())
         title = "Untitled";

      FilePath filePath = module_context::resolveAliasedPath(htmlFile);
      FilePath rmdPath = originalRmd.empty() ? FilePath() :
         module_context::resolveAliasedPath(originalRmd);
      s_pCurrentUploads[contextId] = RPubsUpload::create(contextId,
                                                         title,
                                                         rmdPath,
                                                         filePath,
                                                         uploadId,
                                                         isUpdate);
      pResponse->setResult(true);
   }

   return Success();
}

Error terminateRpubsUpload(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string contextId;
   Error error = json::readParam(request.params, 0, &contextId);
   if (error)
      return error;


   if (isUploadRunning(contextId))
      s_pCurrentUploads[contextId]->terminate();

   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "rpubs_is_published", rpubsIsPublished))
      (bind(registerRpcMethod, "rpubs_upload", rpubsUpload))
      (bind(registerRpcMethod, "terminate_rpubs_upload", terminateRpubsUpload))
   ;
   return initBlock.execute();
}
   
   
} // namespace rpubs
} // namespace modules

namespace module_context {

std::string previousRpubsUploadId(const FilePath& filePath)
{
   Settings settings;
   getUploadIdSettings(&settings);
   return settings.get(pathIdentifier(filePath));
}

} // namespace module_context

} // namespace session
} // namespace rstudio

