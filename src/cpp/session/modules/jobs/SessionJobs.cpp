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


SEXP rs_setJobProgress(SEXP jobSEXP, SEXP unitsSEXP)
{
   std::string id = r::sexp::safeAsString(jobId, "");
   int units = r::sexp::asInteger(unitsSEXP);
}
      
Error getJobs(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   json::Object result;

   // convert all jobs to json
   for (auto& job: s_jobs)
   {
      result[job.first] = job.second->toJson();
   }

   pResponse->setResult(result);

   return Success();
}

} // anonymous namespace

core::Error initialize()
{
   // register API handlers
   RS_REGISTER_CALL_METHOD(rs_addJob, 9);

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

