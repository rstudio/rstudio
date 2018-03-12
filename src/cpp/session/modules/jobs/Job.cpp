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

Job::Job():
   max_(0),
   progress_(0),
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

core::json::Object Job::toJson()
{
   return core::json::Object();
}

Job Job::fromJson(const core::json::Object& src)
{
   return Job();
}


} // namepsace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

