/*
 * SessionHTMLPreview.cpp
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


#include "SessionHTMLPreview.hpp"

#include <boost/bind.hpp>
#include <boost/format.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <boost/iostreams/copy.hpp>
#include <boost/iostreams/concepts.hpp>
#include <boost/iostreams/filter/regex.hpp>
#include <boost/iostreams/filtering_stream.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/Base64.hpp>
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

class HTMLPreview : boost::noncopyable,
                    public boost::enable_shared_from_this<HTMLPreview>
{
public:
   static boost::shared_ptr<HTMLPreview> create(const FilePath& targetFile,
                                                const std::string& encoding,
                                                bool isMarkdown,
                                                bool knit)
   {
      boost::shared_ptr<HTMLPreview> pPreview(new HTMLPreview(targetFile));
      pPreview->start(encoding, isMarkdown, knit);
      return pPreview;
   }

private:
   HTMLPreview(const FilePath& targetFile)
      : targetFile_(targetFile),
        isMarkdown_(false),
        requiresKnit_(false),
        isRunning_(false),
        terminationRequested_(false)
   {
   }

   void start(const std::string& encoding, bool isMarkdown, bool requiresKnit)
   {
      enqueHTMLPreviewStarted(targetFile_);

      isMarkdown_ = isMarkdown;
      requiresKnit_ = requiresKnit;

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
      if (requiresKnit)
      {
         performKnit(encoding, isMarkdown);
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

   bool isMarkdown() { return isMarkdown_; }

   bool requiresKnit() { return requiresKnit_; }

   FilePath targetFile() const
   {
      return targetFile_;
   }

   FilePath targetDirectory() const
   {
      return targetFile_.parent();
   }

   FilePath knitrOutputFile() const
   {
      return knitrOutputFile_;
   }

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

   FilePath htmlPreviewFile()
   {
      FilePath baseFile = requiresKnit() ? knitrOutputFile() : targetFile();
      if (isMarkdown())
         return baseFile.parent().childPath(baseFile.stem() + ".html");
      else
         return baseFile;
   }

private:

   void performKnit(const std::string& encoding, bool isMarkdown)
   {
      // set running flag
      isRunning_ = true;

      // predict the name of the output file -- if we can't do this then
      // we instrument our call to knitr to return it in a temp file
      FilePath outputFileTempFile;
      if (isMarkdown && targetIsRMarkdown())
         knitrOutputFile_ = outputFileForTarget(".md");
      else if (targetFile_.extensionLowerCase() == ".rhtml")
         knitrOutputFile_= outputFileForTarget(".html");
      else
         outputFileTempFile = module_context::tempFile("knitr-output", "out");

      // R binary
      std::string rProgramPath = r::exec::rBinaryPath().absolutePath();

      // args
      std::vector<std::string> args;
      args.push_back("--silent");
      args.push_back("-e");
      if (!knitrOutputFile_.empty())
      {
         boost::format fmt("options(encoding='%1%'); "
                           "require(knitr); "
                           "knit('%2%');");
         std::string cmd = boost::str(fmt % encoding % targetFile_.filename());
         args.push_back(cmd);
      }
      else
      {
         std::string tempFilePath = string_utils::utf8ToSystem(
                                           outputFileTempFile.absolutePath());
         boost::format fmt("options(encoding='%1%'); "
                           "require(knitr); "
                           "o <- knit('%2%'); "
                           "cat(o, file='%3%');");
         std::string cmd = boost::str(fmt % encoding
                                          % targetFile_.filename()
                                          % tempFilePath);
         args.push_back(cmd);
      }

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
                                _1, outputFileTempFile, encoding, isMarkdown);

      // execute knitr
      module_context::processSupervisor().runProgram(rProgramPath,
                                                     args,
                                                     options,
                                                     cb);
   }

   bool targetIsRMarkdown()
   {
      std::string ext = targetFile_.extensionLowerCase();
      return ext == ".rmd" || ext == ".rmarkdown";
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
                        const std::string& encoding,
                        bool isMarkdown)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         // determine the path of the knitr output file if necessary
         if (knitrOutputFile_.empty())
         {
            std::string outputFile;
            Error error = core::readStringFromFile(outputPathTempFile,
                                                   &outputFile);
            if (error)
            {
               terminateWithError(error);
               return;
            }
            boost::algorithm::trim(outputFile);
            knitrOutputFile_ = targetFile_.parent().complete(outputFile);
         }

         // read the file using the specified encoding
         std::string output;
         Error error = module_context::readAndDecodeFile(knitrOutputFile_,
                                                         encoding,
                                                         true,
                                                         &output);
         if (error)
            terminateWithError(error);
         else
            terminateWithContent(output, isMarkdown);
      }
      else
      {
         boost::format fmt("\nknitr terminated with status %1%\n");
         terminateWithError(boost::str(fmt % exitStatus));
      }
   }

   FilePath outputFileForTarget(const std::string& ext)
   {
      return targetFile_.parent().childPath(targetFile_.stem() + ext);
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

      enqueHTMLPreviewSucceeded(kHTMLPreview "/",
                                targetFile(),
                                htmlPreviewFile(),
                                isMarkdown());
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
                                         const FilePath& sourceFile,
                                         const FilePath& htmlFile,
                                         bool enableSaveAs)
   {
      json::Object resultJson;
      resultJson["succeeded"] = true;
      resultJson["source_file"] = module_context::createAliasedPath(sourceFile);
      resultJson["html_file"] = module_context::createAliasedPath(htmlFile);
      resultJson["preview_url"] = previewUrl;
      resultJson["enable_saveas"] = enableSaveAs;
      ClientEvent event(client_events::kHTMLPreviewCompletedEvent, resultJson);
      module_context::enqueClientEvent(event);
   }

   static FilePath createOutputFile()
   {
      return module_context::tempFile("html_preview", "htm");
   }

private:
   FilePath targetFile_;
   bool isMarkdown_;
   bool requiresKnit_;

   FilePath knitrOutputFile_;
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
   Error error = json::readObjectParam(request.params, 0,
                                       "path", &file,
                                       "encoding", &encoding,
                                       "is_markdown", &isMarkdown,
                                       "requires_knit", &knit);
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

Error getHTMLCapabilities(const json::JsonRpcRequest&,
                           json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(html_preview::capabilitiesAsJson());
   return Success();
}


// convert images to base64
class Base64ImageFilter : public boost::iostreams::regex_filter
{
public:
   Base64ImageFilter(const FilePath& basePath)
      : boost::iostreams::regex_filter(
          boost::regex(
           "(<\\s*[Ii][Mm][Gg] [^\\>]*[Ss][Rr][Cc]\\s*=\\s*)([\"'])(.*?)(\\2)"),
           boost::bind(&Base64ImageFilter::toBase64Image, this, _1)),
        basePath_(basePath)
   {
   }

private:
   std::string toBase64Image(const boost::cmatch& match)
   {
      // extract image reference
      std::string imgRef = match[3];

      // see if this is an image within the base directory. if it is then
      // base64 encode it
      FilePath imagePath = basePath_.childPath(imgRef);
      if (imagePath.exists() &&
          boost::algorithm::starts_with(imagePath.mimeContentType(), "image/"))
      {
         std::string imageContents;
         Error error = core::readStringFromFile(imagePath, &imageContents);
         if (!error)
         {
            std::string imageBase64;
            Error error = core::base64::encode(imageContents, &imageBase64);
            if (!error)
            {
               imgRef = "data:" + imagePath.mimeContentType() + ";base64,";
               imgRef.append(imageBase64);
            }
            else
            {
               LOG_ERROR(error);
            }
         }
         else
         {
            LOG_ERROR(error);
         }
      }

      // return the filtered result
      return match[1] + match[2] + imgRef + match[4];
   }

private:
   FilePath basePath_;
};


// add no-highlight designator to vanilla blocks
class NoHighlightFilter : public boost::iostreams::regex_filter
{
public:
   NoHighlightFilter()
      : boost::iostreams::regex_filter(
          boost::regex("^<pre><code>"),
          boost::bind(&NoHighlightFilter::noHighlight, this, _1))
   {
   }

private:
   std::string noHighlight(const boost::cmatch&)
   {
      return "<pre><code class=\"no-highlight\">";
   }
};

bool requiresHighlighting(const std::string& htmlOutput)
{
   boost::regex hlRegex("^<pre><code class=(?!\"no-highlight\")");
   return boost::regex_search(htmlOutput, hlRegex);
}

bool requiresMathjax(const std::string& htmlOutput)
{
   boost::regex inlineMathRegex("\\$[^\\n]+\\$");
   if (boost::regex_search(htmlOutput, inlineMathRegex))
      return true;

   boost::regex mathmlRegex("<math[>\\s](?s).*?</math>");
   if (boost::regex_search(htmlOutput, mathmlRegex))
      return true;

   std::size_t escapeLoc = htmlOutput.find("$$");
   if (escapeLoc == std::string::npos)
      return false;
   else
      return htmlOutput.find("$$", escapeLoc) != std::string::npos;
}

void handleMarkdownPreviewRequest(http::Response* pResponse)
{
   try
   {
      // read output
      std::string htmlOutput = s_pCurrentPreview_->readOutput();

      // open input file (template)
      FilePath resPath = session::options().rResourcesPath();
      FilePath htmlPreviewFile = resPath.childPath("html_preview.htm");
      boost::shared_ptr<std::istream> pIfs;
      Error error = htmlPreviewFile.open_r(&pIfs);
      if (error)
      {
         pResponse->setError(error);
         return;
      }
      pIfs->exceptions(std::istream::failbit | std::istream::badbit);

      // inject highlight.js if necessary
      std::string highlightJs, highlightStyles;
      if (requiresHighlighting(htmlOutput))
      {
         error = readStringFromFile(resPath.childPath("highlight.pack.js"),
                                    &highlightJs);
         if (error)
            LOG_ERROR(error);
        highlightJs += "\n   hljs.initHighlightingOnLoad();";

         error = readStringFromFile(resPath.childPath("highlight.styles.css"),
                                    &highlightStyles);
         if (error)
            LOG_ERROR(error);
      }

      // inject mathjax if necessary
      std::string mathjaxJs;
      if (requiresMathjax(htmlOutput))
      {
         mathjaxJs = "<script type=\"text/javascript\" "
                     "src=\"https://d3eoax9i5htok0.cloudfront.net/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML\">"
                     "</script>"
                     "<script type=\"text/javascript\">"
                     "MathJax.Hub.Config({"
                        "tex2jax: {"
                           "processEscapes: true, "
                           "inlineMath: [ ['$','$'] ], "
                           "displayMath: [ ['$$','$$'] ] "
                        "}, "
                        "asciimath2jax: {"
                           "delimiters: [ ['$','$'] ] "
                        "}"
                     "});"
                     "</script>";
      }

      // setup template filter
      std::map<std::string,std::string> vars;
      vars["highlight_js"] = highlightJs;
      vars["highlight_js_styles"] = highlightStyles;
      vars["mathjax_js"] = mathjaxJs;
      vars["html_output"] = htmlOutput;
      text::TemplateFilter templateFilter(vars);

      // setup filters
      Base64ImageFilter imageFilter(s_pCurrentPreview_->targetDirectory());
      NoHighlightFilter noHighlightFilter;

      // open output file
      FilePath outputFile = s_pCurrentPreview_->htmlPreviewFile();
      boost::shared_ptr<std::ostream> pOfs;
      error = outputFile.open_w(&pOfs);
      if (error)
      {
         pResponse->setError(error);
         return;
      }
      pOfs->exceptions(std::istream::failbit | std::istream::badbit);

      // copy to output file with filters
      boost::iostreams::filtering_ostream filteringStream ;
      filteringStream.push(templateFilter);
      filteringStream.push(imageFilter);
      filteringStream.push(noHighlightFilter);
      filteringStream.push(*pOfs);
      boost::iostreams::copy(*pIfs, filteringStream, 128);

      // send response
      pResponse->setBody(outputFile);
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      pResponse->setError(error);
   }
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

   // disable caching entirely
   pResponse->setNoCacheHeaders();

   // get the requested path
   std::string path = http::util::pathAfterPrefix(request,
                                                  kHTMLPreviewLocation);

   // if it is empty then this is the main request
   if (path.empty())
   {
      if (s_pCurrentPreview_->isMarkdown())
         handleMarkdownPreviewRequest(pResponse);
      else if (s_pCurrentPreview_->requiresKnit())
         pResponse->setFile(s_pCurrentPreview_->knitrOutputFile(), request);
      else
         pResponse->setFile(s_pCurrentPreview_->targetFile(), request);
   }

   // request for dependent file
   else
   {
      FilePath filePath = s_pCurrentPreview_->targetDirectory().childPath(path);
      pResponse->setFile(filePath, request);
   }
}


   
} // anonymous namespace

core::json::Object capabilitiesAsJson()
{
   // default to unsupported
   std::string htmlVersion = "0.4.13";
   std::string markdownVersion = "0.4.13";
   json::Object capsJson;
   capsJson["r_html_version"] = htmlVersion;
   capsJson["r_markdown_version"] = markdownVersion;
   capsJson["r_html_supported"] = false;
   capsJson["r_markdown_supported"] = false;

   r::sexp::Protect rProtect;
   SEXP capsSEXP;
   r::exec::RFunction func(".rs.getHTMLCapabilities");
   func.addParam(htmlVersion);
   func.addParam(markdownVersion);
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
      (bind(registerRpcMethod, "get_html_capabilities", getHTMLCapabilities))
      (bind(registerUriHandler, kHTMLPreviewLocation, handlePreviewRequest))
   ;
   return initBlock.execute();
}
   


} // namespace html_preview
} // namespace modules
} // namesapce session

