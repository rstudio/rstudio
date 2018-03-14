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
#define kJobState       "state"

#define kJobStateIdle      "idle"
#define kJobStateRunning   "running"
#define kJobStateSucceeded "succeeded"
#define kJobStateCancelled "cancelled"
#define kJobStateFailed    "failed"

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
         JobState state):
   id_(id), 
   name_(name),
   status_(status),
   group_(group),
   state_(state),
   progress_(progress),
   max_(max)
{
}

Job::Job():
   state_(JobIdle),
   progress_(0),
   max_(0)
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

JobState Job::state()
{
    return state_; 
}

json::Object Job::toJson()
{
   json::Object job;

   // fill out fields from local information
   job[kJobId]       = id_;
   job[kJobName]     = name_;
   job[kJobStatus]   = status_;
   job[kJobProgress] = progress_;
   job[kJobMax]      = max_;
   job[kJobState]    = static_cast<int>(state_);

   // append description
   job["state_description"] = stateAsString(state_);

   return job;
}

Error Job::fromJson(const json::Object& src, boost::shared_ptr<Job> *pJobOut)
{
   boost::shared_ptr<Job> pJob = boost::make_shared<Job>();
   int state = static_cast<int>(JobIdle);
   Error error = json::readObject(src,
      kJobId,       &pJob->id_,
      kJobName,     &pJob->name_,
      kJobStatus,   &pJob->status_,
      kJobProgress, &pJob->progress_,
      kJobMax,      &pJob->max_,
      kJobState,    &state);
   if (error)
      return error;

   pJob->setState(static_cast<JobState>(state));

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

void Job::setState(JobState state)
{
   state_ = state;
}

std::string Job::stateAsString(JobState state)
{
   switch(state)
   {
      case JobIdle:      return kJobStateIdle;
      case JobRunning:   return kJobStateRunning;
      case JobSucceeded: return kJobStateSucceeded;
      case JobCancelled: return kJobStateCancelled;
      case JobFailed:    return kJobStateFailed;
      case JobInvalid:   return "";
   }
   return "";
}

JobState Job::stringAsState(const std::string& state)
{
   if (state == kJobStateIdle)            return JobIdle;
   else if (state == kJobStateRunning)    return JobRunning;
   else if (state == kJobStateSucceeded)  return JobSucceeded;
   else if (state == kJobStateCancelled)  return JobCancelled;
   else if (state == kJobStateFailed)     return JobFailed;

   return JobInvalid;
}

} // namepsace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

