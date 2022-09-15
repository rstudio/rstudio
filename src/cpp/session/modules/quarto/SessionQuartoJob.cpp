/*
 * SessionQuartoJob.cpp
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

#include "SessionQuartoJob.hpp"

#include <shared_core/Error.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/jobs/JobsApi.hpp>

#include <session/SessionQuarto.hpp>
#include "SessionQuarto.hpp"

using namespace rstudio::core;
using namespace rstudio::session::module_context;
using namespace boost::placeholders;

namespace rstudio {
namespace session {

using namespace quarto;

namespace modules {
namespace quarto {

Error QuartoJob::start()
{
   // options
   core::system::ProcessOptions options;
#ifdef _WIN32
   options.createNewConsole = true;
#else
   options.terminateChildren = true;
#endif
   options.workingDir = workingDir();
   options.callbacksRequireMainThread = true;

   // set environment variables
   core::system::Options env;
   core::system::environment(&env);
   environment(&env);
   options.environment = env;

   // this runs in the job pane as a child process of this process
   core::system::setenv(&(options.environment.get()), "RSTUDIO_CHILD_PROCESS_PANE", "job");
   core::system::setenv(&(options.environment.get()), "RSTUDIO_SESSION_PID", safe_convert::numberToString(::getpid()));
   
   // callbacks
   core::system::ProcessCallbacks cb;
   cb.onStarted = boost::bind(&QuartoJob::onStarted, QuartoJob::shared_from_this(), _1);
   cb.onContinue = boost::bind(&QuartoJob::onContinue,
                               QuartoJob::shared_from_this());
   cb.onStdout = boost::bind(&QuartoJob::onStdOut,
                             QuartoJob::shared_from_this(), _2);
   cb.onStderr = boost::bind(&QuartoJob::onStdErr,
                             QuartoJob::shared_from_this(), _2);
   cb.onExit =  boost::bind(&QuartoJob::onCompleted,
                             QuartoJob::shared_from_this(), _1);

   Error error = processSupervisor().runProgram(
            string_utils::utf8ToSystem(quartoExecutablePath()),
            args(),
            options,
            cb);

   if (error)
      return error;

   // create job and emit some output (to prevent the "has not emitted output" message)
   using namespace jobs;
   JobActions jobActions;
   // note that we pass raw 'this' b/c the "stop" action will never be executed after we
   // hit onCompleted (because our status won't be "running"). if we passed shared_from_this
   // then we'd be keeping this object around forever (because jobs are never discarded).
   jobActions.push_back(std::make_pair("stop", boost::bind(&QuartoJob::stop, this)));
   pJob_ = addJob(name(), "", "", 0, false, JobRunning, JobTypeSession, false, R_NilValue, jobActions, true, {});
   pJob_->addOutput("\n", true);

   // return success
   return Success();
}

ParsedServerLocation quartoServerLocationFromOutput(const std::string& output)
{
   boost::regex browseRe("http:\\/\\/localhost:(\\d{2,})\\/(web\\/viewer\\.html)?");
   boost::smatch match;
   if (regex_utils::search(output, match, browseRe))
   {
      int port = safe_convert::stringTo<int>(match[1], 0);
      std::string path = match.size() > 2 ? std::string(match[2]) : "";
      std::string filteredOutput =  boost::regex_replace(output, browseRe, "");
      return ParsedServerLocation(port, path, filteredOutput);
   }
   else
   {
      return ParsedServerLocation(output);
   }
}

} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio
