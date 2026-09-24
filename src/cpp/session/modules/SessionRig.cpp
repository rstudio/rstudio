/*
 * SessionRig.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionRig.hpp"

#include <deque>

#include <boost/bind/bind.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/regex.hpp>

#include <shared_core/Error.hpp>
#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/Log.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Process.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/jobs/JobsApi.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace rig {

namespace {

// Installing R runs as a sequence of steps (download rig, extract rig, run
// 'rig add'), each reporting its outcome through a callback exactly once.
// Steps that run a program complete when the program exits; steps that call
// into R complete synchronously.
typedef boost::function<void(bool, const std::string&)> StepCallback;

struct Step
{
   std::string label;
   boost::function<void(StepCallback)> run;
};

class RInstallJob : public boost::enable_shared_from_this<RInstallJob>
{
public:
   static boost::shared_ptr<RInstallJob> create(const std::string& version)
   {
      boost::shared_ptr<RInstallJob> pJob(new RInstallJob(version));
      pJob->initialize();
      return pJob;
   }

   bool running() const
   {
      return pJob_ && !pJob_->complete();
   }

   std::string jobId() const
   {
      return pJob_ ? pJob_->id() : std::string();
   }

private:
   explicit RInstallJob(const std::string& version)
      : version_(version),
        pOperations_(nullptr)
   {
   }

   void initialize()
   {
      std::string name = "Install R " + version_;

      jobs::JobActions actions;
      actions.push_back(std::make_pair("stop", boost::bind(&RInstallJob::onStop, shared_from_this(), _1)));

      pJob_ = jobs::addJob(
               name,
               "",
               "",
               0,
               false,
               jobs::JobRunning,
               jobs::JobTypeSession,
               false,
               R_NilValue,
               actions,
               true,
               {});

      queueSteps();
      runNextStep();
   }

   void queueSteps()
   {
      Error error = r::exec::RFunction(".rs.rig.find").call(&rigPath_);
      if (error)
         LOG_ERROR(error);

      if (rigPath_.empty())
      {
         steps_.push_back({ "Downloading rig", boost::bind(&RInstallJob::downloadRig, shared_from_this(), _1) });
         steps_.push_back({ "Installing rig", boost::bind(&RInstallJob::installRig, shared_from_this(), _1) });
      }

      steps_.push_back({ "Installing R " + version_, boost::bind(&RInstallJob::installR, shared_from_this(), _1) });
   }

   void runNextStep()
   {
      if (steps_.empty())
      {
         finish(true, "");
         return;
      }

      Step step = steps_.front();
      steps_.pop_front();

      jobs::setJobStatus(pJob_, step.label);
      log("==> " + step.label + "\n");
      step.run(boost::bind(&RInstallJob::onStepCompleted, shared_from_this(), _1, _2));
   }

   void onStepCompleted(bool ok, const std::string& error)
   {
      if (ok)
         runNextStep();
      else
         finish(false, error);
   }

   // Step: download the rig release archive. curl is preferred as it runs
   // in the background with its output in the job; when it is unavailable
   // the download happens in R and blocks the session briefly.
   void downloadRig(StepCallback callback)
   {
      std::string url, curl;
      Error error = r::exec::RFunction(".rs.rig.downloadUrl").call(&url);
      if (!error)
         error = r::exec::RFunction(".rs.rig.curlPath").call(&curl);

      if (error)
      {
         callback(false, error.getSummary());
         return;
      }

      log(url + "\n");

      if (curl.empty())
      {
         error = r::exec::RFunction(".rs.rig.downloadArchive")
               .addParam("url", url)
               .call(&archivePath_);
         callback(!error, error ? error.getSummary() : "");
         return;
      }

      archivePath_ = module_context::tempFile("rig-", url.find(".zip") != std::string::npos ? "zip" : "tar.gz").getAbsolutePath();
      std::vector<std::string> args = { "-fSL", "--retry", "3", "-o", archivePath_, url };
      runProgram(curl, args, callback);
   }

   // Step: extract the downloaded archive into RStudio's data directory.
   void installRig(StepCallback callback)
   {
      Error error = r::exec::RFunction(".rs.rig.installArchive")
            .addParam("archive", archivePath_)
            .call(&rigPath_);

      if (!error)
         log("rig installed to " + rigPath_ + "\n");

      callback(!error, error ? error.getSummary() : "");
   }

   // Step: have rig install the requested version of R.
   void installR(StepCallback callback)
   {
      std::vector<std::string> modeArgs;
      Error error = r::exec::RFunction(".rs.rig.modeArgs")
            .addParam("rig", rigPath_)
            .call(&modeArgs);

      if (error)
      {
         callback(false, error.getSummary());
         return;
      }

      std::vector<std::string> args = { "add", version_ };
      args.insert(args.end(), modeArgs.begin(), modeArgs.end());
      runProgram(rigPath_, args, callback);
   }

   void runProgram(const std::string& program,
                   const std::vector<std::string>& args,
                   StepCallback callback)
   {
      log("$ " + program + " " + core::algorithm::join(args, " ") + "\n");

      boost::shared_ptr<RInstallJob> self = shared_from_this();

      core::system::ProcessCallbacks callbacks;
      callbacks.onStarted = [self](core::system::ProcessOperations& operations)
      {
         self->pOperations_ = &operations;
      };
      callbacks.onStdout = [self](core::system::ProcessOperations&, const std::string& output)
      {
         self->log(output);
      };
      callbacks.onStderr = [self](core::system::ProcessOperations&, const std::string& output)
      {
         self->log(output);
      };
      callbacks.onExit = [self, callback](int status)
      {
         self->pOperations_ = nullptr;
         if (self->stopped_)
            self->finish(false, "cancelled");
         else if (status == EXIT_SUCCESS)
            callback(true, "");
         else
            callback(false, "exited with status " + std::to_string(status));
      };

      core::system::ProcessOptions options;
      options.terminateChildren = true;
      options.exitWithParent = true;

      Error error = module_context::processSupervisor().runProgram(program, args, options, callbacks);
      if (error)
         callback(false, error.getSummary());
   }

   void onStop(const std::string&)
   {
      stopped_ = true;
      if (pOperations_ != nullptr)
      {
         Error error = pOperations_->terminate();
         if (error)
            LOG_ERROR(error);
      }
      else
      {
         finish(false, "cancelled");
      }
   }

   void finish(bool ok, const std::string& error)
   {
      if (pJob_->complete())
         return;

      json::Value installed;
      if (ok)
      {
         Error lookupError = findInstalledRVersion(version_, &installed);
         if (lookupError)
            LOG_ERROR(lookupError);

         if (installed.isNull())
         {
            ok = false;
            log("R " + version_ + " was not found after installation\n");
         }
      }
      else
      {
         log("Installation of R " + version_ + " failed: " + error + "\n");
      }

      if (ok)
      {
         jobs::setJobStatus(pJob_, "R " + version_ + " installed");
         jobs::setJobState(pJob_, jobs::JobSucceeded);
      }
      else
      {
         jobs::setJobStatus(pJob_, error.empty() ? "failed" : error);
         jobs::setJobState(pJob_, stopped_ ? jobs::JobCancelled : jobs::JobFailed);
      }

      json::Object data;
      data["version"] = version_;
      data["success"] = ok;
      data["error"] = error;
      data["installed"] = installed;
      module_context::enqueClientEvent(ClientEvent(client_events::kRInstallCompleted, data));
   }

   void log(const std::string& text)
   {
      pJob_->addOutput(text, false);
   }

private:
   std::string version_;
   std::string rigPath_;
   std::string archivePath_;
   std::deque<Step> steps_;
   boost::shared_ptr<jobs::Job> pJob_;
   core::system::ProcessOperations* pOperations_;
   bool stopped_ = false;
};

// only one installation runs at a time
boost::shared_ptr<RInstallJob> s_pInstallJob;

Error rigInstallRVersion(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   std::string version;
   Error error = json::readParams(request.params, &version);
   if (error)
      return error;

   if (!boost::regex_match(version, boost::regex("\\d+\\.\\d+(\\.\\d+)?")))
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   if (s_pInstallJob && s_pInstallJob->running())
      return Error("RInstallInProgress", 1, "An R installation is already in progress", ERROR_LOCATION);

   s_pInstallJob = RInstallJob::create(version);
   pResponse->setResult(s_pInstallJob->jobId());
   return Success();
}

Error rigListRVersions(const json::JsonRpcRequest&,
                       json::JsonRpcResponse* pResponse)
{
   json::Value versions;
   Error error = r::exec::RFunction(".rs.rig.listAsJson").call(&versions);
   if (error)
      return error;

   pResponse->setResult(versions);
   return Success();
}

} // anonymous namespace

bool isRigAvailable()
{
   std::string path;
   Error error = r::exec::RFunction(".rs.rig.find").call(&path);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return !path.empty();
}

Error findInstalledRVersion(const std::string& version, json::Value* pInstalled)
{
   return r::exec::RFunction(".rs.rig.findRVersion")
         .addParam("version", version)
         .call(pInstalled);
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "rig_install_r_version", rigInstallRVersion))
      (bind(registerRpcMethod, "rig_list_r_versions", rigListRVersions))
      (bind(sourceModuleRFile, "SessionRig.R"));

   return initBlock.execute();
}

} // namespace rig
} // namespace modules
} // namespace session
} // namespace rstudio
