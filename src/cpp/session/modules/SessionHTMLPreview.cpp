/*
 * SessionHTMLPreview.cpp
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


#include "SessionHTMLPreview.hpp"

#include <boost/bind.hpp>
#include <boost/format.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <boost/iostreams/copy.hpp>
#include <boost/iostreams/concepts.hpp>
#include <boost/iostreams/filter/regex.hpp>
#include <boost/iostreams/filtering_stream.hpp>

#include <boost/algorithm/string/join.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/HtmlUtils.hpp>
#include <core/http/Util.hpp>
#include <core/PerformanceTimer.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/system/Process.hpp>
#include <core/StringUtils.hpp>
#include <core/HtmlUtils.hpp>

#include <core/markdown/Markdown.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/RUtil.hpp>
#include <r/ROptions.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionAsyncRProcess.hpp>

#define kHTMLPreview "html_preview"
#define kHTMLPreviewLocation "/" kHTMLPreview "/"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace html_preview {

namespace {

class HTMLPreview : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<HTMLPreview> create(const FilePath& targetFile,
                                                const std::string& encoding,
                                                bool isMarkdown,
                                                bool isNotebook,
                                                bool knit)
   {
      boost::shared_ptr<HTMLPreview> pPreview(new HTMLPreview(targetFile));
      pPreview->start(encoding, isMarkdown, isNotebook, knit);
      return pPreview;
   }

private:
   HTMLPreview(const FilePath& targetFile)
      : targetFile_(targetFile),
        isMarkdown_(false),
        isInternalMarkdown_(false),
        isNotebook_(false),
        requiresKnit_(false)
   {
   }

   void start(const std::string& encoding,
              bool isMarkdown,
              bool isNotebook,
              bool requiresKnit)
   {
      enqueHTMLPreviewStarted(targetFile_);

      isMarkdown_ = isMarkdown;
      isNotebook_ = isNotebook;
      requiresKnit_ = requiresKnit;

      // determine whether we need to knit the file
      if (requiresKnit)
      {
         performKnit(encoding);
      }

      // otherwise we can just either copy or generate the html inline
      // (for markdown) and return with success
      else
      {
         terminateWithContent(targetFile_, encoding);
      }
   }

public:
   ~HTMLPreview()
   {
   }

   // COPYING: prohibited

public:

   bool isMarkdown() { return isMarkdown_; }

   bool isInternalMarkdown() { return isInternalMarkdown_; }

   bool isNotebook() { return isNotebook_; }

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

   void performKnit(const std::string& encoding)
   {
      // predict the name of the output file -- if we can't do this then
      // we instrument our call to knitr to return it in a temp file
      FilePath outputFileTempFile;
      if (isMarkdown() && targetIsRMarkdown())
         knitrOutputFile_ = outputFileForTarget(".md");
      else if (targetFile_.extensionLowerCase() == ".rhtml")
         knitrOutputFile_= outputFileForTarget(".html");
      else
         outputFileTempFile = module_context::tempFile("knitr-output", "out");

      // R binary
      FilePath rProgramPath;
      Error error = module_context::rScriptPath(&rProgramPath);
      if (error)
      {
         terminateWithError(error);
         return;
      }

      // args
      std::string cmd;
      if (!knitrOutputFile_.empty())
      {
         boost::format fmt;

         fmt = boost::format("require(knitr); "
                              "knit('%2%', encoding='%1%');");

         cmd = boost::str(fmt % encoding % targetFile_.filename());
      }
      else
      {
         std::string tempFilePath = string_utils::utf8ToSystem(
                                           outputFileTempFile.absolutePath());
         boost::format fmt;
         fmt = boost::format("require(knitr); "
                             "knit('%2%', encoding='%1%'); "
                             "cat(o, file='%3%');");
         cmd = boost::str(fmt % encoding % targetFile_.filename() % 
                          tempFilePath);
      }

      outputPathTempFile_ = outputFileTempFile;
      encoding_ = encoding;

      async_r::AsyncRProcess::start(cmd.c_str(), targetFile_.parent(),
                                    async_r::R_PROCESS_REDIRECTSTDERR);
   }

   bool targetIsRMarkdown()
   {
      std::string ext = targetFile_.extensionLowerCase();
      return ext == ".rmd" || ext == ".rmarkdown";
   }

   void onStdout (const std::string& output)
   {
      enqueHTMLPreviewOutput(output);
   }

   void onStderr (const std::string& output)
   {
      enqueHTMLPreviewOutput(output);
   }

   void onCompleted(int exitStatus)
   {
      onKnitCompleted(exitStatus, outputPathTempFile_, encoding_);
   }

   void onKnitCompleted(int exitStatus,
                        const FilePath& outputPathTempFile,
                        const std::string& encoding)
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

         // terminate with content
         terminateWithContent(knitrOutputFile_, encoding);
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

   void terminateWithContent(const FilePath& filePath,
                             const std::string& encoding)
   {
      // lookup the custom renderer function and calculate
      // whether we are going to use it
      SEXP renderMarkdownSEXP = r::options::getOption(
                                       "rstudio.markdownToHTML");
      r::sexp::Protect rProtect(renderMarkdownSEXP);
      bool usingCustomMarkdownRenderer =
                        isMarkdown() && !isNotebook() &&
                        !r::sexp::isNull(renderMarkdownSEXP);

      // call custom renderer if necessary
      if (usingCustomMarkdownRenderer)
      {
         // fulfill semantics of calling the custom function
         // from the directory of the input file
         RestoreCurrentPathScope restorePathScope(
                           module_context::safeCurrentPath());
         Error error = filePath.parent().makeCurrentPath();
         if (error)
         {
            terminateWithError(error);
            return;
         }

         // call the function
         r::exec::RFunction renderMarkdownFunc(renderMarkdownSEXP);
         renderMarkdownFunc.addParam(
            string_utils::utf8ToSystem(filePath.filename()));
         renderMarkdownFunc.addParam(
            string_utils::utf8ToSystem(htmlPreviewFile().filename()));
         error = renderMarkdownFunc.call();
         if (error)
         {
            terminateWithError(error);
            return;
         }

         // terminate with the target file
         terminateWithSuccess(htmlPreviewFile());
      }
      else
      {
         // read file contents
         std::string content;
         Error error = module_context::readAndDecodeFile(filePath,
                                                         encoding,
                                                         true,
                                                         &content);
         if (error)
         {
            terminateWithError(error);
            return;
         }

         // if this is markdown then convert it
         if (isMarkdown())
         {
            // set flag indicating that we used the internal
            // markdown renderer (so we know to do mathjax,
            // highlighting, base64 encoding, etc.)
            isInternalMarkdown_ = true;

            // run markdownToHTML
            std::string htmlContent;
            Error error = markdown::markdownToHTML(
                                            content,
                                            markdown::Extensions(),
                                            markdown::HTMLOptions(),
                                            &htmlContent);
            if (error)
            {
               terminateWithError(error);
               return;
            }

            // substitute html version
            content = htmlContent;
         }

         // create an output file and write to it
         FilePath outputFile = createOutputFile();
         error = core::writeStringToFile(outputFile, content);
         if (error)
            terminateWithError(error);
         else
            terminateWithSuccess(outputFile);
      }
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
      markCompleted();
      enqueHTMLPreviewOutput(message);
      enqueHTMLPreviewFailed();
   }

   void terminateWithSuccess(const FilePath& outputFile)
   {
      markCompleted();
      outputFile_ = outputFile;

      enqueHTMLPreviewSucceeded(kHTMLPreview "/",
                                targetFile(),
                                htmlPreviewFile(),
                                isMarkdown(),
                                !isNotebook());
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
                                         bool enableSaveAs,
                                         bool enableRefresh)
   {
      using namespace module_context;
      json::Object resultJson;
      resultJson["succeeded"] = true;
      resultJson["source_file"] = module_context::createAliasedPath(sourceFile);
      resultJson["html_file"] = module_context::createAliasedPath(htmlFile);
      resultJson["preview_url"] = previewUrl;
      resultJson["enable_saveas"] = enableSaveAs;
      resultJson["enable_refresh"] = enableRefresh;
      resultJson["previously_published"] = !previousRpubsUploadId(htmlFile).empty();
      ClientEvent event(client_events::kHTMLPreviewCompletedEvent, resultJson);
      module_context::enqueClientEvent(event);
   }

   static FilePath createOutputFile()
   {
      return module_context::tempFile("html_preview", "htm");
   }

private:
   FilePath targetFile_;
   FilePath outputPathTempFile_;
   bool isMarkdown_;
   bool isInternalMarkdown_;
   bool isNotebook_;
   bool requiresKnit_;
   std::string encoding_;

   FilePath knitrOutputFile_;
   FilePath outputFile_;
};

std::string deriveNotebookPath(const std::string& path)
{
   if (path.size() > 2
       && (boost::algorithm::ends_with(path, ".r") ||
           boost::algorithm::ends_with(path, ".R")))
   {
      return path.substr(0, path.size()-2) + ".Rmd";
   }
   return path + ".Rmd";
}

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
   bool isMarkdown, knit, isNotebook;
   Error error = json::readObjectParam(request.params, 0,
                                       "path", &file,
                                       "encoding", &encoding,
                                       "is_markdown", &isMarkdown,
                                       "requires_knit", &knit,
                                       "is_notebook", &isNotebook);

   if (error)
      return error;

   if (isNotebook)
      file = deriveNotebookPath(file);
   
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
                                               isNotebook,
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


const char* const MAGIC_GUID = "12861c30b10411e1afa60800200c9a66";
const char* const FIGURE_DIR = "figure-compile-notebook-12861c30b";

bool okToGenerateFile(const FilePath& rmdPath,
                      const std::string& extension,
                      std::string* pErrMsg)
{
   FilePath filePath = rmdPath.parent().complete(
                                    rmdPath.stem() + extension);

   if (filePath.exists())
   {
      boost::shared_ptr<std::istream> pStr;
      Error error = filePath.open_r(&pStr);
      if (error)
      {
         *pErrMsg = "Error opening file: " + error.summary();
         return false;
      }

      // copy the file's contents
      std::vector<char> contents;
      std::copy(std::istream_iterator<char>(*pStr),
                std::istream_iterator<char>(),
                std::back_inserter(contents));

      std::string magicGuid(MAGIC_GUID);
      if (contents.end() == std::search(contents.begin(),
                                        contents.end(),
                                        magicGuid.begin(),
                                        magicGuid.end()))
      {
         *pErrMsg = "Unable to generate the file '" +
                    filePath.filename() + "' because it already "
                    "exists.\n\n"
                    "You need to move or delete this file prior to "
                    "compiling a notebook for this R script.";
         return false;
      }
      else
      {
         return true;
      }
   }
   else
   {
      return true;
   }
}

bool okToGenerateFiles(const FilePath& rmdPath, std::string* pErrMsg)
{
   return okToGenerateFile(rmdPath, ".Rmd", pErrMsg) &&
          okToGenerateFile(rmdPath, ".md", pErrMsg) &&
          okToGenerateFile(rmdPath, ".html", pErrMsg);
}

Error createNotebook(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string id, prefix, suffix, type;
   bool sessionInfo;
   Error error = json::readObjectParam(request.params, 0,
                                       "id", &id,
                                       "prefix", &prefix,
                                       "suffix", &suffix,
                                       "session_info", &sessionInfo,
                                       "notebook_type", &type);
   if (error)
      return error;

   boost::shared_ptr<source_database::SourceDocument> pDoc(
         new source_database::SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
      return error;

   std::string path = pDoc->path();
   path = deriveNotebookPath(path);

   FilePath realPath = module_context::resolveAliasedPath(path);

   std::string signature =
     "\n<!-- Automatically generated by RStudio [" +
                        std::string(MAGIC_GUID) + "] -->\n";

   // make sure we can overwrite any existing files
   std::string errMsg;
   if (!okToGenerateFiles(realPath, &errMsg))
   {
      json::Object resultJson;
      resultJson["succeeded"] = false;
      resultJson["failure_message"] =  errMsg;
      pResponse->setResult(resultJson);
      return Success();
   }

   if (type == "default")
   {
      // Add the signature to the prefix
      prefix = signature + prefix;

      error = r::util::iconvstr(prefix,
                                "UTF-8",
                                pDoc->encoding(),
                                true,
                                &prefix);
      if (error)
         return error;
      error = r::util::iconvstr(suffix,
                                "UTF-8",
                                pDoc->encoding(),
                                true,
                                &suffix);
      if (error)
         return error;

      std::string contents;
      contents.append(prefix);
      contents.append("`r knitr::opts_chunk$set(tidy=FALSE, comment=NA, "
                      "fig.path='" +std::string(FIGURE_DIR) + "/')`");
      contents.append("\n");
      contents.append("```{r}\n");
      contents.append(pDoc->contents());
      contents.append("\n```\n");
      contents.append(suffix);

      // write the file
      error = writeStringToFile(realPath, contents);
      if (error)
         return error;
   }
   else
   {
      // switch to the source document's directory for this operation
      RestoreCurrentPathScope restorePathScope(
                              module_context::safeCurrentPath());

      FilePath scriptPath = module_context::resolveAliasedPath(
                                                      pDoc->path());
      Error error = scriptPath.parent().makeCurrentPath();
      if (error)
         return error;

      // determine the function to be used to convert .R to .Rmd
      std::string rmdFunction;
      if (type == "spin")
         rmdFunction = ".rs.spinScript";
      else
         return Error(json::errc::ParamInvalid, ERROR_LOCATION);

      // call the function
      std::string scriptFileName =
               string_utils::utf8ToSystem(scriptPath.filename());
      error = r::exec::RFunction(rmdFunction,
                                    scriptFileName,
                                    signature).call();
      if (error)
      {
         json::Object result;
         result["succeeded"] = false;
         result["failure_message"] = r::endUserErrorMessage(error);
         pResponse->setResult(result);
         return Success();
      }
   }

   // return success
   json::Object resultJson;
   resultJson["succeeded"] = true;
   pResponse->setResult(resultJson);
   return Success();
}


bool requiresHighlighting(const std::string& htmlOutput)
{
   boost::regex hlRegex("<pre><code class=\"(r|cpp)\"");
   return regex_utils::search(htmlOutput, hlRegex);
}


// for whatever reason when we host an iFrame in a Qt WebKit instance
// it only looks at the very first font listed in the font-family
// attribute. if the font isn't found then it displays a non-monospace
// font by default. so to make preview mode always work we need to
// order the font-family list according to the current platform.
std::string preFontFamily()
{
   std::vector<std::string> linuxFonts;
   linuxFonts.push_back("'DejaVu Sans Mono'");
   linuxFonts.push_back("'Droid Sans Mono'");

   std::vector<std::string> windowsFonts;
   windowsFonts.push_back("'Lucida Console'");
   windowsFonts.push_back("Consolas");

   std::vector<std::string> macFonts;
   macFonts.push_back("Monaco");

   std::vector<std::string> universalFonts;
   universalFonts.push_back("monospace");

   std::vector<std::string> fonts;
#if defined(__APPLE__)
   fonts.insert(fonts.end(), macFonts.begin(), macFonts.end());
   fonts.insert(fonts.end(), linuxFonts.begin(), linuxFonts.end());
   fonts.insert(fonts.end(), windowsFonts.begin(), windowsFonts.end());
#elif defined(_WIN32)
   fonts.insert(fonts.end(), windowsFonts.begin(), windowsFonts.end());
   fonts.insert(fonts.end(), linuxFonts.begin(), linuxFonts.end());
   fonts.insert(fonts.end(), macFonts.begin(), macFonts.end());
#else
   fonts.insert(fonts.end(), linuxFonts.begin(), linuxFonts.end());
   fonts.insert(fonts.end(), windowsFonts.begin(), windowsFonts.end());
   fonts.insert(fonts.end(), macFonts.begin(), macFonts.end());
#endif
   fonts.insert(fonts.end(), universalFonts.begin(), universalFonts.end());

   return boost::algorithm::join(fonts, ", ");
}


void modifyOutputForPreview(std::string* pOutput)
{
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      // use correct font ordering for this platform
      *pOutput = boost::regex_replace(
           *pOutput,
            boost::regex("tt, code, pre \\{\\n\\s+font-family:[^\n]+;"),
           "tt, code, pre {\n   font-family: " + preFontFamily() + ";");

#if defined(_WIN32)
      // add HTML-CSS options required for correct qtwebkit rendering
      std::string target = "<!-- MathJax scripts -->";
      boost::algorithm::replace_first(
               *pOutput,
               target,
               target + "\n" kQtMathJaxConfigScript);
#endif
   }

   // serve mathjax locally from this directory
   std::string previewMathjax = "mathjax-26";

   // replace old mathjax CDN for compatibility with R packages that still use
   // the deprecated URL
   boost::algorithm::replace_first(
        *pOutput,
        "https://cdn.mathjax.org/mathjax/latest",
        previewMathjax);

   // replace RStudio MathJax URL
   boost::algorithm::replace_first(
        *pOutput,
        "https://mathjax.rstudio.com/latest",
        previewMathjax);
}


Error readPreviewTemplate(const FilePath& resPath,
                          std::string* pPreviewTemplate)
{

   FilePath htmlPreviewFile = resPath.childPath("markdown.html");
   return core::readStringFromFile(htmlPreviewFile, pPreviewTemplate);
}

void setVarFromHtmlResourceFile(const std::string& name,
                                const std::string& fileName,
                                std::map<std::string,std::string>* pVars)
{
   std::string fileContents = module_context::resourceFileAsString(fileName);
   (*pVars)[name] = fileContents;
}

void setVarFromHtmlResourceFile(const std::string& name,
                                std::map<std::string,std::string>* pVars)
{
   setVarFromHtmlResourceFile(name, name + ".html", pVars);
}

void handleInternalMarkdownPreviewRequest(
                                  const http::Request& request,
                                  http::Response* pResponse)
{
   try
   {
      // read input template
      FilePath resPath = session::options().rResourcesPath();
      std::string previewTemplate;
      Error error = readPreviewTemplate(resPath, &previewTemplate);
      if (error)
      {
         pResponse->setError(error);
         return;
      }

      // read output
      std::string htmlOutput = s_pCurrentPreview_->readOutput();

      // define template filter
      std::map<std::string,std::string> vars;
      vars["title"] = html_utils::defaultTitle(htmlOutput);
      setVarFromHtmlResourceFile("markdown_css", "markdown.css", &vars);
      if (requiresHighlighting(htmlOutput))
         setVarFromHtmlResourceFile("r_highlight", &vars);
      else
         vars["r_highlight"]  = "";
      if (markdown::isMathJaxRequired(htmlOutput))
         setVarFromHtmlResourceFile("mathjax", &vars);
      else
         vars["mathjax"] = "";
      vars["html_output"] = htmlOutput;
      text::TemplateFilter templateFilter(vars);

      // define base64 image filter
      html_utils::Base64ImageFilter imageFilter(
                                    s_pCurrentPreview_->targetDirectory());

      // write into in-memory string
      std::istringstream previewInputStream(previewTemplate);
      std::stringstream previewStrStream;
      previewStrStream.exceptions(std::istream::failbit | std::istream::badbit);
      boost::iostreams::filtering_ostream previewOutputStream ;
      previewOutputStream.push(templateFilter);
      previewOutputStream.push(imageFilter);
      previewOutputStream.push(previewStrStream);
      boost::iostreams::copy(previewInputStream,
                             previewOutputStream,
                             128);


      // write to output file
      std::string previewHtml = previewStrStream.str();
      error = core::writeStringToFile(s_pCurrentPreview_->htmlPreviewFile(),
                                      previewHtml);
      if (error)
      {
         pResponse->setError(error);
         return;
      }

      // remove generated files if this was a notebook
      if (s_pCurrentPreview_->isNotebook())
      {
         error = s_pCurrentPreview_->targetFile().removeIfExists();
         if (error)
            LOG_ERROR(error);

         error = s_pCurrentPreview_->knitrOutputFile()
                                                .removeIfExists();
         if (error)
            LOG_ERROR(error);

         error = s_pCurrentPreview_->targetDirectory()
                           .complete(FIGURE_DIR).removeIfExists();
         if (error)
            LOG_ERROR(error);
      }

      // modify outpout then write back to client
      modifyOutputForPreview(&previewHtml);
      pResponse->setDynamicHtml(previewHtml, request);
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      LOG_ERROR(error);
      pResponse->setError(error);
   }
}

void handlePreviewRequest(const http::Request& request,
                          http::Response* pResponse)
{
   // if there isn't a current preview this is an error
   if (!s_pCurrentPreview_)
   {
      pResponse->setNotFoundError(request.uri());
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
      {
         if (s_pCurrentPreview_->isInternalMarkdown())
            handleInternalMarkdownPreviewRequest(request, pResponse);
         else
            pResponse->setFile(s_pCurrentPreview_->htmlPreviewFile(),
                               request);
      }
      else if (s_pCurrentPreview_->requiresKnit())
      {
         pResponse->setFile(s_pCurrentPreview_->knitrOutputFile(), request);
      }
      else
      {
         pResponse->setFile(s_pCurrentPreview_->targetFile(), request);
      }
   }

   // request for mathjax file
   else if (boost::algorithm::starts_with(path, "mathjax-26"))
   {

      FilePath filePath =
            session::options().mathjaxPath().parent().childPath(path);
      pResponse->setFile(filePath, request);
   }

   // request for dependent file
   else
   {
      FilePath filePath = s_pCurrentPreview_->targetDirectory().childPath(path);
      addFileSpecificHeaders(filePath, pResponse);
      pResponse->setFile(filePath, request);
   }
}

   
} // anonymous namespace
   

void addFileSpecificHeaders(const FilePath& filePath, http::Response* pResponse)
{
   std::string videoExts(".mov|.mp4|.m4v|.3gp|.avi");
   std::string ext = filePath.extensionLowerCase();
   if (ext.length() == 4 &&
       videoExts.find(ext) != std::string::npos &&
       session::options().programMode() == kSessionProgramModeDesktop)
   {
      // mp4 and QuickTime files served in IFrames cause problems on
      // some desktop configurations (see case 3828)
      pResponse->addHeader("X-Frame-Options", "deny");
   }
}

core::json::Object capabilitiesAsJson()
{
   // default to unsupported
   json::Object capsJson;
   capsJson["r_markdown_supported"] = false;
   capsJson["stitch_supported"] = false;

   // query for required versions
   std::string markdownVersion = "1.2";
   std::string stitchVersion = "1.2";
   r::sexp::Protect rProtect;
   SEXP capsSEXP;
   r::exec::RFunction func(".rs.getHTMLCapabilities");
   func.addParam(markdownVersion);
   func.addParam(stitchVersion);
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
      (bind(registerRpcMethod, "create_notebook", createNotebook))
      (bind(registerUriHandler, kHTMLPreviewLocation, handlePreviewRequest))
   ;
   return initBlock.execute();
}
   


} // namespace html_preview
} // namespace modules
} // namespace session
} // namespace rstudio

