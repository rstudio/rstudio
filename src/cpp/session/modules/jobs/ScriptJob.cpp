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

#include <boost/asio.hpp>

#include <core/Algorithm.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>
 
#include "ScriptJob.hpp"
#include "JobsApi.hpp"

#define kProgressDelim  "__"
#define kProgressStart  kProgressDelim "rs_progress_0" kProgressDelim " " 
#define kProgressEnd    " " kProgressDelim "rs_progress_1" kProgressDelim
#define kProgressMiddle " " kProgressDelim " "

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {      
namespace jobs {

namespace {

class ScriptJob : public async_r::AsyncRProcess 
{
public:
   static boost::shared_ptr<ScriptJob> create(const FilePath& path, 
         boost::function<void()> onComplete)
   {
      boost::shared_ptr<ScriptJob> pJob(new ScriptJob(path, onComplete));
      pJob->start();
      return pJob;
   }
private:
   ScriptJob(const FilePath& path, boost::function<void()> onComplete):
      path_(path),
      completed_(false),
      onComplete_(onComplete)
   {
   }

   void start()
   {
      // form the command to send to R
      std::string cmd = "source('" +
         string_utils::singleQuotedStrEscape(
               session::options().modulesRSourcePath()
                                 .complete("SourceWithProgress.R").absolutePath()) + 
         "'); sourceWithProgress(script = '" +
         string_utils::singleQuotedStrEscape(path_.absolutePath()) +
         "', con = stdout())";
        
      core::system::Options environment;
      FilePath working = FilePath::safeCurrentPath(FilePath());
      async_r::AsyncRProcess::start(cmd.c_str(), environment, working,
                                    async_r::R_PROCESS_NO_RDATA);

      // add the job -- currently idle until we get some content from it
      job_ = addJob(path_.filename(), "", "", 0, JobIdle, false);
   }

   void onStdout(const std::string& output)
   {
      // split into lines
      std::vector<std::string> lines = core::algorithm::split(output, "\n");

      // examine each line and look for progress markers; for performance we do this using
      // piecewise string indexing rather than regular expressions
      for (auto line: lines)
      {
         // initialize string positions (where we found the progress marker)
         size_t start = std::string::npos, 
                middle = std::string::npos, 
                end = std::string::npos;

         // look for the start of the progress marker
         start = line.find(kProgressStart);
         if (start != std::string::npos)
         {
            // ... and for the middle ...
            middle = line.find(kProgressMiddle, 
                               start + std::strlen(kProgressStart));
            if (middle != std::string::npos)
            {
               // ... and finally the end.
               size_t end = line.find(kProgressEnd, middle + std::strlen(kProgressMiddle));
               if (end != std::string::npos)
               {
                  // we found all three, so split up the string accordingly.
                  size_t begin = start + std::strlen(kProgressStart);
                  std::string cat = line.substr(begin, middle - begin);
                  size_t mid = middle + std::strlen(kProgressMiddle);
                  std::string arg = line.substr(mid, end - mid);

                  // invoke the progress handler with our discoveries
                  onProgress(cat, arg);
               }
            }
         }
         if (job_)
         {
            if (start != std::string::npos)
            {
               // emit the output that occurred before the start marker
               job_->addOutput(line.substr(0, start), false);
            }
            if (end != std::string::npos)
            {
               // emit the output that ocurred after the end marker
               job_->addOutput(line.substr(end + std::strlen(kProgressEnd)), false);
            }
            if (start == std::string::npos && end == std::string::npos)
            {
               // no progress markers were found; just emit the whole line
               job_->addOutput(line, false);
            }
         }
      }
   }

   void onStderr(const std::string& output)
   {
      if (job_)
         job_->addOutput(output, true);
   }

   void onCompleted(int exitStatus)
   {
      // mark job state
      if (job_)
         setJobState(job_, exitStatus == 0 && completed_ ? JobSucceeded : JobFailed);

      // run caller-provided completion function
      onComplete_();
   }

   void onProgress(const std::string& cat, const std::string& argument)
   {
      // process argument according to category
      if (cat == "count")
      {
         // record job as started
         int count = safe_convert::stringTo<int>(argument, 0);
         job_->setProgressMax(count);

         // set state to running
         setJobState(job_, JobRunning);
      }
      else if (cat == "statement" && job_)
      {
         int statement = safe_convert::stringTo<int>(argument, 0);
         setJobProgress(job_, statement);
      }
      else if (cat == "completed")
      {
         completed_ = true;
      }
      else if (cat == "section" && job_)
      {
         setJobStatus(job_, argument);
      }
   }

   boost::shared_ptr<Job> job_;
   FilePath path_;
   FilePath progress_;
   bool completed_;
   boost::function<void()> onComplete_;
};


std::vector<boost::shared_ptr<ScriptJob> > s_scripts;

} // anonymous namespace

Error startScriptJob(const core::FilePath& path)
{
   boost::shared_ptr<ScriptJob> job = ScriptJob::create(
         module_context::resolveAliasedPath(path.absolutePath()),
         [&]() 
         { 
            // remove the script from the list of those running
            s_scripts.erase(std::remove(s_scripts.begin(), s_scripts.end(), job), s_scripts.end());
         });

   s_scripts.push_back(job);
   return Success();
}

} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

