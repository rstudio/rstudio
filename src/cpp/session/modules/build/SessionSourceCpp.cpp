/*
 * SessionSourceCpp.cpp
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

#include "SessionSourceCpp.hpp"

#include <boost/signal.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {  
namespace modules {
namespace build {
namespace source_cpp {

namespace {

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

      // capture source file
      sourceFile_ = sourceFile;

      // if this is from code then don't do anything
      if (fromCode)
         return true;

      // capture all output that goes to the console
      module_context::events().onConsoleOutput.connect(
            boost::bind(&SourceCppContext::onConsoleOutput, this, _1, _2));

      return true;
   }

   void onBuildComplete(bool succeeded, const std::string& output)
   {
      // defer handling of build complete so we make sure to get all of the
      // stderr output from console std stream capture
      module_context::scheduleIncrementalWork(
               boost::posix_time::milliseconds(100),
               boost::bind(&SourceCppContext::handleBuildComplete,
                           this, succeeded, output));
   }

private:

   bool handleBuildComplete(bool succeeded, const std::string& output)
   {
      // collect all build output
      std::string buildOutput = output + consoleOutputBuffer_;

      // reset state
      reset();

      // one time only
      return false;
   }


   void onConsoleOutput(module_context::ConsoleOutputType type,
                        const std::string& output)
   {
      consoleOutputBuffer_.append(output);
   }

   void reset()
   {
      sourceFile_ = FilePath();
      consoleOutputBuffer_.clear();
      module_context::events().onConsoleOutput.disconnect(
         boost::bind(&SourceCppContext::onConsoleOutput, this, _1, _2));
   }

private:
   FilePath sourceFile_;
   std::string consoleOutputBuffer_;
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
   std::string output = sOutput != R_NilValue ? r::sexp::asString(sOutput) : "";

   sourceCppContext().onBuildComplete(succeeded, output);

   return R_NilValue;
}


} // anonymous namespace


Error initialize()
{

   R_CallMethodDef sourceCppOnBuildMethodDef ;
   sourceCppOnBuildMethodDef.name = "rs_sourceCppOnBuild" ;
   sourceCppOnBuildMethodDef.fun = (DL_FUNC)rs_sourceCppOnBuild ;
   sourceCppOnBuildMethodDef.numArgs = 3;
   r::routines::addCallMethod(sourceCppOnBuildMethodDef);

   R_CallMethodDef sourceCppOnBuildCompleteMethodDef ;
   sourceCppOnBuildCompleteMethodDef.name = "rs_sourceCppOnBuildComplete";
   sourceCppOnBuildCompleteMethodDef.fun = (DL_FUNC)rs_sourceCppOnBuildComplete;
   sourceCppOnBuildCompleteMethodDef.numArgs = 2;
   r::routines::addCallMethod(sourceCppOnBuildCompleteMethodDef);





   return Success();
}

} // namespace source_cpp
} // namespace build
} // namespace modules
} // namespace session
