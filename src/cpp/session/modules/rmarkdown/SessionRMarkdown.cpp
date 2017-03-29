/*
 * SessionRMarkdown.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include "SessionRMarkdown.hpp"
#include "SessionRmdNotebook.hpp"
#include "../SessionHTMLPreview.hpp"
#include "../build/SessionBuildErrors.hpp"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/iostreams/filter/regex.hpp>
#include <boost/format.hpp>
#include <boost/foreach.hpp>
#include <boost/scope_exit.hpp>

#include <core/FileSerializer.hpp>
#include <core/Exec.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/StringUtils.hpp>
#include <core/Algorithm.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/ROptions.hpp>
#include <r/RUtil.hpp>
#include <r/RRoutines.hpp>
#include <r/RCntxtUtils.hpp>

#include <core/r_util/RProjectFile.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/SessionAsyncRProcess.hpp>

#include <session/projects/SessionProjects.hpp>

#include "RMarkdownPresentation.hpp"

#define kRmdOutput "rmd_output"
#define kRmdOutputLocation "/" kRmdOutput "/"

#define kMathjaxSegment "mathjax"
#define kMathjaxBeginComment "<!-- dynamically load mathjax"

#define kStandardRenderFunc "rmarkdown::render"
#define kShinyRenderFunc "rmarkdown::run"

#define kShinyContentWarning "Warning: Shiny application in a static R Markdown document"

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace {

enum 
{
   RExecutionReady = 0,
   RExecutionBusy  = 1
};

std::string projectBuildDir()
{
   return string_utils::utf8ToSystem(
       projects::projectContext().buildTargetPath().absolutePath());
}

std::string s_websiteOutputDir;

void initWebsiteOutputDir()
{
   if (!module_context::isWebsiteProject())
      return;

   r::exec::RFunction websiteOutputDir(".rs.websiteOutputDir",
                                       projectBuildDir());
   std::string outputDirFullPath;
   Error error = websiteOutputDir.call(&outputDirFullPath);
   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      if (outputDirFullPath != projectBuildDir())
         s_websiteOutputDir = FilePath(outputDirFullPath).filename();
      else
         s_websiteOutputDir = "";
   }
}

} // anonymous namespace

namespace module_context {

FilePath extractOutputFileCreated(const FilePath& inputFile,
                                  const std::string& output)
{
   // check each line of the emitted output; if it starts with a token
   // indicating rendering is complete, store the remainder of the emitted
   // line as the file we rendered
   std::vector<std::string> completeMarkers;
   completeMarkers.push_back("Preview created: ");
   completeMarkers.push_back("Output created: ");
   std::string renderLine;
   std::stringstream outputStream(output);
   while (std::getline(outputStream, renderLine))
   {
      BOOST_FOREACH(const std::string& marker, completeMarkers)
      {
         if (boost::algorithm::starts_with(renderLine, marker))
         {
            std::string fileName = renderLine.substr(marker.length());

            // trim any whitespace from the end of the filename (on Windows
            // this includes part of CR-LF)
            boost::algorithm::trim(fileName);

            // if the path looks absolute, use it as-is; otherwise, presume
            // it to be in the same directory as the input file
            return inputFile.parent().complete(fileName);
         }
      }
   }

   return FilePath();
}

} // namespace module_context

namespace modules {
namespace rmarkdown {

namespace {


enum RenderTerminateType
{
   renderTerminateNormal,
   renderTerminateAbnormal,
   renderTerminateQuiet
};

// s_renderOutputs is a rotating buffer that maps an output identifier to a
// target location on disk, to give the client access to the last few renders
// that occurred in this session without exposing disk paths in the URL. it's
// unlikely that the client will ever request a render output other than the
// one that was just completed, so keeping > 2 render paths is conservative.
#define kMaxRenderOutputs 5
std::vector<std::string> s_renderOutputs(kMaxRenderOutputs);
int s_currentRenderOutput = 0;

FilePath outputCachePath()
{
   return module_context::sessionScratchPath().childPath("rmd-outputs");
}

std::string assignOutputUrl(const std::string& outputFile)
{
   std::string outputUrl(kRmdOutput "/");
   s_currentRenderOutput = (s_currentRenderOutput + 1) % kMaxRenderOutputs;
   s_renderOutputs[s_currentRenderOutput] = outputFile;
   outputUrl.append(boost::lexical_cast<std::string>(s_currentRenderOutput));
   outputUrl.append("/");
   return outputUrl;
}

void getOutputFormat(const std::string& path,
                     const std::string& encoding,
                     json::Object* pResultJson)
{
   // query rmarkdown for the output format
   json::Object& resultJson = *pResultJson;
   r::sexp::Protect protect;
   SEXP sexpOutputFormat;
   Error error = r::exec::RFunction("rmarkdown:::default_output_format",
                                    string_utils::utf8ToSystem(path), encoding)
                                   .call(&sexpOutputFormat, &protect);
   if (error)
   {
      resultJson["format_name"] = "";
      resultJson["self_contained"] = false;
   }
   else
   {
      std::string formatName;
      error = r::sexp::getNamedListElement(sexpOutputFormat, "name",
                                           &formatName);
      if (error)
         LOG_ERROR(error);
      resultJson["format_name"] = formatName;

      SEXP sexpOptions;
      bool selfContained = false;
      error = r::sexp::getNamedListSEXP(sexpOutputFormat, "options",
                                        &sexpOptions);
      if (error)
         LOG_ERROR(error);
      else
      {
         error = r::sexp::getNamedListElement(sexpOptions, "self_contained",
                                              &selfContained, false);
         if (error)
            LOG_ERROR(error);
      }

      resultJson["self_contained"] = selfContained;
   }
}


class RenderRmd : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<RenderRmd> create(const FilePath& targetFile,
                                              int sourceLine,
                                              const std::string& format,
                                              const std::string& encoding,
                                              const std::string& paramsFile,
                                              bool sourceNavigation,
                                              bool asTempfile,
                                              bool asShiny,
                                              const std::string& existingOutputFile,
                                              const std::string& workingDir,
                                              const std::string& viewerType)
   {
      boost::shared_ptr<RenderRmd> pRender(new RenderRmd(targetFile,
                                                         sourceLine,
                                                         sourceNavigation,
                                                         asShiny));
      pRender->start(format, encoding, paramsFile, asTempfile, 
                     existingOutputFile, workingDir, viewerType);
      return pRender;
   }

   void terminateProcess(RenderTerminateType terminateType)
   {
      terminateType_ = terminateType;
      async_r::AsyncRProcess::terminate();
   }

   FilePath outputFile()
   {
      return outputFile_;
   }

   void getPresentationDetails(int sourceLine, json::Object* jsonObject)
   {
      // default to no slide info
      (*jsonObject)["preview_slide"] = -1;
      (*jsonObject)["slide_navigation"] = json::Value();

      // only allow extended results if we have a source file to
      // navigate back to (otherwise you could navigate to the temp
      // file used for preview)
      if (sourceNavigation_)
      {
         rmarkdown::presentation::ammendResults(
                  outputFormat_["format_name"].get_str(),
                  targetFile_,
                  sourceLine,
                  jsonObject);
      }
   }

   std::string getRuntime(const FilePath& targetFile)
   {
      std::string runtime;
      Error error = r::exec::RFunction(
         ".rs.getRmdRuntime",
         string_utils::utf8ToSystem(targetFile.absolutePath())).call(
                                                               &runtime);
      if (error)
         LOG_ERROR(error);
      return runtime;
   }

private:
   RenderRmd(const FilePath& targetFile, int sourceLine, bool sourceNavigation,
             bool asShiny) :
      terminateType_(renderTerminateAbnormal),
      isShiny_(asShiny),
      hasShinyContent_(false),
      targetFile_(targetFile),
      sourceLine_(sourceLine),
      sourceNavigation_(sourceNavigation)
   {}

   void start(const std::string& format,
              const std::string& encoding,
              const std::string& paramsFile,
              bool asTempfile,
              const std::string& existingOutputFile,
              const std::string& workingDir,
              const std::string& viewerType)
   {
      Error error;
      json::Object dataJson;
      getOutputFormat(targetFile_.absolutePath(), encoding, &outputFormat_);
      dataJson["output_format"] = outputFormat_;
      dataJson["target_file"] = module_context::createAliasedPath(targetFile_);
      ClientEvent event(client_events::kRmdRenderStarted, dataJson);
      module_context::enqueClientEvent(event);

      // save encoding and viewer type
      encoding_ = encoding;
      viewerType_ = viewerType;

      std::string renderFunc;
      if (isShiny_)
      {
         // if a Shiny render was requested, use the Shiny render function
         // regardless of what was specified in the doc
         renderFunc = kShinyRenderFunc;
      }
      else
      {
         // see if the input file has a custom render function
         error = r::exec::RFunction(
            ".rs.getCustomRenderFunction",
            string_utils::utf8ToSystem(targetFile_.absolutePath())).call(
                                                                  &renderFunc);
         if (error)
            LOG_ERROR(error);

         if (renderFunc.empty())
            renderFunc = kStandardRenderFunc;
         else if (renderFunc == kShinyRenderFunc)
            isShiny_ = true;
      }

      std::string extraParams;
      std::string targetFile =
              string_utils::utf8ToSystem(targetFile_.absolutePath());

      std::string renderOptions("encoding = '" + encoding + "'");

      // output to a specific format if specified
      if (!format.empty())
      {
         renderOptions += ", output_format = '" + format + "'";
      }

      // include params if specified
      if (!paramsFile.empty())
      {
         renderOptions += ", params = readRDS('" + paramsFile + "')";
      }

      // use the stated working directory if specified
      if (!workingDir.empty())
      {
         renderOptions += ", knit_root_dir = '" + 
                          string_utils::utf8ToSystem(workingDir) + "'";
      }

      // output to a temporary directory if specified (no need to do this
      // for Shiny since it already renders to a temporary dir)
      if (asTempfile && !isShiny_)
      {
         FilePath tmpDir = module_context::tempFile("preview-", "dir");
         Error error = tmpDir.ensureDirectory();
         if (!error)
         {
            std::string dir = string_utils::utf8ToSystem(tmpDir.absolutePath());
            renderOptions += ", output_dir = '" + dir + "'";
         }
         else
         {
            LOG_ERROR(error);
         }
      }

      if (isShiny_)
      {
         extraParams += "shiny_args = list(launch.browser = FALSE), "
                        "auto_reload = FALSE, ";
         extraParams += "dir = '" + string_utils::utf8ToSystem(
                     targetFile_.parent().absolutePath()) + "', ";

         // provide render_args in render_args parameter
         renderOptions = "render_args = list(" + renderOptions + ")";
      }

      // render command
      boost::format fmt("%1%('%2%', %3% %4%);");
      std::string cmd = boost::str(fmt %
                             renderFunc %
                             string_utils::singleQuotedStrEscape(targetFile) %
                             extraParams %
                             renderOptions);

      // environment
      core::system::Options environment;
      std::string tempDir;
      error = r::exec::RFunction("tempdir").call(&tempDir);
      if (!error)
         environment.push_back(std::make_pair("RMARKDOWN_PREVIEW_DIR", tempDir));
      else
         LOG_ERROR(error);

      // set the not cran env var
      environment.push_back(std::make_pair("NOT_CRAN", "true"));

      // render unless we were handed an existing output file
      allOutput_.clear();
      if (existingOutputFile.empty())
      {
         // launch the R session in the document's directory by default, unless
         // a working directory was supplied
         FilePath working = targetFile_.parent();
         if (!workingDir.empty())
            working = module_context::resolveAliasedPath(workingDir);

         async_r::AsyncRProcess::start(cmd.c_str(), environment, working,
                                       async_r::R_PROCESS_NO_RDATA);
      }
      else
      {
         // if we are handed an existing output file then this is the build
         // tab previewing a website. in this case the build tab opened
         // for the build and forced the viewer pane to a smaller height. as
         // a result we want to do a forceMaximize to restore the Viewer
         // pane. Note that these two concerns happen to conflate here but
         // it's conceivable that there would be other forceMaximize
         // scenarios or that other types of previews where an output file
         // was already in hand would NOT want to do a forceMaximize. We're
         // leaving this coupling for now to minimze the scope of the change
         // required to allow website previews to restore the viewer pane, we
         // may want a more intrusive change if/when we discover other
         // scenarios.
         outputFile_ = module_context::resolveAliasedPath(existingOutputFile);
         terminate(true, true);
      }
   }

   void onStdout(const std::string& output)
   {
      onRenderOutput(module_context::kCompileOutputNormal,
                     string_utils::systemToUtf8(output));
   }

   void onStderr(const std::string& output)
   {
      onRenderOutput(module_context::kCompileOutputError,
                     string_utils::systemToUtf8(output));
   }

   void onRenderOutput(int type, const std::string& output)
   {
      // buffer output
      allOutput_.append(output);

      enqueRenderOutput(type, output);

      std::vector<std::string> outputLines;
      boost::algorithm::split(outputLines, output,
                              boost::algorithm::is_any_of("\n\r"));
      BOOST_FOREACH(std::string& outputLine, outputLines)
      {
         // if this is a Shiny render, check to see if Shiny started listening
         if (isShiny_)
         {
            const boost::regex shinyListening("^Listening on (http.*)$");
            boost::smatch matches;
            if (regex_utils::match(outputLine, matches, shinyListening))
            {
               json::Object startedJson;
               startedJson["target_file"] =
                     module_context::createAliasedPath(targetFile_);
               startedJson["output_format"] = outputFormat_;
               std::string url(module_context::mapUrlPorts(matches[1].str()));

               // add a / to the URL if it doesn't have one already
               // (typically portmapped URLs do, but the raw URL returned by
               // Shiny doesn't)
               if (url[url.length() - 1] != '/')
                  url += "/";

               getPresentationDetails(sourceLine_, &startedJson);

               startedJson["url"] = url + targetFile_.filename();

               startedJson["runtime"] = getRuntime(targetFile_);

               module_context::enqueClientEvent(ClientEvent(
                           client_events::kRmdShinyDocStarted,
                           startedJson));
               break;
            }
         }

         // check to see if a warning was emitted indicating that this document
         // contains Shiny content
         if (outputLine.substr(0, sizeof(kShinyContentWarning)) ==
             kShinyContentWarning)
         {
            hasShinyContent_ = true;
         }
      }
   }

   void onCompleted(int exitStatus)
   {
      // see if we can determine the output file
      FilePath outputFile = module_context::extractOutputFileCreated
                                                   (targetFile_, allOutput_);
      if (!outputFile.empty())
         outputFile_ = outputFile;

      // the process may be terminated normally by the IDE (e.g. to stop the
      // Shiny server); alternately, a termination is considered normal if
      // the process succeeded and produced output.
      terminate(terminateType_ == renderTerminateNormal ||
                (exitStatus == 0 && outputFile_.exists()));
   }

   void terminateWithError(const Error& error)
   {
      std::string message =
            "Error rendering R Markdown for " +
            module_context::createAliasedPath(targetFile_) + " " +
            error.summary();
      terminateWithError(message);
   }

   void terminateWithError(const std::string& message)
   {
      enqueRenderOutput(module_context::kCompileOutputError, message);
      terminate(false);
   }

   void terminate(bool succeeded)
   {
      terminate(succeeded, false);
   }

   void terminate(bool succeeded, bool forceMaximize)
   {
      using namespace module_context;

      markCompleted();

      // if a quiet terminate was requested, don't queue any client events
      if (terminateType_ == renderTerminateQuiet)
         return;

      json::Object resultJson;
      resultJson["succeeded"] = succeeded;
      resultJson["target_file"] = createAliasedPath(targetFile_);
      resultJson["target_encoding"] = encoding_;
      resultJson["target_line"] = sourceLine_;

      std::string outputFile = createAliasedPath(outputFile_);
      resultJson["output_file"] = outputFile;
      resultJson["knitr_errors"] = sourceMarkersAsJson(knitrErrors_);

      resultJson["output_url"] = assignOutputUrl(outputFile);
      resultJson["output_format"] = outputFormat_;

      resultJson["is_shiny_document"] = isShiny_;
      resultJson["has_shiny_content"] = hasShinyContent_;

      resultJson["runtime"] = getRuntime(targetFile_);

      json::Value websiteDir;
      if (outputFile_.extensionLowerCase() == ".html")
      {
         // check for previous publishing
         resultJson["rpubs_published"] =
               !module_context::previousRpubsUploadId(outputFile_).empty();

         // check to see if this is a website directory
         if (r_util::isWebsiteDirectory(targetFile_.parent()))
            websiteDir = createAliasedPath(targetFile_.parent());
      }
      else
      {
         resultJson["rpubs_published"] = false;
      }

      resultJson["website_dir"] = websiteDir;

      // view options
      resultJson["force_maximize"] = forceMaximize;
      resultJson["viewer_type"] = viewerType_;

      // allow for format specific additions to the result json
      std::string formatName =  outputFormat_["format_name"].get_str();

      // populate slide information if available
      getPresentationDetails(sourceLine_, &resultJson);

      // if we failed then we may want to enque additional diagnostics
      if (!succeeded)
         enqueFailureDiagnostics(formatName);

      ClientEvent event(client_events::kRmdRenderCompleted, resultJson);
      module_context::enqueClientEvent(event);
   }

   void enqueFailureDiagnostics(const std::string& formatName)
   {
      if ((formatName == "pdf_document" ||
           formatName == "beamer_presentation")
          && !module_context::isPdfLatexInstalled())
      {
         enqueRenderOutput(module_context::kCompileOutputError,
            "\nNo TeX installation detected (TeX is required "
            "to create PDF output). You should install "
            "a recommended TeX distribution for your platform:\n\n"
            "  Windows: MiKTeX (Complete) - http://miktex.org/2.9/setup\n"
            "  (NOTE: Be sure to download the Complete rather than Basic installation)\n\n"
            "  Mac OS X: TexLive 2013 (Full) - http://tug.org/mactex/\n"
            "  (NOTE: Download with Safari rather than Chrome _strongly_ recommended)\n\n"
            "  Linux: Use system package manager\n\n");
      }
   }

   void enqueRenderOutput(int type,
                          const std::string& output)
   {
      using namespace module_context;
      if (type == module_context::kCompileOutputError &&  sourceNavigation_)
      {
         // this is an error, parse it to see if it looks like a knitr error
         const boost::regex knitrErr(
                  "^Quitting from lines (\\d+)-(\\d+) \\(([^)]+)\\)(.*)");
         boost::smatch matches;
         if (regex_utils::match(output, matches, knitrErr))
         {
            // looks like a knitr error; compose a compile error object and
            // emit it to the client when the render is complete
            SourceMarker err(
                     SourceMarker::Error,
                     targetFile_.parent().complete(matches[3].str()),
                     boost::lexical_cast<int>(matches[1].str()),
                     1,
                     core::html_utils::HTML(matches[4].str()),
                     true);
            knitrErrors_.push_back(err);
         }
      }
      CompileOutput compileOutput(type, output);
      ClientEvent event(client_events::kRmdRenderOutput,
                        compileOutputAsJson(compileOutput));
      module_context::enqueClientEvent(event);
   }

   RenderTerminateType terminateType_;
   bool isShiny_;
   bool hasShinyContent_;
   FilePath targetFile_;
   int sourceLine_;
   FilePath outputFile_;
   std::string encoding_;
   std::string viewerType_;
   bool sourceNavigation_;
   json::Object outputFormat_;
   std::vector<module_context::SourceMarker> knitrErrors_;
   std::string allOutput_;
};

boost::shared_ptr<RenderRmd> s_pCurrentRender_;

// replaces references to MathJax with references to our built-in resource
// handler.
// in:  script src = "http://foo/bar/Mathjax.js?abc=123"
// out: script src = "mathjax/MathJax.js?abc=123"
//
// if no MathJax use is found in the document, removes the script src statement
// entirely, so we don't incur the cost of loading MathJax in preview mode
// unless the document actually has markup.
class MathjaxFilter : public boost::iostreams::regex_filter
{
public:
   MathjaxFilter()
      // the regular expression matches any of the three tokens that look
      // like the beginning of math, and the "script src" line itself
      : boost::iostreams::regex_filter(
            boost::regex(kMathjaxBeginComment "|"
                         "\\\\\\[|\\\\\\(|<math|"
                         "^(\\s*script.src\\s*=\\s*)\"http.*?(MathJax.js[^\"]*)\""),
            boost::bind(&MathjaxFilter::substitute, this, _1)),
        hasMathjax_(false)
   {
   }

private:
   std::string substitute(const boost::cmatch& match)
   {
      std::string result;

      if (match[0] == "\\[" ||
          match[0] == "\\(" ||
          match[0] == "<math")
      {
         // if we found one of the MathJax markup start tokens, we need to emit
         // MathJax scripts
         hasMathjax_ = true;
         return match[0];
      }
      else if (match[0] == kMathjaxBeginComment)
      {
         // we found the start of the MathJax section; add the MathJax config
         // block if we're in a configuration that requires it
#if defined(_WIN32)
         if (session::options().programMode() != kSessionProgramModeDesktop)
            return match[0];

         result.append(kQtMathJaxConfigScript "\n");
         result.append(match[0]);
#else
         return match[0];
#endif
      }
      else if (hasMathjax_)
      {
         // this is the MathJax script itself; emit it if we found a start token
         result.append(match[1]);
         result.append("\"" kMathjaxSegment "/");
         result.append(match[2]);
         result.append("\"");
      }

      return result;
   }

   bool hasMathjax_;
};

bool isRenderRunning()
{
   return s_pCurrentRender_ && s_pCurrentRender_->isRunning();
}


// environment variables to initialize
const char * const kRStudioPandoc = "RSTUDIO_PANDOC";
const char * const kRmarkdownMathjaxPath = "RMARKDOWN_MATHJAX_PATH";

void initEnvironment()
{
   // set RSTUDIO_PANDOC (leave existing value alone)
   std::string rstudioPandoc = core::system::getenv(kRStudioPandoc);
   if (rstudioPandoc.empty())
      rstudioPandoc = session::options().pandocPath().absolutePath();
   r::exec::RFunction sysSetenv("Sys.setenv");
   sysSetenv.addParam(kRStudioPandoc, rstudioPandoc);

   // set RMARKDOWN_MATHJAX_PATH (leave existing value alone)
   std::string rmarkdownMathjaxPath = core::system::getenv(kRmarkdownMathjaxPath);
   if (rmarkdownMathjaxPath.empty())
     rmarkdownMathjaxPath = session::options().mathjaxPath().absolutePath();
   sysSetenv.addParam(kRmarkdownMathjaxPath, rmarkdownMathjaxPath);

   // call Sys.setenv
   Error error = sysSetenv.call();
   if (error)
      LOG_ERROR(error);
}

bool haveMarkdownToHTMLOption()
{
   SEXP markdownToHTMLOption = r::options::getOption("rstudio.markdownToHTML");
   return !r::sexp::isNull(markdownToHTMLOption);
}

// when the RMarkdown package is installed, give .Rmd files the extended type
// "rmarkdown", unless there is a marker that indicates we should
// use the previous rendering strategy
std::string onDetectRmdSourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!pDoc->path().empty())
   {
      FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
      if ((filePath.extensionLowerCase() == ".rmd" ||
           filePath.extensionLowerCase() == ".md") &&
          !boost::algorithm::icontains(pDoc->contents(),
                                       "<!-- rmarkdown v1 -->") &&
          rmarkdownPackageAvailable())
      {
         return "rmarkdown";
      }
   }
   return std::string();
}

void onClientInit()
{
   // if a new client is connecting, shut any running render process
   // (these processes can have virtually unbounded lifetime because they
   // leave a server running in the Shiny document case)
   if (s_pCurrentRender_ && s_pCurrentRender_->isRunning())
      s_pCurrentRender_->terminateProcess(renderTerminateQuiet);
}

Error getRMarkdownContext(const json::JsonRpcRequest&,
                          json::JsonRpcResponse* pResponse)
{
   json::Object contextJson;
   pResponse->setResult(contextJson);
   return Success();
}

void doRenderRmd(const std::string& file,
                 int line,
                 const std::string& format,
                 const std::string& encoding,
                 const std::string& paramsFile,
                 bool sourceNavigation,
                 bool asTempfile,
                 bool asShiny,
                 const std::string& existingOutputFile,
                 const std::string& workingDir,
                 const std::string& viewerType,
                 json::JsonRpcResponse* pResponse)
{
   if (s_pCurrentRender_ &&
       s_pCurrentRender_->isRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      s_pCurrentRender_ = RenderRmd::create(
               module_context::resolveAliasedPath(file),
               line,
               format,
               encoding,
               paramsFile,
               sourceNavigation,
               asTempfile,
               asShiny,
               existingOutputFile,
               workingDir,
               viewerType);
      pResponse->setResult(true);
   }
}

Error renderRmd(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   int line = -1, type = kRenderTypeStatic;
   std::string file, format, encoding, paramsFile, existingOutputFile,
               workingDir, viewerType;
   bool asTempfile = false;
   Error error = json::readParams(request.params,
                                  &file,
                                  &line,
                                  &format,
                                  &encoding,
                                  &paramsFile,
                                  &asTempfile,
                                  &type,
                                  &existingOutputFile,
                                  &workingDir,
                                  &viewerType);
   if (error)
      return error;

   if (type == kRenderTypeNotebook)
   {
      // if this is a notebook, it's pre-rendered
      FilePath inputFile = module_context::resolveAliasedPath(file); 
      FilePath outputFile = inputFile.parent().complete(inputFile.stem() + 
                                                        kNotebookExt);

      // extract the output format
      json::Object outputFormat;

      // TODO: this should use getOutputFormat(), but we can't read format
      // defaults yet since the html_notebook type doesn't exist in the
      // rmarkdown package yet
      outputFormat["format_name"] = "html_notebook";
      outputFormat["self_contained"] = true;

      json::Object resultJson;
      resultJson["succeeded"] = outputFile.exists();
      resultJson["target_file"] = file;
      resultJson["target_encoding"] = encoding;
      resultJson["target_line"] = line;
      resultJson["output_file"] = module_context::createAliasedPath(outputFile);
      resultJson["knitr_errors"] = json::Array();
      resultJson["output_url"] = assignOutputUrl(outputFile.absolutePath());
      resultJson["output_format"] = outputFormat;
      resultJson["is_shiny_document"] = false;
      resultJson["website_dir"] = json::Value();
      resultJson["has_shiny_content"] = false;
      resultJson["rpubs_published"] =
            !module_context::previousRpubsUploadId(outputFile).empty();
      resultJson["force_maximize"] = false;
      resultJson["viewer_type"] = viewerType;
      ClientEvent event(client_events::kRmdRenderCompleted, resultJson);
      module_context::enqueClientEvent(event);
   }
   else
   {
      // not a notebook, do render work
      doRenderRmd(file, line, format, encoding, paramsFile,
                  true, asTempfile, type == kRenderTypeShiny, existingOutputFile, 
                  workingDir, viewerType, pResponse);
   }

   return Success();
}

Error renderRmdSource(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string source;
   Error error = json::readParams(request.params, &source);
   if (error)
      return error;

   // create temp file
   FilePath rmdTempFile = module_context::tempFile("Preview-", "Rmd");
   error = core::writeStringToFile(rmdTempFile, source);
   if (error)
      return error;

   doRenderRmd(rmdTempFile.absolutePath(), -1, "", "UTF-8", "",
               false, false, false, "", "", "", pResponse);

   return Success();
}


Error terminateRenderRmd(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse*)
{
   bool normal;
   Error error = json::readParams(request.params, &normal);
   if (error)
      return error;

   if (isRenderRunning())
      s_pCurrentRender_->terminateProcess(
               normal ? renderTerminateNormal :
                        renderTerminateAbnormal);

   return Success();
}

// return the path to the local copy of MathJax
FilePath mathJaxDirectory()
{
   // prefer the environment variable if it exists
   std::string mathjaxPath = core::system::getenv(kRmarkdownMathjaxPath);
   if (!mathjaxPath.empty() && FilePath::exists(mathjaxPath))
      return FilePath(mathjaxPath);
   else
      return session::options().mathjaxPath();
}

// Handles a request for RMarkdown output. This request embeds the name of
// the file to be viewed as an encoded part of the URL. For instance, requests
// to show render output for ~/abc.html and its resources look like:
//
// http://<server>/rmd_output/~%252Fabc.html/...
//
// Note that this requires two URL encoding passes at the origin, since a
// a URL decoding pass is made on the whole URL before this handler is invoked.
void handleRmdOutputRequest(const http::Request& request,
                            http::Response* pResponse)
{
   std::string path = http::util::pathAfterPrefix(request,
                                                  kRmdOutputLocation);

   // find the portion of the URL containing the output identifier
   size_t pos = path.find('/', 1);
   if (pos == std::string::npos)
   {
      pResponse->setNotFoundError(request.uri());
      return;
   }

   // extract the output identifier
   int outputId = 0;
   try
   {
      outputId = boost::lexical_cast<int>(path.substr(0, pos));
   }
   catch (boost::bad_lexical_cast const&)
   {
      pResponse->setNotFoundError(request.uri());
      return ;
   }

   // make sure the output identifier refers to a valid file
   std::string outputFile = s_renderOutputs[outputId];
   FilePath outputFilePath(module_context::resolveAliasedPath(outputFile));
   if (!outputFilePath.exists())
   {
      pResponse->setNotFoundError(outputFile);
      return;
   }

   // strip the output identifier from the URL
   path = path.substr(pos + 1, path.length());

   if (path.empty())
   {
      // disable caching; the request path looks identical to the browser for
      // each main request for content
      pResponse->setNoCacheHeaders();

      // serve the contents of the file with MathJax URLs mapped to our
      // own resource handler
      MathjaxFilter mathjaxFilter;
      pResponse->setFile(outputFilePath, request, mathjaxFilter);

      // set the content-type to ensure UTF-8 (all pandoc output
      // is UTF-8 encoded)
      pResponse->setContentType("text/html; charset=utf-8");
   }
   else if (boost::algorithm::starts_with(path, kMathjaxSegment))
   {
      // serve the MathJax resource: find the requested path in the MathJax
      // directory
      pResponse->setCacheableFile(mathJaxDirectory().complete(
                                    path.substr(sizeof(kMathjaxSegment))),
                                  request);
   }
   else
   {
      // serve a file resource from the output folder
      FilePath filePath = outputFilePath.parent().childPath(path);
      html_preview::addFileSpecificHeaders(filePath, pResponse);
      pResponse->setNoCacheHeaders();
      pResponse->setFile(filePath, request);
   }
}


Error createRmdFromTemplate(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   std::string filePath, templatePath, resultPath;
   bool createDir;
   Error error = json::readParams(request.params,
                                  &filePath,
                                  &templatePath,
                                  &createDir);
   if (error)
      return error;

   r::exec::RFunction draft("rmarkdown:::draft");
   draft.addParam("file", filePath);
   draft.addParam("template", templatePath);
   draft.addParam("create_dir", createDir);
   draft.addParam("edit", false);
   error = draft.call(&resultPath);

   if (error)
      return error;

   json::Object jsonResult;
   jsonResult["path"] = resultPath;
   pResponse->setResult(jsonResult);

   return Success();
}

Error getRmdTemplate(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParams(request.params, &path);
   if (error)
      return error;

   json::Object jsonResult;

   // locate the template skeleton on disk (if it doesn't exist we'll just
   // return an empty string)
   FilePath skeletonPath = FilePath(path).complete("skeleton/skeleton.Rmd");
   std::string templateContent;
   if (skeletonPath.exists())
   {
      error = readStringFromFile(skeletonPath, &templateContent, string_utils::LineEndingPosix);
      if (error)
         return error;
   }
   jsonResult["content"] = templateContent;
   pResponse->setResult(jsonResult);

   return Success();
}

Error prepareForRmdChunkExecution(const json::JsonRpcRequest& request,
                                  json::JsonRpcResponse* pResponse)
{
   // read id param
   std::string id;
   Error error = json::readParams(request.params, &id);
   if (error)
      return error;

   error = evaluateRmdParams(id);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // indicate to the client whether R currently has executing code on the
   // stack
   json::Object result;
   result["state"] = r::context::globalContext().nextcontext() ?
      RExecutionReady : RExecutionBusy;
   pResponse->setResult(result);

   return Success();
}


Error maybeCopyWebsiteAsset(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   std::string file;
   Error error = json::readParams(request.params, &file);
   if (error)
      return error;

   // don't copy if we build inline
   std::string websiteOutputDir = module_context::websiteOutputDir();
   if (websiteOutputDir.empty())
   {
      pResponse->setResult(true);
      return Success();
   }

   // get the path relative to the website dir
   FilePath websiteDir = projects::projectContext().buildTargetPath();
   FilePath filePath = module_context::resolveAliasedPath(file);
   std::string relativePath = filePath.relativePath(websiteDir);

   // get the list of copyable site resources
   std::vector<std::string> copyableResources;
   r::exec::RFunction func("rmarkdown:::copyable_site_resources");
   func.addParam("input", string_utils::utf8ToSystem(websiteDir.absolutePath()));
   func.addParam("encoding", projects::projectContext().config().encoding);
   error = func.call(&copyableResources);
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setResult(false);
      return Success();
   }

   // get the name to target -- if it's in the root dir it's the filename
   // otherwise it's the directory name
   std::string search;
   if (filePath.parent() == websiteDir)
      search = filePath.filename();
   else
      search = relativePath.substr(0, relativePath.find_first_of('/'));

   // if it's not in the list we don't copy it
   if (!algorithm::contains(copyableResources, search))
   {
       pResponse->setResult(false);
       return Success();
   }

   // copy the file (removing it first)
   FilePath outputDir = FilePath(websiteOutputDir);
   FilePath outputFile = outputDir.childPath(relativePath);
   if (outputFile.exists())
   {
      error = outputFile.remove();
      if (error)
      {
         LOG_ERROR(error);
         pResponse->setResult(false);
         return Success();
      }
   }

   error = outputFile.parent().ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setResult(false);
      return Success();
   }

   error = filePath.copy(outputFile);
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setResult(false);
   }
   else
   {
      pResponse->setResult(true);
   }

   return Success();
}

SEXP rs_paramsFileForRmd(SEXP fileSEXP)
{
   static std::map<std::string,std::string> s_paramsFiles;

   std::string file = r::sexp::safeAsString(fileSEXP);

   using namespace module_context;
   if (s_paramsFiles.find(file) == s_paramsFiles.end())
      s_paramsFiles[file] = createAliasedPath(tempFile("rmdparams", "rds"));

   r::sexp::Protect rProtect;
   return r::sexp::create(s_paramsFiles[file], &rProtect);
}


void onShutdown(bool terminatedNormally)
{
   Error error = core::writeStringVectorToFile(outputCachePath(), 
                                               s_renderOutputs);
   if (error)
      LOG_ERROR(error);
}
 
void onSuspend(const r::session::RSuspendOptions&, core::Settings*)
{
   onShutdown(true);
}

void onResume(const Settings&)
{
}

} // anonymous namespace

Error evaluateRmdParams(const std::string& docId)
{
   // get document contents
   using namespace source_database;
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   Error error = source_database::get(docId, pDoc);
   if (error)
      return error;

   // evaluate params if we can
   if (module_context::isPackageVersionInstalled("knitr", "1.10"))
   {
      error = r::exec::RFunction(".rs.evaluateRmdParams", pDoc->contents())
                                .call();
      if (error)
         return error;
   }
   return Success();
}

bool knitParamsAvailable()
{
   return module_context::isPackageVersionInstalled("rmarkdown", "0.7.3") &&
          module_context::isPackageVersionInstalled("knitr", "1.10.18");
}

bool knitWorkingDirAvailable()
{
   return module_context::isPackageVersionInstalled("rmarkdown", "1.1.9017");
}

bool rmarkdownPackageAvailable()
{
   if (!haveMarkdownToHTMLOption())
   {
      return r::util::hasRequiredVersion("3.0");
   }
   else
   {
      return false;
   }
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   R_CallMethodDef methodDef ;
   methodDef.name = "rs_paramsFileForRmd" ;
   methodDef.fun = (DL_FUNC)rs_paramsFileForRmd ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   initEnvironment();

   module_context::events().onDeferredInit.connect(
                                 boost::bind(initWebsiteOutputDir));
   module_context::events().onDetectSourceExtendedType
                                        .connect(onDetectRmdSourceType);
   module_context::events().onClientInit.connect(onClientInit);
   module_context::events().onShutdown.connect(onShutdown);
   module_context::addSuspendHandler(SuspendHandler(onSuspend, onResume));

   // load output paths if saved
   FilePath cachePath = outputCachePath();
   if (cachePath.exists())
   {
      Error error = core::readStringVectorFromFile(cachePath, &s_renderOutputs);
      if (error)
         LOG_ERROR(error);
      else
      {
         s_currentRenderOutput = s_renderOutputs.size();
         s_renderOutputs.reserve(kMaxRenderOutputs);
      }
   }

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_rmarkdown_context", getRMarkdownContext))
      (bind(registerRpcMethod, "render_rmd", renderRmd))
      (bind(registerRpcMethod, "render_rmd_source", renderRmdSource))
      (bind(registerRpcMethod, "terminate_render_rmd", terminateRenderRmd))
      (bind(registerRpcMethod, "create_rmd_from_template", createRmdFromTemplate))
      (bind(registerRpcMethod, "get_rmd_template", getRmdTemplate))
      (bind(registerRpcMethod, "prepare_for_rmd_chunk_execution", prepareForRmdChunkExecution))
      (bind(registerRpcMethod, "maybe_copy_website_asset", maybeCopyWebsiteAsset))
      (bind(registerUriHandler, kRmdOutputLocation, handleRmdOutputRequest))
      (bind(module_context::sourceModuleRFile, "SessionRMarkdown.R"));

   return initBlock.execute();
}

} // namespace rmarkdown
} // namespace modules

namespace module_context {

bool isWebsiteProject()
{
   if (!modules::rmarkdown::rmarkdownPackageAvailable())
      return false;

   return (projects::projectContext().config().buildType ==
           r_util::kBuildTypeWebsite);
}

bool isBookdownWebsite()
{
   if (!isWebsiteProject())
      return false;

   bool isBookdown = false;
   std::string encoding = projects::projectContext().defaultEncoding();
   Error error = r::exec::RFunction(".rs.isBookdownWebsite",
                              projectBuildDir(), encoding).call(&isBookdown);
   if (error)
      LOG_ERROR(error);
   return isBookdown;
}

std::string websiteOutputDir()
{
   return s_websiteOutputDir;
}

} // namespace module_context

} // namespace session
} // namespace rstudio

