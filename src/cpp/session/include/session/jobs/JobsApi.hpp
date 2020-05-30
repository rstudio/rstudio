/*
 * JobsApi.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef SESSION_JOBS_HPP
#define SESSION_JOBS_HPP

#include <session/jobs/Job.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules {      
namespace jobs {

boost::shared_ptr<Job> addJob(
      const std::string& id,
      time_t recorded,
      time_t started,
      time_t completed,
      const std::string& name,
      const std::string& status,
      const std::string& group,
      int progress,
      JobState state,
      JobType type,
      const std::string& cluster,
      bool autoRemove,
      SEXP actions,
      bool show,
      bool saveOutput,
      std::vector<std::string> tags);

boost::shared_ptr<Job> addJob(
      const std::string& name,
      const std::string& status,
      const std::string& group,
      int progress,
      JobState state,
      JobType type,
      bool autoRemove,
      SEXP actions,
      bool show,
      std::vector<std::string> tags);

void removeJob(boost::shared_ptr<Job> pJob);

bool lookupJob(const std::string& id, boost::shared_ptr<Job> *pJob);

void setJobProgress(boost::shared_ptr<Job> pJob, int units);

void setJobProgressMax(boost::shared_ptr<Job> pJob, int max);

void setJobState(boost::shared_ptr<Job> pJob, JobState state);

void setJobStatus(boost::shared_ptr<Job> pJob, const std::string& status);

core::json::Object jobsAsJson();

void removeAllJobs();

void removeAllLocalJobs();

void removeAllLauncherJobs();

void removeCompletedLocalJobs();

void endAllJobStreaming();

bool localJobsRunning();

} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
