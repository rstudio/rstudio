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

#include <boost/bind/bind.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/regex.hpp>

#include <shared_core/Error.hpp>
#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/Log.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>

#include <r/RErrorCategory.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/jobs/JobsApi.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace rig {

namespace {

SEXP rs_rigPath()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(options().rigPath().getAbsolutePath(), &rProtect);
}

// The snapshot of rig's default-R setup taken before an install, kept on
// disk so that an install the session does not outlive is still undone.
FilePath installSnapshotPath()
{
   return module_context::userScratchPath().completePath("rig-install-snapshot.rds");
}

// Installing R runs 'rig add' as a job, reporting its outcome through a
// client event exactly once.
class RInstallJob : public boost::enable_shared_from_this<RInstallJob>
{
public:
   static boost::shared_ptr<RInstallJob> create(const std::string& version,
                                                const std::string& architecture)
   {
      boost::shared_ptr<RInstallJob> pJob(new RInstallJob(version, architecture));
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

   const std::string& version() const
   {
      return version_;
   }

   // The session is exiting while rig runs. rig is stopped here, rather than
   // left to the session's final child cleanup, so that the job finishes and
   // undoes what rig added before the session is gone; if rig does not exit
   // in time, the snapshot stays on disk and the next session undoes it.
   void onSessionShutdown()
   {
      if (!processRunning_)
         return;

      stopped_ = true;

      // polled directly rather than with the supervisor's wait(), which
      // runs until every supervised child is gone (the assistant's agent
      // and the chat backend among them), not just rig
      for (int i = 0; processRunning_ && i < 100; i++)
      {
         module_context::processSupervisor().poll();
         boost::this_thread::sleep_for(boost::chrono::milliseconds(10));
      }
   }

private:
   RInstallJob(const std::string& version, const std::string& architecture)
      : version_(version), architecture_(architecture)
   {
   }

   void initialize()
   {
      std::string name = "Install R " + version_;
      if (!architecture_.empty())
         name += " (" + architecture_ + ")";

      // the job outlives this object (jobs are never discarded), so its stop
      // action must not keep this object alive
      boost::weak_ptr<RInstallJob> pWeakThis = shared_from_this();

      jobs::JobActions actions;
      actions.push_back(std::make_pair("stop", [pWeakThis](const std::string&)
      {
         if (boost::shared_ptr<RInstallJob> pThis = pWeakThis.lock())
            pThis->onStop();
      }));

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

      // rig is bundled with the desktop, so it is only missing when the
      // installation is damaged
      std::string rigPath;
      Error error = r::exec::RFunction(".rs.rig.find").call(&rigPath);
      if (error)
         LOG_ERROR(error);

      if (rigPath.empty())
      {
         finish(false, "rig was not found");
         return;
      }

      installR(rigPath);
   }

   // Have rig install the requested version of R. Installing in admin mode
   // needs sudo, which can't be answered from here, so R is always installed
   // in user mode (into the home directory), even when rig itself is
   // configured for admin mode.
   void installR(const std::string& rigPath)
   {
      jobs::setJobStatus(pJob_, "Installing R " + version_);

      std::vector<std::string> args = { "add", version_, "--user" };
      if (!architecture_.empty())
      {
         args.push_back("--arch");
         args.push_back(architecture_);
      }
      log("$ " + rigPath + " " + core::algorithm::join(args, " ") + "\n");

      boost::shared_ptr<RInstallJob> self = shared_from_this();

      // a stop is honored at the next poll, which also covers a stop that
      // arrives before the process has been polled for the first time
      core::system::ProcessCallbacks callbacks;
      callbacks.onContinue = [self](core::system::ProcessOperations&)
      {
         return !self->stopped_;
      };
      callbacks.onStarted = [self](core::system::ProcessOperations& ops)
      {
         std::vector<std::string> notes;
         Error error = r::exec::RFunction(".rs.rig.userInstallStarted", static_cast<int>(ops.getPid())).call(&notes);
         if (error)
            LOG_ERROR(error);
         self->logNotes(notes);
      };
      callbacks.onStdout = [self](core::system::ProcessOperations&, const std::string& output)
      {
         self->log(output);
      };
      callbacks.onStderr = [self](core::system::ProcessOperations&, const std::string& output)
      {
         self->log(output);
      };
      callbacks.onExit = [self](int status)
      {
         self->processRunning_ = false;
         if (self->stopped_)
            self->finish(false, "cancelled");
         else if (status == EXIT_SUCCESS)
            self->finish(true, "");
         else
            self->finish(false, "exited with status " + std::to_string(status));
      };

      // the first R rig installs in user mode becomes the default R outside
      // RStudio as well (R/Rscript quick links, and on macOS a login
      // LaunchAgent that sets RSTUDIO_WHICH_R); what the install adds or
      // changes is put back when the job finishes (#19229)
      std::vector<std::string> notes;
      Error beginError = r::exec::RFunction(".rs.rig.userInstallBegin", rigPath, installSnapshotPath().getAbsolutePath()).call(&notes);
      logNotes(notes);
      if (beginError)
      {
         // without the snapshot there is no undoing what rig adds
         finish(false, "could not record rig's default R setup: " + r::endUserErrorMessage(beginError));
         return;
      }

      core::system::ProcessOptions options;
      options.terminateChildren = true;
      options.exitWithParent = true;

#ifndef _WIN32
      // rig also appends to the shell startup files unless its user-mode
      // quick-link directory precedes /usr/local/bin on its PATH
      core::system::Options environment;
      core::system::environment(&environment);
      std::string home = core::system::getenv(environment, "HOME");
      if (!home.empty())
         core::system::addToPath(&environment, home + "/.local/bin", true);
      options.environment = environment;
#endif

      Error error = module_context::processSupervisor().runProgram(rigPath, args, options, callbacks);
      if (error)
      {
         finish(false, error.getSummary());
         return;
      }

      processRunning_ = true;
   }

   void onStop()
   {
      stopped_ = true;

      // a running program is terminated by its next poll, and the job
      // finishes when it exits
      if (!processRunning_)
         finish(false, "cancelled");
   }

   void finish(bool ok, std::string error)
   {
      if (pJob_->complete())
         return;

      std::vector<std::string> notes;
      Error restoreError = r::exec::RFunction(".rs.rig.userInstallEnd").call(&notes);
      if (restoreError)
         LOG_ERROR(restoreError);
      logNotes(notes);

      json::Value installed;
      if (ok)
      {
         Error lookupError = findInstalledRVersion(version_, architecture_, &installed);
         if (lookupError)
            LOG_ERROR(lookupError);

         if (!installed.isObject())
         {
            ok = false;
            error = "R " + version_ + " was not found after installation";
            log(error + "\n");
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
      data["installed"] = installed.isObject() ? installed : json::Value();
      module_context::enqueClientEvent(ClientEvent(client_events::kRInstallCompleted, data));
   }

   void log(const std::string& text)
   {
      pJob_->addOutput(text, false);
   }

   void logNotes(const std::vector<std::string>& notes)
   {
      for (const std::string& note : notes)
         log(note + "\n");
   }

private:
   std::string version_;
   std::string architecture_;
   boost::shared_ptr<jobs::Job> pJob_;
   bool processRunning_ = false;
   bool stopped_ = false;
};

// only one installation runs at a time
boost::shared_ptr<RInstallJob> s_pInstallJob;

void onShutdown(bool)
{
   if (s_pInstallJob)
      s_pInstallJob->onSessionShutdown();
}

// undo an install a previous session did not get to undo
void onDeferredInit(bool)
{
   std::vector<std::string> notes;
   Error error = r::exec::RFunction(".rs.rig.userInstallRecover", installSnapshotPath().getAbsolutePath()).call(&notes);
   if (error)
      LOG_ERROR(error);

   for (const std::string& note : notes)
      WLOGF("{}", note);
}

Error rigInstallRVersion(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // server installations of R are managed by administrators; only the
   // desktop, which can relaunch the session with another R, installs it
   if (options().programMode() != kSessionProgramModeDesktop)
      return Error(json::errc::MethodUnexpected, ERROR_LOCATION);

   std::string version;
   Error error = json::readParams(request.params, &version);
   if (error)
      return error;

   if (!boost::regex_match(version, boost::regex("\\d+\\.\\d+(\\.\\d+)?")))
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   // the build of R to install where there's a choice; empty for rig's default
   std::string architecture;
   if (request.params.getSize() > 1)
   {
      if (!request.params[1].isString())
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      architecture = request.params[1].getString();
   }
   if (!architecture.empty() && architecture != "arm64" && architecture != "x86_64")
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   if (s_pInstallJob && s_pInstallJob->running())
      return Error("RInstallInProgress", 1, "An R installation is already in progress", ERROR_LOCATION);

   s_pInstallJob = RInstallJob::create(version, architecture);
   pResponse->setResult(s_pInstallJob->jobId());
   return Success();
}

} // anonymous namespace

Error findInstalledRVersion(const std::string& version,
                            const std::string& architecture,
                            json::Value* pInstalled)
{
   r::exec::RFunction findInstalled(".rs.findInstalledRVersion");
   findInstalled.addParam("version", version);
   if (!architecture.empty())
      findInstalled.addParam("architecture", architecture);
   return findInstalled.call(pInstalled);
}

std::string installInProgress()
{
   return s_pInstallJob && s_pInstallJob->running() ? s_pInstallJob->version() : std::string();
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_rigPath);

   events().onShutdown.connect(onShutdown);
   events().onDeferredInit.connect(onDeferredInit);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "rig_install_r_version", rigInstallRVersion))
      (bind(sourceModuleRFile, "SessionRig.R"));

   return initBlock.execute();
}

} // namespace rig
} // namespace modules
} // namespace session
} // namespace rstudio
