/*
 * SessionBuild.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionBuild.hpp"

#include <vector>

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
#include <core/system/Environment.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/r_util/RPackageInfo.hpp>


#ifdef _WIN32
#include <core/r_util/RToolsInfo.hpp>
#endif

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RSessionUtils.hpp>
#include <r/session/RConsoleHistory.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionBuildEnvironment.hpp"
#include "SessionBuildErrors.hpp"
#include "SessionBuildUtils.hpp"
#include "SessionSourceCpp.hpp"

using namespace core;

namespace session {

namespace {

std::string quoteString(const std::string& str)
{
   return "'" + str + "'";
}

shell_utils::ShellCommand buildRCmd(const core::FilePath& rBinDir)
{
#if defined(_WIN32)
   shell_utils::ShellCommand rCmd(rBinDir.childPath("Rcmd.exe"));
#else
   shell_utils::ShellCommand rCmd(rBinDir.childPath("R"));
   rCmd << "CMD";
#endif
   return rCmd;
}


bool isPackageBuildError(const std::string& output)
{
   std::string input = boost::algorithm::trim_copy(output);
   return boost::algorithm::istarts_with(input, "warning: ") ||
          boost::algorithm::istarts_with(input, "error: ") ||
          boost::algorithm::ends_with(input, "WARNING");
}

// R command invocation -- has two representations, one to be submitted
// (shellCmd_) and one to show the user (cmdString_)
class RCommand
{
public:
   explicit RCommand(const FilePath& rBinDir)
      : shellCmd_(buildRCmd(rBinDir))
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

} // anonymous namespace

namespace modules { 
namespace build {

namespace {

// track whether to force a package rebuild. we do this if the user
// saves a header file (since the R CMD INSTALL makefile doesn't
// force a rebuild for those changes)
bool s_forcePackageRebuild = false;

bool isPackageHeaderFile(const FilePath& filePath)
{
   if (projects::projectContext().hasProject() &&
       (projects::projectContext().config().buildType ==
                                              r_util::kBuildTypePackage) &&
       boost::algorithm::starts_with(filePath.extensionLowerCase(), ".h"))
   {
      FilePath pkgPath = projects::projectContext().buildTargetPath();
      std::string pkgRelative = filePath.relativePath(pkgPath);
      if (boost::algorithm::starts_with(pkgRelative, "src"))
         return true;
      else if (boost::algorithm::starts_with(pkgRelative, "inst/include"))
        return true;
   }

   return false;
}

void onFileChanged(FilePath sourceFilePath)
{
   if (!s_forcePackageRebuild)
   {
      if (isPackageHeaderFile(sourceFilePath))
         s_forcePackageRebuild = true;
   }
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   if (!s_forcePackageRebuild)
   {
      for (size_t i=0; i<events.size(); i++)
      {
         FilePath filePath(events[i].fileInfo().absolutePath());
         onFileChanged(filePath);
      }
   }
}

bool collectForcePackageRebuild()
{
   if (s_forcePackageRebuild)
   {
      s_forcePackageRebuild = false;
      return true;
   }
   else
   {
      return false;
   }
}


const char * const kRoxygenizePackage = "roxygenize-package";
const char * const kBuildSourcePackage = "build-source-package";
const char * const kBuildBinaryPackage = "build-binary-package";
const char * const kCheckPackage = "check-package";
const char * const kBuildAndReload = "build-all";
const char * const kRebuildAll = "rebuild-all";

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
      cb.onStdout = boost::bind(&Build::onStandardOutput,
                                Build::shared_from_this(), _2);
      cb.onStderr = boost::bind(&Build::onStandardError,
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

      FilePath buildTargetPath = projects::projectContext().buildTargetPath();
      const core::r_util::RProjectConfig& config = projectConfig();
      if (config.buildType == r_util::kBuildTypePackage)
      {
         options.workingDir = buildTargetPath.parent();
         executePackageBuild(type, buildTargetPath, options, cb);
      }
      else if (config.buildType == r_util::kBuildTypeMakefile)
      {
         options.workingDir = buildTargetPath;
         executeMakefileBuild(type, buildTargetPath, options, cb);
      }
      else if (config.buildType == r_util::kBuildTypeCustom)
      {
         options.workingDir = buildTargetPath.parent();
         executeCustomBuild(type, buildTargetPath, options, cb);
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
      // validate that this is a package
      if (!r_util::isPackageDirectory(packagePath))
      {
         boost::format fmt ("ERROR: The build directory does "
                            "not contain a DESCRIPTION\n"
                            "file so cannot be built as a package.\n\n"
                            "Build directory: %1%\n");
         terminateWithError(boost::str(
                 fmt % module_context::createAliasedPath(packagePath)));
         return;
      }

      // get package info
      Error error = pkgInfo_.read(packagePath);
      if (error)
      {
         terminateWithError("Reading package DESCRIPTION", error);
         return;
      }

      // if this package links to Rcpp then we run compileAttributes
      if (pkgInfo_.linkingTo().find("Rcpp") != std::string::npos)
         if (!compileRcppAttributes(packagePath))
            return;

      if (type == kRoxygenizePackage)
      {
         if (roxygenize(packagePath))
            enqueBuildCompleted();
      }
      else
      {
         if (roxygenizeRequired(type))
         {
            if (!roxygenize(packagePath))
               return;
         }

         // build the package
         buildPackage(type, packagePath, options, cb);
      }
   }

   bool roxygenizeRequired(const std::string& type)
   {
      if (!projectConfig().packageRoxygenize.empty())
      {
         if ((type == kBuildAndReload || type == kRebuildAll) &&
             options_.autoRoxygenizeForBuildAndReload)
         {
            return true;
         }
         else if ( (type == kBuildSourcePackage ||
                    type == kBuildBinaryPackage) &&
                   options_.autoRoxygenizeForBuildPackage)
         {
            return true;
         }
         else if ( (type == kCheckPackage) &&
                   options_.autoRoxygenizeForCheck &&
                   !useDevtools())
         {
            return true;
         }
         else
         {
            return false;
         }
      }
      else
      {
         return false;
      }
   }

   bool roxygenize(const FilePath& packagePath)
   {
      // build the call to roxygenize
      std::vector<std::string> roclets;
      boost::algorithm::split(roclets,
                              projectConfig().packageRoxygenize,
                              boost::algorithm::is_any_of(","));
      BOOST_FOREACH(std::string& roclet, roclets)
      {
         roclet = "'" + roclet + "'";
      }

      boost::format fmt;
      if (useDevtools())
         fmt = boost::format(" devtools::document('.', roclets=c(%1%))");
      else
         fmt = boost::format(" roxygen2::roxygenize('.', roclets=c(%1%))");
      std::string roxygenizeCall = boost::str(
         fmt % boost::algorithm::join(roclets, ", "));

      // show the user the call to roxygenize
      enqueCommandString(roxygenizeCall);
      enqueBuildOutput(kBuildOutputNormal, "* checking for changes ... ");

      // format the command to send to R
      boost::format cmdFmt(
         "capture.output(suppressPackageStartupMessages("
            "{oldLC <- Sys.getlocale(category = 'LC_COLLATE'); "
            " Sys.setlocale(category = 'LC_COLLATE', locale = 'C'); "
            " on.exit(Sys.setlocale(category = 'LC_COLLATE', locale = oldLC));"
            " %1%; }"
          "))");
      std::string cmd = boost::str(cmdFmt % roxygenizeCall);


      // change to the package dir (and make sure we restore
      // before leaving the function)
      RestoreCurrentPathScope restorePathScope(
                                 module_context::safeCurrentPath());
      Error error = packagePath.makeCurrentPath();
      if (error)
      {
         terminateWithError("setting the working directory for roxygenize",
                            error);
         return false;
      }

      // execute it
      std::string output;
      error = r::exec::evaluateString(cmd, &output);
      if (error && (error.code() != r::errc::NoDataAvailableError))
      {
         enqueBuildOutput(kBuildOutputError, "ERROR\n\n");
         terminateWithError(r::endUserErrorMessage(error));
         return false;
      }
      else
      {
         // update progress
         enqueBuildOutput(kBuildOutputNormal, "DONE\n");

         // show output
         enqueBuildOutput(kBuildOutputNormal,
                          output + (output.empty() ? "\n" : "\n\n"));

         return true;
      }
   }

   bool compileRcppAttributes(const FilePath& packagePath)
   {
      if (module_context::haveRcppAttributes())
      {
         core::system::ProcessResult result;
         Error error = module_context::sourceModuleRFileWithResult(
                                             "SessionCompileAttributes.R",
                                             packagePath,
                                             &result);
         if (error)
         {
            LOG_ERROR(error);
            enqueCommandString("Rcpp::compileAttributes()");
            terminateWithError(r::endUserErrorMessage(error));
            return false;
         }
         else if (!result.stdOut.empty() || !result.stdErr.empty())
         {
            enqueCommandString("Rcpp::compileAttributes()");
            enqueBuildOutput(kBuildOutputNormal, result.stdOut);
            if (!result.stdErr.empty())
               enqueBuildOutput(kBuildOutputError, result.stdErr);
            enqueBuildOutput(kBuildOutputNormal, "\n");
            return result.exitStatus == EXIT_SUCCESS;
         }
         else
         {
            return true;
         }
      }
      else
      {
         return true;
      }
   }

   void buildPackage(const std::string& type,
                     const FilePath& packagePath,
                     const core::system::ProcessOptions& options,
                     const core::system::ProcessCallbacks& cb)
   {      

      // if this action is going to INSTALL the package then on
      // windows we need to unload the library first
#ifdef _WIN32
      if (packagePath.childPath("src").exists() &&
         (type == kBuildAndReload || type == kRebuildAll ||
          type == kBuildBinaryPackage))
      {
         std::string pkg = pkgInfo_.name();
         Error error = r::exec::RFunction(".rs.forceUnloadPackage", pkg).call();
         if (error)
            LOG_ERROR(error);
      }
#endif

      // use both the R and gcc error parsers
      CompileErrorParsers parsers;
      parsers.add(rErrorParser(packagePath.complete("R")));
      parsers.add(gccErrorParser(packagePath.complete("src")));
      initErrorParser(packagePath, parsers);

      // make a copy of options so we can customize the environment
      core::system::ProcessOptions pkgOptions(options);
      core::system::Options childEnv;
      core::system::environment(&childEnv);

      // allow child process to inherit our R_LIBS
      std::string libPaths = module_context::libPathsString();
      if (!libPaths.empty())
         core::system::setenv(&childEnv, "R_LIBS", libPaths);

      // prevent spurious cygwin warnings on windows
#ifdef _WIN32
      core::system::setenv(&childEnv, "CYGWIN", "nodosfilewarning");
#endif

      // set the not cran env var
      core::system::setenv(&childEnv, "NOT_CRAN", "true");

      // add r tools to path if necessary
      addRtoolsToPathIfNecessary(&childEnv, &postBuildWarning_);

      pkgOptions.environment = childEnv;

      // get R bin directory
      FilePath rBinDir;
      Error error = module_context::rBinDir(&rBinDir);
      if (error)
      {
         terminateWithError("attempting to locate R binary", error);
         return;
      }

      // install an error filter (because R package builds produce much
      // of their output on stderr)
      errorOutputFilterFunction_ = isPackageBuildError;

      // build command
      if (type == kBuildAndReload || type == kRebuildAll)
      {
         // restart R after build is completed
         restartR_ = true;

         // build command
         RCommand rCmd(rBinDir);
         rCmd << "INSTALL";

         // get extra args
         std::string extraArgs = projectConfig().packageInstallArgs;

         // add --preclean if this is a rebuild all
         if (collectForcePackageRebuild() || (type == kRebuildAll))
         {
            if (!boost::algorithm::contains(extraArgs, "--preclean"))
               rCmd << "--preclean";
         }

         // add extra args if provided
         rCmd << extraArgs;

         // add filename as a FilePath so it is escaped
         rCmd << FilePath(packagePath.filename());

         // show the user the command
         enqueCommandString(rCmd.commandString());

         // run R CMD INSTALL <package-dir>
         module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                        pkgOptions,
                                                        cb);
      }

      else if (type == kBuildSourcePackage)
      {
         // compose the build command
         RCommand rCmd(rBinDir);
         rCmd << "build";

         // add extra args if provided
         std::string extraArgs = projectConfig().packageBuildArgs;
         rCmd << extraArgs;

         // add filename as a FilePath so it is escaped
         rCmd << FilePath(packagePath.filename());

         // show the user the command
         enqueCommandString(rCmd.commandString());

         // set a success message
         successMessage_ = buildPackageSuccessMsg("Source");

         // run R CMD build <package-dir>
         module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                        pkgOptions,
                                                        cb);
      }

      else if (type == kBuildBinaryPackage)
      {
         // compose the INSTALL --binary
         RCommand rCmd(rBinDir);
         rCmd << "INSTALL";
         rCmd << "--build";
         rCmd << "--preclean";

         // add extra args if provided
         std::string extraArgs = projectConfig().packageBuildBinaryArgs;
         rCmd << extraArgs;

         // add filename as a FilePath so it is escaped
         rCmd << FilePath(packagePath.filename());

         // show the user the command
         enqueCommandString(rCmd.commandString());

         // set a success message
         successMessage_ = "\n" + buildPackageSuccessMsg("Binary");

         // run R CMD INSTALL --build <package-dir>
         module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                        pkgOptions,
                                                        cb);
      }

      else if (type == kCheckPackage)
      {
         if (useDevtools())
            devtoolsCheckPackage(packagePath, cb, pkgOptions);
         else
            checkPackage(rBinDir, packagePath, cb, pkgOptions);
      }
   }

   void checkPackage(const FilePath& rBinDir,
                     const FilePath& packagePath,
                     const core::system::ProcessCallbacks& cb,
                     const core::system::ProcessOptions& pkgOptions)
   {
      // first build then check

      // compose the build command
      RCommand rCmd(rBinDir);
      rCmd << "build";

      // add extra args if provided
      rCmd << projectConfig().packageBuildArgs;

      // add --no-manual and --no-build-vignettes if they are in the check options
      std::string checkArgs = projectConfig().packageCheckArgs;
      if (checkArgs.find("--no-manual") != std::string::npos)
         rCmd << "--no-manual";
      if (checkArgs.find("--no-build-vignettes") != std::string::npos)
         rCmd << "--no-build-vignettes";

      // add filename as a FilePath so it is escaped
      rCmd << FilePath(packagePath.filename());

      // compose the check command (will be executed by the onExit
      // handler of the build cmd)
      RCommand rCheckCmd(rBinDir);
      rCheckCmd << "check";

      // add extra args if provided
      std::string extraArgs = projectConfig().packageCheckArgs;
      rCheckCmd << extraArgs;

      // add filename as a FilePath so it is escaped
      rCheckCmd << FilePath(pkgInfo_.sourcePackageFilename());

      // special callback for build result
      core::system::ProcessCallbacks buildCb = cb;
      buildCb.onExit =  boost::bind(&Build::onBuildForCheckCompleted,
                                    Build::shared_from_this(),
                                    _1,
                                    rCheckCmd,
                                    pkgOptions,
                                    buildCb);

      // show the user the command
      enqueCommandString(rCmd.commandString());

      // set a success message
      successMessage_ = "R CMD check succeeded\n";

      // bind a success function if appropriate
      if (userSettings().cleanupAfterRCmdCheck())
      {
         successFunction_ = boost::bind(&Build::cleanupAfterCheck,
                                        Build::shared_from_this(),
                                        pkgInfo_);
      }

      if (userSettings().viewDirAfterRCmdCheck())
      {
         failureFunction_ = boost::bind(
                  &Build::viewDirAfterFailedCheck,
                  Build::shared_from_this(),
                  pkgInfo_);
      }

      // run the source build
      module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                     pkgOptions,
                                                     buildCb);
   }

   void devtoolsCheckPackage(const FilePath& packagePath,
                             const core::system::ProcessCallbacks& cb,
                             const core::system::ProcessOptions& pkgOptions)
   {
      // Find the path to R
      FilePath rProgramPath;
      Error error = module_context::rScriptPath(&rProgramPath);
      if (error)
      {
         terminateWithError("attempting to locate R binary", error);
         return;
      }

      // standard args
      std::vector<std::string> args;
      args.push_back("--slave");
      args.push_back("--no-save");
      args.push_back("--no-restore");

      // build the call to check
      std::ostringstream ostr;
      ostr << "devtools::check('" << packagePath.filename() << "'";
      if (projectConfig().packageRoxygenize.empty() ||
          !options_.autoRoxygenizeForCheck)
         ostr << ", document = FALSE";

      if (!userSettings().cleanupAfterRCmdCheck())
          ostr << ", cleanup = FALSE";

      // optional extra check args
      if (!projectConfig().packageCheckArgs.empty())
      {
         // additional args
         std::vector<std::string> extraArgs;
         boost::algorithm::split(extraArgs,
                                 projectConfig().packageCheckArgs,
                                 boost::is_space(),
                                 boost::algorithm::token_compress_on);

         std::vector<std::string> quotedArgs;
         std::transform(extraArgs.begin(),
                        extraArgs.end(),
                        std::back_inserter(quotedArgs),
                        quoteString);

         ostr << ", args = c(" << boost::algorithm::join(quotedArgs, ",");
         ostr << ")";
      }

      // optional extra build args
      if (!projectConfig().packageBuildArgs.empty())
      {
         std::vector<std::string> extraBuildArgs;
         boost::algorithm::split(extraBuildArgs,
                                 projectConfig().packageBuildArgs,
                                 boost::is_space(),
                                 boost::algorithm::token_compress_on);

         // propagate check vignette args
         // add --no-manual and --no-build-vignettes if they are specified
         std::string checkArgs = projectConfig().packageCheckArgs;
         if (checkArgs.find("--no-manual") != std::string::npos)
            extraBuildArgs.push_back("--no-manual");
         if (checkArgs.find("--no-build-vignettes") != std::string::npos)
            extraBuildArgs.push_back("--no-build-vignettes");

         std::vector<std::string> quotedArgs;
         std::transform(extraBuildArgs.begin(),
                        extraBuildArgs.end(),
                        std::back_inserter(quotedArgs),
                        quoteString);

         ostr << ", build_args = c(" << boost::algorithm::join(quotedArgs, ",");
         ostr << ")";
      }

      // enque the command string without the check_dir
      enqueCommandString(ostr.str() + ")");

      // now complete the command
      ostr << ", check_dir = '.')";
      args.push_back("-e");
      args.push_back(ostr.str());

      // set a success message
      successMessage_ = "\nR CMD check succeeded\n";

      if (userSettings().viewDirAfterRCmdCheck())
      {
         failureFunction_ = boost::bind(&Build::viewDirAfterFailedCheck,
                                        Build::shared_from_this(),
                                        pkgInfo_);
      }

      // run it
      module_context::processSupervisor().runProgram(
               string_utils::utf8ToSystem(rProgramPath.absolutePath()),
               args,
               pkgOptions,
               cb);
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
      FilePath buildPath = projects::projectContext().buildTargetPath().parent();
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

   void viewDirAfterFailedCheck(const r_util::RPackageInfo& pkgInfo)
   {
      FilePath buildPath = projects::projectContext().buildTargetPath().parent();
      FilePath chkDirPath = buildPath.childPath(pkgInfo.name() + ".Rcheck");

      json::Object dataJson;
      dataJson["directory"] = module_context::createAliasedPath(chkDirPath);
      dataJson["activate"] = true;
      ClientEvent event(client_events::kDirectoryNavigate, dataJson);

      module_context::enqueClientEvent(event);
   }

   void executeMakefileBuild(const std::string& type,
                             const FilePath& targetPath,
                             const core::system::ProcessOptions& options,
                             const core::system::ProcessCallbacks& cb)
   {
      // validate that there is a Makefile file
      FilePath makefilePath = targetPath.childPath("Makefile");
      if (!makefilePath.exists())
      {
         boost::format fmt ("ERROR: The build directory does "
                            "not contain a Makefile\n"
                            "so the target cannot be built.\n\n"
                            "Build directory: %1%\n");
         terminateWithError(boost::str(
                 fmt % module_context::createAliasedPath(targetPath)));
         return;
      }

      // install the gcc error parser
      initErrorParser(targetPath, gccErrorParser(targetPath));

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


   void terminateWithErrorStatus(int exitStatus)
   {
      boost::format fmt("\nExited with status %1%.\n\n");
      enqueBuildOutput(kBuildOutputError, boost::str(fmt % exitStatus));
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
      enqueBuildOutput(kBuildOutputError, msg);
      enqueBuildCompleted();
   }

   bool useDevtools()
   {
      if (projectConfig().packageUseDevtools)
      {
         bool haveRequiredDevtools = false;
         Error error = r::exec::RFunction(".rs.haveRequiredDevtools")
                                               .call(&haveRequiredDevtools);
         if (error)
            LOG_ERROR(error);
         return haveRequiredDevtools;
      }
      else
      {
         return false;
      }
   }

public:
   virtual ~Build()
   {
   }

   bool isRunning() const { return isRunning_; }

   const std::string& errorsBaseDir() const { return errorsBaseDir_; }
   const json::Array& errorsAsJson() const { return errorsJson_; }
   json::Array outputAsJson() const
   {
      json::Array outputJson;
      std::transform(output_.begin(),
                     output_.end(),
                     std::back_inserter(outputJson),
                     buildOutputAsJson);
      return outputJson;
   }

   std::string outputAsText()
   {
      std::string output;
      BOOST_FOREACH(const BuildOutput& buildOutput, output_)
      {
         output.append(buildOutput.output);
      }
      return output;
   }

   void terminate()
   {
      enqueBuildOutput(kBuildOutputNormal, "\n");
      terminationRequested_ = true;
   }

private:
   bool onContinue()
   {
      return !terminationRequested_;
   }

   void outputWithFilter(const std::string& output)
   {
      // split into lines
      std::vector<std::string> lines;
      boost::algorithm::split(lines, output,  boost::algorithm::is_any_of("\n"));

      // apply filter to each line
      int size = lines.size();
      for (int i=0; i<size; i++)
      {
         // apply filter
         std::string line = lines.at(i);
         int type = errorOutputFilterFunction_(line) ?
                                 kBuildOutputError : kBuildOutputNormal;

         // add newline if this wasn't the last line
         if (i != (size-1))
            line.append("\n");

         // enque the output
         enqueBuildOutput(type, line);
      }
   }

   void onStandardOutput(const std::string& output)
   {
      if (errorOutputFilterFunction_)
         outputWithFilter(output);
      else
         enqueBuildOutput(kBuildOutputNormal, output);
   }

   void onStandardError(const std::string& output)
   {
      if (errorOutputFilterFunction_)
         outputWithFilter(output);
      else
         enqueBuildOutput(kBuildOutputError, output);
   }

   void onCompleted(int exitStatus)
   {
      // call the error parser if one has been specified
      if (errorParser_)
      {
         std::vector<CompileError> errors = errorParser_(outputAsText());
         if (!errors.empty())
         {
            errorsJson_ = compileErrorsAsJson(errors);
            enqueBuildErrors(errorsJson_);
         }
      }

      if (exitStatus != EXIT_SUCCESS)
      {
         boost::format fmt("\nExited with status %1%.\n\n");
         enqueBuildOutput(kBuildOutputError, boost::str(fmt % exitStatus));

         // if this is a package build then check if we can build
         // C++ code at all
         if (!pkgInfo_.empty() && postBuildWarning_.empty())
         {
            if (!module_context::canBuildCpp())
            {
               postBuildWarning_ =
                 "WARNING: The tools required to build R packages "
                 "are not currently installed. Additional information on "
                 "installing the required tools for your platform can be "
                 "found here:\n\n"
                 "http://www.rstudio.com/ide/docs/packages/prerequisites";
            }
         }

         // never restart R after a failed build
         restartR_ = false;

         // take other actions
         if (failureFunction_)
            failureFunction_();
      }
      else
      {
         if (!successMessage_.empty())
            enqueBuildOutput(kBuildOutputNormal, successMessage_ + "\n");

         if (successFunction_)
            successFunction_();
      }

      enqueBuildCompleted();
   }

   void enqueBuildOutput(int type, const std::string& output)
   {
      BuildOutput buildOutput(type, output);

      output_.push_back(buildOutput);

      ClientEvent event(client_events::kBuildOutput,
                        buildOutputAsJson(buildOutput));

      module_context::enqueClientEvent(event);
   }

   void enqueCommandString(const std::string& cmd)
   {
      enqueBuildOutput(kBuildOutputCommand, "==> " + cmd + "\n\n");
   }

   void enqueBuildErrors(const json::Array& errors)
   {
      json::Object jsonData;
      jsonData["base_dir"] = errorsBaseDir_;
      jsonData["errors"] = errors;

      ClientEvent event(client_events::kBuildErrors, jsonData);
      module_context::enqueClientEvent(event);
   }

   void enqueBuildCompleted()
   {
      isRunning_ = false;

      if (!postBuildWarning_.empty())
         enqueBuildOutput(kBuildOutputError, postBuildWarning_ + "\n\n");

      // enque event
      std::string afterRestartCommand;
      if (restartR_)
         afterRestartCommand = "library(" + pkgInfo_.name() + ")";
      json::Object dataJson;
      dataJson["restart_r"] = restartR_;
      dataJson["after_restart_command"] = afterRestartCommand;
      ClientEvent event(client_events::kBuildCompleted, dataJson);
      module_context::enqueClientEvent(event);
   }

   const r_util::RProjectConfig& projectConfig()
   {
      return projects::projectContext().config();
   }

   std::string buildPackageSuccessMsg(const std::string& type)
   {
      FilePath writtenPath = projects::projectContext().buildTargetPath().parent();
      std::string written = module_context::createAliasedPath(writtenPath);
      if (written == "~")
         written = writtenPath.absolutePath();

      return type + " package written to " + written;
   }

   void initErrorParser(const FilePath& baseDir, CompileErrorParser parser)
   {
      // set base dir -- make sure it ends with a / so the slash is
      // excluded from error display
      errorsBaseDir_ = module_context::createAliasedPath(baseDir);
      if (!errorsBaseDir_.empty() &&
          !boost::algorithm::ends_with(errorsBaseDir_, "/"))
      {
         errorsBaseDir_.append("/");
      }

      errorParser_ = parser;
   }

private:
   bool isRunning_;
   bool terminationRequested_;
   std::vector<BuildOutput> output_;
   CompileErrorParser errorParser_;
   std::string errorsBaseDir_;
   json::Array errorsJson_;
   r_util::RPackageInfo pkgInfo_;
   projects::RProjectBuildOptions options_;
   std::string successMessage_;
   std::string postBuildWarning_;
   boost::function<void()> successFunction_;
   boost::function<void()> failureFunction_;
   boost::function<bool(const std::string&)> errorOutputFilterFunction_;
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

Error getCppCapabilities(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   json::Object capsJson;
   capsJson["can_build"] = module_context::canBuildCpp();
   capsJson["can_source_cpp"] = module_context::haveRcppAttributes();
   pResponse->setResult(capsJson);

   return Success();
}

Error devtoolsLoadAllPath(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   // compute the path to use for devtools::load_all
   std::string loadAllPath;

   // start with the build target path
   FilePath buildTargetPath = projects::projectContext().buildTargetPath();
   FilePath currentPath = module_context::safeCurrentPath();

   // if the build target path and the current working directory
   // are the same then return "."
   if (buildTargetPath == currentPath)
   {
      loadAllPath = ".";
   }
   else if (buildTargetPath.isWithin(currentPath))
   {
      loadAllPath = buildTargetPath.relativePath(currentPath);
   }
   else
   {
      loadAllPath = module_context::createAliasedPath(buildTargetPath);
   }

   pResponse->setResult(loadAllPath);

   return Success();
}


struct BuildContext
{
   bool empty() const { return errors.empty() && outputs.empty(); }
   std::string errorsBaseDir;
   json::Array errors;
   json::Array outputs;
};

BuildContext s_suspendBuildContext;


void writeBuildContext(const BuildContext& buildContext,
                       core::Settings* pSettings)
{
   std::ostringstream ostr;
   json::write(buildContext.outputs, ostr);
   pSettings->set("build-last-outputs", ostr.str());

   std::ostringstream ostrErrors;
   json::write(buildContext.errors, ostrErrors);
   pSettings->set("build-last-errors", ostrErrors.str());

   pSettings->set("build-last-errors-base-dir", buildContext.errorsBaseDir);
}

void onSuspend(core::Settings* pSettings)
{
   if (s_pBuild)
   {
      BuildContext buildContext;
      buildContext.outputs = s_pBuild->outputAsJson();
      buildContext.errors = s_pBuild->errorsAsJson();
      buildContext.errorsBaseDir = s_pBuild->errorsBaseDir();
      writeBuildContext(buildContext, pSettings);
   }
   else if (!s_suspendBuildContext.empty())
   {
      writeBuildContext(s_suspendBuildContext, pSettings);
   }
   else
   {
      BuildContext emptyBuildContext;
      writeBuildContext(emptyBuildContext, pSettings);
   }
}

void onResume(const core::Settings& settings)
{
   std::string buildLastOutputs = settings.get("build-last-outputs");
   if (!buildLastOutputs.empty())
   {
      json::Value outputsJson;
      if (json::parse(buildLastOutputs, &outputsJson) &&
          json::isType<json::Array>(outputsJson))
      {
         s_suspendBuildContext.outputs = outputsJson.get_array();
      }
   }

   s_suspendBuildContext.errorsBaseDir = settings.get("build-last-errors-base-dir");
   std::string buildLastErrors = settings.get("build-last-errors");
   if (!buildLastErrors.empty())
   {
      json::Value errorsJson;
      if (json::parse(buildLastErrors, &errorsJson) &&
          json::isType<json::Array>(errorsJson))
      {
         s_suspendBuildContext.errors = errorsJson.get_array();
      }
   }
}


SEXP rs_canBuildCpp()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(module_context::canBuildCpp(), &rProtect);
}

std::string s_previousPath;
SEXP rs_restorePreviousPath()
{
#ifdef _WIN32
    if (!s_previousPath.empty())
        core::system::setenv("PATH", s_previousPath);
    s_previousPath.clear();
#endif
    return R_NilValue;
}

SEXP rs_addRToolsToPath()
{
#ifdef _WIN32
    s_previousPath = core::system::getenv("PATH");
    std::string newPath = s_previousPath;
    std::string warningMsg;
    build::addRtoolsToPathIfNecessary(&newPath, &warningMsg);
    core::system::setenv("PATH", newPath);

#endif
    return R_NilValue;
}

SEXP rs_installPackage(SEXP pkgPathSEXP, SEXP libPathSEXP)
{
   // get R bin directory
   FilePath rBinDir;
   Error error = module_context::rBinDir(&rBinDir);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }

   // run command
   RCommand installCommand(rBinDir);
   installCommand << "INSTALL";
   installCommand << "-l";
   installCommand << "\"" + r::sexp::asString(libPathSEXP) + "\"";
   installCommand << "\"" + r::sexp::asString(pkgPathSEXP) + "\"";
   core::system::ProcessResult result;
   error = core::system::runCommand(installCommand.commandString(),
                                    core::system::ProcessOptions(),
                                    &result);
   if (error)
      LOG_ERROR(error);
   if ((result.exitStatus != EXIT_SUCCESS) && !result.stdErr.empty())
      LOG_ERROR_MESSAGE("Error install package: " + result.stdErr);

   return R_NilValue;
}


} // anonymous namespace

json::Value buildStateAsJson()
{
   if (s_pBuild)
   {
      json::Object stateJson;
      stateJson["running"] = s_pBuild->isRunning();
      stateJson["outputs"] = s_pBuild->outputAsJson();
      stateJson["errors_base_dir"] = s_pBuild->errorsBaseDir();
      stateJson["errors"] = s_pBuild->errorsAsJson();
      return stateJson;
   }
   else if (!s_suspendBuildContext.empty())
   {
      json::Object stateJson;
      stateJson["running"] = false;
      stateJson["outputs"] = s_suspendBuildContext.outputs;
      stateJson["errors_base_dir"] = s_suspendBuildContext.errorsBaseDir;
      stateJson["errors"] = s_suspendBuildContext.errors;
      return stateJson;
   }
   else
   {
      return json::Value();
   }
}

void onDeferredInit(bool newSession)
{
   if (newSession)
   {
      // if we are on mavericks then provide an .R/Makevars that points
      // to clang if necessary
      using namespace module_context;
      FilePath makevarsPath = userHomePath().childPath(".R/Makevars");
      if (isOSXMavericks() && !makevarsPath.exists() && !canBuildCpp())
      {
         Error error = makevarsPath.parent().ensureDirectory();
         if (!error)
         {
            std::string makevars = "CC=clang\nCXX=clang++\n";
            error = core::writeStringToFile(makevarsPath, makevars);
            if (error)
               LOG_ERROR(error);
         }
         else
         {
            LOG_ERROR(error);
         }
      }
   }
}

Error initialize()
{
   R_CallMethodDef canBuildMethodDef ;
   canBuildMethodDef.name = "rs_canBuildCpp" ;
   canBuildMethodDef.fun = (DL_FUNC) rs_canBuildCpp ;
   canBuildMethodDef.numArgs = 0;
   r::routines::addCallMethod(canBuildMethodDef);

   R_CallMethodDef addRToolsToPathMethodDef ;
   addRToolsToPathMethodDef.name = "rs_addRToolsToPath" ;
   addRToolsToPathMethodDef.fun = (DL_FUNC) rs_addRToolsToPath ;
   addRToolsToPathMethodDef.numArgs = 0;
   r::routines::addCallMethod(addRToolsToPathMethodDef);

   R_CallMethodDef restorePreviousPathMethodDef ;
   restorePreviousPathMethodDef.name = "rs_restorePreviousPath" ;
   restorePreviousPathMethodDef.fun = (DL_FUNC) rs_restorePreviousPath ;
   restorePreviousPathMethodDef.numArgs = 0;
   r::routines::addCallMethod(restorePreviousPathMethodDef);

   R_CallMethodDef installPackageMethodDef ;
   installPackageMethodDef.name = "rs_installPackage" ;
   installPackageMethodDef.fun = (DL_FUNC) rs_installPackage;
   installPackageMethodDef.numArgs = 2;
   r::routines::addCallMethod(installPackageMethodDef);

   // subscribe to deferredInit for build tools fixup
   module_context::events().onDeferredInit.connect(onDeferredInit);

   // subscribe to file monitor and source editor file saved so we
   // can tickle a flag to indicates when we should force an R
   // package rebuild
   session::projects::FileMonitorCallbacks cb;
   cb.onFilesChanged = onFilesChanged;
   projects::projectContext().subscribeToFileMonitor("", cb);
   module_context::events().onSourceEditorFileSaved.connect(onFileChanged);

   // add suspend handler
   addSuspendHandler(module_context::SuspendHandler(boost::bind(onSuspend, _2),
                                                    onResume));

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "start_build", startBuild))
      (bind(registerRpcMethod, "terminate_build", terminateBuild))
      (bind(registerRpcMethod, "get_cpp_capabilities", getCppCapabilities))
      (bind(registerRpcMethod, "devtools_load_all_path", devtoolsLoadAllPath))
      (bind(sourceModuleRFile, "SessionBuild.R"))
      (bind(source_cpp::initialize));
   return initBlock.execute();
}


} // namespace build
} // namespace modules

namespace module_context {

bool haveRcppAttributes()
{
   bool haveAttributes = false;
   Error error = r::exec::RFunction(".rs.haveRcppAttributes")
                                                        .call(&haveAttributes);
   if (error)
      LOG_ERROR(error);
   return haveAttributes;
}

bool canBuildCpp()
{
   // try to build a simple c file to test whether we have build tools available
   FilePath cppPath = module_context::tempFile("test", "c");
   Error error = core::writeStringToFile(cppPath, "void test() {}\n");
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // get R bin directory
   FilePath rBinDir;
   error = module_context::rBinDir(&rBinDir);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // try to run build tools
   RCommand rCmd(rBinDir);
   rCmd << "SHLIB";
   rCmd << cppPath.filename();

   core::system::ProcessOptions options;
   options.workingDir = cppPath.parent();
   core::system::Options childEnv;
   core::system::environment(&childEnv);
   std::string warningMsg;
   modules::build::addRtoolsToPathIfNecessary(&childEnv, &warningMsg);
   options.environment = childEnv;

   core::system::ProcessResult result;
   error = core::system::runCommand(rCmd.commandString(), options, &result);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return result.exitStatus == EXIT_SUCCESS;
}

}

} // namesapce session

