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

#include <boost/algorithm/string/predicate.hpp>
#include <boost/bind/bind.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/regex.hpp>

#include <shared_core/Error.hpp>
#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>

#include <r/RExec.hpp>

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
   static boost::shared_ptr<RInstallJob> create(const std::string& version,
                                               const std::string& directory)
   {
      boost::shared_ptr<RInstallJob> pJob(new RInstallJob(version, directory));
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

private:
   RInstallJob(const std::string& version, const std::string& directory)
      : version_(version), directory_(directory)
   {
   }

   void initialize()
   {
      std::string name = "Install R " + version_;

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

      queueSteps();
      runNextStep();
   }

   // the steps are owned by this object, so they can refer to it directly
   void queueSteps()
   {
      Error error = r::exec::RFunction(".rs.rig.find").call(&rigPath_);
      if (error)
         LOG_ERROR(error);

      if (rigPath_.empty())
      {
         steps_.push_back({ "Downloading rig", boost::bind(&RInstallJob::downloadRig, this, _1) });
         steps_.push_back({ "Installing rig", boost::bind(&RInstallJob::installRig, this, _1) });
      }

      steps_.push_back({ "Installing R " + version_, boost::bind(&RInstallJob::installR, this, _1) });
   }

   void runNextStep()
   {
      if (stopped_)
      {
         finish(false, "cancelled");
         return;
      }

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
      Error error = r::exec::RFunction(".rs.rig.downloadUrl").call(&url_);
      if (error)
      {
         callback(false, error.getSummary());
         return;
      }

      log(url_ + "\n");

      FilePath curl = module_context::findProgram("curl");
      if (curl.isEmpty())
      {
         error = r::exec::RFunction(".rs.rig.downloadArchive")
               .addParam("url", url_)
               .call(&archivePath_);
         callback(!error, error ? error.getSummary() : "");
         return;
      }

      std::string extension = url_.find(".zip") != std::string::npos ? "zip" : "tar.gz";
      archivePath_ = module_context::tempFile("rig-", extension).getAbsolutePath();
      std::vector<std::string> args = { "-fSL", "--retry", "3", "-o", archivePath_, url_ };
      runProgram(curl.getAbsolutePath(), args, callback);
   }

   // Step: check the downloaded archive against the digest published for
   // it, then extract it into RStudio's data directory.
   void installRig(StepCallback callback)
   {
      FilePath archive(archivePath_);
      Error error = verifyArchive(archive);
      if (!error)
      {
         error = r::exec::RFunction(".rs.rig.installArchive")
               .addParam("archive", archivePath_)
               .call(&rigPath_);
      }

      Error removeError = archive.removeIfExists();
      if (removeError)
         LOG_ERROR(removeError);

      if (!error)
         log("rig installed to " + rigPath_ + "\n");

      callback(!error, error ? error.getSummary() : "");
   }

   Error verifyArchive(const FilePath& archive)
   {
      std::string expected;
      Error error = r::exec::RFunction(".rs.rig.archiveChecksum")
            .addParam("url", url_)
            .call(&expected);
      if (error)
         return error;

      if (expected.empty())
      {
         return systemError(
                  boost::system::errc::operation_not_permitted,
                  "no checksum is known for " + url_,
                  ERROR_LOCATION);
      }

      error = verifyArchiveDigest(archive, expected);
      if (error)
      {
         std::string actual = error.getProperty("actual");
         if (!actual.empty())
            log("SHA-256 mismatch: expected " + expected + ", got " + actual + "\n");
      }

      return error;
   }

   // Step: have rig install the requested version of R. Installing in
   // admin mode needs sudo, which can't be answered from here, so R is
   // always installed in user mode, at the confirmed destination, even when
   // rig itself is configured for admin mode.
   void installR(StepCallback callback)
   {
      std::vector<std::string> args = { "add", version_, "--user" };
      log("Installing into " + directory_ + "\n");
      runProgram(rigPath_, args, callback, directory_);
   }

   void runProgram(const std::string& program,
                   const std::vector<std::string>& args,
                   StepCallback callback,
                   const std::string& installDirectory = std::string())
   {
      log("$ " + program + " " + core::algorithm::join(args, " ") + "\n");

      boost::shared_ptr<RInstallJob> self = shared_from_this();

      // a stop is honored at the next poll, which also covers a stop that
      // arrives before the process has been polled for the first time
      core::system::ProcessCallbacks callbacks;
      callbacks.onContinue = [self](core::system::ProcessOperations&)
      {
         return !self->stopped_;
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
         self->processRunning_ = false;
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

      if (!installDirectory.empty())
      {
         core::system::Options environment;
         core::system::environment(&environment);
         core::system::setenv(&environment, "RIG_R_INSTALL_DIR", installDirectory);
         options.environment = environment;
      }

      Error error = module_context::processSupervisor().runProgram(program, args, options, callbacks);
      if (error)
      {
         callback(false, error.getSummary());
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

      json::Value installed;
      if (ok)
      {
         Error lookupError = r::exec::RFunction(".rs.rig.installedRVersion")
               .addParam("version", version_)
               .addParam("directory", directory_)
               .call(&installed);
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

private:
   std::string version_;
   std::string directory_;
   std::string url_;
   std::string rigPath_;
   std::string archivePath_;
   std::deque<Step> steps_;
   boost::shared_ptr<jobs::Job> pJob_;
   bool processRunning_ = false;
   bool stopped_ = false;
};

// only one installation runs at a time
boost::shared_ptr<RInstallJob> s_pInstallJob;

Error rigGetInstallDir(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   if (options().programMode() != kSessionProgramModeDesktop)
      return Error(json::errc::MethodUnexpected, ERROR_LOCATION);

   std::string directory;
   Error error = r::exec::RFunction(".rs.rig.defaultInstallDir").call(&directory);
   if (error)
      return error;
   pResponse->setResult(directory);
   return Success();
}

Error rigInstallRVersion(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // server installations of R are managed by administrators; only the
   // desktop, which can relaunch the session with another R, installs it
   if (options().programMode() != kSessionProgramModeDesktop)
      return Error(json::errc::MethodUnexpected, ERROR_LOCATION);

   std::string version, directory;
   Error error = json::readParams(request.params, &version, &directory);
   if (error)
      return error;

   if (!boost::regex_match(version, boost::regex("\\d+\\.\\d+(\\.\\d+)?")))
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   if (s_pInstallJob && s_pInstallJob->running())
      return Error("RInstallInProgress", 1, "An R installation is already in progress", ERROR_LOCATION);

   std::string normalizedDirectory;
   error = r::exec::RFunction(".rs.rig.validateInstallDir")
         .addParam("directory", directory)
         .call(&normalizedDirectory);
   if (error)
      return error;

   error = r::exec::RFunction(".rs.rig.rememberInstallDir")
         .addParam("directory", normalizedDirectory)
         .call();
   if (error)
      return error;

   s_pInstallJob = RInstallJob::create(version, normalizedDirectory);
   pResponse->setResult(s_pInstallJob->jobId());
   return Success();
}

} // anonymous namespace

Error verifyArchiveDigest(const FilePath& archive, const std::string& expected)
{
   if (expected.empty())
   {
      return systemError(
               boost::system::errc::operation_not_permitted,
               "no checksum is known for " + archive.getFilename(),
               ERROR_LOCATION);
   }

   std::string contents, actual;
   Error error = readStringFromFile(archive, &contents);
   if (!error)
      error = core::system::crypto::sha256Hex(contents, &actual);
   if (error)
      return error;

   if (!boost::algorithm::iequals(actual, expected))
   {
      error = systemError(
               boost::system::errc::illegal_byte_sequence,
               "the downloaded archive failed verification",
               ERROR_LOCATION);
      error.addProperty("expected", expected);
      error.addProperty("actual", actual);
      return error;
   }

   return Success();
}

Error findInstalledRVersion(const std::string& version, json::Value* pInstalled)
{
   return r::exec::RFunction(".rs.findInstalledRVersion")
         .addParam("version", version)
         .call(pInstalled);
}

std::string installInProgress()
{
   return s_pInstallJob && s_pInstallJob->running() ? s_pInstallJob->version() : std::string();
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "rig_install_r_version", rigInstallRVersion))
      (bind(registerRpcMethod, "rig_get_install_dir", rigGetInstallDir))
      (bind(sourceModuleRFile, "SessionRig.R"));

   return initBlock.execute();
}

} // namespace rig
} // namespace modules
} // namespace session
} // namespace rstudio
