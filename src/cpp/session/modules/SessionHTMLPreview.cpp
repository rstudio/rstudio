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
#include <core/text/TemplateFilter.hpp>

#include <core/markdown/Markdown.hpp>

#include <session/SessionModuleContext.hpp>

#define kHTMLPreview "html_preview"
#define kHTMLPreviewLocation "/" kHTMLPreview "/"

using namespace core;

namespace session {
namespace modules { 
namespace html_preview {

namespace {

class HTMLPreview : boost::noncopyable,
                    public boost::enable_shared_from_this<HTMLPreview>
{
public:
   static boost::shared_ptr<HTMLPreview> create(
                     const FilePath& targetFile,
                     const std::string& encoding,
                     bool isMarkdown,
                     bool knit)
   {
      return boost::shared_ptr<HTMLPreview>(new HTMLPreview(targetFile,
                                                            encoding,
                                                            isMarkdown,
                                                            knit));
   }

private:
   HTMLPreview(const FilePath& targetFile,
               const std::string& encoding,
               bool isMarkdown,
               bool knit)
      : targetFile_(targetFile), isRunning_(false), terminationRequested_(false)
   {
      enqueHTMLPreviewStarted(targetFile_);

      if (knit)
      {
         isRunning_ = true;



         terminateWithSuccess(createOutputFile());
      }
      else
      {
         // read the file using the specified encoding
         std::string fileContents;
         Error error = module_context::readAndDecodeFile(targetFile,
                                                         encoding,
                                                         true,
                                                         &fileContents);
         if (error)
         {
            terminateWithError(error);
            return;
         }

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
         error = core::writeStringToFile(outputFile, previewHTML);
         if (error)
            terminateWithError(error);
         else
            terminateWithSuccess(outputFile);
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

   void terminateWithError(const Error& error)
   {
      isRunning_ = false;
      std::string message =
         "Error generating HTML preview for " +
         module_context::createAliasedPath(targetFile_) + " " +
         error.summary();
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
   bool isMarkdown, knit;
   Error error = json::readParams(request.params, &file,
                                                  &encoding,
                                                  &isMarkdown,
                                                  &knit);
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
                                               isMarkdown,
                                               knit);
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

Error initialize()
{  
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "preview_html", previewHTML))
      (bind(registerRpcMethod, "terminate_preview_html", terminatePreviewHTML))
      (bind(registerUriHandler, kHTMLPreviewLocation, handlePreviewRequest))
   ;
   return initBlock.execute();
}
   


} // namespace html_preview
} // namespace modules
} // namesapce session

