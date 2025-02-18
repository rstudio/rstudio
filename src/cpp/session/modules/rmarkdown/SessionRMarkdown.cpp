/*
 * SessionRMarkdown.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "session-config.h"

#include "SessionRMarkdown.hpp"

#include <gsl/gsl>

#include "SessionRmdNotebook.hpp"
#include "../SessionHTMLPreview.hpp"
#include "../build/SessionBuildErrors.hpp"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/iostreams/filter/regex.hpp>
#include <boost/format.hpp>
#include <boost/scope_exit.hpp>

#include <shared_core/Hash.hpp>

#include <core/Base64.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/StringUtils.hpp>
#include <core/Algorithm.hpp>
#include <core/YamlUtil.hpp>
#include <core/r_util/RProjectFile.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/ROptions.hpp>
#include <r/RUtil.hpp>
#include <r/RRoutines.hpp>
#include <r/RCntxtUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionUrlPorts.hpp>
#include <session/SessionQuarto.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "SessionBlogdown.hpp"
#include "RMarkdownPresentation.hpp"

#define kRmdOutput "rmd_output"
#define kRmdOutputLocation "/" kRmdOutput "/"

#define kMathjaxSegment "mathjax"
#define kMathjaxBeginComment "<!-- dynamically load mathjax"

#define kStandardRenderFunc "rmarkdown::render"
#define kShinyRenderFunc "rmarkdown::run"

#define kShinyContentWarning "Warning: Shiny application in a static R Markdown document"

#define kAnsiEscapeRegex "(?:\033\\[\\d+m)*"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {

namespace {

bool s_rmarkdownAvailable = false;
bool s_rmarkdownAvailableInited = false;

#ifdef _WIN32

// TODO: promote to StringUtils?
std::string utf8ToConsole(const std::string& string)
{
   std::vector<wchar_t> wide(string.length() + 1);
   int chars = ::MultiByteToWideChar(
            CP_UTF8, 0,
            string.data(),
            gsl::narrow_cast<int>(string.size()),
            &wide[0],
            gsl::narrow_cast<int>(wide.size()));

   if (chars == 0)
   {
      LOG_ERROR(LAST_SYSTEM_ERROR());
      return string;
   }
   
   std::ostringstream output;
   char buffer[16];
   
   // force C locale (ensures that any non-ASCII characters
   // will fail to convert and hence must be unicode escaped)
   const char* locale = ::setlocale(LC_CTYPE, nullptr);
   ::setlocale(LC_CTYPE, "C");

   for (int i = 0; i < chars; i++)
   {
      int n = ::wctomb(buffer, wide[i]);
      
      // use Unicode escaping for characters that cannot be represented
      // as well as for single-byte upper ASCII
      if (n == -1 || (n == 1 && static_cast<unsigned char>(buffer[0]) > 127))
      {
         output << "\\u{" << std::hex << wide[i] << "}";
      }
      else
      {
         output.write(buffer, n);
      }
   }
   
   ::setlocale(LC_CTYPE, locale);
   
   return output.str();
   
}

#else

std::string utf8ToConsole(const std::string& string)
{
   return string_utils::utf8ToSystem(string);
}

#endif

enum 
{
   RExecutionReady = 0,
   RExecutionBusy  = 1
};

std::string projectBuildDir()
{
   return string_utils::utf8ToSystem(
      projects::projectContext().buildTargetPath().getAbsolutePath());
}

Error detectWebsiteOutputDir(const std::string& siteDir,
                             std::string* pWebsiteOutputDir)
{
   r::exec::RFunction websiteOutputDir(".rs.websiteOutputDir", siteDir);
   return websiteOutputDir.call(pWebsiteOutputDir);
}

std::string s_websiteOutputDir;

bool haveMarkdownToHTMLOption()
{
   SEXP markdownToHTMLOption = r::options::getOption("rstudio.markdownToHTML");
   return !r::sexp::isNull(markdownToHTMLOption);
}

void initRmarkdownPackageAvailable()
{
   if (!haveMarkdownToHTMLOption())
   {
      s_rmarkdownAvailable = r::util::hasRequiredVersion("3.0");
   }
   else
   {
      s_rmarkdownAvailable = false;
   }
}

void initWebsiteOutputDir()
{
   if (!module_context::isWebsiteProject())
      return;

   std::string outputDirFullPath;
   Error error = detectWebsiteOutputDir(projectBuildDir(), &outputDirFullPath);
   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      if (outputDirFullPath != projectBuildDir())
         s_websiteOutputDir = FilePath(outputDirFullPath).getFilename();
      else
         s_websiteOutputDir = "";
   }
}

} // anonymous namespace

namespace module_context {

FilePath extractOutputFileCreated(const FilePath& inputDir,
                                  const std::string& output,
                                  bool ignoreHugo)
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
      for (const std::string& marker : completeMarkers)
      {
         if (boost::algorithm::starts_with(renderLine, marker))
         {
            std::string fileName = renderLine.substr(marker.length());

            // trim any whitespace from the end of the filename (on Windows
            // this includes part of CR-LF)
            boost::algorithm::trim(fileName);

            // if the path looks absolute, use it as-is; otherwise, presume
            // it to be in the same directory as the input file
            FilePath outputFile = inputDir.completePath(fileName);
            if (outputFile.exists())
               core::system::realPath(outputFile, &outputFile);

            // if it's a plain .md file and we are in a Hugo project then
            // don't preview it (as the user is likely running a Hugo preview)
            if (outputFile.getExtensionLowerCase() == ".md" &&
                ignoreHugo &&
                session::modules::rmarkdown::blogdown::isHugoProject())
            {
               return FilePath();
            }

            // return the output file
            return outputFile;
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
   return module_context::sessionScratchPath().completeChildPath("rmd-outputs");
}

std::string assignOutputUrl(const std::string& outputFile)
{
   std::string outputUrl(kRmdOutput "/");
   s_currentRenderOutput = (s_currentRenderOutput + 1) % kMaxRenderOutputs;

   // if this is a website project and the file is not at the root then we need
   // to do some special handling to make sure that the HTML can refer to
   // locations in parent directories (e.g. for navigation links)
   std::string path = "/";
   FilePath outputPath = module_context::resolveAliasedPath(outputFile);
   FilePath websiteDir = r_util::websiteRootDirectory(outputPath);
   if (!websiteDir.isEmpty())
   {
      std::string websiteOutputDir;
      Error error = detectWebsiteOutputDir(websiteDir.getAbsolutePath(), &websiteOutputDir);
      if (error)
      {
         websiteDir = FilePath();
         LOG_ERROR(error);
      }
      else
      {
         websiteDir = FilePath(websiteOutputDir);
      }
   }

   // figure out the project directory
   FilePath projDir = outputPath.getParent();
   if (projDir.getFilename() == "_site")
      projDir = projDir.getParent();

   // detect whether we're creating a book output vs. a website page
   if (!websiteDir.isEmpty() && outputPath.isWithin(websiteDir) && !r_util::isWebsiteDirectory(projDir))
   {
      std::string renderedPath;
      Error error = r::exec::RFunction(".rs.bookdown.renderedOutputPath")
            .addUtf8Param(websiteDir)
            .addUtf8Param(outputPath)
            .callUtf8(&renderedPath);
      if (error)
         LOG_ERROR(error);
      
      s_renderOutputs[s_currentRenderOutput] = renderedPath;
      
      // compute relative path to target file and append it to the path
      std::string relativePath = outputPath.getRelativePath(websiteDir);
      path += relativePath;
   }
   else
   {
      s_renderOutputs[s_currentRenderOutput] = outputFile;
   }

   outputUrl.append(boost::lexical_cast<std::string>(s_currentRenderOutput));
   outputUrl.append(path);
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
      async_r::AsyncRProcess::terminate(isQuarto_);
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
         rmarkdown::presentation::amendResults(
                  outputFormat_["format_name"].getString(),
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
         string_utils::utf8ToSystem(targetFile.getAbsolutePath())).call(
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
   {
   }

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
      if (format == "revealjs")
      {
         outputFormat_["format_name"] = "revealjs";
         outputFormat_["self_contained"] = false;
      }
      else
      {
         getOutputFormat(targetFile_.getAbsolutePath(), encoding, &outputFormat_);
      }
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
         std::string extendedType;
         Error error = source_database::detectExtendedType(targetFile_, &extendedType);
         if (error)
            LOG_ERROR(error);
         if (extendedType == "quarto-document")
            renderFunc = "quarto serve";
         else
            renderFunc = kShinyRenderFunc;
      }
      else
      {
         // see if the input file has a custom render function
         error = r::exec::RFunction(
            ".rs.getCustomRenderFunction",
            string_utils::utf8ToSystem(targetFile_.getAbsolutePath())).call(
                                                                  &renderFunc);
         if (error)
            LOG_ERROR(error);

         if (renderFunc.empty())
            renderFunc = kStandardRenderFunc;
         else if (renderFunc == kShinyRenderFunc)
            isShiny_ = true;
      }

      // if we are using a quarto command to render, we must be a quarto doc. read
      // all of the input file lines to be used in error navigation
      if (renderFunc == "quarto serve")
      {
          isQuarto_ = true;
          Error error = core::readLinesFromFile(targetFile_, &targetFileLines_);
          if (error)
             LOG_ERROR(error);
      }

      std::string extraParams;
      std::string targetFile =
            utf8ToConsole(targetFile_.getAbsolutePath());

      std::string renderOptions("encoding = '" + encoding + "'");

      // output to a specific format if specified
      if (!format.empty())
      {
         renderOptions += ", output_format = '" + format + "'";
      }

      // include params if specified
      if (!paramsFile.empty())
      {
         renderOptions += ", params = readRDS('" + utf8ToConsole(paramsFile) + "')";
      }

      // use the stated working directory if specified and we're using the default render function
      // (other render functions may not accept knit_root_dir)
      if (!workingDir.empty() && renderFunc == kStandardRenderFunc)
      {
         renderOptions += ", knit_root_dir = '" + 
                          utf8ToConsole(workingDir) + "'";
      }

      // output to a temporary directory if specified (no need to do this
      // for Shiny since it already renders to a temporary dir)
      if (asTempfile && !isShiny_)
      {
         FilePath tmpDir = module_context::tempFile("preview-", "dir");
         Error error = tmpDir.ensureDirectory();
         if (!error)
         {
            std::string dir = utf8ToConsole(tmpDir.getAbsolutePath());
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
         std::string parentDir = utf8ToConsole(targetFile_.getParent().getAbsolutePath());
         extraParams += "dir = '" + string_utils::singleQuotedStrEscape(parentDir) + "', ";

         // provide render_args in render_args parameter
         renderOptions = "render_args = list(" + renderOptions + ")";
      }

      // fallback for custom render function that isn't actually a function
      if (renderFunc != kStandardRenderFunc && renderFunc != kShinyRenderFunc)
      {
         std::string extraArgs;
         r::sexp::Protect rProtect;
         SEXP renderFuncSEXP;
         error = r::exec::evaluateString(renderFunc, &renderFuncSEXP, &rProtect);
         if (error || !r::sexp::isFunction((renderFuncSEXP)))
         {
            boost::format fmt("(function(input, ...) { invisible(system(paste0('%1% \"', input, '\" ', '%2%'))) })");
            renderFunc = boost::str(fmt % renderFunc % extraArgs);
         }
      }

      // render command
      boost::format fmt("%1%('%2%', %3% %4%);");
      std::string cmd = boost::str(fmt %
                             renderFunc %
                             string_utils::singleQuotedStrEscape(targetFile) %
                             extraParams %
                             renderOptions);
      
      // un-escape unicode escapes
#ifdef _WIN32
      cmd = boost::algorithm::replace_all_copy(cmd, "\\\\u{", "\\u{");
#endif
      
      // environment
      core::system::Options environment;
      std::string tempDir;
      error = r::exec::RFunction("tempdir").call(&tempDir);
      if (!error)
         environment.push_back(std::make_pair("RMARKDOWN_PREVIEW_DIR", tempDir));
      else
         LOG_ERROR(error);

      // pass along the RSTUDIO_VERSION
      environment.push_back(std::make_pair("RSTUDIO_VERSION", parsableRStudioVersion()));
      environment.push_back(std::make_pair("RSTUDIO_LONG_VERSION", RSTUDIO_VERSION));

      // inform that this runs in the Render pane
      environment.push_back(std::make_pair("RSTUDIO_CHILD_PROCESS_PANE", "render"));
      
      // set the not cran env var
      environment.push_back(std::make_pair("NOT_CRAN", "true"));

      // pass along the current Python environment, if any
      std::string reticulatePython;
      error = r::exec::RFunction(".rs.inferReticulatePython").call(&reticulatePython);
      if (error)
         LOG_ERROR(error);
      
      // pass along current PATH
      std::string currentPath = core::system::getenv("PATH");
      core::system::setenv(&environment, "PATH", currentPath);
      
      if (!reticulatePython.empty())
      {
         // we found a Python version; forward it
         environment.push_back({"RETICULATE_PYTHON_FALLBACK", reticulatePython});
         
         // also update the PATH so this version of Python is visible
         core::system::addToPath(
                  &environment,
                  FilePath(reticulatePython).getParent().getAbsolutePath(),
                  true);
      }

      // render unless we were handed an existing output file
      allOutput_.clear();
      if (existingOutputFile.empty())
      {
         // launch the R session in the document's directory by default, unless
         // a working directory was supplied
         FilePath working = targetFile_.getParent();
         if (!workingDir.empty())
            working = module_context::resolveAliasedPath(workingDir);

         // tell the user the command we're using to render the doc if requested
         if (prefs::userPrefs().showRmdRenderCommand())
         {
            onRenderOutput(module_context::kCompileOutputNormal, "==> " + cmd + "\n");
         }

         // start the render process
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
         // leaving this coupling for now to minimize the scope of the change
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
      for (std::string& outputLine : outputLines)
      {
         // if this is a Shiny render, check to see if Shiny started listening
         if (isShiny_)
         {
            // NOTE: This code path is also used for preview of Quarto documents using
            // 'server: shiny', so we try to be permissive in terms of what kind of
            // output we accept when we seeing Quarto is running.
            //
            // github.com/rstudio/rstudio/issues/14039
            const boost::regex shinyListening(
                     "^" kAnsiEscapeRegex
                     "(?:Listening on |Browse at )?(https?://[^\033]+)"
                     kAnsiEscapeRegex "$");
            
            boost::smatch matches;
            if (regex_utils::match(outputLine, matches, shinyListening))
            {
               json::Object startedJson;
               startedJson["target_file"] =
                     module_context::createAliasedPath(targetFile_);
               startedJson["output_format"] = outputFormat_;
               std::string url(url_ports::mapUrlPorts(matches[1].str()));

               // add a / to the URL if it doesn't have one already
               // (typically portmapped URLs do, but the raw URL returned by
               // Shiny doesn't)
               if (url[url.length() - 1] != '/')
                  url += "/";

               getPresentationDetails(sourceLine_, &startedJson);

               startedJson["url"] = url + targetFile_.getFilename();

               startedJson["runtime"] = getRuntime(targetFile_);

               startedJson["is_quarto"] = isQuarto_;

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
      using namespace module_context;

      // see if we can determine the output file
      FilePath outputFile = extractOutputFileCreated(targetFile_.getParent(), allOutput_);
      if (!outputFile.isEmpty())
         outputFile_ = outputFile;

      // the process may be terminated normally by the IDE (e.g. to stop the
      // Shiny server); alternately, a termination is considered normal if
      // the process succeeded and produced output.
      terminate(
          terminateType_ == renderTerminateNormal ||
          (exitStatus == 0 && outputFile_.exists()));
   }

   void terminateWithError(const Error& error)
   {
      std::string message =
            "Error rendering R Markdown for " +
            module_context::createAliasedPath(targetFile_) + " " +
            error.getSummary();
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
      
      std::vector<SourceMarker> knitrErrors;
      if (!terminationRequested() && renderErrorMarker_)
      {
         renderErrorMarker_.message = core::html_utils::HTML(renderErrorMessage_.str());
         knitrErrors.push_back(renderErrorMarker_);
      }
      resultJson["knitr_errors"] = sourceMarkersAsJson(knitrErrors);

      resultJson["output_url"] = assignOutputUrl(outputFile);
      resultJson["output_format"] = outputFormat_;

      resultJson["is_shiny_document"] = isShiny_;
      resultJson["has_shiny_content"] = hasShinyContent_;
      resultJson["is_quarto"] = isQuarto_;

      resultJson["runtime"] = getRuntime(targetFile_);

      json::Value websiteDir;
      if (outputFile_.getExtensionLowerCase() == ".html")
      {
         // check for previous publishing
         resultJson["rpubs_published"] =
               !module_context::previousRpubsUploadId(outputFile_).empty();

         FilePath webPath = session::projects::projectContext().fileUnderWebsitePath(targetFile_);
         if (!webPath.isEmpty())
            websiteDir = createAliasedPath(webPath);
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
      std::string formatName =  outputFormat_["format_name"].getString();

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
            "\nNo LaTeX installation detected (LaTeX is required "
            "to create PDF output). You should install "
            "a LaTeX distribution for your platform: "
            "https://www.latex-project.org/get/\n\n"
            "  If you are not sure, you may install TinyTeX in R: tinytex::install_tinytex()\n\n"
            "  Otherwise consider MiKTeX on Windows - http://miktex.org\n\n"
            "  MacTeX on macOS - https://tug.org/mactex/\n"
            "  (NOTE: Download with Safari rather than Chrome _strongly_ recommended)\n\n"
            "  Linux: Use system package manager\n\n");
      }
   }

   void enqueRenderOutput(int type,
                          const std::string& output)
   {
      using namespace module_context;
      
      if (type == kCompileOutputError && sourceNavigation_)
      {
         if (renderErrorMarker_)
         {
            // if an error occurred during rendering, then any
            // subsequent output should be gathered as part of
            // that error
            renderErrorMessage_ << output;
         }
         else if (isQuarto_)
         {
            navigateToRenderPreviewError(targetFile_, targetFileLines_, output, allOutput_);
         }
         else
         {
            int errorLine        = -1;
            int backtraceLine    = -1;
            int quittingFromLine = -1;

            boost::regex reRenderError(kKnitrErrorRegex);
            boost::smatch match;

            auto lines = core::algorithm::split(output, "\n");
            for (int i = 0, n = lines.size(); i < n; i++)
            {
               std::cerr << lines[i] << std::endl;

               if (lines[i] == "Error:" || boost::algorithm::starts_with(lines[i], "Error in "))
               {
                  errorLine = i;
               }
               else if (lines[i] == "Backtrace:")
               {
                  backtraceLine = i;
               }
               else if (regex_utils::match(lines[i], match, reRenderError))
               {
                  // looks like a knitr error; compose a compile error object and
                  // emit it to the client when the render is complete
                  quittingFromLine = i;
                  int line = core::safe_convert::stringTo<int>(match[1].str(), -1);
                  FilePath file = targetFile_.getParent().completePath(match[3].str());
                  renderErrorMarker_ = SourceMarker(SourceMarker::Error, file, line, 1, {}, true);
                  break;
               }
            }

            if (quittingFromLine == -1)
            {
               // nothing to do; didn't see the expected knitr error line?
            }
            else if (errorLine == -1)
            {
               // didn't find an error line; assume error output
               // follows the 'Quitting from:' line
               std::string errorMessage = core::algorithm::join(
                        lines.begin() + quittingFromLine,
                        lines.end(),
                        "\n");
               renderErrorMessage_ << errorMessage;
            }
            else
            {
               // found an 'Error:' line; collect error from then
               // to the 'Quitting from:' line
               std::string errorMessage = core::algorithm::join(
                        lines.begin() + errorLine,
                        lines.begin() + (backtraceLine == -1 ? quittingFromLine : backtraceLine),
                        "\n");
               renderErrorMessage_ << errorMessage;
            }
         }
      }
      
      // always enque quarto as normal output (it does it's own colorizing of error output)
      if (isQuarto_)
         type = module_context::kCompileOutputNormal;

      CompileOutput compileOutput(type, output);
      ClientEvent event(
               client_events::kRmdRenderOutput,
               compileOutputAsJson(compileOutput));
      module_context::enqueClientEvent(event);
   }

   RenderTerminateType terminateType_;
   bool isShiny_;
   bool hasShinyContent_;
   bool isQuarto_ = false;
   FilePath targetFile_;
   std::vector<std::string> targetFileLines_;
   int sourceLine_;
   FilePath outputFile_;
   std::string encoding_;
   std::string viewerType_;
   bool sourceNavigation_;
   json::Object outputFormat_;
   module_context::SourceMarker renderErrorMarker_;
   std::stringstream renderErrorMessage_;
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
      rstudioPandoc = session::options().pandocPath().getAbsolutePath();
   
   r::exec::RFunction sysSetenv("Sys.setenv");
   sysSetenv.addParam(kRStudioPandoc, rstudioPandoc);

   // set RMARKDOWN_MATHJAX_PATH (leave existing value alone)
   std::string rmarkdownMathjaxPath = core::system::getenv(kRmarkdownMathjaxPath);
   if (rmarkdownMathjaxPath.empty())
     rmarkdownMathjaxPath = session::options().mathjaxPath().getAbsolutePath();
   sysSetenv.addParam(kRmarkdownMathjaxPath, rmarkdownMathjaxPath);

   // call Sys.setenv
   Error error = sysSetenv.call();
   if (error)
      LOG_ERROR(error);
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
      if ((filePath.getExtensionLowerCase() == ".rmd" ||
           filePath.getExtensionLowerCase() == ".md") &&
          !boost::algorithm::icontains(pDoc->contents(),
                                       "<!-- rmarkdown v1 -->") &&
          rmarkdownPackageAvailable())
      {
         // if we find html_notebook in the YAML header, presume that this is an R Markdown notebook
         // (this isn't 100% foolproof but this check runs frequently so needs to be fast; more
         // thorough type introspection is done on the client)
         std::string yamlHeader = yaml::extractYamlHeader(pDoc->contents());
         if (boost::algorithm::contains(yamlHeader, "html_notebook"))
         {
            return "rmarkdown-notebook";
         }

         // otherwise, it's a regular R Markdown document
         return "rmarkdown-document";
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
      FilePath outputFile = inputFile.getParent().completePath(inputFile.getStem() + 
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
      resultJson["output_url"] = assignOutputUrl(outputFile.getAbsolutePath());
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

   doRenderRmd(
      rmdTempFile.getAbsolutePath(), -1, "", "UTF-8", "",
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
   {
      module_context::clearViewerCurrentUrl();

      s_pCurrentRender_->terminateProcess(
               normal ? renderTerminateNormal :
                        renderTerminateAbnormal);

   }

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
      pResponse->setNotFoundError(request);
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
      pResponse->setNotFoundError(request);
      return;
   }

   // make sure the output identifier refers to a valid file
   std::string outputFile = s_renderOutputs[outputId];
   FilePath outputFilePath(module_context::resolveAliasedPath(outputFile));
   if (!outputFilePath.exists())
   {
      pResponse->setNotFoundError(outputFile, request);
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
      pResponse->setCacheableFile(
         mathJaxDirectory().completePath(
            path.substr(sizeof(kMathjaxSegment))),
                                  request);
   }
   else
   {
      // serve a file resource from the output folder
      FilePath filePath = outputFilePath.getParent().completeChildPath(path);

      // if it's a directory then auto-append index.html
      if (filePath.isDirectory())
         filePath = filePath.completeChildPath("index.html");

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

   // locate the template skeleton on disk
   // (return empty string if none found)
   std::string templateContent;
   for (auto&& suffix : { "skeleton/skeleton.Rmd", "skeleton/skeleton.rmd" })
   {
      FilePath skeletonPath = FilePath(path).completePath(suffix);
      if (!skeletonPath.exists())
         continue;

      Error error = readStringFromFile(skeletonPath, &templateContent, string_utils::LineEndingPosix);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      break;
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
   std::string relativePath = filePath.getRelativePath(websiteDir);

   // get the list of copyable site resources
   std::vector<std::string> copyableResources;
   r::exec::RFunction func("rmarkdown:::copyable_site_resources");
   func.addParam("input", string_utils::utf8ToSystem(websiteDir.getAbsolutePath()));
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
   if (filePath.getParent() == websiteDir)
      search = filePath.getFilename();
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
   FilePath outputFile = outputDir.completeChildPath(relativePath);
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

   error = outputFile.getParent().ensureDirectory();
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

Error rmdImportImages(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   // read params
   json::Array imagesJson;
   std::string imagesDir;
   Error error = json::readParams(request.params, &imagesJson, &imagesDir);
   if (error)
      return error;

   // determine images dir
   FilePath imagesPath = module_context::resolveAliasedPath(imagesDir);
   error = imagesPath.ensureDirectory();
   if (error)
      return error;

   // build list of target image paths
   json::Array importedImagesJson;

   // copy each image to the target directory (renaming with a unique stem as required)
   std::vector<std::string> images;
   imagesJson.toVectorString(images);
   for (auto image : images)
   {
      // skip if it doesn't exist
      FilePath imagePath = module_context::resolveAliasedPath(image);
      if (!imagePath.exists())
         continue;

      // find a unique target path
      std::string targetStem = imagePath.getStem();
      std::string extension = imagePath.getExtension();
      FilePath targetPath = imagesPath.completeChildPath(targetStem + extension);
      if (imagesPath.completeChildPath(targetStem + extension).exists())
      {
         std::string resolvedStem;
         module_context::uniqueSaveStem(imagesPath, targetStem, "-", &resolvedStem);
         targetPath = imagesPath.completeChildPath(resolvedStem + extension);
      }

      // only copy it if it's not the same path
      if (imagePath != targetPath)
      {
         Error error = imagePath.copy(targetPath);
         if (error)
            LOG_ERROR(error);
      }

      // update imported images
      importedImagesJson.push_back(module_context::createAliasedPath(targetPath));
   }

   pResponse->setResult(importedImagesJson);
   return Success();
}

Error rmdSaveBase64Images(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // read params
   json::Array imageDataJson;
   std::string imagesDir;
   Error error = json::readParams(request.params, &imageDataJson, &imagesDir);
   if (error)
      return error;

   // determine images dir
   FilePath imagesPath = module_context::resolveAliasedPath(imagesDir);
   error = imagesPath.ensureDirectory();
   if (error)
      return error;
   
   // build list of target image paths
   std::vector<std::string> createdImages;

   // start decoding and writing image data to file
   std::vector<std::string> imageData;
   imageDataJson.toVectorString(imageData);
   for (auto&& image : imageData)
   {
      // data:[<mime type>][;charset=<charset>][;base64],<encoded data>
      boost::regex reDataImage(
               "^data:image/([^;,]+)"  // data + mime type prefix; capture the image type
               "(;charset=[^;,]+)?"    // optional charset
               "(;base64)?"            // optional base64 declaration
               ",(.*)$"                // comma separating prefix from data
      );
      
      boost::smatch match;
      if (boost::regex_match(image, match, reDataImage))
      {
         // extract the captured image data
         std::string rawData = match[4];
         if (match[3].matched)
         {
            Error error = base64::decode(rawData, &rawData);
            if (error)
               LOG_ERROR(error);
         }
         
         // figure out an appropriate extension
         std::string mimeType = match[1];
         std::string fileExtension = mimeType;
         if (mimeType == "svg+xml")
            fileExtension = "svg";
         
         // create the file path
         std::string crcHash = core::hash::crc32Hash(rawData);
         std::string fileName = fmt::format("clipboard-{}.{}", crcHash, fileExtension);
         FilePath imagePath = imagesPath.completeChildPath(fileName);
         
         // write to file
         Error error = core::writeStringToFile(imagePath, rawData);
         if (error)
            LOG_ERROR(error);
         
         // return path to generated image
         std::string resolvedPath = fmt::format("images/{}", fileName);
         createdImages.push_back(resolvedPath);
      }
      else
      {
         static const boost::regex rePrefix("^(data:[^,]+,)");
         
         boost::smatch match;
         if (boost::regex_match(image, match, rePrefix))
         {
            WLOGF("Don't know how to handle image data {}", match[1].str());
         }
         else
         {
            auto n = std::min(std::size_t(16), image.length());
            WLOGF("Don't know how to handle image data {}", image.substr(0, n));
         }
      }
   }
   
   // send back new image paths to client
   json::Array createdImagesJson = core::json::toJsonArray(createdImages);
   pResponse->setResult(createdImagesJson);
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

SEXP rs_getWebsiteOutputDir()
{
   SEXP absolutePathSEXP = R_NilValue;

   FilePath outputDir(module_context::websiteOutputDir());
   if (!outputDir.isEmpty())
   {
      r::sexp::Protect protect;
      absolutePathSEXP = r::sexp::create(outputDir.getAbsolutePath(), &protect);
   }
   return absolutePathSEXP;
}

void onShutdown(bool terminatedNormally)
{
   Error error = core::writeStringVectorToFile(outputCachePath(), 
                                               s_renderOutputs);
   if (error)
      LOG_ERROR(error);

#ifdef _WIN32
   // Windows has issues with the Quarto background process running when the session shuts down
   // It requires that the Quarto process is terminated first
   if (isRenderRunning())
   {
      s_pCurrentRender_->terminateProcess(renderTerminateQuiet);
   }
#endif
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

bool pptAvailable()
{
   return module_context::isPackageVersionInstalled("rmarkdown", "1.8.10");
}

bool rmarkdownPackageAvailable()
{
   if (!s_rmarkdownAvailableInited)
   {
      if (!ASSERT_MAIN_THREAD())
      {
         return s_rmarkdownAvailable;
      }

      s_rmarkdownAvailableInited = true;
      initRmarkdownPackageAvailable();
   }

   return s_rmarkdownAvailable;
}

bool isSiteProject(const std::string& site)
{
   if (!modules::rmarkdown::rmarkdownPackageAvailable() ||
       !projects::projectContext().hasProject() ||
       projects::projectContext().config().buildType != r_util::kBuildTypeWebsite)
      return false;

   bool isSite = false;
   std::string encoding = projects::projectContext().defaultEncoding();
   Error error = r::exec::RFunction(".rs.isSiteProject",
                                    projectBuildDir(), encoding, site).call(&isSite);
   if (error)
      LOG_ERROR(error);
   return isSite;
}

std::string parsableRStudioVersion()
{
   std::string version(RSTUDIO_VERSION_MAJOR);
   version.append(".")
         .append(RSTUDIO_VERSION_MINOR)
         .append(".")
         .append(RSTUDIO_VERSION_PATCH)
         .append(".")
         .append(boost::regex_replace(
               std::string(RSTUDIO_VERSION_SUFFIX),
               boost::regex("[a-zA-Z\\-+]"),
               ""));
   return version;
}


Error initialize()
{
   using boost::bind;
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_paramsFileForRmd);
   RS_REGISTER_CALL_METHOD(rs_getWebsiteOutputDir);

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
         s_currentRenderOutput = gsl::narrow_cast<int>(s_renderOutputs.size());
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
      (bind(registerRpcMethod, "rmd_import_images", rmdImportImages))
      (bind(registerRpcMethod, "rmd_save_base64_images", rmdSaveBase64Images))
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

// used to determine if this is both a website build target AND a bookdown target
bool isBookdownWebsite()
{
   return isWebsiteProject() && isBookdownProject();
}

// used to determine whether the current project directory has a bookdown project
// (distinct from isBookdownWebsite b/c includes scenarios where the book is
// built by a makefile rather than "Build Website"
bool isBookdownProject()
{
   if (!projects::projectContext().hasProject())
      return false;

   bool isBookdown = false;
   std::string encoding = projects::projectContext().defaultEncoding();
   Error error = r::exec::RFunction(".rs.isBookdownDir",
                              projectBuildDir(), encoding).call(&isBookdown);
   if (error)
      LOG_ERROR(error);
   return isBookdown;
}

bool isDistillProject()
{
   if (!isWebsiteProject())
      return false;
   
   return session::modules::rmarkdown::isSiteProject("distill_website");
}


std::string websiteOutputDir()
{
   return s_websiteOutputDir;
}

} // namespace module_context

} // namespace session
} // namespace rstudio

