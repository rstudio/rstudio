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

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/ROptions.hpp>
#include <r/RUtil.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>

#include "RMarkdownInstall.hpp"
#include "RMarkdownPresentation.hpp"

#define kRmdOutput "rmd_output"
#define kRmdOutputLocation "/" kRmdOutput "/"

#define kMathjaxSegment "mathjax"
#define kMathjaxBeginComment "<!-- dynamically load mathjax"

#define kStandardRenderFunc "rmarkdown::render"
#define kShinyRenderFunc "rmarkdown::run"

#define kShinyContentWarning "Warning: Shiny application in a static R Markdown document"

using namespace core;

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

class RenderRmd : boost::noncopyable,
                  public boost::enable_shared_from_this<RenderRmd>
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
      terminationRequested_ = true;
      terminateType_ = terminateType;
   }

   bool isRunning()
   {
      return isRunning_;
   }

   FilePath outputFile()
   {
      return outputFile_;
   }

   bool hasOutput()
   {
      return !isRunning_ && outputFile_.exists();
   }

private:
   RenderRmd(const FilePath& targetFile, int sourceLine, bool sourceNavigation,
             bool asShiny) :
      isRunning_(false),
      terminationRequested_(false),
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
      json::Object dataJson;
      getOutputFormat(targetFile_.absolutePath(), encoding, &outputFormat_);
      dataJson["output_format"] = outputFormat_;
      dataJson["target_file"] = module_context::createAliasedPath(targetFile_);
      ClientEvent event(client_events::kRmdRenderStarted, dataJson);
      module_context::enqueClientEvent(event);
      isRunning_ = true;

      performRender(format, encoding, asTempfile);
   }

   void performRender(const std::string& format,
                      const std::string& encoding,
                      bool asTempfile)
   {
      // save encoding
      encoding_ = encoding;

      // R binary
      FilePath rProgramPath;
      Error error = module_context::rScriptPath(&rProgramPath);
      if (error)
      {
         LOG_ERROR(error);
         terminateWithError(error);
         return;
      }

      // args
      std::vector<std::string> args;
      args.push_back("--slave");
      args.push_back("--no-save");
      args.push_back("--no-restore");
      args.push_back("-e");

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
      std::string targetFile(targetFile_.filename());

      std::string renderOptions("encoding = '" + encoding + "'");

      if (isShiny_)
      {
         extraParams += "shiny_args = list(launch.browser = FALSE), "
                        "auto_reload = FALSE, ";
         extraParams += "dir = '" + targetFile_.parent().absolutePath() + "', ";

         // inject the RStudio IFrame helper script (for syncing scroll position
         // and anchor information cross-domain)
         renderOptions = "render_args = list(" + renderOptions + ", "
               "output_options = list(extra_dependencies = "
                  "list(structure(list("
                        "name = 'rstudio-iframe', "
                        "version = '0.1', "
                        "path = '" +
                            session::options().rResourcesPath().absolutePath() +
                        "', "
                        "script = 'rsiframe.js'), "
                     "class = 'html_dependency'))))";
      }
      if (!format.empty())
      {
         extraParams += "output_format = rmarkdown::" + format + "(), ";
      }

      if (asTempfile)
      {
         FilePath tmpDir = module_context::tempFile("preview-", "dir");
         Error error = tmpDir.ensureDirectory();
         if (!error)
         {
            std::string dir = string_utils::utf8ToSystem(tmpDir.absolutePath());
            extraParams += "output_dir = '" + dir + "', ";
         }
         else
         {
            LOG_ERROR(error);
         }
      }

      // render command
      boost::format fmt("%1%('%2%', %3% %4%);");
      std::string cmd = boost::str(fmt %
                                   renderFunc %
                                   targetFile %
                                   extraParams %
                                   renderOptions);


      args.push_back(cmd);

      // options
      core::system::ProcessOptions options;
      options.terminateChildren = true;
      options.workingDir = targetFile_.parent();

      // buffer the output so we can inspect it for the completed marker
      boost::shared_ptr<std::string> pAllOutput = boost::make_shared<std::string>();

      core::system::ProcessCallbacks cb;
      using namespace module_context;
      cb.onContinue = boost::bind(&RenderRmd::onRenderContinue,
                                  RenderRmd::shared_from_this());
      cb.onStdout = boost::bind(&RenderRmd::onRenderOutput,
                                RenderRmd::shared_from_this(),
                                kCompileOutputNormal,
                                _2,
                                pAllOutput);
      cb.onStderr = boost::bind(&RenderRmd::onRenderOutput,
                                RenderRmd::shared_from_this(),
                                kCompileOutputError,
                                _2,
                                pAllOutput);
      cb.onExit =  boost::bind(&RenderRmd::onRenderCompleted,
                                RenderRmd::shared_from_this(),
                               _1,
                               encoding,
                               pAllOutput);

      error = module_context::processSupervisor().runProgram(
               rProgramPath.absolutePath(),
               args,
               options,
               cb);
      if (error)
      {
         LOG_ERROR(error);
         terminateWithError(error);
      }
   }

   bool onRenderContinue()
   {
      return !terminationRequested_;
   }

   void onRenderOutput(int type, const std::string& output,
                       boost::shared_ptr<std::string> pAllOutput)
   {
      // buffer output
      pAllOutput->append(output);

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
               std::string url(module_context::mapUrlPorts(matches[1].str()));
               
               // add a / to the URL if it doesn't have one already
               // (typically portmapped URLs do, but the raw URL returned by
               // Shiny doesn't)
               if (url[url.length() - 1] != '/')
                  url += "/";
               
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

   void onRenderCompleted(int exitStatus,
                          const std::string& encoding,
                          boost::shared_ptr<std::string> pAllOutput)
   {
      // check each line of the emitted output; if it starts with a token
      // indicating rendering is complete, store the remainder of the emitted
      // line as the file we rendered
      std::string completeMarker("Output created: ");
      std::string renderLine;
      std::stringstream outputStream(*pAllOutput);
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
      isRunning_ = false;

      // if a quiet terminate was requested, don't queue any client events
      if (terminateType_ == renderTerminateQuiet)
         return;

      json::Object resultJson;
      resultJson["succeeded"] = succeeded;
      resultJson["target_file"] =
            module_context::createAliasedPath(targetFile_);
      resultJson["target_encoding"] = encoding_;
      resultJson["target_line"] = sourceLine_;

      std::string outputFile = module_context::createAliasedPath(outputFile_);
      resultJson["output_file"] = outputFile;
      resultJson["knitr_errors"] = build::compileErrorsAsJson(knitrErrors_);

      // A component of the output URL is the full (aliased) path of the output
      // file, on which the renderer bases requests. This path is a URL
      // component (see notes in handleRmdOutputRequest) and thus needs to
      // arrive URL-escaped.
      std::string outputUrl(kRmdOutput "/");
      std::string encodedOutputFile =
                       http::util::urlEncode(
                       http::util::urlEncode(outputFile, false), false);
#ifndef __APPLE__
      // on the desktop (except Cocoa) we need to make another escaping pass
      if (session::options().programMode() == kSessionProgramModeDesktop)
         encodedOutputFile = http::util::urlEncode(encodedOutputFile, false);
#endif
      outputUrl.append(encodedOutputFile);
      outputUrl.append("/");
      resultJson["output_url"] = outputUrl;

      resultJson["output_format"] = outputFormat_;

      resultJson["is_shiny_document"] = isShiny_;
      resultJson["has_shiny_content"] = hasShinyContent_;

      // default to no slide info
      resultJson["preview_slide"] = -1;
      resultJson["slide_navigation"] = json::Value();

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

      // only allow extended results if we have a source file to
      // navigate back to (otherwise you could navigate to the temp
      // file used for preview)
      if (sourceNavigation_)
      {
         rmarkdown::presentation::ammendResults(
                                     formatName,
                                     targetFile_,
                                     sourceLine_,
                                     &resultJson);
      }

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

      if (isShiny_ && !canRenderShinyDocs())
      {
         enqueRenderOutput(module_context::kCompileOutputError,
            "\n"
            "The development versions of knitr and shiny are required to\n"
            "render Shiny documents. You can install them using devtools\n"
            "as follows:\n\n"
            "devtools::install_github(c(\"yihui/knitr\",\"rstudio/shiny\"))\n\n");
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
                                       path, encoding)
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
            build::CompileError err(
                     build::CompileError::Error,
                     targetFile_.parent().complete(matches[3].str()),
                     boost::lexical_cast<int>(matches[1].str()),
                     1,
                     matches[4].str(),
                     true);
            knitrErrors_.push_back(err);
         }
      }
      CompileOutput compileOutput(type, output);
      ClientEvent event(client_events::kRmdRenderOutput,
                        compileOutputAsJson(compileOutput));
      module_context::enqueClientEvent(event);
   }

   bool isRunning_;
   bool terminationRequested_;
   RenderTerminateType terminateType_;
   bool isShiny_;
   bool hasShinyContent_;
   FilePath targetFile_;
   int sourceLine_;
   FilePath outputFile_;
   std::string encoding_;
   bool sourceNavigation_;
   json::Object outputFormat_;
   std::vector<build::CompileError> knitrErrors_;
};

boost::shared_ptr<RenderRmd> s_pCurrentRender_;

// This class's job is to asynchronously read template locations from the R
// Markdown package, and emit each template as a client event. This should
// generally be fast (a few milliseconds); we use this asynchronous
// implementation in case the file system is slow (e.g. slow or remote disk)
// or there are many thousands of packages (e.g. all of CRAN).
class DiscoverTemplates :
      boost::noncopyable,
      public boost::enable_shared_from_this<DiscoverTemplates>
{
public:

   static boost::shared_ptr<DiscoverTemplates> create()
   {
      boost::shared_ptr<DiscoverTemplates> pDiscover(new DiscoverTemplates());
      pDiscover->start();
      return pDiscover;
   }

   bool isRunning()
   {
      return isRunning_;
   }

private:

   DiscoverTemplates() : isRunning_(false)
   { }

   void start()
   {
      // R binary
      FilePath rProgramPath;
      Error error = module_context::rScriptPath(&rProgramPath);
      if (error)
      {
         LOG_ERROR(error);
         onCompleted(0);
         return;
      }

      // args
      std::vector<std::string> args;
      args.push_back("--slave");
      args.push_back("--vanilla");
      args.push_back("-e");
      args.push_back("rmarkdown:::list_template_dirs()");

      // options
      core::system::ProcessOptions options;
      options.terminateChildren = true;

      // we want to discover packages in the libraries visible to this process,
      // so forward the R_LIBS environment variable to the child process
      core::system::Options childEnv;
      core::system::environment(&childEnv);
      std::string libPaths = module_context::libPathsString();
      if (!libPaths.empty())
      {
         core::system::setenv(&childEnv, "R_LIBS", libPaths);
         options.environment = childEnv;
      }

      core::system::ProcessCallbacks cb;
      using namespace module_context;
      cb.onStdout = boost::bind(&DiscoverTemplates::onOutput,
                                DiscoverTemplates::shared_from_this(),
                                _2);
      cb.onExit =  boost::bind(&DiscoverTemplates::onCompleted,
                                DiscoverTemplates::shared_from_this(),
                                _1);

      error = module_context::processSupervisor().runProgram(
               rProgramPath.absolutePath(),
               args,
               options,
               cb);
      if (error)
      {
         LOG_ERROR(error);
         onCompleted(0);
      }
      else
      {
         isRunning_ = true;
      }
   }

   void onOutput(const std::string& output)
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

         // try to get the template's YAML; without the YAML the template is
         // invalid, so skip this template if it has no YAML
         FilePath yamlPath = FilePath(path).complete("template.yaml");
         if (!yamlPath.exists())
            continue;

         SEXP templateDetails;
         error = r::exec::RFunction(
            "yaml:::yaml.load_file",
            string_utils::utf8ToSystem(yamlPath.absolutePath())).call(
                  &templateDetails, &protect);
         if (!error)
         {
            bool createDirFlag = false;
            r::sexp::getNamedListElement(templateDetails,
                                         "name", &name);
            r::sexp::getNamedListElement(templateDetails,
                                         "description", &description);
            error = r::sexp::getNamedListElement(templateDetails,
                                                 "create_dir",
                                                 &createDirFlag);
            if (!error)
            {
                createDir = createDirFlag ? "true" : "false";
            }
         }

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
      isRunning_ = false;
      module_context::enqueClientEvent(
               ClientEvent(client_events::kRmdTemplateDiscoveryCompleted));
   }

   bool isRunning_;
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
#ifdef __APPLE__
         return match[0];
#else
         if (session::options().programMode() != kSessionProgramModeDesktop)
            return match[0];

         result.append(kQtMathJaxConfigScript "\n");
         result.append(match[0]);
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

void initPandocPath()
{
   r::exec::RFunction sysSetenv("Sys.setenv");
   sysSetenv.addParam("RSTUDIO_PANDOC", 
                      session::options().pandocPath().absolutePath());
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
   // check the current status
   install::Status status = install::status();

   // silent upgrade if we have an older version
   if (status == install::InstalledRequiresUpdate)
   {
      Error error = install::silentUpdate();
      if (error)
         LOG_ERROR(error);
   }

   // return installation status
   json::Object contextJson;
   contextJson["rmarkdown_installed"] = install::haveRequiredVersion();
   pResponse->setResult(contextJson);

   return Success();
}

Error installRMarkdown(const json::JsonRpcRequest&,
                       json::JsonRpcResponse* pResponse)
{
   boost::shared_ptr<console_process::ConsoleProcess> pCP;
   Error error = install::installWithProgress(&pCP);
   if (error)
      return error;

   pResponse->setResult(pCP->toJson());

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

// return the path to the local copy of MathJax installed with the RMarkdown
// package
FilePath mathJaxDirectory()
{
   std::string path;
   FilePath mathJaxDir;

   // call system.file to find the appropriate path
   r::exec::RFunction findMathJax("system.file", "rmd/h/m");
   findMathJax.addParam("package", "rmarkdown");
   Error error = findMathJax.call(&path);

   // we don't expect this to fail since we shouldn't be here if RMarkdown
   // is not installed at the correct verwion
   if (error)
      LOG_ERROR(error);
   else
      mathJaxDir = FilePath(path);
   return mathJaxDir;
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

   // Read the desired output file name from the URL
   size_t pos = path.find('/', 1);
   if (pos == std::string::npos)
   {
      pResponse->setError(http::status::NotFound, "No output file found");
      return;
   }
   std::string outputFile = http::util::urlDecode(path.substr(0, pos));
   FilePath outputFilePath(module_context::resolveAliasedPath(outputFile));
   if (!outputFilePath.exists())
   {
      pResponse->setError(http::status::NotFound, outputFile + " not found");
      return;
   }

   // Strip the output file name from the URL
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
      pResponse->setCacheableFile(filePath, request);
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
} // anonymous namespace

bool canRenderShinyDocs()
{
   return module_context::isPackageVersionInstalled("shiny", "0.9.1.9006") &&
          module_context::isPackageVersionInstalled("knitr", "1.5.32");
}

bool rmarkdownPackageAvailable()
{
   if (!haveMarkdownToHTMLOption())
   {
#ifdef _WIN32
      return r::util::hasRequiredVersion("3.0");
#else
      return r::util::hasRequiredVersion("2.14.1");
#endif
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

   initPandocPath();

   module_context::events().onDetectSourceExtendedType
                                        .connect(onDetectRmdSourceType);
   module_context::events().onClientInit.connect(onClientInit);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (install::initialize)
      (bind(registerRpcMethod, "get_rmarkdown_context", getRMarkdownContext))
      (bind(registerRpcMethod, "install_rmarkdown", installRMarkdown))
      (bind(registerRpcMethod, "render_rmd", renderRmd))
      (bind(registerRpcMethod, "render_rmd_source", renderRmdSource))
      (bind(registerRpcMethod, "terminate_render_rmd", terminateRenderRmd))
      (bind(registerRpcMethod, "discover_rmd_templates", discoverRmdTemplates))
      (bind(registerRpcMethod, "create_rmd_from_template", createRmdFromTemplate))
      (bind(registerUriHandler, kRmdOutputLocation, handleRmdOutputRequest))
      (bind(module_context::sourceModuleRFile, "SessionRMarkdown.R"));

   return initBlock.execute();
}
   
} // namespace rmarkdown
} // namespace modules
} // namespace session

