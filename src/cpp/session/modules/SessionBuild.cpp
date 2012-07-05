/*
 * SessionBuild.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionBuild.hpp"

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/format.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/Exec.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace build {

namespace {

class Build : boost::noncopyable,
              public boost::enable_shared_from_this<Build>
{
public:
   static boost::shared_ptr<Build> create(const std::string& type)
   {
      boost::shared_ptr<Build> pBuild(new Build());
      pBuild->start(type);
      return pBuild;
   }

private:
   Build()
      : isRunning_(false), terminationRequested_(false)
   {
   }

   void start(const std::string& type)
   {
      ClientEvent event(client_events::kBuildStatus, "started");
      module_context::enqueClientEvent(event);

      isRunning_ = true;

      // callbacks
      core::system::ProcessCallbacks cb;
      cb.onContinue = boost::bind(&Build::onContinue,
                                  Build::shared_from_this());
      cb.onStdout = boost::bind(&Build::onOutput,
                                Build::shared_from_this(), _2);
      cb.onStderr = boost::bind(&Build::onOutput,
                                Build::shared_from_this(), _2);
      cb.onExit =  boost::bind(&Build::onCompleted,
                                Build::shared_from_this(),
                                _1);

      // execute build
      executeBuild(type, cb);
   }


   void executeBuild(const std::string& type,
                     const core::system::ProcessCallbacks& cb)
   {
      // options
      core::system::ProcessOptions options;
      options.terminateChildren = true;
      options.redirectStdErrToStdOut = true;

      const core::r_util::RProjectConfig& config =
                                          projects::projectContext().config();
      if (config.buildType == r_util::kBuildTypePackage)
      {
         FilePath packagePath = projectPath(config.packagePath);
         options.workingDir = packagePath.parent();
         executePackageBuild(type, packagePath, options, cb);
      }
      else if (config.buildType == r_util::kBuildTypeMakefile)
      {
         FilePath makefilePath = projectPath(config.makefilePath);
         options.workingDir = makefilePath;
         executeMakefileBuild(type, options, cb);
      }
      else if (config.buildType == r_util::kBuildTypeCustom)
      {
         FilePath scriptPath = projectPath(config.customScriptPath);
         options.workingDir = scriptPath.parent();
         executeCustomBuild(type, scriptPath, options, cb);
      }
      else
      {
         terminateWithError("Unrecognized build type: " + config.buildType);
      }
   }

   void executePackageBuild(const std::string& type,
                            const FilePath& packagePath,
                            const core::system::ProcessOptions& options,
                            const core::system::ProcessCallbacks& cb)
   {
      // R bin directory
      FilePath rBinDir;
      Error error = module_context::rBinDir(&rBinDir);
      if (error)
      {
         std::string msg = "Error attempting to locate R binary: " +
                           error.summary();
         terminateWithError(msg);
         return;
      }

      // R binary and intial args
      FilePath rProgram;
      std::vector<std::string> args;
  #ifdef _WIN32
      rProgram = rBinDir.childPath("Rcmd.exe");
  #else
      rProgram = rBinDir.childPath("R");
      args.push_back("CMD");
#endif

      if (type == "build-all")
      {
         args.push_back("build");
      }
      else if (type == "check-package")
      {
         args.push_back("check");
      }
      args.push_back(packagePath.filename());

      // run process
      module_context::processSupervisor().runProgram(rProgram.absolutePath(),
                                                     args,
                                                     options,
                                                     cb);
   }


   void executeMakefileBuild(const std::string& type,
                             const core::system::ProcessOptions& options,
                             const core::system::ProcessCallbacks& cb)
   {
      std::string cmd;
      if (type == "build-all")
      {
         cmd = "make";
      }
      else if (type == "clean-all")
      {
         cmd = "make clean";
      }
      else if (type == "rebuild-all")
      {
         cmd = shell_utils::join_and("make clean", "make");
      }

      module_context::processSupervisor().runCommand(cmd,
                                                     options,
                                                     cb);
   }

   void executeCustomBuild(const std::string& type,
                           const FilePath& customScriptPath,
                           const core::system::ProcessOptions& options,
                           const core::system::ProcessCallbacks& cb)
   {
      module_context::processSupervisor().runCommand(
                           shell_utils::ShellCommand(customScriptPath),
                           options,
                           cb);
   }

   FilePath projectPath(const std::string& path)
   {
      if (boost::algorithm::starts_with(path, "~/") ||
          FilePath::isRootPath(path))
      {
         return module_context::resolveAliasedPath(path);
      }
      else
      {
         return projects::projectContext().directory().complete(path);
      }
   }

   void terminateWithError(const std::string& msg)
   {
      enqueBuildOutput(msg);
      enqueBuildCompleted();
   }

public:
   virtual ~Build()
   {
   }

   bool isRunning() const { return isRunning_; }

   const std::string& output() const { return output_; }

   void terminate()
   {
      enqueBuildOutput("\n");
      terminationRequested_ = true;
   }

private:
   bool onContinue()
   {
      return !terminationRequested_;
   }

   void onOutput(const std::string& output)
   {
      enqueBuildOutput(output);
   }

   void onCompleted(int exitStatus)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         enqueBuildOutput("Completed successfully.\n");
      }
      else
      {
         boost::format fmt("\nExited with status %1%.\n\n");
         enqueBuildOutput(boost::str(fmt % exitStatus));
      }

      enqueBuildCompleted();
   }

   void enqueBuildOutput(const std::string& output)
   {
      output_.append(output);

      ClientEvent event(client_events::kBuildOutput, output);
      module_context::enqueClientEvent(event);
   }

   void enqueBuildCompleted()
   {
      isRunning_ = false;

      ClientEvent event(client_events::kBuildStatus, "completed");
      module_context::enqueClientEvent(event);
   }

private:
   bool isRunning_;
   bool terminationRequested_;
   std::string output_;
};

boost::shared_ptr<Build> s_pBuild;


bool isBuildRunning()
{
   return s_pBuild && s_pBuild->isRunning();
}

Error startBuild(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   // get type
   std::string type;
   Error error = json::readParam(request.params, 0, &type);
   if (error)
      return error;

   // if we have a build already running then just return false
   if (isBuildRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      s_pBuild = Build::create(type);
      pResponse->setResult(true);
   }

   return Success();
}



Error terminateBuild(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   if (isBuildRunning())
      s_pBuild->terminate();

   pResponse->setResult(true);

   return Success();
}

} // anonymous namespace


core::json::Value buildStateAsJson()
{
   if (s_pBuild)
   {
      json::Object stateJson;
      stateJson["running"] = s_pBuild->isRunning();
      stateJson["output"] = s_pBuild->output();
      return stateJson;
   }
   else
   {
      return json::Value();
   }
}

Error initialize()
{
   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "start_build", startBuild))
      (bind(registerRpcMethod, "terminate_build", terminateBuild));
   return initBlock.execute();
}


} // namespace build
} // namespace modules
} // namesapce session

