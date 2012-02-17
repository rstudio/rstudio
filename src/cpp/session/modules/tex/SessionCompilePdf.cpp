/*
 * SessionCompilePdf.cpp
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

#include "SessionCompilePdf.hpp"

#include <boost/format.hpp>
#include <boost/foreach.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/FilePath.hpp>
#include <core/Exec.hpp>
#include <core/Settings.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/ShellUtils.hpp>

#include <core/tex/TexLogParser.hpp>
#include <core/tex/TexMagicComment.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionPdfLatex.hpp"
#include "SessionTexi2Dvi.hpp"
#include "SessionRnwWeave.hpp"
#include "SessionRnwConcordance.hpp"
#include "SessionCompilePdfSupervisor.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace compile_pdf {

namespace {

// status constants
const int kStatusStarted  = 0;
const int kStatusCompleted = 1;

// track compile pdf state so we can send it to the client on client init
class CompilePdfState : boost::noncopyable
{
public:
   CompilePdfState()
      : tabVisible_(false), running_(false)
   {
   }

   virtual ~CompilePdfState() {}
   // COPYING: noncoypable

   Error readFromJson(const json::Object& asJson)
   {
      clear();
      return json::readObject(asJson,
                              "tab_visible", &tabVisible_,
                              "running", &running_,
                              "target_file", &targetFile_,
                              "output", &output_,
                              "errors", &errors_);
   }


   void onStarted(const std::string& targetFile)
   {
      clear();
      running_ = true;
      tabVisible_ = true;
      targetFile_ = targetFile;
   }

   void addOutput(const std::string& output)
   {
      output_ += output;
   }

   void setErrors(const json::Array& errors)
   {
      errors_ = errors;
   }

   void onStopped()
   {
      running_ = false;
   }

   void clear()
   {
      tabVisible_ = false;
      running_ = false;
      targetFile_.clear();
      output_.clear();
      errors_.clear();
   }

   json::Object asJson() const
   {
      json::Object obj;
      obj["tab_visible"] = tabVisible_;
      obj["running"] = running_;
      obj["target_file"] = targetFile_;
      obj["output"] = output_;
      obj["errors"] = errors_;
      return obj;
   }

private:
   bool tabVisible_;
   bool running_;
   std::string targetFile_;
   std::string output_;
   json::Array errors_;
};

CompilePdfState s_compilePdfState;

void onSuspend(Settings* pSettings)
{
   std::ostringstream os;
   json::write(s_compilePdfState.asJson(), os);
   pSettings->set("compile_pdf_state", os.str());
}


void onResume(const Settings& settings)
{
   std::string state = settings.get("compile_pdf_state");
   if (!state.empty())
   {
      json::Value stateJson;
      if (!json::parse(state, &stateJson))
      {
         LOG_WARNING_MESSAGE("invalid compile pdf state json");
         return;
      }

      Error error = s_compilePdfState.readFromJson(stateJson.get_obj());
      if (error)
         LOG_ERROR(error);
   }
}

void enqueOutputEvent(const std::string& output)
{
   s_compilePdfState.addOutput(output);

   ClientEvent event(client_events::kCompilePdfOutputEvent, output);
   module_context::enqueClientEvent(event);
}

void enqueStatusEvent(int status, const std::string& text = std::string())
{
   if (status == kStatusStarted)
      s_compilePdfState.onStarted(text);
   else if (status == kStatusCompleted)
      s_compilePdfState.onStopped();

   json::Object dataJson;
   dataJson["status"] = status;
   dataJson["text"] = text;
   ClientEvent event(client_events::kCompilePdfStatusEvent, dataJson);
   module_context::enqueClientEvent(event);
}

void enqueErrorsEvent(const json::Array& logEntriesJson)
{
   s_compilePdfState.setErrors(logEntriesJson);

   ClientEvent event(client_events::kCompilePdfErrorsEvent, logEntriesJson);
   module_context::enqueClientEvent(event);
}

json::Object logEntryJson(const core::tex::LogEntry& logEntry)
{
   json::Object obj;
   obj["type"] = static_cast<int>(logEntry.type());
   obj["path"] = module_context::createAliasedPath(logEntry.filePath());
   obj["line"] = logEntry.line();
   obj["message"] = logEntry.message();
   return obj;
}

void showLogEntries(const core::tex::LogEntries& logEntries,
                    const rnw_concordance::Concordances& rnwConcordances =
                                             rnw_concordance::Concordances())
{
   json::Array logEntriesJson;
   BOOST_FOREACH(const core::tex::LogEntry& logEntry, logEntries)
   {
      using namespace tex::rnw_concordance;
      core::tex::LogEntry rnwEntry = rnwConcordances.fixup(logEntry);
      logEntriesJson.push_back(logEntryJson(rnwEntry));
   }

   enqueErrorsEvent(logEntriesJson);
}

FilePath ancillaryFilePath(const FilePath& texFilePath, const std::string& ext)
{
   return texFilePath.parent().childPath(texFilePath.stem() + ext);
}

FilePath latexLogPath(const FilePath& texFilePath)
{
   return ancillaryFilePath(texFilePath, ".log");
}

FilePath bibtexLogPath(const FilePath& texFilePath)
{
   return ancillaryFilePath(texFilePath, ".blg");
}

bool showCompilationErrors(const FilePath& texPath,
                           const rnw_concordance::Concordances& concordances)
{
   // latex log file
   core::tex::LogEntries logEntries;
   FilePath logPath = latexLogPath(texPath);
   if (logPath.exists())
   {
      Error error = core::tex::parseLatexLog(logPath, &logEntries);
      if (error)
         LOG_ERROR(error);
   }

   // bibtex log file
   core::tex::LogEntries bibtexLogEntries;
   logPath = bibtexLogPath(texPath);
   if (logPath.exists())
   {
      Error error = core::tex::parseBibtexLog(logPath, &bibtexLogEntries);
      if (error)
         LOG_ERROR(error);
   }

   // concatenate them together
   std::copy(bibtexLogEntries.begin(),
             bibtexLogEntries.end(),
             std::back_inserter(logEntries));

   // show errors if necessary
   if (!logEntries.empty())
   {
      showLogEntries(logEntries, concordances);
      return true;
   }
   else
   {
      return false;
   }
}

void removeExistingLogs(const FilePath& texFilePath)
{
   Error error = latexLogPath(texFilePath).removeIfExists();
   if (error)
      LOG_ERROR(error);

   error = bibtexLogPath(texFilePath).removeIfExists();
   if (error)
      LOG_ERROR(error);
}

class AuxillaryFileCleanupContext : boost::noncopyable
{
public:
   AuxillaryFileCleanupContext()
      : cleanLog_(true)
   {
   }

   virtual ~AuxillaryFileCleanupContext()
   {
      try
      {
         cleanup();
      }
      catch(...)
      {
      }
   }

   void init(const FilePath& targetFilePath)
   {
      basePath_ = targetFilePath.parent().childPath(
                                    targetFilePath.stem()).absolutePath();
   }

   void preserveLog()
   {
      cleanLog_ = false;
   }

   void cleanup()
   {
      if (!basePath_.empty())
      {
         // remove known auxillary files
         remove(".out");
         remove(".aux");

         // only clean bbl if .bib exists
         if (exists(".bib"))
            remove(".bbl");

         // clean anciallary logs if requested (never clean latex log)
         if (cleanLog_)
         {
            remove(".blg");
         }

         // reset base path so we only do this one
         basePath_.clear();
      }
   }

private:
   bool exists(const std::string& extension)
   {
      return FilePath(basePath_ + extension).exists();
   }

   void remove(const std::string& extension)
   {
      Error error = FilePath(basePath_ + extension).removeIfExists();
      if (error)
         LOG_ERROR(error);
   }

private:
   std::string basePath_;
   bool cleanLog_;
};

// implement pdf compilation within a class so we can maintain state
// accross the various async callbacks the compile is composed of
class AsyncPdfCompiler : boost::noncopyable,
                    public boost::enable_shared_from_this<AsyncPdfCompiler>
{
public:
   static void start(const FilePath& targetFilePath,
                    const boost::function<void()>& onCompleted)
   {
      boost::shared_ptr<AsyncPdfCompiler> pCompiler(
            new AsyncPdfCompiler(targetFilePath, onCompleted));

      pCompiler->start();
   }

   virtual ~AsyncPdfCompiler() {}

private:
   AsyncPdfCompiler(const FilePath& targetFilePath,
                    const boost::function<void()>& onCompleted)
      : targetFilePath_(targetFilePath), onCompleted_(onCompleted)
   {
   }

   void start()
   {
      // enque started event
      enqueStartedEvent();

      // ensure no spaces in path
      std::string filename = targetFilePath_.filename();
      if (filename.find(' ') != std::string::npos)
      {
         terminateWithError("Invalid filename: '" + filename +
                     "' (TeX does not understand paths with spaces)");
         return;
      }

      // parse magic comments
      Error error = core::tex::parseMagicComments(targetFilePath_,
                                                  &magicComments_);
      if (error)
         LOG_ERROR(error);

      // determine tex program path
      std::string userErrMsg;
      if (!pdflatex::latexProgramForFile(magicComments_,
                                         &texProgramPath_,
                                         &userErrMsg))
      {
         terminateWithError(userErrMsg);
         return;
      }

      // see if we need to weave
      std::string ext = targetFilePath_.extensionLowerCase();
      bool isRnw = ext == ".rnw" || ext == ".snw" || ext == ".nw";
      if (isRnw)
      {
         // attempt to weave the rnw
         rnw_weave::runWeave(targetFilePath_,
                             magicComments_,
                             enqueOutputEvent,
                             boost::bind(
                              &AsyncPdfCompiler::onWeaveCompleted,
                                 AsyncPdfCompiler::shared_from_this(), _1));
      }
      else
      {
         runLatexCompiler();
      }

   }

private:

   void onWeaveCompleted(const rnw_weave::Result& result)
   {
      if (result.succeeded)
         runLatexCompiler(result.concordances);
      else if (!result.errorLogEntries.empty())
         terminateWithErrorLogEntries(result.errorLogEntries);
      else
         terminateWithError(result.errorMessage);
   }

   void runLatexCompiler(const rnw_concordance::Concordances& concordances =
                                            rnw_concordance::Concordances())
   {
      // configure pdflatex options
      pdflatex::PdfLatexOptions options;
      options.fileLineError = false;
      options.syncTex = true;
      options.shellEscape = userSettings().enableLaTeXShellEscape();

      // get back-end version info
      core::system::ProcessResult result;
      Error error = core::system::runProgram(
                  string_utils::utf8ToSystem(texProgramPath_.absolutePath()),
                  core::shell_utils::ShellArgs() << "--version",
                  "",
                  core::system::ProcessOptions(),
                  &result);
      if (error)
         LOG_ERROR(error);
      else if (result.exitStatus != EXIT_SUCCESS)
         LOG_ERROR_MESSAGE("Error probing for latex version: "+ result.stdErr);
      else
         options.versionInfo = result.stdOut;

      // compute tex file path
      FilePath texFilePath = targetFilePath_.parent().complete(
                                                targetFilePath_.stem() +
                                                ".tex");

      // remove log files if they exist (avoids confusion created by parsing
      // old log files for errors)
      removeExistingLogs(texFilePath);

      // setup cleanup context if clean was specified
      if (userSettings().cleanTexi2DviOutput())
         auxillaryFileCleanupContext_.init(texFilePath);

      // run latex compile


      // try to use texi2dvi if we can
      if (userSettings().useTexi2Dvi() && tex::texi2dvi::isAvailable())
      {
         enqueOutputEvent("\nRunning texi2dvi on " +
                          texFilePath.filename() + "...\n");

         Error error = tex::texi2dvi::texToPdf(
                           texProgramPath_,
                           texFilePath,
                           options,
                           boost::bind(
                              &AsyncPdfCompiler::onLatexCompileCompleted,
                                 AsyncPdfCompiler::shared_from_this(),
                                 _1,
                                 texFilePath,
                                 concordances));
         if (error)
            terminateWithError("Unable to compile pdf: " + error.summary());
      }

      // call pdflatex directly (but still try to run bibtex as necessary)
      else
      {
         // this is our "simulated" texi2dvi -- this was originally
         // coded as a sequence of sync calls to pdflatex, bibtex, and
         // makeindex. re-coding it as async is going to be a bit
         // involved so considering that this is not the default
         // codepath we'll leave it sync for now (and then just call
         // the (typically) async callback function onLatexCompileCompleted
         // directly after the function returns

         enqueOutputEvent("\nRunning " + texProgramPath_.filename() +
                          " on " + texFilePath.filename() + "...\n");

         Error error = tex::pdflatex::texToPdf(texProgramPath_,
                                               texFilePath,
                                               options,
                                               &result);

         if (error)
         {
            terminateWithError("Unable to compile pdf: " + error.summary());
         }
         else
         {
            onLatexCompileCompleted(result.exitStatus,
                                    texFilePath,
                                    concordances);
         }
      }
   }

   void onLatexCompileCompleted(int exitStatus,
                                const FilePath& texFilePath,
                                const rnw_concordance::Concordances& concords)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         std::string pdfFile = module_context::createAliasedPath(
                                    ancillaryFilePath(texFilePath, ".pdf"));
         std::string completed = "\nCompile PDF completed: " + pdfFile + "\n";
         enqueOutputEvent(completed);

         if (onCompleted_)
            onCompleted_();
      }
      else
      {
         enqueOutputEvent("\n[Stopped: Errors encountered]\n");

         // don't remove the log
         auxillaryFileCleanupContext_.preserveLog();

         // try to show compilation errors -- if none are found then print
         // a general error message and stderr
         if (!showCompilationErrors(texFilePath, concords))
         {
            boost::format fmt("Error running %1% (exit code %2%)");
            std::string msg(boost::str(fmt % texProgramPath_.absolutePath()
                                           % exitStatus));
            enqueOutputEvent(msg + "\n");
         }
      }

      // fire event indicating completion
      enqueCompletedEvent();
   }

   void terminateWithError(const std::string& message)
   {
      enqueOutputEvent(message + "\n");
      enqueCompletedEvent();
   }

   void terminateWithErrorLogEntries(const core::tex::LogEntries& logEntries)
   {
      showLogEntries(logEntries);
      enqueCompletedEvent();
   }

   void enqueStartedEvent()
   {
      enqueStatusEvent(
               kStatusStarted,
               module_context::createAliasedPath(targetFilePath_));
   }

   void enqueCompletedEvent()
   {
      enqueStatusEvent(kStatusCompleted);
   }

private:
   const FilePath targetFilePath_;
   const boost::function<void()> onCompleted_;
   core::tex::TexMagicComments magicComments_;
   FilePath texProgramPath_;
   AuxillaryFileCleanupContext auxillaryFileCleanupContext_;
};


} // anonymous namespace


bool startCompile(const core::FilePath& targetFilePath,
                  const boost::function<void()>& onCompleted)
{
   if (!compile_pdf_supervisor::hasRunningChildren())
   {
      AsyncPdfCompiler::start(targetFilePath, onCompleted);
      return true;
   }
   else
   {
      return false;
   }
}

bool compileIsRunning()
{
   return compile_pdf_supervisor::hasRunningChildren();
}

bool terminateCompile()
{
   Error error = compile_pdf_supervisor::terminateAll(
                                             boost::posix_time::seconds(1));
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   else
   {
      enqueOutputEvent("\n[Compile PDF Stopped]\n");
      return true;
   }
}

void notifyTabClosed()
{
   s_compilePdfState.clear();
}

json::Object currentStateAsJson()
{
   return s_compilePdfState.asJson();
}

Error initialize()
{
   // register suspend handler
   using namespace module_context;
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   return Success();
}

} // namespace compile_pdf
} // namespace tex
} // namespace modules
} // namesapce session

