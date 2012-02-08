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

#include <core/FilePath.hpp>
#include <core/Exec.hpp>
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

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace compile_pdf {

namespace {

void viewPdf(const FilePath& texPath)
{
   FilePath pdfPath = texPath.parent().complete(texPath.stem() + ".pdf");
   module_context::showFile(pdfPath, "_rstudio_compile_pdf");
}

void publishPdf(const FilePath& texPath)
{
   std::string aliasedPath = module_context::createAliasedPath(texPath);
   ClientEvent event(client_events::kPublishPdf, aliasedPath);
   module_context::enqueClientEvent(event);
}

void showLogEntry(const core::tex::LogEntry& logEntry)
{
   boost::format fmt("%1% (line %2%): %3%\n");
   std::string err = boost::str(
               fmt % logEntry.file() % logEntry.line() % logEntry.message());
   module_context::consoleWriteError(err);
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

bool showCompilationErrors(const FilePath& texPath)
{
   // latex log file
   core::tex::LogEntries latexLogEntries;
   FilePath logPath = latexLogPath(texPath);
   if (logPath.exists())
   {
      Error error = core::tex::parseLatexLog(logPath, &latexLogEntries);
      if (error)
         LOG_ERROR(error);

      // show errors
      if (!latexLogEntries.empty())
      {
         module_context::consoleWriteError("LaTeX errors:\n");
         std::for_each(latexLogEntries.begin(),
                       latexLogEntries.end(),
                       showLogEntry);
         module_context::consoleWriteError("\n");
      }
   }

   // bibtex log file
   core::tex::LogEntries bibtexLogEntries;
   logPath = bibtexLogPath(texPath);
   if (logPath.exists())
   {
      Error error = core::tex::parseBibtexLog(logPath, &bibtexLogEntries);
      if (error)
         LOG_ERROR(error);

      // show errors
      if (!bibtexLogEntries.empty())
      {
         module_context::consoleWriteError("BibTeX errors:\n");
         std::for_each(bibtexLogEntries.begin(),
                       bibtexLogEntries.end(),
                       showLogEntry);
         module_context::consoleWriteError("\n");
      }
   }

   // return true if we printed at least one entry
   return (latexLogEntries.size() + bibtexLogEntries.size()) > 0;
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

         // clean log if requested
         if (cleanLog_)
         {
            remove(".blg");
            remove(".log");
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

bool compilePdf(const FilePath& targetFilePath,
                const std::string& completedAction,
                std::string* pUserErrMsg)
{
   // set the working directory for the duration of the compile
   RestoreCurrentPathScope pathScope(module_context::safeCurrentPath());
   Error error = targetFilePath.parent().makeCurrentPath();
   if (error)
   {
      *pUserErrMsg = "Error setting current path: " + error.summary();
      return false;
   }

   // ensure no spaces in path
   std::string filename = targetFilePath.filename();
   if (filename.find(' ') != std::string::npos)
   {
      *pUserErrMsg = "Invalid filename: '" + filename +
                     "' (TeX does not understand paths with spaces)";
      return false;
   }

   // parse out magic comments
   core::tex::TexMagicComments magicComments;
   error = core::tex::parseMagicComments(targetFilePath, &magicComments);
   if (error)
      LOG_ERROR(error);

   // discover and validate tex program path
   FilePath texProgramPath;
   if (!pdflatex::latexProgramForFile(magicComments,
                                      &texProgramPath,
                                      pUserErrMsg))
   {
      return false;
   }

   // see if we need to sweave
   std::string ext = targetFilePath.extensionLowerCase();
   if (ext == ".rnw" || ext == ".snw" || ext == ".nw")
   {
      // attempt to weave the rnw
      bool success = rnw_weave::runWeave(targetFilePath,
                                         magicComments,
                                         pUserErrMsg);
      if (!success)
         return false;
   }

   // configure pdflatex options
   pdflatex::PdfLatexOptions options;
   options.fileLineError = true;
   options.syncTex = true;
   options.shellEscape = userSettings().enableLaTeXShellEscape();

   // get back-end version info
   core::system::ProcessResult result;
   error = core::system::runProgram(
                  string_utils::utf8ToSystem(texProgramPath.absolutePath()),
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
   FilePath texFilePath = targetFilePath.parent().complete(
                                             targetFilePath.stem() +
                                             ".tex");

   // remove log files if they exist (avoids confusion created by parsing
   // old log files for errors)
   removeExistingLogs(texFilePath);

   // setup cleanup context if clean was specified
   AuxillaryFileCleanupContext fileCleanupContext;
   if (userSettings().cleanTexi2DviOutput())
      fileCleanupContext.init(texFilePath);

   // run tex compile
   if (userSettings().useTexi2Dvi() && tex::texi2dvi::isAvailable())
   {
      error = tex::texi2dvi::texToPdf(texProgramPath,
                                      texFilePath,
                                      options,
                                      &result);
   }
   else
   {
      error = tex::pdflatex::texToPdf(texProgramPath,
                                      texFilePath,
                                      options,
                                      &result);
   }

   if (error)
   {
      *pUserErrMsg = "Unable to compile pdf: " + error.summary();
      return false;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      // don't remove the log
      fileCleanupContext.preserveLog();

      // try to show compilation errors -- if none are found then print
      // a general error message and stderr
      if (!showCompilationErrors(texFilePath))
      {
         boost::format fmt("Error running %1% (exit code %2%): %3%\n");
         std::string msg(boost::str(fmt % texProgramPath.absolutePath()
                                        % result.exitStatus
                                        % result.stdErr));
         module_context::consoleWriteError(msg);
      }
      return false;
   }
   else
   {
      if (completedAction == "view")
         viewPdf(targetFilePath);
      else if (completedAction == "publish")
         publishPdf(targetFilePath);

      return true;
   }
}

SEXP rs_compilePdf(SEXP filePathSEXP, SEXP completedActionSEXP)
{
   try
   {
      // extract parameters
      FilePath targetFilePath = module_context::resolveAliasedPath(
                                          r::sexp::asString(filePathSEXP));
      std::string completedAction = r::sexp::asString(completedActionSEXP);

      // compile pdf
      std::string userErrMsg;
      if (!compilePdf(targetFilePath, completedAction, &userErrMsg))
      {
         if (!userErrMsg.empty())
            throw r::exec::RErrorException(userErrMsg);
      }
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}

} // anonymous namespace


Error initialize()
{
   R_CallMethodDef compilePdfMethodDef;
   compilePdfMethodDef.name = "rs_compilePdf" ;
   compilePdfMethodDef.fun = (DL_FUNC) rs_compilePdf ;
   compilePdfMethodDef.numArgs = 2;
   r::routines::addCallMethod(compilePdfMethodDef);



   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionCompilePdf.R"));
   return initBlock.execute();

}


} // namespace compile_pdf
} // namespace tex
} // namespace modules
} // namesapce session

