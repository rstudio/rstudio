/*
 * ScriptJob.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>
 
#include "ScriptJob.hpp"
#include "JobsApi.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {      
namespace jobs {

namespace {

class ScriptJob : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<ScriptJob> create(const FilePath& path)
   {
      boost::shared_ptr<ScriptJob> pJob(new ScriptJob(path));
      pJob->start();
      return pJob;
   }
private:
   ScriptJob(const FilePath& path):
      path_(path)
   {
   }

   void start()
   {
      // create a temporary file path to write 
      FilePath::tempFilePath(&progress_);

      // form the command to send to R
      std::string cmd = "source('" +
         string_utils::singleQuotedStrEscape(
               session::options().modulesRSourcePath()
                                 .complete("SourceWithProgress.R").absolutePath()) + 
         "'); sourceWithProgress('" +
         string_utils::singleQuotedStrEscape(path_.absolutePath()) +
         "', '" + 
         string_utils::singleQuotedStrEscape(progress_.absolutePath()) +
         "')";
        
      core::system::Options environment;
      FilePath working = FilePath::safeCurrentPath(FilePath());
      async_r::AsyncRProcess::start(cmd.c_str(), environment, working,
                                    async_r::R_PROCESS_NO_RDATA);

      // start job
      job_ = addJob(path_.filename(), "", "", 0, JobRunning, false);
   }

   void onStdout(const std::string& output)
   {
      job_->addOutput(output, false);
   }

   void onStderr(const std::string& output)
   {
      job_->addOutput(output, true);
   }

   void onCompleted(int exitStatus)
   {
      setJobState(job_, exitStatus == 0 ? JobSucceeded : JobFailed);
   }

   boost::shared_ptr<Job> job_;
   FilePath path_;
   FilePath progress_;
};


std::vector<boost::shared_ptr<ScriptJob> > s_scripts;

} // anonymous namespace

Error startScriptJob(const core::FilePath& path)
{
   boost::shared_ptr<ScriptJob> job = ScriptJob::create(path);
   s_scripts.push_back(job);
   return Success();
}

} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

