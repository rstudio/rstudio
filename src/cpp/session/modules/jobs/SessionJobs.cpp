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

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace jobs {

namespace {

// map of job ID to jobs
std::map<std::string, boost::shared_ptr<Job> > s_jobs;

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
   bool running       = r::sexp::asLogical(runningSEXP);
   
   // find an unused job id
   std::string id;
   do
   {
      id = core::system::generateShortenedUuid();
   }
   while(s_jobs.find(id) != s_jobs.end());

   // create the job!
   boost::shared_ptr<Job> pJob = boost::make_shared<Job>(
         id, name, status, group, 0 /* completed units */, progress, running, 
         false /* job complete */);

   // cache job and return its id
   r::sexp::Protect protect;
   s_jobs[id] = pJob;
   return r::sexp::create(id, &protect);
}

SEXP rs_removeJob(SEXP jobSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (lookupJob(jobSEXP, &pJob))
   {
      s_jobs.erase(s_jobs.find(pJob->id()));
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
   pJob->setProgress(units);

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

   return R_NilValue;
}

SEXP rs_setJobStatus(SEXP jobSEXP, SEXP statusSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(jobSEXP, &pJob))
      return R_NilValue;

   pJob->setStatus(r::sexp::safeAsString(statusSEXP));

   return R_NilValue;
}

SEXP rs_setJobRunning(SEXP jobSEXP, SEXP runningSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(jobSEXP, &pJob))
      return R_NilValue;

   pJob->setRunning(r::sexp::asLogical(runningSEXP));

   return R_NilValue;
}

Error getJobs(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   json::Object jobs;

   // convert all jobs to json
   for (auto& job: s_jobs)
   {
      jobs[job.first] = job.second->toJson();
   }

   pResponse->setResult(jobs);

   return Success();
}

} // anonymous namespace

core::Error initialize()
{
   // register API handlers
   RS_REGISTER_CALL_METHOD(rs_addJob, 9);
   RS_REGISTER_CALL_METHOD(rs_removeJob, 1);
   RS_REGISTER_CALL_METHOD(rs_setJobProgress, 2);
   RS_REGISTER_CALL_METHOD(rs_addJobProgress, 2);
   RS_REGISTER_CALL_METHOD(rs_setJobStatus, 2);
   RS_REGISTER_CALL_METHOD(rs_setJobRunning, 2);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(module_context::registerRpcMethod, "get_jobs", getJobs))
      (bind(module_context::sourceModuleRFile, "SessionJobs.R"));

   return initBlock.execute();
}

} // namepsace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

