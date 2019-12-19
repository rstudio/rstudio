/*
 * ShinyAsyncJob.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#define kShinyAppStarted "Shiny started; listening on URL: " 

#include "ShinyAsyncJob.hpp"
#include "../SessionShinyViewer.hpp"

#include <session/jobs/JobsApi.hpp>

#include <session/SessionUrlPorts.hpp>
#include <session/SessionModuleContext.hpp>


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace shiny {

ShinyAsyncJob::ShinyAsyncJob(const std::string& name, 
      const FilePath& path,
      const std::string& viewerType,
      const std::string& runCmd):
   AsyncRJob(name),
   path_(path),
   viewerType_(viewerType),
   runCmd_(runCmd),
   running_(false)
{
}

void ShinyAsyncJob::start()
{
   // set a status before we start R
   jobs::setJobStatus(job_, "Starting");

   // create the string to send to R
   std::string cmd(
         "options(shiny.launch.browser = function(url) { "
         "   cat(\"" kShinyAppStarted "\", url)"
         "}); " +
         runCmd_);

   core::system::Options environment;
   async_r::AsyncRProcess::start(cmd.c_str(), environment, path_.getParent(), 
         async_r::AsyncRProcessOptions::R_PROCESS_NO_RDATA);
}

void ShinyAsyncJob::enqueueStateEvent(const std::string& state)
{
   json::Object dataJson;
   dataJson["url"] = url_ports::mapUrlPorts(url_);
   dataJson["path"] = module_context::createAliasedPath(path_);
   dataJson["state"] = "started";
   dataJson["viewer"] = viewerType_;
   dataJson["options"] = shiny_viewer::SHINY_VIEWER_OPTIONS_NONE;
   dataJson["id"] = job_->id();
   ClientEvent event(client_events::kShinyViewer, dataJson);
   module_context::enqueClientEvent(event);
}

void ShinyAsyncJob::onStdout(const std::string& output)
{
   size_t pos = output.find(kShinyAppStarted);
   if (pos != std::string::npos)
   {
      // extract the URL, which is the portion of the output string following the start token
      url_ = output.substr(pos);

      // create an event to let the client know to start viewing the running application
      enqueueStateEvent("started");

      // set the job state so the Jobs tab will show the app 
      jobs::setJobStatus(job_, "Running");

      // no need to echo this to the user
      return;
   }

   // forward output to base class so it can be emitted to the client
   AsyncRJob::onStdout(output);
}

void ShinyAsyncJob::onCompleted(int exitStatus)
{
   if (running_)
   {
      // if the app is still running, tell the client it stopped
      // TODO: maybe not necessary -- shiny disconnect should do this for us
      running_ = false;
   }

   AsyncRJob::onCompleted(exitStatus);
}
                      
} // namespace shiny
} // namespace modules
} // namespace session
} // namespace rstudio


