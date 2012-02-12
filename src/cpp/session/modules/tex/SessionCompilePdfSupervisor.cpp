/*
 * SessionCompilePdfSupervisor.cpp
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

#include "SessionCompilePdfSupervisor.hpp"

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

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

} // anonymous namespace

bool hasRunningChildren()
{
   return s_processSupervisor.hasRunningChildren();
}

Error runProgram(const core::FilePath& programFilePath,
                 const std::vector<std::string>& args,
                 const core::system::Options& extraEnvVars,
                 const core::FilePath& workingDir,
                 const boost::function<void(const std::string&)>& onOutput,
                 const boost::function<void(int)>& onExited)
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
   core::system::ProcessCallbacks cb;
   cb.onStdout = boost::bind(onOutput, _2);
   cb.onStderr = boost::bind(onOutput, _2);
   cb.onExit = onExited;

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
} // namesapce session

