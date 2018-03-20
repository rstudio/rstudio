/*
 * SessionJobs.cpp
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

#include <string>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/system/System.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include "Job.hpp"
#include "SessionJobs.hpp"

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

void removeJob(boost::shared_ptr<Job> pJob)
{
   notifyClient(JobRemoved, pJob);
   pJob->cleanup();
   s_jobs.erase(s_jobs.find(pJob->id()));
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

bool lookupJob(SEXP jobSEXP, boost::shared_ptr<Job> *pJob)
{
   std::string id = r::sexp::safeAsString(jobSEXP, "");
   if (id.empty())
   {
      r::exec::error("A job ID must be specified.");
      return false;
   }
   auto it = s_jobs.find(id);
   if (it == s_jobs.end())
   {
      r::exec::error("Job ID '" + id + "' does not exist.");
      return false;
   }
   *pJob = it->second;
   return true;
}

SEXP rs_addJob(SEXP nameSEXP, SEXP statusSEXP, SEXP progressUnitsSEXP, SEXP actionsSEXP,
      SEXP estimateSEXP, SEXP estimateRemainingSEXP, SEXP runningSEXP, SEXP autoRemoveSEXP,
      SEXP groupSEXP) 
{
   // convert to native types
   std::string name   = r::sexp::safeAsString(nameSEXP, "");
   std::string status = r::sexp::safeAsString(statusSEXP, "");
   std::string group  = r::sexp::safeAsString(groupSEXP, "");
   int progress       = r::sexp::asInteger(progressUnitsSEXP);
   JobState state     = r::sexp::asLogical(runningSEXP) ? JobRunning : JobIdle;
   bool autoRemove    = r::sexp::asLogical(autoRemoveSEXP);
   
   // find an unused job id
   std::string id;
   do
   {
      id = core::system::generateShortenedUuid();
   }
   while(s_jobs.find(id) != s_jobs.end());

   // create the job!
   boost::shared_ptr<Job> pJob = boost::make_shared<Job>(
         id, name, status, group, 0 /* completed units */, progress, state, autoRemove); 

   // cache job and notify client
   s_jobs[id] = pJob;
   notifyClient(JobAdded, pJob);

   // return job id
   r::sexp::Protect protect;
   return r::sexp::create(id, &protect);
}

SEXP rs_removeJob(SEXP jobSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (lookupJob(jobSEXP, &pJob))
   {
      removeJob(pJob);
   }
   return R_NilValue;
}
      
SEXP rs_setJobProgress(SEXP jobSEXP, SEXP unitsSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(jobSEXP, &pJob))
      return R_NilValue;

   // test boundaries
   int units = r::sexp::asInteger(unitsSEXP);
   if (pJob->max() == 0)
   {
      r::exec::error("Cannot set progress for this job because it does not have a progress "
                     "maximum defined.");
      return R_NilValue;
   }
   if (units < pJob->progress())
   {
      r::exec::error("Progress cannot go backwards; specify a progress value more than the "
                     "current progress.");
      return R_NilValue;
   }

   // add progress
   pJob->setProgress(units);
   processUpdate(pJob);

   return R_NilValue;
}

SEXP rs_addJobProgress(SEXP jobSEXP, SEXP unitsSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(jobSEXP, &pJob))
      return R_NilValue;

   // test boundaries
   int units = r::sexp::asInteger(unitsSEXP);
   if (pJob->max() == 0)
   {
      r::exec::error("Cannot add more progress for this job because it does not have a progress "
                     "maximum defined.");
      return R_NilValue;
   }
   if (units < 0)
   {
      r::exec::error("Progress cannot go backwards; specify a positive progress value.");
      return R_NilValue;
   }

   // clamp to maximum number of units specified by job
   int total = pJob->progress() + units;
   if (total > pJob->max()) 
   {
      total = pJob->max();
   }
   pJob->setProgress(total);
   processUpdate(pJob);

   return R_NilValue;
}

SEXP rs_setJobStatus(SEXP jobSEXP, SEXP statusSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(jobSEXP, &pJob))
      return R_NilValue;

   pJob->setStatus(r::sexp::safeAsString(statusSEXP));
   processUpdate(pJob);

   return R_NilValue;
}

SEXP rs_setJobState(SEXP jobSEXP, SEXP stateSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(jobSEXP, &pJob))
      return R_NilValue;

   std::string state = r::sexp::safeAsString(stateSEXP);
   JobState jobState = Job::stringAsState(state);
   if (state.empty())
   {
      r::exec::error("The state '" + state + "' is not a valid job state.");
      return R_NilValue;
   }

   pJob->setState(jobState);
   processUpdate(pJob);

   return R_NilValue;
}

SEXP rs_addJobOutput(SEXP jobSEXP, SEXP outputSEXP, SEXP errorSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(jobSEXP, &pJob))
      return R_NilValue;

   pJob->addOutput(r::sexp::safeAsString(outputSEXP), r::sexp::asLogical(errorSEXP));
   return R_NilValue;
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

Error getJobs(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(jobsAsJson());

   return Success();
}

Error jobOutput(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   // extract job ID
   std::string id;
   int position;
   Error error = json::readParams(request.params, &id, &position);
   if (error)
      return error;

   // look up in cache
   auto it = s_jobs.find(id);
   if (it == s_jobs.end())
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   // show output
   pResponse->setResult(it->second->output(position));

   return Success();
}

void onSuspend(const r::session::RSuspendOptions&, core::Settings*)
{
}

void onResume(const Settings& settings)
{
   module_context::enqueClientEvent(
         ClientEvent(client_events::kJobRefresh, jobsAsJson()));
}

} // anonymous namespace

core::json::Object jobState()
{
   return jobsAsJson();
}

core::Error initialize()
{
   // register API handlers
   RS_REGISTER_CALL_METHOD(rs_addJob, 9);
   RS_REGISTER_CALL_METHOD(rs_removeJob, 1);
   RS_REGISTER_CALL_METHOD(rs_setJobProgress, 2);
   RS_REGISTER_CALL_METHOD(rs_addJobProgress, 2);
   RS_REGISTER_CALL_METHOD(rs_setJobStatus, 2);
   RS_REGISTER_CALL_METHOD(rs_setJobState, 2);
   RS_REGISTER_CALL_METHOD(rs_addJobOutput, 3);

   module_context::addSuspendHandler(module_context::SuspendHandler(
            onSuspend, onResume));

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(module_context::registerRpcMethod, "get_jobs", getJobs))
      (bind(module_context::registerRpcMethod, "job_output", jobOutput))
      (bind(module_context::sourceModuleRFile, "SessionJobs.R"));

   return initBlock.execute();
}

} // namepsace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

