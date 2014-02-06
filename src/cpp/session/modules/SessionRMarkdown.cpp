/*
 * SessionRMarkdown.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include "SessionRMarkdown.hpp"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/format.hpp>

#include <core/FileSerializer.hpp>
#include <core/Exec.hpp>
#include <core/system/Process.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace rmarkdown {

namespace {

class RenderRmd : boost::noncopyable,
                  public boost::enable_shared_from_this<RenderRmd>
{
public:
   static boost::shared_ptr<RenderRmd> create(const FilePath& targetFile,
                                              const std::string& encoding)
   {
      boost::shared_ptr<RenderRmd> pRender(new RenderRmd(targetFile));
      pRender->start(encoding);
      return pRender;
   }

   void terminate()
   {
      terminationRequested_ = true;
   }

   bool isRunning()
   {
      return isRunning_;
   }

private:
   RenderRmd(const FilePath& targetFile) :
      isRunning_(false),
      terminationRequested_(false),
      targetFile_(targetFile)
   {}

   void start(const std::string& encoding)
   {
      json::Object dataJson;
      dataJson["target_file"] = module_context::createAliasedPath(targetFile_);
      ClientEvent event(client_events::kRmdRenderStarted, dataJson);
      module_context::enqueClientEvent(event);

      performRender(encoding);
   }

   void performRender(const std::string& encoding)
   {
      // R binary
      FilePath rProgramPath;
      Error error = module_context::rScriptPath(&rProgramPath);
      if (error)
      {
         terminateWithError(error);
         return;
      }

      // args
      std::vector<std::string> args;
      args.push_back("--silent");
      args.push_back("--no-save");
      args.push_back("--no-restore");
      args.push_back("-e");

      // render command
      boost::format fmt("rmarkdown::render('%1%', encoding='%2%');");
      std::string cmd = boost::str(fmt % targetFile_.filename() % encoding);
      args.push_back(cmd);

      // options
      core::system::ProcessOptions options;
      options.terminateChildren = true;
      options.redirectStdErrToStdOut = true;
      options.workingDir = targetFile_.parent();

      core::system::ProcessCallbacks cb;
      cb.onContinue = boost::bind(&RenderRmd::onRenderContinue,
                                  RenderRmd::shared_from_this());
      cb.onStdout = boost::bind(&RenderRmd::onRenderOutput,
                                RenderRmd::shared_from_this(), _2);
      cb.onStderr = boost::bind(&RenderRmd::onRenderOutput,
                                RenderRmd::shared_from_this(), _2);
      cb.onExit =  boost::bind(&RenderRmd::onRenderCompleted,
                                RenderRmd::shared_from_this(), _1, encoding);

      module_context::processSupervisor().runProgram(rProgramPath.absolutePath(),
                                                     args,
                                                     options,
                                                     cb);
   }

   bool onRenderContinue()
   {
      return !terminationRequested_;
   }

   void onRenderOutput(const std::string& output)
   {
      enqueRenderOutput(output);
   }

   void onRenderCompleted(int exitStatus, const std::string& encoding)
   {
      terminate(true);
   }

   void terminateWithError(const Error& error)
   {
      std::string message =
            "Error rendering R Markdown for " +
            module_context::createAliasedPath(targetFile_) + " " +
            error.summary();
      terminateWithError(message);
   }

   void terminateWithError(const std::string& message)
   {
      enqueRenderOutput(message);
      terminate(false);
   }

   void terminate(bool succeeded)
   {
      isRunning_ = false;
      json::Object resultJson;
      resultJson["succeeded"] = true;
      ClientEvent event(client_events::kRmdRenderCompleted, resultJson);
      module_context::enqueClientEvent(event);
   }

   static void enqueRenderOutput(const std::string& output)
   {
      ClientEvent event(client_events::kRmdRenderOutput, output);
      module_context::enqueClientEvent(event);
   }

   bool isRunning_;
   bool terminationRequested_;
   FilePath targetFile_;
};

boost::shared_ptr<RenderRmd> s_pCurrentRender_;

bool isRenderRunning()
{
   return s_pCurrentRender_ && s_pCurrentRender_->isRunning();
}

void initPandocPath()
{
   r::exec::RFunction sysSetenv("Sys.setenv");
   sysSetenv.addParam("RSTUDIO_PANDOC", 
                      session::options().pandocPath().absolutePath());
   Error error = sysSetenv.call();
   if (error)
      LOG_ERROR(error);
}

// when the RMarkdown package is installed, give .Rmd files the extended type
// "rmarkdown", unless they contain a special marker that indicates we should
// use the previous rendering strategy
std::string onDetectRmdSourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!pDoc->path().empty())
   {
      FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
      if (filePath.extensionLowerCase() == ".rmd" &&
          !boost::algorithm::icontains(pDoc->contents(),
                                       "<!-- rmarkdown v1 -->"))
      {
         return "rmarkdown";
      }
   }
   return std::string();
}

Error renderRmd(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   std::string file, encoding;
   Error error = json::readParams(request.params, &file, &encoding);
   if (error)
      return error;

   s_pCurrentRender_ = RenderRmd::create(
            module_context::resolveAliasedPath(file),
            encoding);

   // TODO: Return false if there's already a render running
   pResponse->setResult(true);

   return Success();
}

Error terminateRenderRmd(const json::JsonRpcRequest&,
                         json::JsonRpcResponse*)
{
   if (isRenderRunning())
      s_pCurrentRender_->terminate();

   return Success();
}

} // anonymous namespace

Error initialize()
{
   using namespace module_context;

   initPandocPath();

   if (module_context::isPackageVersionInstalled("rmarkdown", "0.1"))
      module_context::events().onDetectSourceExtendedType
                              .connect(onDetectRmdSourceType);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "render_rmd", renderRmd))
      (bind(registerRpcMethod, "terminate_render_rmd", terminateRenderRmd));

   return initBlock.execute();
}
   
} // namepsace rmarkdown
} // namespace modules
} // namesapce session

