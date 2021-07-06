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
#include <core/RegexUtils.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Process.hpp>
#include <core/WaitUtils.hpp>

#include <r/RExec.hpp>

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

const char * const kRenderNone = "none";

FilePath quartoProjectDir()
{
   return module_context::resolveAliasedPath(
      module_context::quartoConfig().project_dir
   );
}

std::string serverUrl(long port, const FilePath& outputFile = FilePath())
{
   // url w. port
   std::string url = "http://localhost:" + safe_convert::numberToString(port) + "/";

   // append doc path if we have one
   if (!outputFile.isEmpty())
   {
      FilePath quartoProjectOutputDir = quartoProjectDir().completeChildPath(
         quartoConfig().project_output_dir
      );
      std::string path = outputFile.isWithin(quartoProjectOutputDir)
                            ? outputFile.getRelativePath(quartoProjectOutputDir)
                            :  std::string();
      url = url + path;
   }

   // return url
   return url;
}

class QuartoServe : boost::noncopyable,
                    public boost::enable_shared_from_this<QuartoServe>
{
public:
   static Error create(const std::string& render,
                       const core::FilePath& initialDocPath,
                       boost::shared_ptr<QuartoServe>* pServe)
   {
      pServe->reset(new QuartoServe(initialDocPath));
      return (*pServe)->start(render);
   }

   bool isRunning()
   {
      return pJob_->state() == jobs::JobRunning;
   }

   long port()
   {
      return port_;
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
      : initialDocPath_(initialDocPath), stopRequested_(false)
   {
   }

   Error start(const std::string& render)
   {
      // quarto binary
      FilePath quartoProgramPath;
      Error error = core::system::findProgramOnPath("quarto", &quartoProgramPath);
      if (error)
         return error;

      // args
      std::vector<std::string> args({"serve", "--no-browse"});
      if (render != kRenderNone)
      {
         args.push_back("--render");
         args.push_back(render);
      }

      // options
      core::system::ProcessOptions options;
#ifdef _WIN32
      options.createNewConsole = true;
#else
      options.terminateChildren = true;
#endif
      options.workingDir = quartoProjectDir();

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

      // determine job name
      const std::string type = quartoConfig().project_type == kQuartoProjectBook ? "Book" : "Site";
      const std::string name = (render != kRenderNone ? "Render and " : "")  + std::string("Serve ") + type;

      // create job and emit some output (to prevent the "has not emitted output" message)
      using namespace jobs;
      JobActions jobActions;
      // note that we pass raw 'this' b/c the "stop" action will never be executed after we
      // hit onCompleted (becuase our status won't be "running"). if we passed shared_from_this
      // then we'd be keeping this object around forever (because jobs are never discarded).
      jobActions.push_back(std::make_pair("stop", boost::bind(&QuartoServe::stop, this)));
      pJob_ = addJob(name, "", "", 0, false, JobRunning, JobTypeSession, false, R_NilValue, jobActions, true, {});
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
      boost::regex browseRe("http:\\/\\/localhost:(\\d{2,})\\/");
      boost::smatch match;
      if (regex_utils::search(error, match, browseRe))
      {
         // save port
         port_ = safe_convert::stringTo<int>(match[1], 0);

         // launch viewer
         module_context::viewer(serverUrl(port_, initialDocPath_), true /* Quarto website */, -1);

         // now that the dev server is running restore the console tab
         ClientEvent activateConsoleEvent(client_events::kConsoleActivate, false);
         module_context::enqueClientEvent(activateConsoleEvent);
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
   long port_;
   FilePath initialDocPath_;
   bool stopRequested_;
   boost::shared_ptr<jobs::Job> pJob_;

};

// serve singleton
static boost::shared_ptr<QuartoServe> s_pServe;

// stop any running server and remove the job
void stopServer()
{
   if (s_pServe)
   {
      // stop the job if it's running
      if (s_pServe->isRunning())
         s_pServe->stop();

      // remove the job (will be replaced by a new quarto serve)
      s_pServe->remove();
   }
}

Error quartoServe(const std::string& render = kRenderNone,
                  const core::FilePath& docPath = FilePath())
{
   // stop any running server
   stopServer();

   // start a new server
   return QuartoServe::create(render, docPath, &s_pServe);
}

Error quartoServeRpc(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse*)
{
   // read params
   std::string render;
   Error error = json::readParams(request.params, &render);
   if (error)
      return error;

   return quartoServe(render);
}

bool isJobServeRunning()
{
   return s_pServe && s_pServe->isRunning();
}

long pkgServePort()
{
   // quarto package server
   double port = 0;
   Error error = r::exec::RFunction(".rs.quarto.servePort")
         .call(&port);
   if (error)
      LOG_ERROR(error);
   return std::lround(port);
}


void navigateToViewer(long port, const core::FilePath& docPath)
{
   // if the viewer is already on the site just activate it
   if (boost::algorithm::starts_with(
          module_context::viewerCurrentUrl(false), serverUrl(port)))
   {
      module_context::activatePane("viewer");
   }
   else
   {
      module_context::viewer(serverUrl(port, docPath), true /* Quarto website */, -1);
   }
}

bool isNewQuartoBuild(const std::string& renderOutput)
{
   static const boost::regex quartoBuildRe("file:\\/.*?\\/src\\/quarto.ts\\s");
   return regex_utils::textMatches(renderOutput, quartoBuildRe, false, true);
}


} // anonymous namespace

void previewDoc(const std::string& renderOutput, const core::FilePath& docPath)
{
   if (isJobServeRunning() && !isNewQuartoBuild(renderOutput))
   {
      navigateToViewer(s_pServe->port(), docPath);
   }
   else
   {
       long port = pkgServePort();
       if (port > 0)
       {
         navigateToViewer(port, docPath);
       }
       else
       {
          Error error = quartoServe(kRenderNone, docPath);
          if (error)
             LOG_ERROR(error);
       }
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
