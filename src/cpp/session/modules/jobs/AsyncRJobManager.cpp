/*
 * AsyncRJobManager.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "AsyncRJobManager.hpp"

#include <r/RExec.hpp>

#include <session/jobs/JobsApi.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace jobs {
namespace {

// Vector of currently active async R jobs
std::vector<boost::shared_ptr<AsyncRJob> > s_jobs;

} // anonymous namespace


AsyncRJob::AsyncRJob(const std::string& name):
   completed_(false),
   cancelled_(false),
   name_(name)
{
}

void AsyncRJob::registerJob()
{
   Error error;
   r::sexp::Protect protect;

   // create the actions list
   SEXP actions = R_NilValue;
   error = r::exec::RFunction(".rs.scriptActions").call(&actions, &protect);
   if (error)
      LOG_ERROR(error);

   // add the job -- currently idle until we get some content from it
   job_ = addJob(name_, "", "", 0, true, JobIdle, JobTypeSession, false, actions, JobActions(), true, {});
}

void AsyncRJob::onStderr(const std::string& output)
{
   if (job_)
      job_->addOutput(output, true);
}

void AsyncRJob::onStdout(const std::string& output)
{
   if (job_)
      job_->addOutput(output, false);
}

Error AsyncRJob::reset()
{
   // clear progress-related state
   completed_ = false;
   cancelled_ = false;

   // reset underlying job
   if (job_)
   {
      return job_->reset();
   }

   return Success();
}

Error AsyncRJob::replay()
{
   // this base implementation does not know how to replay itself
   Error error = systemError(boost::system::errc::not_supported, ERROR_LOCATION);
   if (job_)
   {
      error.addProperty("id", job_->id());
   }
   error.addProperty("description", "Job does not support replay");
   return error;
}

void AsyncRJob::onCompleted(int exitStatus)
{
   // if the job has not yet been marked complete, do so now
   if (!job_->complete())
   {
      if (cancelled_)
      {
         // if we know that this job exited due to a cancel, set that state
         setJobState(job_, JobState::JobCancelled);
      }
      else
      {
         // otherwise infer state from the exit status
         setJobState(job_, exitStatus == 0 ?  
            JobState::JobSucceeded : JobState::JobFailed);
      }
   }
   
   // run all finalizers
   for (const auto& onComplete: onComplete_)
   {
      onComplete();
   }
}

std::string AsyncRJob::id()
{
   if (job_)
      return job_->id();
   return "";
}

void AsyncRJob::cancel()
{
   if (job_)
   {
      cancelled_ = true;
      // request terminate
      terminate();
   }
}

void AsyncRJob::addOnComplete(boost::function<void()> onComplete)
{
   onComplete_.push_back(onComplete);
}

Error registerAsyncRJob(boost::shared_ptr<AsyncRJob> job,
      std::string *pId)
{
   // create the job; note that once registered the job remains registered until the session ends,
   // so that it can be replayed if requested
   job->registerJob();

   // return and register the job
   if (pId != nullptr)
      *pId = job->id();

   s_jobs.push_back(job);

   return Success();
}

core::Error getAsyncRJob(const std::string& id, boost::shared_ptr<AsyncRJob> *pJob)
{
   for (auto job: s_jobs)
   {
      if (job->id() == id)
      {
         *pJob = job;
         return Success();
      }
   }

   // indicate that we didn't find the job
   Error error = systemError(boost::system::errc::no_such_file_or_directory, 
         ERROR_LOCATION);
   error.addProperty("id", id);
   return error;
}


core::Error stopAsyncRJob(const std::string& id)
{
   boost::shared_ptr<AsyncRJob> pJob;
   Error error = getAsyncRJob(id, &pJob);
   if (error)
   {
      return error;
   }

   pJob->cancel();
   return Success();
}

core::Error replayAsyncRJob(const std::string& id)
{
   boost::shared_ptr<AsyncRJob> pJob;
   Error error = getAsyncRJob(id, &pJob);
   if (error)
   {
      return error;
   }

   return pJob->replay();
}
 
} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio


