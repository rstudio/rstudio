/*
 * SessionQuartoServe.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include "SessionQuartoServe.hpp"

#include <string>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/jobs/JobsApi.hpp>


using namespace rstudio::core;
using namespace rstudio::session::module_context;

namespace rstudio {
namespace session {
namespace modules {
namespace quarto {
namespace serve {

namespace {

class QuartoServe : boost::noncopyable,
                    public boost::enable_shared_from_this<QuartoServe>
{
public:
   static Error create(const core::FilePath& initialDocPath, boost::shared_ptr<QuartoServe>* pServe)
   {
      pServe->reset(new QuartoServe(initialDocPath));
      return (*pServe)->start();
   }

   bool isRunning()
   {
      return pJob_->state() == jobs::JobRunning;
   }

   void stop()
   {
      stopRequested_ = true;
   }

   void remove()
   {
      // only remove if we are still in the list
      boost::shared_ptr<jobs::Job> pJob;
      if (jobs::lookupJob(pJob_->id(), &pJob))
         jobs::removeJob(pJob_);
   }


   virtual ~QuartoServe()
   {
   }

private:
   explicit QuartoServe(const core::FilePath& initialDocPath)
      : stopRequested_(false), initialDocPath_(initialDocPath)
   {
   }

   Error start()
   {
      // quarto binary
      FilePath quartoProgramPath;
      Error error = core::system::findProgramOnPath("quarto", &quartoProgramPath);
      if (error)
         return error;

      // args
      std::vector<std::string> args({"serve", "--no-browse"});

      // options
      core::system::ProcessOptions options;
#ifdef _WIN32
      options.createNewConsole = true;
#else
      options.terminateChildren = true;
#endif
      options.workingDir = module_context::resolveAliasedPath(module_context::quartoConfig().project_dir);

      // set initial doc path if we have one
      if (!initialDocPath_.isEmpty())
      {
         core::system::Options childEnv;
         core::system::environment(&childEnv);
         core::system::setenv(&childEnv, "QUARTO_SERVE_PREVIEW_DOC", initialDocPath_.getAbsolutePath());
         options.environment = childEnv;
      }

      // callbacks
      core::system::ProcessCallbacks cb;
      cb.onContinue = boost::bind(&QuartoServe::onContinue,
                                  QuartoServe::shared_from_this());
      cb.onStdout = boost::bind(&QuartoServe::onStdOut,
                                QuartoServe::shared_from_this(), _2);
      cb.onStderr = boost::bind(&QuartoServe::onStdErr,
                                QuartoServe::shared_from_this(), _2);
      cb.onExit =  boost::bind(&QuartoServe::onCompleted,
                                QuartoServe::shared_from_this(), _1);

      error = processSupervisor().runProgram(string_utils::utf8ToSystem(quartoProgramPath.getAbsolutePath()),
                                     args,
                                     options,
                                     cb);

      if (error)
         return error;

      // create job and emit some output (to prevent the "has not emitted output" message)
      using namespace jobs;
      JobActions jobActions;
      // note that we pass raw 'this' b/c the "stop" action will never be executed after we
      // hit onCompleted (becuase our status won't be "running"). if we passed shared_from_this
      // then we'd be keeping this object around forever (because jobs are never discarded).
      jobActions.push_back(std::make_pair("stop", boost::bind(&QuartoServe::stop, this)));
      pJob_ = addJob("quarto serve", "", "", 0, JobRunning, JobTypeSession, false, R_NilValue, jobActions, true, {});
      pJob_->addOutput("\n", true);

      // return success
      return Success();
   }

   bool onContinue()
   {
      return !stopRequested_;
   }

   void onStdOut(const std::string& output)
   {
      pJob_->addOutput(output, false);

   }

   void onStdErr(const std::string& error)
   {
      pJob_->addOutput(error, true);

      // detect browse directive
      boost::regex browseRe("http:\\/\\/localhost:(\\d{2,})\\/(.*\\.html)?");
      boost::smatch match;
      if (regex_utils::search(error, match, browseRe))
      {
         std::string port = match[1];
         std::string url = "http://localhost:" + port + "/";
         if (match.size() > 2)
            url = url + match[2];
         module_context::viewer(url, -1);
      }

   }

   void onCompleted(int exitStatus)
   {
      if (stopRequested_)
      {
         setJobState(pJob_, jobs::JobCancelled);
         remove();
      }
      else if (exitStatus == EXIT_SUCCESS)
      {
         setJobState(pJob_, jobs::JobSucceeded);
      }
      else
      {
         setJobState(pJob_, jobs::JobFailed);
      }
   }

private:

   bool stopRequested_;
   FilePath initialDocPath_;
   boost::shared_ptr<jobs::Job> pJob_;

};

// serve singleton
static boost::shared_ptr<QuartoServe> s_pServe;

Error quartoServe(const core::FilePath& docPath = FilePath())
{
   // stop any running server and remove the job
   if (s_pServe)
   {
      // stop the job if it's running
      if (s_pServe->isRunning())
         s_pServe->stop();

      // remove the job (will be replaced by a new quarto serve)
      s_pServe->remove();
   }

   // start a new server
   return QuartoServe::create(docPath, &s_pServe);
}

Error quartoServeRpc(const json::JsonRpcRequest&,
                  json::JsonRpcResponse*)
{
   return quartoServe();
}



}

void previewDoc(const core::FilePath& docPath)
{
   if (!s_pServe || !s_pServe->isRunning())
   {
      Error error = quartoServe(docPath);
      if (error)
         LOG_ERROR(error);
   }
   else
   {
      module_context::activatePane("viewer");
   }
}

Error initialize()
{

   // register rpc functions

   ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(module_context::registerRpcMethod, "quarto_serve", quartoServeRpc))
   ;
   return initBlock.execute();
}

} // namespace serve
} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio
