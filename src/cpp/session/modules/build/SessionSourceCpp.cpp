/*
 * SessionSourceCpp.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include "SessionSourceCpp.hpp"

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/join.hpp>

#include <core/BoostSignals.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/StringUtils.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionBuildErrors.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {  
namespace modules {
namespace build {
namespace source_cpp {

namespace {

struct SourceCppState
{
   bool empty() const { return errors.isEmpty() && outputs.isEmpty(); }

   void clear()
   {
      targetFile.clear();
      errors.clear();
      outputs.clear();
   }

   void addOutput(int type, const std::string& output)
   {
      using namespace module_context;
      outputs.push_back(compileOutputAsJson(CompileOutput(type,output)));
   }

   json::Value asJson() const
   {
      json::Object stateJson;
      stateJson["target_file"] = targetFile;
      stateJson["outputs"] = outputs;
      stateJson["errors"] = errors;
      return std::move(stateJson);
   }

   std::string targetFile;
   json::Array errors;
   json::Array outputs;
};

void enqueSourceCppStarted()
{
   ClientEvent event(client_events::kSourceCppStarted);
   module_context::enqueClientEvent(event);
}

void enqueSourceCppCompleted(const FilePath& sourceFile,
                             const std::string& output,
                             const std::string& errorOutput)
{
   // reset last sourceCpp state with new data
   using namespace module_context;
   SourceCppState sourceCppState;
   sourceCppState.targetFile = module_context::createAliasedPath(sourceFile);
   sourceCppState.addOutput(kCompileOutputNormal, output);
   sourceCppState.addOutput(kCompileOutputError, errorOutput);

   // parse errors
   std::string allOutput = output + "\n" + errorOutput;
   CompileErrorParser errorParser = gccErrorParser(sourceFile.getParent());
   std::vector<SourceMarker> errors = errorParser(allOutput);
   sourceCppState.errors = sourceMarkersAsJson(errors);

   // enque event
   ClientEvent event(client_events::kSourceCppCompleted,
                     sourceCppState.asJson());
   module_context::enqueClientEvent(event);
}


class SourceCppContext : boost::noncopyable
{
private:
   SourceCppContext() {}
   friend SourceCppContext& sourceCppContext();

public:
   bool onBuild(const FilePath& sourceFile, bool fromCode, bool showOutput)
   {
      // always clear state before starting a new build
      reset();

      // capture params
      sourceFile_ = sourceFile;
      fromCode_ = fromCode;
      showOutput_ = showOutput;

      // fixup path if necessary
      std::string path = core::system::getenv("PATH");
      std::string newPath = path;
      if (module_context::addRtoolsToPathIfNecessary(&newPath, &rToolsWarning_))
      {
          previousPath_ = path;
          core::system::setenv("PATH", newPath);
      }

      // capture all output that goes to the console
      module_context::events().onConsoleOutput.connect(
            boost::bind(&SourceCppContext::onConsoleOutput, this, _1, _2));

      // enque build started
      enqueSourceCppStarted();

      // return true to indicate it's okay to build
      return true;
   }

   void onBuildComplete(bool succeeded, const std::string& output)
   {
      // defer handling of build complete so we make sure to get all of the
      // stderr output from console std stream capture
      module_context::scheduleDelayedWork(
               boost::posix_time::milliseconds(200),
               boost::bind(&SourceCppContext::handleBuildComplete,
                           this, succeeded, output),
               true); // idle only
   }

private:

   void handleBuildComplete(bool succeeded, const std::string& output)
   {
      // restore previous path
      if (!previousPath_.empty())
         core::system::setenv("PATH", previousPath_);

      // collect all build output (do this before r tools warning so
      // it's output doesn't end up in consoleErrorBuffer_)
      std::string buildOutput;
      if (!succeeded || showOutput_)
         buildOutput = consoleOutputBuffer_;
      else
         buildOutput = output;

      // if we failed and there was an R tools warning then show it
      if (!succeeded)
      {
         if (!rToolsWarning_.empty())
            module_context::consoleWriteError(rToolsWarning_);

         // prompted install of Rtools on Win32
#ifdef _WIN32
         if (!module_context::canBuildCpp())
            module_context::installRBuildTools("Compiling C/C++ code for R");
#endif
      }

      // parse for gcc errors for sourceCpp
      if (!fromCode_)
         enqueSourceCppCompleted(sourceFile_, buildOutput, consoleErrorBuffer_);

      // reset state
      reset();
   }


   void onConsoleOutput(module_context::ConsoleOutputType type,
                        std::string output)
   {
#ifdef _WIN32
      // on windows make sure that output ends with a newline (because
      // standard output and error both come in on the same channel not
      // separated by newlines which prevents us from parsing errors)
      if (!boost::algorithm::ends_with(output, "\n"))
         output += "\n";
#endif

      if (type == module_context::ConsoleOutputNormal)
         consoleOutputBuffer_.append(output);
      else
         consoleErrorBuffer_.append(output);
   }

   void reset()
   {
      sourceFile_ = FilePath();
      showOutput_ = false;
      fromCode_ = false;
      consoleOutputBuffer_.clear();
      consoleErrorBuffer_.clear();
      module_context::events().onConsoleOutput.disconnect(
         boost::bind(&SourceCppContext::onConsoleOutput, this, _1, _2));
      previousPath_.clear();
      rToolsWarning_.clear();
   }

private:
   FilePath sourceFile_;
   bool showOutput_;
   bool fromCode_;
   std::string consoleOutputBuffer_;
   std::string consoleErrorBuffer_;
   std::string previousPath_;
   std::string rToolsWarning_;
};

SourceCppContext& sourceCppContext()
{
   static SourceCppContext instance;
   return instance;
}



SEXP rs_sourceCppOnBuild(SEXP sFile, SEXP sFromCode, SEXP sShowOutput)
{
   std::string file = r::sexp::asString(sFile);
   FilePath filePath(string_utils::systemToUtf8(file));
   bool fromCode = r::sexp::asLogical(sFromCode);
   bool showOutput = r::sexp::asLogical(sShowOutput);

   bool doBuild = sourceCppContext().onBuild(filePath, fromCode, showOutput);

   r::sexp::Protect rProtect;
   return r::sexp::create(doBuild, &rProtect);
}

SEXP rs_sourceCppOnBuildComplete(SEXP sSucceeded, SEXP sOutput)
{
   bool succeeded = r::sexp::asLogical(sSucceeded);

   std::string output;
   if (sOutput != R_NilValue)
   {
      std::vector<std::string> outputLines;
      Error error = r::sexp::extract(sOutput, &outputLines);
      if (error)
         LOG_ERROR(error);
      output = boost::algorithm::join(outputLines, "\n");
   }

   sourceCppContext().onBuildComplete(succeeded, output);

   return R_NilValue;
}


} // anonymous namespace


Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_sourceCppOnBuild);
   RS_REGISTER_CALL_METHOD(rs_sourceCppOnBuildComplete);
   return Success();
}

} // namespace source_cpp
} // namespace build
} // namespace modules
} // namespace session
} // namespace rstudio
