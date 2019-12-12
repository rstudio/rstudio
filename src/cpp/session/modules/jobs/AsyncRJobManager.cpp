/*
 * AsyncRJobManager.cpp
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

#include "AsyncRJobManager.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace jobs {
namespace {

std::vector<boost::shared_ptr<AsyncRJob> > s_jobs;

} // anonymous namespace


AsyncRJob::AsyncRJob():
   completed_(false),
   cancelled_(false)
{
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

void AsyncRJob::onCompleted(int exitStatus)
{
   onComplete_();
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

Error startAsyncRJob(boost::shared_ptr<AsyncRJob> job,
      std::string *pId)
{
   // remove the job from the registry when it's done
   job->setOnComplete([&]() 
   { 
      // remove the job from the list of those running
      s_scripts.erase(std::remove(s_jobs.begin(), s_jobs.end(), job), s_jobs.end());
   });

   // start the job now
   job->start();

   // return and register the job
   if (pId != nullptr)
      *pId = job->id();

   s_jobs.push_back(job);

   return Success();
}

core::Error stopAsyncRJob(const std::string& id)
{
   for (auto job: s_jobs)
   {
      if (job->id() == id)
      {
         job->cancel();
         return Success();
      }
   }

   // indicate that we didn't find the job
   Error error = systemError(boost::system::errc::no_such_file_or_directory, 
         ERROR_LOCATION);
   error.addProperty("id", id);
   return error;
}
 
} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio


