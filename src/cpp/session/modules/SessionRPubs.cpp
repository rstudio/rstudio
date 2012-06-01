/*
 * SessionRPubs.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

using namespace core ;

namespace session {
namespace modules { 
namespace rpubs {

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

void saveUploadId(const FilePath& filePath, const std::string& uploadId)
{
   Settings settings;
   getUploadIdSettings(&settings);
   settings.set(pathIdentifier(filePath), uploadId);
}


class RPubsUpload : boost::noncopyable,
                    public boost::enable_shared_from_this<RPubsUpload>
{
public:
   static boost::shared_ptr<RPubsUpload> create(const std::string& title,
                                                const FilePath& htmlFile,
                                                bool allowUpdate)
   {
      boost::shared_ptr<RPubsUpload> pUpload(new RPubsUpload());
      pUpload->start(title, htmlFile, allowUpdate);
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
   RPubsUpload()
      : terminationRequested_(false), isRunning_(false)
   {
   }

   void start(const std::string& title, const FilePath& htmlFile, bool allowUpdate)
   {
      using namespace core::string_utils;
      using namespace module_context;

      htmlFile_ = htmlFile;
      csvOutputFile_ = module_context::tempFile("rpubsupload", "csv");
      isRunning_ = true;

      // see if we already know of an upload id for this file
      std::string id = allowUpdate ? previousUploadId(htmlFile_) : std::string();

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
               "source(\"%1%\"); "
               "result <- rpubsUpload(\"%2%\", \"%3%\", %4%); "
               "utils::write.csv(as.data.frame(result), "
                               " file=\"%5%\", "
                               " row.names=FALSE);");

      FilePath modulesPath = session::options().modulesRSourcePath();;
      std::string scriptPath = utf8ToSystem(
                        modulesPath.complete("SessionRPubs.R").absolutePath());
      std::string htmlPath = utf8ToSystem(htmlFile.absolutePath());
      std::string outputPath = utf8ToSystem(csvOutputFile_.absolutePath());

      std::string escapedScriptPath = string_utils::jsLiteralEscape(scriptPath);
      std::string escapedTitle = string_utils::jsLiteralEscape(title);
      std::string escapedHtmlPath = string_utils::jsLiteralEscape(htmlPath);
      std::string escapedId = string_utils::jsLiteralEscape(id);
      std::string escapedOutputPath = string_utils::jsLiteralEscape(outputPath);

      std::string cmd = boost::str(fmt %
                    escapedScriptPath % escapedTitle % escapedHtmlPath %
                    (!escapedId.empty() ? "\"" + escapedId + "\"" : "NULL") %
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

      if (!result.id.empty())
         saveUploadId(htmlFile_, result.id);

      json::Object statusJson;
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
   FilePath htmlFile_;
   bool terminationRequested_;
   bool isRunning_;
   std::string output_;
   std::string error_;
   FilePath csvOutputFile_;
};

boost::shared_ptr<RPubsUpload> s_pCurrentUpload;

bool isUploadRunning()
{
   return s_pCurrentUpload && s_pCurrentUpload->isRunning();
}

Error rpubsIsPublished(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
    std::string htmlFile;
    Error error = json::readParams(request.params, &htmlFile);
    if (error)
       return error;

    FilePath filePath = module_context::resolveAliasedPath(htmlFile);

    pResponse->setResult(!previousUploadId(filePath).empty());

    return Success();
}

Error rpubsUpload(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   std::string title, htmlFile;
   bool isUpdate;
   Error error = json::readParams(request.params, &title, &htmlFile, &isUpdate);
   if (error)
      return error;

   if (isUploadRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      FilePath filePath = module_context::resolveAliasedPath(htmlFile);
      s_pCurrentUpload = RPubsUpload::create(title, filePath, isUpdate);
      pResponse->setResult(true);
   }

   return Success();
}

Error terminateRpubsUpload(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   if (isUploadRunning())
      s_pCurrentUpload->terminate();

   return Success();
}

} // anonymous namespace

std::string previousUploadId(const FilePath& filePath)
{
   Settings settings;
   getUploadIdSettings(&settings);
   return settings.get(pathIdentifier(filePath));
}

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
} // namesapce session

