/*
 * SessionCompilePdfSupervisor.cpp
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

#include "SessionCompilePdfSupervisor.hpp"

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace tex {
namespace compile_pdf_supervisor {

namespace {

// supervisor is a module level static so that we can terminateChildren
// upon exit of the session (otherwise we could leave a long running
// operation still hogging cpu after we exit)
core::system::ProcessSupervisor s_processSupervisor;

void onBackgroundProcessing(bool)
{
   s_processSupervisor.poll();
}

void onShutdown(bool)
{
   // send kill signal
   s_processSupervisor.terminateAll();

   // wait and reap children (but for no longer than 1 second)
   if (!s_processSupervisor.wait(boost::posix_time::milliseconds(10),
                                 boost::posix_time::milliseconds(1000)))
   {
      LOG_WARNING_MESSAGE("Compile PDF supervisor didn't terminate in <1 sec");
   }
}

// define class which accumulates output and passes it to onExited
class CB : boost::noncopyable
{
public:
   CB(const boost::function<void(const std::string&)>& onOutput,
      const boost::function<void(int,const std::string&)>& onExited)
         : onOutput_(onOutput), onExited_(onExited)
   {
   }
   virtual ~CB() {}

public:
   void onOutput(const std::string& output)
   {
      onOutput_(output);
      output_ += output;
   }

   void onExit(int exitStatus)
   {
      onExited_(exitStatus, output_);
   }

private:
   std::string output_;
   boost::function<void(const std::string&)> onOutput_;
   boost::function<void(int,const std::string&)> onExited_;
};


} // anonymous namespace

bool hasRunningChildren()
{
   return s_processSupervisor.hasRunningChildren();
}

Error terminateAll(const boost::posix_time::time_duration& waitDuration)
{
   // send the kill signals
   s_processSupervisor.terminateAll();

   // wait for the processes to exit
   if (!s_processSupervisor.wait(boost::posix_time::milliseconds(100),
                                 waitDuration))
   {
      return systemError(boost::system::errc::timed_out,
                         "CompilePDF didn't terminate within timeout interval",
                         ERROR_LOCATION);
   }
   else
   {
      return Success();
   }
}

Error runProgram(const core::FilePath& programFilePath,
                 const std::vector<std::string>& args,
                 const core::system::Options& extraEnvVars,
                 const core::FilePath& workingDir,
                 const boost::function<void(const std::string&)>& onOutput,
                 const boost::function<void(int,const std::string&)>& onExited)
{
   // get system program file path
   std::string programPath = string_utils::utf8ToSystem(
                                          programFilePath.absolutePath());

   // setup options
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   options.redirectStdErrToStdOut = true;
   core::system::Options env;
   core::system::getModifiedEnv(extraEnvVars, &env);
   options.environment = env;
   options.workingDir = workingDir;

   // setup callbacks
   boost::shared_ptr<CB> pCB(new CB(onOutput, onExited));
   core::system::ProcessCallbacks cb;
   cb.onStdout = cb.onStderr = boost::bind(&CB::onOutput, pCB, _2);
   cb.onExit = boost::bind(&CB::onExit, pCB, _1);

   // run process using supervisor
   return s_processSupervisor.runProgram(programPath, args, options, cb);
}

Error initialize()
{
   // subscribe to events
   module_context::Events& events = module_context::events();
   events.onBackgroundProcessing.connect(onBackgroundProcessing);
   events.onShutdown.connect(onShutdown);

   return Success();
}

} // namespace compile_pdf_supervisor
} // namespace tex
} // namespace modules
} // namespace session
} // namespace rstudio

