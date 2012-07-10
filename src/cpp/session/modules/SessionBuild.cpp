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
#include <boost/scope_exit.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/join.hpp>

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/DcfParser.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/r_util/RPackageInfo.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace build {

namespace {

FilePath restartContextFilePath()
{
   return module_context::scopedScratchPath().childPath(
                                                   "build_restart_context");
}

void saveRestartContext(const FilePath& packageDir,
                        const std::string& buildOutput)
{
   // read package info
   r_util::RPackageInfo pkgInfo;
   Error error = pkgInfo.read(packageDir);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // save restart context
   core::Settings restartSettings;
   error = restartSettings.initialize(restartContextFilePath());
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   restartSettings.beginUpdate();
   restartSettings.set("package_name", pkgInfo.name());
   restartSettings.set("build_output", buildOutput);
   restartSettings.endUpdate();
}

json::Value collectRestartContext()
{
   FilePath restartSettingsPath = restartContextFilePath();
   if (restartSettingsPath.exists())
   {
      // always cleanup the restart context on scope exit
      BOOST_SCOPE_EXIT( (&restartSettingsPath) )
      {
         Error error = restartSettingsPath.remove();
         if (error)
            LOG_ERROR(error);
      }
      BOOST_SCOPE_EXIT_END

      core::Settings restartSettings;
      Error error = restartSettings.initialize(restartContextFilePath());
      if (error)
      {
         LOG_ERROR(error);
         return json::Value();
      }

      json::Object restartJson;
      restartJson["package_name"] = restartSettings.get("package_name");
      restartJson["build_output"] = restartSettings.get("build_output");
      return restartJson;
   }
   else
   {
      return json::Value();
   }
}


// R command invocation -- has two representations, one to be submitted
// (shellCmd_) and one to show the user (cmdString_)
class RCommand
{
public:
   explicit RCommand(const FilePath& rBinDir)
      : shellCmd_(module_context::rCmd(rBinDir))
   {
#ifdef _WIN32
      cmdString_ = "Rcmd.exe";
#else
      cmdString_ = "R CMD";
#endif

      // set escape mode to files-only. this is so that when we
      // add the group of extra arguments from the user that we
      // don't put quotes around it.
      shellCmd_ << shell_utils::EscapeFilesOnly;
   }

   RCommand& operator<<(const std::string& arg)
   {
      if (!arg.empty())
      {
         cmdString_ += " " + arg;
         shellCmd_ << arg;
      }
      return *this;
   }

   RCommand& operator<<(const FilePath& arg)
   {
      cmdString_ += " " + arg.absolutePath();
      shellCmd_ << arg;
      return *this;
   }


   const std::string& commandString() const
   {
      return cmdString_;
   }

   const shell_utils::ShellCommand& shellCommand() const
   {
      return shellCmd_;
   }

private:
   std::string cmdString_;
   shell_utils::ShellCommand shellCmd_;
};

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
      : isRunning_(false), terminationRequested_(false), restartR_(false)
   {
   }

   void start(const std::string& type)
   {
      ClientEvent event(client_events::kBuildStarted);
      module_context::enqueClientEvent(event);

      isRunning_ = true;

      // read build options
      Error error = projects::projectContext().readBuildOptions(&options_);
      if (error)
      {
         terminateWithError("reading build options file", error);
         return;
      }

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

      const core::r_util::RProjectConfig& config = projectConfig();
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
      // get package info
      r_util::RPackageInfo pkgInfo;
      Error error = pkgInfo.read(packagePath);
      if (error)
      {
         terminateWithError("Reading package DESCRIPTION", error);
         return;
      }

      // bind a function that can be used to build the package
      boost::function<void()> buildFunction = boost::bind(
                         &Build::buildPackage, Build::shared_from_this(),
                             type, packagePath, pkgInfo, options, cb);


      // roxygenize first if necessary
      if (!projectConfig().packageRoxygenize.empty())
      {
         FilePath rScriptPath;
         Error error = module_context::rScriptPath(&rScriptPath);
         if (error)
         {
            terminateWithError("Locating R script", error);
            return;
         }

         // build the roxygenize command
         shell_utils::ShellCommand cmd(rScriptPath);
         cmd << "--slave";
         cmd << "--vanilla";
         cmd << "-e";
         std::string roxygenizeCall = buildRoxygenizeCall(pkgInfo);
         boost::format fmt(
            "suppressPackageStartupMessages({library(roxygen2); %1%;})");
         cmd << boost::str(fmt % roxygenizeCall);

         // show the user the call to roxygenize
         enqueCommandString(roxygenizeCall);

         // special callback for roxygenize result
         system::ProcessCallbacks roxygenizeCb = cb;
         roxygenizeCb.onExit =  boost::bind(&Build::onRoxygenizeCompleted,
                                            Build::shared_from_this(),
                                            _1,
                                            buildFunction);

         // run it
         module_context::processSupervisor().runCommand(cmd,
                                                        options,
                                                        roxygenizeCb);

      }
      else
      {
         // execute the build directly
         buildFunction();
      }
   }

   std::string buildRoxygenizeCall(const r_util::RPackageInfo& pkgInfo)
   {
      std::vector<std::string> roclets;
      boost::algorithm::split(roclets,
                              projectConfig().packageRoxygenize,
                              boost::algorithm::is_any_of(","));
      BOOST_FOREACH(std::string& roclet, roclets)
      {
         roclet = "'" + roclet + "'";
      }

      boost::format fmt("roxygenize('%1%', roclets=c(%2%))");
      return boost::str(fmt % pkgInfo.name() %
                              boost::algorithm::join(roclets, ", "));
   }

   void onRoxygenizeCompleted(int exitStatus,
                              const boost::function<void()>& buildFunction)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         enqueBuildOutput("Roxygen completed\n\n");
         buildFunction();
      }
      else
      {
         terminateWithErrorStatus(exitStatus);
      }
   }

   void buildPackage(const std::string& type,
                     const FilePath& packagePath,
                     const r_util::RPackageInfo& pkgInfo,
                     const core::system::ProcessOptions& options,
                     const core::system::ProcessCallbacks& cb)
   {
      // get R bin directory
      FilePath rBinDir;
      Error error = module_context::rBinDir(&rBinDir);
      if (error)
      {
         terminateWithError("attempting to locate R binary", error);
         return;
      }

      // build command
      if (type == "build-all")
      {
         // restart R after build is completed
         restartR_ = true;

         // build command
         RCommand rCmd(rBinDir);
         rCmd << "INSTALL";

         // add extra args if provided
         rCmd << projectConfig().packageInstallArgs;

         // add filename as a FilePath so it is escaped
         rCmd << FilePath(packagePath.filename());

         // show the user the command
         enqueCommandString(rCmd.commandString());

         // run R CMD INSTALL <package-dir>
         module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                        options,
                                                        cb);
      }

      else if (type == "build-source-package")
      {
         // compose the build command
         RCommand rCmd(rBinDir);
         rCmd << "build";

         // add extra args if provided
         rCmd << projectConfig().packageBuildArgs;

         // add filename as a FilePath so it is escaped
         rCmd << FilePath(packagePath.filename());

         // show the user the command
         enqueCommandString(rCmd.commandString());

         // set a success message
         successMessage_ = buildPackageSuccessMsg("Source");

         // run R CMD build <package-dir>
         module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                        options,
                                                        cb);
      }

      else if (type == "build-binary-package")
      {
         // compose the INSTALL --binary
         RCommand rCmd(rBinDir);
         rCmd << "INSTALL";
         rCmd << "--build";

         // add extra args if provided
         rCmd << projectConfig().packageInstallArgs;

         // add filename as a FilePath so it is escaped
         rCmd << FilePath(packagePath.filename());

         // show the user the command
         enqueCommandString(rCmd.commandString());

         // set a success message
         successMessage_ = "\n" + buildPackageSuccessMsg("Binary");

         // run R CMD INSTALL --build <package-dir>
         module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                        options,
                                                        cb);
      }

      else if (type == "check-package")
      {
         // first build then check

         // compose the build command
         RCommand rCmd(rBinDir);
         rCmd << "build";

         // add extra args if provided
         rCmd << projectConfig().packageBuildArgs;

         // add filename as a FilePath so it is escaped
         rCmd << FilePath(packagePath.filename());

         // compose the check command (will be executed by the onExit
         // handler of the build cmd)
         RCommand rCheckCmd(rBinDir);
         rCheckCmd << "check";

         // add extra args if provided
         rCheckCmd << projectConfig().packageCheckArgs;

         // add filename as a FilePath so it is escaped
         rCheckCmd << FilePath(pkgInfo.sourcePackageFilename());

         // special callback for build result
         system::ProcessCallbacks buildCb = cb;
         buildCb.onExit =  boost::bind(&Build::onBuildForCheckCompleted,
                                       Build::shared_from_this(),
                                       _1,
                                       rCheckCmd,
                                       options,
                                       buildCb);

         // show the user the command
         enqueCommandString(rCmd.commandString());

         // set a success message
         successMessage_ = "R CMD check succeeded\n";

         // bind a success function if appropriate
         if (options_.cleanupAfterCheck)
         {
            successFunction_ = boost::bind(&Build::cleanupAfterCheck,
                                           Build::shared_from_this(),
                                           pkgInfo);
         }

         // run the source build
         module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                        options,
                                                        buildCb);
      }
   }


   void onBuildForCheckCompleted(
                         int exitStatus,
                         const RCommand& checkCmd,
                         const core::system::ProcessOptions& checkOptions,
                         const core::system::ProcessCallbacks& checkCb)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         // show the user the buld command
         enqueCommandString(checkCmd.commandString());

         // run the check
         module_context::processSupervisor().runCommand(checkCmd.shellCommand(),
                                                        checkOptions,
                                                        checkCb);
      }
      else
      {
         terminateWithErrorStatus(exitStatus);
      }
   }


   void cleanupAfterCheck(const r_util::RPackageInfo& pkgInfo)
   {
      // compute paths
      FilePath buildPath = projectPath(projectConfig().packagePath).parent();
      FilePath srcPkgPath = buildPath.childPath(pkgInfo.sourcePackageFilename());
      FilePath chkDirPath = buildPath.childPath(pkgInfo.name() + ".Rcheck");

      // cleanup
      Error error = srcPkgPath.removeIfExists();
      if (error)
         LOG_ERROR(error);
      error = chkDirPath.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }

   void executeMakefileBuild(const std::string& type,
                             const core::system::ProcessOptions& options,
                             const core::system::ProcessCallbacks& cb)
   {
      std::string make = "make";
      if (!options_.makefileArgs.empty())
         make += " " + options_.makefileArgs;

      std::string makeClean = make + " clean";

      std::string cmd;
      if (type == "build-all")
      {
         cmd = make;
      }
      else if (type == "clean-all")
      {
         cmd = makeClean;
      }
      else if (type == "rebuild-all")
      {
         cmd = shell_utils::join_and(makeClean, make);
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

   void terminateWithErrorStatus(int exitStatus)
   {
      boost::format fmt("\nExited with status %1%.\n\n");
      enqueBuildOutput(boost::str(fmt % exitStatus));
      enqueBuildCompleted();
   }

   void terminateWithError(const std::string& context,
                           const Error& error)
   {
      std::string msg = "Error " + context + ": " + error.summary();
      terminateWithError(msg);
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
      if (exitStatus != EXIT_SUCCESS)
      {
         boost::format fmt("\nExited with status %1%.\n\n");
         enqueBuildOutput(boost::str(fmt % exitStatus));

         // never restart R after a failed build
         restartR_ = false;
      }
      else
      {
         if (!successMessage_.empty())
            enqueBuildOutput(successMessage_ + "\n");

         if (successFunction_)
            successFunction_();
      }

      enqueBuildCompleted();
   }

   void enqueBuildOutput(const std::string& output)
   {
      output_.append(output);

      ClientEvent event(client_events::kBuildOutput, output);
      module_context::enqueClientEvent(event);
   }

   void enqueCommandString(const std::string& cmd)
   {
      enqueBuildOutput("==> " + cmd + "\n\n");
   }

   void enqueBuildCompleted()
   {
      isRunning_ = false;

      // save the restart context if necessary
      if ((projectConfig().buildType == r_util::kBuildTypePackage) && restartR_)
      {
         FilePath packagePath = projectPath(projectConfig().packagePath);
         saveRestartContext(packagePath, output_);
      }

      ClientEvent event(client_events::kBuildCompleted, restartR_);
      module_context::enqueClientEvent(event);
   }

   const r_util::RProjectConfig& projectConfig()
   {
      return projects::projectContext().config();
   }

   std::string buildPackageSuccessMsg(const std::string& type)
   {
      FilePath writtenPath = projectPath(projectConfig().packagePath).parent();
      std::string written = module_context::createAliasedPath(writtenPath);
      if (written == "~")
         written = writtenPath.absolutePath();

      return type + " package written to " + written;
   }

private:
   bool isRunning_;
   bool terminationRequested_;
   std::string output_;
   projects::RProjectBuildOptions options_;
   std::string successMessage_;
   boost::function<void()> successFunction_;
   bool restartR_;
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


json::Value buildStateAsJson()
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

json::Value buildRestartContext()
{
   return collectRestartContext();
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

