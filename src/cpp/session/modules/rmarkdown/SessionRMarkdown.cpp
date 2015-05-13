/*
 * SessionRMarkdown.cpp
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

#include "SessionRMarkdown.hpp"
#include "../SessionHTMLPreview.hpp"
#include "../build/SessionBuildErrors.hpp"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/iostreams/filter/regex.hpp>
#include <boost/format.hpp>
#include <boost/foreach.hpp>

#include <core/FileSerializer.hpp>
#include <core/Exec.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/StringUtils.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/ROptions.hpp>
#include <r/RUtil.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/SessionAsyncRProcess.hpp>

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
// that occurred in this session. it's unlikely that the client will ever 
// request a render output other than the one that was just completed, so 
// keeping > 2 render paths is conservative.
#define kMaxRenderOutputs 5
std::string s_renderOutputs[kMaxRenderOutputs];
int s_currentRenderOutput = 0;

class RenderRmd : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<RenderRmd> create(const FilePath& targetFile,
                                              int sourceLine,
                                              const std::string& format,
                                              const std::string& encoding,
                                              bool sourceNavigation,
                                              bool asTempfile,
                                              bool asShiny)
   {
      boost::shared_ptr<RenderRmd> pRender(new RenderRmd(targetFile,
                                                         sourceLine,
                                                         sourceNavigation,
                                                         asShiny));
      pRender->start(format, encoding, asTempfile);
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
              bool asTempfile)
   {
      Error error;
      json::Object dataJson;
      getOutputFormat(targetFile_.absolutePath(), encoding, &outputFormat_);
      dataJson["output_format"] = outputFormat_;
      dataJson["target_file"] = module_context::createAliasedPath(targetFile_);
      ClientEvent event(client_events::kRmdRenderStarted, dataJson);
      module_context::enqueClientEvent(event);

      // save encoding
      encoding_ = encoding;

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

         std::string rsIFramePath("rsiframe.js");

#ifndef __APPLE__
         // on Qt platforms, rsiframe.js needs to have its origin specified
         // explicitly; Qt 5.4 disables document.referrer
         if (session::options().programMode() == kSessionProgramModeDesktop)
         {
             rsIFramePath += "?origin=" +
                     session::options().wwwAddress() + ":" +
                     session::options().wwwPort();
         }
#endif

         std::string extraDependencies("htmltools::htmlDependency("
                     "name = 'rstudio-iframe', "
                     "version = '0.1', "
                     "src = '" +
                         session::options().rResourcesPath().absolutePath() +
                     "', "
                     "script = '" + rsIFramePath + "')");

         std::string outputOptions("extra_dependencies = list(" + 
               extraDependencies + ")");

#ifndef __APPLE__
         // on Qt platforms, use local MathJax: it contains a patch that allows
         // math to render immediately (otherwise it fails to load due to 
         // timeouts waiting for font variants to load)
         if (session::options().programMode() == kSessionProgramModeDesktop) 
         {
            outputOptions += ", mathjax = 'local'";
         }
#endif

         // inject the RStudio IFrame helper script (for syncing scroll position
         // and anchor information cross-domain), and wrap the other render
         // options discovered so far in the render_args parameter
         renderOptions = "render_args = list(" + renderOptions + ", "
               "output_options = list(" + outputOptions + "))";
      }

      // render command
      boost::format fmt("%1%('%2%', %3% %4%);");
      std::string cmd = boost::str(fmt %
                                   renderFunc %
                                   targetFile %
                                   extraParams %
                                   renderOptions);

      // start the async R process with the render command
      allOutput_.clear();
      async_r::AsyncRProcess::start(cmd.c_str(), targetFile_.parent(),
                                    async_r::R_PROCESS_NO_RDATA);
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
            if (boost::regex_match(outputLine, matches, shinyListening))
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
      // check each line of the emitted output; if it starts with a token
      // indicating rendering is complete, store the remainder of the emitted
      // line as the file we rendered
      std::string completeMarker("Output created: ");
      std::string renderLine;
      std::stringstream outputStream(allOutput_);
      while (std::getline(outputStream, renderLine))
      {
         if (boost::algorithm::starts_with(renderLine, completeMarker))
         {
            std::string fileName = renderLine.substr(completeMarker.length());

            // trim any whitespace from the end of the filename (on Windows this
            // includes part of CR-LF)
            boost::algorithm::trim(fileName);

            // if the path looks absolute, use it as-is; otherwise, presume
            // it to be in the same directory as the input file
            outputFile_ = targetFile_.parent().complete(fileName);
            break;
         }
      }

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

      std::string outputUrl(kRmdOutput "/");

      // assign the result to one of our output slots
      s_currentRenderOutput = (s_currentRenderOutput + 1) % kMaxRenderOutputs;
      s_renderOutputs[s_currentRenderOutput] = outputFile;
      outputUrl.append(boost::lexical_cast<std::string>(s_currentRenderOutput));
      outputUrl.append("/");

      resultJson["output_url"] = outputUrl;

      resultJson["output_format"] = outputFormat_;

      resultJson["is_shiny_document"] = isShiny_;
      resultJson["has_shiny_content"] = hasShinyContent_;

      // for HTML documents, check to see whether they've been published
      if (outputFile_.extensionLowerCase() == ".html")
      {
         resultJson["rpubs_published"] =
               !module_context::previousRpubsUploadId(outputFile_).empty();
      }
      else
      {
         resultJson["rpubs_published"] = false;
      }

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
         if (boost::regex_match(output, matches, knitrErr))
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
   bool sourceNavigation_;
   json::Object outputFormat_;
   std::vector<module_context::SourceMarker> knitrErrors_;
   std::string allOutput_;
};

boost::shared_ptr<RenderRmd> s_pCurrentRender_;

// This class's job is to asynchronously read template locations from the R
// Markdown package, and emit each template as a client event. This should
// generally be fast (a few milliseconds); we use this asynchronous
// implementation in case the file system is slow (e.g. slow or remote disk)
// or there are many thousands of packages (e.g. all of CRAN).
class DiscoverTemplates : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<DiscoverTemplates> create()
   {
      boost::shared_ptr<DiscoverTemplates> pDiscover(new DiscoverTemplates());
      pDiscover->start("rmarkdown:::list_template_dirs()", FilePath(),
                       async_r::R_PROCESS_VANILLA);
      return pDiscover;
   }

private:
   void onStdout(const std::string& output)
   {
      r::sexp::Protect protect;
      Error error;

      // the output vector may contain more than one path if paths are returned
      // very quickly, so split it into lines and emit a client event for
      // each line
      std::vector<std::string> paths;
      boost::algorithm::split(paths, output,
                              boost::algorithm::is_any_of("\n\r"));
      BOOST_FOREACH(std::string& path, paths)
      {
         if (path.empty())
            continue;

         // record the template's path (absolute for the filesystem)
         json::Object dataJson;

         std::string name;
         std::string description;
         std::string createDir = "default";
         std::string package;

         // if the template's owning package is known, emit that
         size_t pipePos = path.find_first_of('|');
         if (pipePos != std::string::npos)
         {
            package = path.substr(0, pipePos);

            // remove package name from string, leaving just the path segment
            path = path.substr(pipePos + 1, path.length() - pipePos);
         }

         SEXP templateDetails;
         error = r::exec::RFunction(
            ".rs.getTemplateDetails",string_utils::utf8ToSystem(path))
            .call(&templateDetails, &protect);

         // .rs.getTemplateDetails may return null if the template is not
         // well-formed
         if (error || TYPEOF(templateDetails) == NILSXP)
            continue;

         r::sexp::getNamedListElement(templateDetails,
                                      "name", &name);
         r::sexp::getNamedListElement(templateDetails,
                                      "description", &description);

         bool createDirFlag = false;
         error = r::sexp::getNamedListElement(templateDetails,
                                              "create_dir",
                                              &createDirFlag);
         createDir = createDirFlag ? "true" : "false";

         dataJson["package_name"] = package;
         dataJson["path"] = path;
         dataJson["name"] = name;
         dataJson["description"] = description;
         dataJson["create_dir"] = createDir;

         // emit to the client
         ClientEvent event(client_events::kRmdTemplateDiscovered, dataJson);
         module_context::enqueClientEvent(event);
      }
   }

   void onCompleted(int exitStatus)
   {
      module_context::enqueClientEvent(
               ClientEvent(client_events::kRmdTemplateDiscoveryCompleted));
   }
};

boost::shared_ptr<DiscoverTemplates> s_pTemplateDiscovery_;

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


void initEnvironment()
{
   r::exec::RFunction sysSetenv("Sys.setenv");
   sysSetenv.addParam("RSTUDIO_PANDOC",
                      session::options().pandocPath().absolutePath());
   sysSetenv.addParam("RMARKDOWN_MATHJAX_PATH",
                      session::options().mathjaxPath().absolutePath());
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
                 bool sourceNavigation,
                 bool asTempfile,
                 bool asShiny,
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
               sourceNavigation,
               asTempfile,
               asShiny);
      pResponse->setResult(true);
   }
}

Error renderRmd(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   int line = -1;
   std::string file, format, encoding;
   bool asTempfile, asShiny = false;
   Error error = json::readParams(request.params,
                                  &file,
                                  &line,
                                  &format,
                                  &encoding,
                                  &asTempfile,
                                  &asShiny);
   if (error)
      return error;

   doRenderRmd(file, line, format, encoding, true, asTempfile, asShiny,
               pResponse);

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

   doRenderRmd(rmdTempFile.absolutePath(), -1, "", "UTF-8", false, false, false,
               pResponse);

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
      if (session::options().programMode() == kSessionProgramModeDesktop)
      {
         pResponse->setNoCacheHeaders();
         pResponse->setFile(filePath, request);
      }
      else
      {
         pResponse->setCacheableFile(filePath, request);
      }
   }
}


Error discoverRmdTemplates(const json::JsonRpcRequest&,
                           json::JsonRpcResponse* pResponse)
{
   if (s_pTemplateDiscovery_ &&
       s_pTemplateDiscovery_->isRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      s_pTemplateDiscovery_ = DiscoverTemplates::create();
      pResponse->setResult(true);
   }

   return Success();
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
      error = readStringFromFile(skeletonPath, &templateContent);
      if (error)
         return error;
   }
   jsonResult["content"] = templateContent;
   pResponse->setResult(jsonResult);

   return Success();
}



Error prepareForRmdChunkExecution(const json::JsonRpcRequest& request,
                                  json::JsonRpcResponse*)
{
   // read id param
   std::string id;
   Error error = json::readParams(request.params, &id);
   if (error)
      return error;

   // get document contents
   using namespace source_database;
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   error = source_database::get(id, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // evaluate params if we can
   if (module_context::isPackageVersionInstalled("knitr", "1.10"))
   {
      error = r::exec::RFunction(".rs.evaluateRmdParams", pDoc->contents())
                                                                      .call();
      if (error)
      {
         LOG_ERROR(error);
         return error;
      }
   }

   return Success();
}

} // anonymous namespace

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

   initEnvironment();

   module_context::events().onDetectSourceExtendedType
                                        .connect(onDetectRmdSourceType);
   module_context::events().onClientInit.connect(onClientInit);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_rmarkdown_context", getRMarkdownContext))
      (bind(registerRpcMethod, "render_rmd", renderRmd))
      (bind(registerRpcMethod, "render_rmd_source", renderRmdSource))
      (bind(registerRpcMethod, "terminate_render_rmd", terminateRenderRmd))
      (bind(registerRpcMethod, "discover_rmd_templates", discoverRmdTemplates))
      (bind(registerRpcMethod, "create_rmd_from_template", createRmdFromTemplate))
      (bind(registerRpcMethod, "get_rmd_template", getRmdTemplate))
      (bind(registerRpcMethod, "prepare_for_rmd_chunk_execution", prepareForRmdChunkExecution))
      (bind(registerUriHandler, kRmdOutputLocation, handleRmdOutputRequest))
      (bind(module_context::sourceModuleRFile, "SessionRMarkdown.R"));

   return initBlock.execute();
}

} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

