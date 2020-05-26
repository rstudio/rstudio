/*
 * JobsApi.cpp
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

#include <session/jobs/JobsApi.hpp>
#include <session/SessionModuleContext.hpp>

enum JobUpdateType
{
   JobAdded   = 0,
   JobUpdated = 1,
   JobRemoved = 2
};

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {      
namespace jobs {

namespace {

// map of job ID to jobs
std::map<std::string, boost::shared_ptr<Job> > s_jobs;

// notify client that job has been updated
void notifyClient(JobUpdateType update, boost::shared_ptr<Job> pJob)
{
   json::Object data;
   data["type"] = static_cast<int>(update);
   data["job"]  = pJob->toJson();
   module_context::enqueClientEvent(
         ClientEvent(client_events::kJobUpdated, data));
}

void processUpdate(boost::shared_ptr<Job> pJob)
{
   if (pJob->complete() && pJob->autoRemove())
   {
      // if this job is now complete, and the job wants to be removed when complete, remove it 
      removeJob(pJob);
   }
   else
   {
      // otherwise, notify the client of the changes in the job
      notifyClient(JobUpdated, pJob);
   }
}

} // anonymous namespace

void removeJob(boost::shared_ptr<Job> pJob)
{
   notifyClient(JobRemoved, pJob);
   pJob->cleanup();
   s_jobs.erase(s_jobs.find(pJob->id()));
}

bool lookupJob(const std::string& id, boost::shared_ptr<Job> *pJob)
{
   auto it = s_jobs.find(id);
   if (it == s_jobs.end())
      return false;
   *pJob = it->second;
   return true;
}

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
      std::vector<std::string> tags)
{
   // find an unused job id
   std::string id;
   do
   {
      id = core::system::generateShortenedUuid();
   } while (s_jobs.find(id) != s_jobs.end());
   return addJob(id,
         ::time(0), /*recorded*/
         0, /*started*/
         0, /*completed*/
         name, status, group, progress, state, type,
         "" /*cluster*/,
         autoRemove, actions, show,
         true, /*saveOutput*/ 
         tags);
}

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
      std::vector<std::string> tags)
{
   // create the job!
   boost::shared_ptr<Job> pJob = boost::make_shared<Job>(
         id, recorded, started, completed, name, status, group, 0 /* completed units */,
         progress, state, type, cluster, autoRemove, actions, show, saveOutput, tags);

   // cache job and notify client
   s_jobs[id] = pJob;
   notifyClient(JobAdded, pJob);

   return pJob;
}

void setJobProgress(boost::shared_ptr<Job> pJob, int units)
{
   pJob->setProgress(units);
   processUpdate(pJob);
}

void setProgressMax(boost::shared_ptr<Job> pJob, int max)
{
   pJob->setProgressMax(max);
   processUpdate(pJob);
}

void setJobState(boost::shared_ptr<Job> pJob, JobState state)
{
   pJob->setState(state);
   processUpdate(pJob);
}

void setJobStatus(boost::shared_ptr<Job> pJob, const std::string& status)
{
   pJob->setStatus(status);
   processUpdate(pJob);
}

json::Object jobsAsJson()
{
   json::Object jobs;

   // convert all jobs to json
   for (auto& job: s_jobs)
   {
      jobs[job.first] = job.second->toJson();
   }

   return jobs;
}

void removeAllJobs()
{
   for (auto& job: s_jobs)
   {
      job.second->cleanup();
      notifyClient(JobRemoved, job.second);
   }
   s_jobs.clear();
}

void removeAllLocalJobs()
{
   for (auto it = s_jobs.cbegin(); it != s_jobs.cend() ; )
   {
      if (it->second->type() == JobType::JobTypeSession)
      {
         it->second->cleanup();
         notifyClient(JobRemoved, it->second);
         it = s_jobs.erase(it);
      }
      else
         ++it;
   }
}

void removeAllLauncherJobs()
{
   for (auto it = s_jobs.cbegin(); it != s_jobs.cend() ; )
   {
      if (it->second->type() == JobType::JobTypeLauncher)
      {
         it->second->cleanup();
         it = s_jobs.erase(it);
      }
      else
         ++it;
   }
}

void removeCompletedLocalJobs()
{
   // collect completed jobs
   std::vector<boost::shared_ptr<Job> > completed;
   for (auto& job: s_jobs)
   {
      if (job.second->complete() && job.second->type() == JobType::JobTypeSession)
         completed.push_back(job.second);
   }

   // and remove them!
   for (auto& job: completed)
   {
      removeJob(job);
   }
}

void endAllJobStreaming()
{
   for (auto& job: s_jobs)
   {
      job.second->setListening(false);
   }
}

bool localJobsRunning()
{
   for (auto& job: s_jobs)
   {
      if (job.second->type() == JobType::JobTypeSession && !job.second->complete())
      {
         return true;
      }
   }
   return false;
}

} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

