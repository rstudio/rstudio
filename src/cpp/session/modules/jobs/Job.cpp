/*
 * SessionJob.cpp
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

#include <boost/make_shared.hpp>
#include <core/json/JsonRpc.hpp>

#include "Job.hpp"

#define kJobId          "id"
#define kJobName        "name"
#define kJobStatus      "status"
#define kJobProgress    "progress"
#define kJobMax         "max"
#define kJobRunning     "running"
#define kJobCompleted   "completed"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace jobs {

Job::Job(const std::string& id, 
         const std::string& name,
         const std::string& status,
         const std::string& group,
         int progress, 
         int max,
         bool running,
         bool completed):
   id_(id), 
   name_(name),
   status_(status),
   group_(group),
   progress_(progress),
   max_(max),
   running_(running),
   completed_(completed)
{
}

Job::Job():
   progress_(0),
   max_(0),
   running_(false),
   completed_(false)
{
}

std::string Job::id()
{
    return id_;
}

std::string Job::name()
{
    return name_;
}

std::string Job::status()
{
    return status_;
}

std::string Job::group()
{
    return group_;
}

int Job::progress()
{
    return progress_;
}

int Job::max()
{
    return max_;
}

bool Job::running()
{
    return running_;
}

bool Job::completed()
{
    return completed_;
}

json::Object Job::toJson()
{
   json::Object job;

   job[kJobId]        = id_;
   job[kJobName]      = name_;
   job[kJobStatus]    = status_;
   job[kJobProgress]  = progress_;
   job[kJobMax]       = max_;
   job[kJobRunning]   = running_;
   job[kJobCompleted] = completed_;

   return job;
}

Error Job::fromJson(const json::Object& src, boost::shared_ptr<Job> *pJobOut)
{
   boost::shared_ptr<Job> pJob = boost::make_shared<Job>();
   Error error = json::readObject(src,
      kJobId,        &pJob->id_,
      kJobName,      &pJob->name_,
      kJobStatus,    &pJob->status_,
      kJobProgress,  &pJob->progress_,
      kJobMax,       &pJob->max_,
      kJobRunning,   &pJob->running_,
      kJobCompleted, &pJob->completed_);
   if (error)
      return error;

   *pJobOut = pJob;
   return Success();
}

void Job::setProgress(int units)
{
   // ensure units doesn't exceed the max
   if (units > max())
      progress_ = max_;
   else
      progress_ = units;
}

void Job::setStatus(const std::string& status)
{
   status_ = status;
}

void Job::setRunning(bool running)
{
   running_ = running;
}

} // namepsace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

