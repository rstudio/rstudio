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
#include "JobsApi.hpp"
#include "ScriptJob.hpp"
#include "SessionJobs.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace jobs {

namespace {

bool lookupJob(SEXP jobSEXP, boost::shared_ptr<Job> *pJob)
{
   std::string id = r::sexp::safeAsString(jobSEXP, "");
   if (id.empty())
   {
      r::exec::error("A job ID must be specified.");
      return false;
   }
   if (lookupJob(id, pJob))
   {
      return true;
   }
   r::exec::error("Job ID '" + id + "' does not exist.");
   return false;
}

SEXP rs_addJob(SEXP nameSEXP, SEXP statusSEXP, SEXP progressUnitsSEXP, SEXP actionsSEXP,
      SEXP estimateSEXP, SEXP estimateRemainingSEXP, SEXP runningSEXP, SEXP autoRemoveSEXP,
      SEXP groupSEXP, SEXP showSEXP) 
{
   // convert to native types
   std::string name   = r::sexp::safeAsString(nameSEXP, "");
   std::string status = r::sexp::safeAsString(statusSEXP, "");
   std::string group  = r::sexp::safeAsString(groupSEXP, "");
   int progress       = r::sexp::asInteger(progressUnitsSEXP);
   JobState state     = r::sexp::asLogical(runningSEXP) ? JobRunning : JobIdle;
   bool autoRemove    = r::sexp::asLogical(autoRemoveSEXP);
   bool show          = r::sexp::asLogical(showSEXP);
      
   // add the job
   boost::shared_ptr<Job> pJob =  
      addJob(name, status, group, progress, state, autoRemove, actionsSEXP, show);

   // return job id
   r::sexp::Protect protect;
   return r::sexp::create(pJob->id(), &protect);
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
   setJobProgress(pJob, units);

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
   setJobProgress(pJob, total);

   return R_NilValue;
}

SEXP rs_setJobStatus(SEXP jobSEXP, SEXP statusSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(jobSEXP, &pJob))
      return R_NilValue;

   setJobStatus(pJob, r::sexp::safeAsString(statusSEXP));

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
   
   setJobState(pJob, jobState);

   return R_NilValue;
}

SEXP rs_addJobOutput(SEXP jobSEXP, SEXP outputSEXP, SEXP errorSEXP)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(jobSEXP, &pJob))
      return R_NilValue;

   // save output to job
   std::string output = r::sexp::safeAsString(outputSEXP);
   bool error = r::sexp::asLogical(errorSEXP);
   pJob->addOutput(output, error);

   return R_NilValue;
}

SEXP rs_runScriptJob(SEXP path, SEXP encoding, SEXP dir, SEXP importEnv, SEXP exportEnv)
{
   r::sexp::Protect protect;
   std::string filePath = r::sexp::safeAsString(path, "");
   if (filePath.empty())
   {
      r::exec::error("Please specify the path to the R script to run.");
      return R_NilValue;
   }
   FilePath scriptPath(filePath);
   if (!scriptPath.exists())
   {
      r::exec::error("The script file '" + filePath + "' does not exist.");
   }

   std::string workingDir = r::sexp::safeAsString(dir);
   if (workingDir.empty())
   {
      // default working dir to parent directory of script
      workingDir = scriptPath.parent().absolutePath();
   }

   FilePath workingDirPath(workingDir);
   if (!workingDirPath.exists())
   {
      r::exec::error("The requested working directory '" + workingDir + "' does not exist.");
   }

   std::string id;
   startScriptJob(ScriptLaunchSpec(
            module_context::resolveAliasedPath(filePath),
            r::sexp::safeAsString(encoding),
            module_context::resolveAliasedPath(workingDir),
            r::sexp::asLogical(importEnv),
            r::sexp::safeAsString(exportEnv)), &id);

   return r::sexp::create(id, &protect);
}

SEXP rs_stopScriptJob(SEXP sexpId)
{
   std::string id = r::sexp::safeAsString(sexpId);
   Error error = stopScriptJob(id);
   if (error.code() == boost::system::errc::no_such_file_or_directory)
   {
      r::exec::error("The script job '" + id + "' was not found.");
   }
   else if (error)
   {
      r::exec::error("Error while stopping script job: " + error.summary());
   }
   return R_NilValue;
}

SEXP rs_executeJobAction(SEXP sexpId, SEXP action)
{
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(sexpId, &pJob))
      return R_NilValue;

   pJob->executeAction(r::sexp::safeAsString(action));

   return R_NilValue;
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
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(id, &pJob))
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   // show output
   pResponse->setResult(pJob->output(position));

   return Success();
}

Error runScriptJob(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   json::Object jobSpec;
   std::string path;
   std::string workingDir;
   std::string encoding;
   bool importEnv;
   std::string exportEnv;
   std::string id;
   Error error = json::readParams(request.params, &jobSpec);
   if (error)
      return error;

   error = json::readObject(jobSpec, "path", &path,
                                     "encoding", &encoding,
                                     "working_dir", &workingDir,
                                     "import_env", &importEnv,
                                     "export_env", &exportEnv);
   if (error)
      return error;

   startScriptJob(ScriptLaunchSpec(
            module_context::resolveAliasedPath(path),
            encoding,
            module_context::resolveAliasedPath(workingDir),
            importEnv,
            exportEnv), &id);

   pResponse->setResult(id);

   return Success();
}

Error clearJobs(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   removeCompletedJobs();
   return Success();
}

Error setJobListening(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   // extract job ID
   std::string id;
   bool listening;
   Error error = json::readParams(request.params, &id, &listening);
   if (error)
      return error;

   // look up in cache
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(id, &pJob))
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   // if listening started, return the output so far
   if (listening)
   {
      pResponse->setResult(pJob->output(0));
   }

   // begin/end listening
   pJob->setListening(listening);

   return Success();
}

Error executeJobAction(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // extract params
   std::string id;
   std::string action;
   Error error = json::readParams(request.params, &id, &action);
   if (error)
      return error;

   // look up in cache
   boost::shared_ptr<Job> pJob;
   if (!lookupJob(id, &pJob))
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   return pJob->executeAction(action);
}

void onSuspend(const r::session::RSuspendOptions&, core::Settings*)
{
   // currently no jobs can survive a suspend, so let the client know they're all finished
   removeAllJobs();
}

void onResume(const Settings& settings)
{
   module_context::enqueClientEvent(
         ClientEvent(client_events::kJobRefresh, jobsAsJson()));
}

void onClientInit()
{
   // when a new client connects, we should stop emitting streaming output from any job currently
   // doing so since the old client is no longer listening
   endAllJobStreaming();
}

void onShutdown(bool terminatedNormally)
{
   removeAllJobs();
}

} // anonymous namespace

core::json::Object jobState()
{
   return jobsAsJson();
}

bool isSuspendable()
{
   // don't suspend while we're running local jobs
   return !localJobsRunning();
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
   RS_REGISTER_CALL_METHOD(rs_runScriptJob, 5);
   RS_REGISTER_CALL_METHOD(rs_stopScriptJob, 1);
   RS_REGISTER_CALL_METHOD(rs_executeJobAction, 2);

   module_context::addSuspendHandler(module_context::SuspendHandler(
            onSuspend, onResume));
   module_context::events().onClientInit.connect(onClientInit);
   module_context::events().onShutdown.connect(onShutdown);

   using boost::bind;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(module_context::registerRpcMethod, "get_jobs", getJobs))
      (bind(module_context::registerRpcMethod, "job_output", jobOutput))
      (bind(module_context::registerRpcMethod, "set_job_listening", setJobListening))
      (bind(module_context::registerRpcMethod, "run_script_job", runScriptJob))
      (bind(module_context::registerRpcMethod, "clear_jobs", clearJobs))
      (bind(module_context::registerRpcMethod, "execute_job_action", executeJobAction))
      (bind(module_context::sourceModuleRFile, "SessionJobs.R"));

   return initBlock.execute();
}

} // namepsace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

