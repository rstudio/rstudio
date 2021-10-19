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
#include <core/WaitUtils.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionQuarto.hpp>

#include "SessionQuartoJob.hpp"

using namespace rstudio::core;
using namespace rstudio::session::module_context;

namespace rstudio {
namespace session {

using namespace quarto;

namespace modules {
namespace quarto {
namespace serve {

namespace {

const char * const kRenderNone = "none";

FilePath quartoProjectDir()
{
   return module_context::resolveAliasedPath(
      quartoConfig().project_dir
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

class QuartoServe : public QuartoJob
{
public:
   static Error create(const std::string& render,
                       const core::FilePath& initialDocPath,
                       boost::shared_ptr<QuartoServe>* pServe)
   {
      pServe->reset(new QuartoServe(render, initialDocPath));
      return (*pServe)->start();
   }

   virtual ~QuartoServe()
   {
   }

   int port()
   {
      return port_;
   }

   std::string jobId()
   {
      return pJob_->id();
   }


protected:
   explicit QuartoServe(const std::string& render, const core::FilePath& initialDocPath)
      : QuartoJob(), port_(0), render_(render), initialDocPath_(initialDocPath)
   {
   }

   virtual std::string name()
   {
      const std::string type =
         quartoConfig().project_type == kQuartoProjectBook
            ? "Book"
            : "Site";
      const std::string name = (render_ != kRenderNone ? "Render and " : "")  + std::string("Serve ") + type;
      return name;
   }

   virtual std::vector<std::string> args()
   {
      std::vector<std::string> args({"serve", "--no-browse"});
      if (render_ != kRenderNone)
      {
         args.push_back("--render");
         args.push_back(render_);
      }
      return args;
   }

   virtual core::FilePath workingDir()
   {
      return quartoProjectDir();
   }

   virtual void onStdErr(const std::string& error)
   {
      // detect browse directive
      if (port_ == 0)
      {
         auto location = quartoServerLocationFromOutput(error);
         if (location.port > 0)
         {
            // set port
            port_ = location.port;

            // launch viewer
            module_context::viewer(serverUrl(port_, initialDocPath_),
                                   -1,
                                   module_context::QuartoNavigate::navWebsite(pJob_->id()));

            // now that the dev server is running restore the console tab
            ClientEvent activateConsoleEvent(client_events::kConsoleActivate, false);
            module_context::enqueClientEvent(activateConsoleEvent);

            // emit filtered output if we are on rstudio server
            if (session::options().programMode() == kSessionProgramModeServer)
            {
               QuartoJob::onStdErr(location.filteredOutput);
               return;
            }
         }
      }

      // standard output forwarding
      QuartoJob::onStdErr(error);
   }

private:
   int port_;
   std::string render_;
   FilePath initialDocPath_;
};

// serve singleton
boost::shared_ptr<QuartoServe> s_pServe;

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


void navigateToViewer(long port, const core::FilePath& docPath, const std::string& jobId)
{
   // if the viewer is already on the site just activate it
   if (boost::algorithm::starts_with(
          module_context::viewerCurrentUrl(false), serverUrl(port)))
   {
      module_context::activatePane("viewer");
   }
   else
   {
      module_context::viewer(
          serverUrl(port, docPath),
          -1,
          module_context::QuartoNavigate::navWebsite(jobId)
      );
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
      navigateToViewer(s_pServe->port(), docPath, s_pServe->jobId());
   }
   else
   {
       long port = pkgServePort();
       if (port > 0)
       {
         navigateToViewer(port, docPath, s_pServe->jobId());
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
