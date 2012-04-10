/*
 * SessionHTMLPreview.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include "SessionHTMLPreview.hpp"

#include <boost/bind.hpp>
#include <boost/format.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/Util.hpp>
#include <core/PerformanceTimer.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/system/Process.hpp>

#include <core/markdown/Markdown.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>

#include <session/SessionModuleContext.hpp>

#define kHTMLPreview "html_preview"
#define kHTMLPreviewLocation "/" kHTMLPreview "/"

using namespace core;

namespace session {
namespace modules { 
namespace html_preview {

namespace {

bool requiresKnit(const std::string& fileContents, bool isMarkdown)
{
   bool requiresKnit = false;
   std::string fileType = isMarkdown ? "gfm" : "html";
   r::exec::RFunction reqFunc(".rs.requiresKnit", fileContents, fileType);
   Error error = reqFunc.call(&requiresKnit);
   if (error)
      LOG_ERROR(error);

   return requiresKnit;
}


class HTMLPreview : boost::noncopyable,
                    public boost::enable_shared_from_this<HTMLPreview>
{
public:
   static boost::shared_ptr<HTMLPreview> create(const FilePath& targetFile,
                                                const std::string& encoding,
                                                bool isMarkdown)
   {
      boost::shared_ptr<HTMLPreview> pPreview(new HTMLPreview(targetFile));
      pPreview->start(encoding, isMarkdown);
      return pPreview;
   }

private:
   HTMLPreview(const FilePath& targetFile)
      : targetFile_(targetFile), isRunning_(false), terminationRequested_(false)
   {
   }

   void start(const std::string& encoding, bool isMarkdown)
   {
      enqueHTMLPreviewStarted(targetFile_);

      // read the file using the specified encoding
      std::string fileContents;
      Error error = module_context::readAndDecodeFile(targetFile_,
                                                      encoding,
                                                      true,
                                                      &fileContents);
      if (error)
      {
         terminateWithError(error);
         return;
      }

      // determine whether we need to knit the file
      if (requiresKnit(fileContents, isMarkdown))
      {
         knit(isMarkdown);
      }

      // otherwise we can just either copy or generate the html inline
      // (for markdown) and return with success
      else
      {
         terminateWithContent(fileContents, isMarkdown);
      }
   }

public:
   ~HTMLPreview()
   {
   }

   // COPYING: prohibited

public:

   bool isRunning() const  { return isRunning_; }

   void terminate() { terminationRequested_ = true; }

   std::string readOutput() const
   {
      if (outputFile_.empty())
         return std::string();

      std::string output;
      Error error = core::readStringFromFile(outputFile_, &output);
      if (error)
         LOG_ERROR(error);

      return output;
   }

   FilePath dependentFilePath(const std::string& fileName)
   {
      return targetFile_.parent().childPath(fileName);
   }


private:

   void knit(bool isMarkdown)
   {
      // set running flag
      isRunning_ = true;

      // create a temp file where we can write the name of the output file to
      FilePath tempFile = module_context::tempFile("knitr-output", "out");
      Error error = core::writeStringToFile(tempFile, "");
      if (error)
      {
         terminateWithError(error);
         return;
      }
      std::string tempFilePath = string_utils::utf8ToSystem(
                                                   tempFile.absolutePath());

      // R binary
      std::string rProgramPath = r::exec::rBinaryPath().absolutePath();

      // args
      std::vector<std::string> args;
      args.push_back("--silent");
      args.push_back("-e");
      boost::format fmt("require(knitr); "
                        "o <- knit('%1%'); "
                        "cat(o, file='%2%');");
      std::string cmd = boost::str(fmt % targetFile_.filename()
                                       % tempFilePath);
      args.push_back(cmd);

      // options
      core::system::ProcessOptions options;
      options.terminateChildren = true;
      options.redirectStdErrToStdOut = true;
      options.workingDir = targetFile_.parent();

      // callbacks
      core::system::ProcessCallbacks cb;
      cb.onContinue = boost::bind(&HTMLPreview::onKnitContinue,
                                  HTMLPreview::shared_from_this());
      cb.onStdout = boost::bind(&HTMLPreview::onKnitOutput,
                                HTMLPreview::shared_from_this(), _2);
      cb.onStderr = boost::bind(&HTMLPreview::onKnitOutput,
                                HTMLPreview::shared_from_this(), _2);
      cb.onExit =  boost::bind(&HTMLPreview::onKnitCompleted,
                                HTMLPreview::shared_from_this(),
                                _1, tempFile, isMarkdown);

      // execute knitr
      module_context::processSupervisor().runProgram(rProgramPath,
                                                     args,
                                                     options,
                                                     cb);
   }

   bool onKnitContinue()
   {
      return !terminationRequested_;
   }

   void onKnitOutput(const std::string& output)
   {
      enqueHTMLPreviewOutput(output);
   }

   void onKnitCompleted(int exitStatus,
                        const FilePath& outputPathTempFile,
                        bool isMarkdown)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         // read the path to the output file
         std::string outputFile;
         Error error = core::readStringFromFile(outputPathTempFile,
                                                &outputFile);
         if (error)
         {
            terminateWithError(error);
         }
         else
         {
            // read the output file
            boost::algorithm::trim(outputFile);
            FilePath outputFilePath = targetFile_.parent().complete(outputFile);
            std::string output;
            error = core::readStringFromFile(outputFilePath, &output);
            if (error)
            {
               terminateWithError(error);
            }
            else
            {
               terminateWithContent(output, isMarkdown);
            }
         }
      }
      else
      {
         boost::format fmt("\nknitr terminated with status %1%\n");
         terminateWithError(boost::str(fmt % exitStatus));
      }
   }

   void terminateWithContent(const std::string& fileContents, bool isMarkdown)
   {
      // determine the preview HTML
      std::string previewHTML;
      if (isMarkdown)
      {
         Error error = markdown::markdownToHTML(fileContents,
                                                markdown::Extensions(),
                                                markdown::HTMLOptions(),
                                                &previewHTML);
         if (error)
         {
            terminateWithError(error);
            return;
         }
      }
      else
      {
         previewHTML = fileContents;
      }

      // create an output file and write to it
      FilePath outputFile = createOutputFile();
      Error error = core::writeStringToFile(outputFile, previewHTML);
      if (error)
         terminateWithError(error);
      else
         terminateWithSuccess(outputFile);
   }


   void terminateWithError(const Error& error)
   {
      std::string message =
         "Error generating HTML preview for " +
         module_context::createAliasedPath(targetFile_) + " " +
         error.summary();
      terminateWithError(message);
   }

   void terminateWithError(const std::string& message)
   {
      isRunning_ = false;
      enqueHTMLPreviewOutput(message);
      enqueHTMLPreviewFailed();
   }

   void terminateWithSuccess(const FilePath& outputFile)
   {
      isRunning_ = false;
      outputFile_ = outputFile;

      // TODO: determine whether we should enable scripts

      enqueHTMLPreviewSucceeded(kHTMLPreview "/", false);
   }

   static void enqueHTMLPreviewStarted(const FilePath& targetFile)
   {
      json::Object dataJson;
      dataJson["target_file"] = module_context::createAliasedPath(targetFile);
      ClientEvent event(client_events::kHTMLPreviewStartedEvent, dataJson);
      module_context::enqueClientEvent(event);
   }

   static void enqueHTMLPreviewOutput(const std::string& output)
   {
      ClientEvent event(client_events::kHTMLPreviewOutputEvent, output);
      module_context::enqueClientEvent(event);
   }

   static void enqueHTMLPreviewFailed()
   {
      json::Object resultJson;
      resultJson["succeeded"] = false;
      ClientEvent event(client_events::kHTMLPreviewCompletedEvent, resultJson);
      module_context::enqueClientEvent(event);
   }

   static void enqueHTMLPreviewSucceeded(const std::string& previewUrl,
                                         bool enableScripts)
   {
      json::Object resultJson;
      resultJson["succeeded"] = true;
      resultJson["preview_url"] = previewUrl;
      resultJson["enable_scripts"] = enableScripts;
      ClientEvent event(client_events::kHTMLPreviewCompletedEvent, resultJson);
      module_context::enqueClientEvent(event);
   }

   static FilePath createOutputFile()
   {
      return module_context::tempFile("html_preview", "htm");
   }

private:
   FilePath targetFile_;
   FilePath outputFile_;
   bool isRunning_;
   bool terminationRequested_;
};

// current preview (stays around after the preview executes so it can
// serve the web content back)
boost::shared_ptr<HTMLPreview> s_pCurrentPreview_;

bool isPreviewRunning()
{
   return s_pCurrentPreview_ && s_pCurrentPreview_->isRunning();
}

Error previewHTML(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   // read params
   std::string file, encoding;
   bool isMarkdown;
   Error error = json::readParams(request.params, &file,
                                                  &encoding,
                                                  &isMarkdown);
   if (error)
      return error;
   FilePath filePath = module_context::resolveAliasedPath(file);

   // if we have a preview already running then just return false
   if (isPreviewRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      s_pCurrentPreview_ = HTMLPreview::create(filePath,
                                               encoding,
                                               isMarkdown);
      pResponse->setResult(true);
   }

   return Success();
}


Error terminatePreviewHTML(const json::JsonRpcRequest&,
                           json::JsonRpcResponse*)
{
   if (isPreviewRunning())
      s_pCurrentPreview_->terminate();

   return Success();
}

void handlePreviewRequest(const http::Request& request,
                          http::Response* pResponse)
{
   // if there isn't a current preview this is an error
   if (!s_pCurrentPreview_)
   {
      pResponse->setError(http::status::NotFound, "No preview available");
      return;
   }

   // get the requested path
   std::string path = http::util::pathAfterPrefix(request,
                                                  kHTMLPreviewLocation);

   // if it is empty then this is the main request
   if (path.empty())
   {
      // determine location of template
      FilePath resourcesPath = session::options().rResourcesPath();
      FilePath htmlPreviewFile = resourcesPath.childPath("html_preview.htm");

      // setup template filter
      std::map<std::string,std::string> vars;
      vars["html_output"] = s_pCurrentPreview_->readOutput();
      text::TemplateFilter filter(vars);

      // send response
      pResponse->setNoCacheHeaders();
      pResponse->setBody(htmlPreviewFile, filter);
   }

   // request for dependent file
   else
   {
      // return the file
      FilePath filePath = s_pCurrentPreview_->dependentFilePath(path);
      pResponse->setCacheableFile(filePath, request);
   }
}

   
} // anonymous namespace

core::json::Object capabilitiesAsJson()
{
   // default to unsupported
   json::Object capsJson;
   capsJson["r_html_supported"] = false;
   capsJson["r_markdown_supported"] = false;

   r::sexp::Protect rProtect;
   SEXP capsSEXP;
   r::exec::RFunction func(".rs.getHTMLCapabilities");
   Error error = func.call(&capsSEXP, &rProtect);
   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      json::Value valJson;
      error = r::json::jsonValueFromList(capsSEXP, &valJson);
      if (error)
         LOG_ERROR(error);
      else if (core::json::isType<core::json::Object>(valJson))
         capsJson = valJson.get_obj();
   }

   return capsJson;
}


Error initialize()
{  
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionHTMLPreview.R"))
      (bind(registerRpcMethod, "preview_html", previewHTML))
      (bind(registerRpcMethod, "terminate_preview_html", terminatePreviewHTML))
      (bind(registerUriHandler, kHTMLPreviewLocation, handlePreviewRequest))
   ;
   return initBlock.execute();
}
   


} // namespace html_preview
} // namespace modules
} // namesapce session

