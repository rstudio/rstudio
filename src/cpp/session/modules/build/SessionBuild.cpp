/*
 * SessionBuild.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include "session-config.h"

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

#include <session/SessionOptions.hpp>

#ifdef _WIN32
#include <core/r_util/RToolsInfo.hpp>
#endif

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>
#include <r/session/RSessionUtils.hpp>
#include <r/session/RConsoleHistory.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "SessionBuildErrors.hpp"
#include "SessionSourceCpp.hpp"
#include "SessionInstallRtools.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace {

static bool s_canBuildCpp = false;

std::string preflightPackageBuildErrorMessage(
      const std::string& message,
      const FilePath& buildDirectory)
{
   std::string fmt =
R"EOF(ERROR: Package build failed.

%1%

Build directory: %2%
)EOF";
   
   auto formatter = boost::format(fmt)
         % message
         % module_context::createAliasedPath(buildDirectory);
   return boost::str(formatter);
}

std::string quoteString(const std::string& str)
{
   return "'" + str + "'";
}

std::string packageArgsVector(std::string args)
{
   // spilt the string
   boost::algorithm::trim(args);
   std::vector<std::string> argList;
   boost::algorithm::split(argList,
                           args,
                           boost::is_space(),
                           boost::algorithm::token_compress_on);

   // quote the args
   std::vector<std::string> quotedArgs;
   std::transform(argList.begin(),
                  argList.end(),
                  std::back_inserter(quotedArgs),
                  quoteString);

   std::ostringstream ostr;
   ostr << "c(" << boost::algorithm::join(quotedArgs, ",") << ")";
   return ostr.str();
}

bool isPackageBuildError(const std::string& output)
{
   std::string input = boost::algorithm::trim_copy(output);
   return boost::algorithm::istarts_with(input, "warning: ") ||
          boost::algorithm::istarts_with(input, "error: ") ||
          boost::algorithm::ends_with(input, "WARNING");
}


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
       (boost::algorithm::starts_with(filePath.getExtensionLowerCase(), ".h") ||
        filePath.getExtensionLowerCase() == ".stan"))
   {
      FilePath pkgPath = projects::projectContext().buildTargetPath();
      std::string pkgRelative = filePath.getRelativePath(pkgPath);
      if (boost::algorithm::starts_with(pkgRelative, "src"))
         return true;
      else if (boost::algorithm::starts_with(pkgRelative, "inst/include"))
        return true;
   }

   return false;
}

void onFileChanged(FilePath sourceFilePath)
{
   // set package rebuild flag
   if (!s_forcePackageRebuild)
   {
      if (isPackageHeaderFile(sourceFilePath))
         s_forcePackageRebuild = true;
   }
}

void onSourceEditorFileSaved(FilePath sourceFilePath)
{
   onFileChanged(sourceFilePath);

   // see if this is a website file and fire an event if it is
   if (module_context::isWebsiteProject())
   {
      // see if the option is enabled for live preview
      projects::RProjectBuildOptions options;
      Error error = projects::projectContext().readBuildOptions(&options);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      FilePath buildTargetPath = projects::projectContext().buildTargetPath();
      if (sourceFilePath.isWithin(buildTargetPath))
      {
         std::string outputDir = module_context::websiteOutputDir();
         FilePath outputDirPath = buildTargetPath.completeChildPath(outputDir);
         if (outputDir.empty() || !sourceFilePath.isWithin(outputDirPath))
         {
            // are we live previewing?
            bool livePreview = options.livePreviewWebsite;

            // force live preview for JS and CSS
            std::string mimeType = sourceFilePath.getMimeContentType();
            if (mimeType == "text/css" || mimeType == "text/javascript")
               livePreview = true;

            if (livePreview)
            {
               json::Object fileJson =
                   module_context::createFileSystemItem(sourceFilePath);
               ClientEvent event(client_events::kWebsiteFileSaved, fileJson);
               module_context::enqueClientEvent(event);
            }
         }
      }
   }
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   if (!s_forcePackageRebuild)
   {
      for (const auto &event : events) {
         FilePath filePath(event.fileInfo().absolutePath());
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
const char * const kTestPackage = "test-package";
const char * const kCheckPackage = "check-package";
const char * const kBuildAndReload = "build-all";
const char * const kRebuildAll = "rebuild-all";
const char * const kTestFile = "test-file";
const char * const kTestShiny = "test-shiny";
const char * const kTestShinyFile = "test-shiny-file";

class Build : boost::noncopyable,
              public boost::enable_shared_from_this<Build>
{
public:
   static boost::shared_ptr<Build> create(const std::string& type,
                                          const std::string& subType)
   {
      boost::shared_ptr<Build> pBuild(new Build());
      pBuild->start(type, subType);
      return pBuild;
   }

private:
   Build()
      : isRunning_(false), terminationRequested_(false), restartR_(false),
        usedDevtools_(false), openErrorList_(true)
   {
   }

   void start(const std::string& type, const std::string& subType)
   {
      json::Object dataJson;
      dataJson["type"] = type;
      dataJson["sub_type"] = subType;
      ClientEvent event(client_events::kBuildStarted, dataJson);
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
      executeBuild(type, subType, cb);
   }


   void executeBuild(const std::string& type,
                     const std::string& subType,
                     const core::system::ProcessCallbacks& cb)
   {
      // options
      core::system::ProcessOptions options;

#ifndef _WIN32
      options.terminateChildren = true;
#endif

      // notify build process of build-pane width
      core::system::Options environment;
      core::system::environment(&environment);
      int buildWidth = r::options::getBuildOptionWidth();
      if (buildWidth > 0)
         core::system::setenv(&environment, "RSTUDIO_CONSOLE_WIDTH",
                              safe_convert::numberToString(buildWidth));
      else
         core::system::unsetenv(&environment, "RSTUDIO_CONSOLE_WIDTH");

      FilePath buildTargetPath = projects::projectContext().buildTargetPath();
      const core::r_util::RProjectConfig& config = projectConfig();
      if (type == kTestFile)
      {
         options.environment = environment;
         options.workingDir = buildTargetPath.getParent();
         FilePath testPath = FilePath(subType);
         executePackageBuild(type, testPath, options, cb);
      }
      else if (type == kTestShiny || type == kTestShinyFile)
      {
         FilePath testPath = FilePath(subType);
         testShiny(testPath, options, cb, type);
      }
      else if (config.buildType == r_util::kBuildTypePackage)
      {
         options.environment = environment;
         options.workingDir = buildTargetPath.getParent();
         executePackageBuild(type, buildTargetPath, options, cb);
      }
      else if (config.buildType == r_util::kBuildTypeMakefile)
      {
         options.environment = environment;
         options.workingDir = buildTargetPath;
         executeMakefileBuild(type, buildTargetPath, options, cb);
      }
      else if (config.buildType == r_util::kBuildTypeWebsite)
      {
         options.workingDir = buildTargetPath;
         
         // pass along R_LIBS
         std::string rLibs = module_context::libPathsString();
         if (!rLibs.empty())
            core::system::setenv(&environment, "R_LIBS", rLibs);

         // pass along RSTUDIO_VERSION
         core::system::setenv(&environment, "RSTUDIO_VERSION", RSTUDIO_VERSION);

         options.environment = environment;
         
         executeWebsiteBuild(type, subType, buildTargetPath, options, cb);
      }
      else if (config.buildType == r_util::kBuildTypeCustom)
      {
         options.environment = environment;
         options.workingDir = buildTargetPath.getParent();
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
      if (type == kTestFile)
      {
          // try to read package from /tests/testthat/filename.R,
          // but ignore errors if not within a package
          FilePath maybePackage = module_context::resolveAliasedPath(
             packagePath.getParent().getParent().getParent().getAbsolutePath()
          );

          pkgInfo_.read(maybePackage);
      }
      else
      {
         // validate that this is a package
         if (!packagePath.completeChildPath("DESCRIPTION").exists())
         {
            std::string message =
                  "The build directory does not contain a DESCRIPTION file and so "
                  "cannot be built as a package.";
            
            terminateWithError(preflightPackageBuildErrorMessage(message, packagePath));
            return;
         }

         // get package info
         Error error = pkgInfo_.read(packagePath);
         if (error)
         {
            // check to see if this was a parse error; if so, report that
            std::string parseError = error.getProperty("parse-error");
            if (!parseError.empty())
            {
               std::string message = "Failed to parse DESCRIPTION: " + parseError;
               terminateWithError(preflightPackageBuildErrorMessage(message, packagePath));
            }
            else
            {
               terminateWithError("reading package DESCRIPTION", error);
            }
            
            return;
         }

         // if this package links to Rcpp then we run compileAttributes
         if (pkgInfo_.linkingTo().find("Rcpp") != std::string::npos)
            if (!compileRcppAttributes(packagePath))
               return;
      }

      if (type == kRoxygenizePackage)
      {
         successMessage_ = "Documentation completed";
         roxygenize(packagePath, options, cb);
      }
      else
      {
         // bind a function that can be used to build the package
         boost::function<void()> buildFunction = boost::bind(
                         &Build::buildPackage, Build::shared_from_this(),
                         type, packagePath, options, cb);

         if (roxygenizeRequired(type))
         {
            // special callback for roxygenize result
            core::system::ProcessCallbacks roxygenizeCb = cb;
            roxygenizeCb.onExit =  boost::bind(&Build::onRoxygenizeCompleted,
                                               Build::shared_from_this(),
                                               _1,
                                               buildFunction);

            // run it
            roxygenize(packagePath, options, roxygenizeCb);
         }
         else
         {
            buildFunction();
         }
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


   std::string buildRoxygenizeCall()
   {
      // build the call to roxygenize
      std::vector<std::string> roclets;
      boost::algorithm::split(roclets,
                              projectConfig().packageRoxygenize,
                              boost::algorithm::is_any_of(","));

      // remove vignette roclet if we don't have the requisite roxygen2 version
      bool haveVignetteRoclet = module_context::isPackageVersionInstalled(
                                                   "roxygen2", "4.1.0.9001");
      if (!haveVignetteRoclet)
      {
         auto it = std::find(roclets.begin(), roclets.end(), "vignette");
         if (it != roclets.end())
            roclets.erase(it);
      }
      
      for (std::string& roclet : roclets)
      {
         roclet = "'" + roclet + "'";
      }

      boost::format fmt;
      if (useDevtools())
         fmt = boost::format("devtools::document(roclets = c(%1%))");
      else
         fmt = boost::format("roxygen2::roxygenize('.', roclets = c(%1%))");
      std::string roxygenizeCall = boost::str(
         fmt % boost::algorithm::join(roclets, ", "));

      // show the user the call to roxygenize
      enqueCommandString(roxygenizeCall);

      // format the command to send to R
      boost::format cmdFmt(
         "suppressPackageStartupMessages("
            "{oldLC <- Sys.getlocale(category = 'LC_COLLATE'); "
            " Sys.setlocale(category = 'LC_COLLATE', locale = 'C'); "
            " on.exit(Sys.setlocale(category = 'LC_COLLATE', locale = oldLC));"
            " %1%; }"
          ")");
      return boost::str(cmdFmt % roxygenizeCall);
   }

   void onRoxygenizeCompleted(int exitStatus,
                              const boost::function<void()>& buildFunction)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         std::string msg = "Documentation completed\n\n";
         enqueBuildOutput(module_context::kCompileOutputNormal, msg);
         buildFunction();
      }
      else
      {
         terminateWithErrorStatus(exitStatus);
      }
   }


   void roxygenize(const FilePath& packagePath,
                   core::system::ProcessOptions options,
                   const core::system::ProcessCallbacks& cb)
   {
      FilePath rScriptPath;
      Error error = module_context::rScriptPath(&rScriptPath);
      if (error)
      {
         terminateWithError("Locating R script", error);
         return;
      }

      // check for required version of roxygen
      if (!module_context::isMinimumRoxygenInstalled())
      {
         terminateWithError("roxygen2 v4.0 (or later) required to "
                            "generate documentation");
      }

      // make a copy of options so we can customize the environment
      core::system::Options childEnv;
      if (options.environment)
         childEnv = *options.environment;
      else
         core::system::environment(&childEnv);

      // allow child process to inherit our R_LIBS
      std::string libPaths = module_context::libPathsString();
      if (!libPaths.empty())
         core::system::setenv(&childEnv, "R_LIBS", libPaths);
      
      options.environment = childEnv;
      
      // build the roxygenize command
      shell_utils::ShellCommand cmd(rScriptPath);
      cmd << "--slave";
      cmd << "--vanilla";
      cmd << "-e";
      cmd << buildRoxygenizeCall();

      // use the package working dir
      options.workingDir = packagePath;

      // run it
      module_context::processSupervisor().runCommand(cmd,
                                                     options,
                                                     cb);
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
            enqueBuildOutput(module_context::kCompileOutputNormal,
                             result.stdOut);
            if (!result.stdErr.empty())
               enqueBuildOutput(module_context::kCompileOutputError,
                                result.stdErr);
            enqueBuildOutput(module_context::kCompileOutputNormal, "\n");
            if (result.exitStatus == EXIT_SUCCESS)
            {
               return true;
            }
            else
            {
               terminateWithErrorStatus(result.exitStatus);
               return false;
            }
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
      if (packagePath.completeChildPath("src").exists() &&
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
      parsers.add(rErrorParser(packagePath.completePath("R")));
      parsers.add(gccErrorParser(packagePath.completePath("src")));

      // track build type
      type_ = type;

      // add testthat and shinytest result parsers
      if (type == kTestFile) {
         openErrorList_ = false;
         parsers.add(testthatErrorParser(packagePath.getParent()));
      }
      else if (type == kTestPackage) {
         openErrorList_ = false;
         parsers.add(testthatErrorParser(packagePath.completePath("tests/testthat")));
      }

      initErrorParser(packagePath, parsers);

      // make a copy of options so we can customize the environment
      core::system::ProcessOptions pkgOptions(options);
      core::system::Options childEnv;
      if (options.environment)
         childEnv = *options.environment;
      else
         core::system::environment(&childEnv);

      // allow child process to inherit our R_LIBS
      std::string libPaths = module_context::libPathsString();
      if (!libPaths.empty())
         core::system::setenv(&childEnv, "R_LIBS", libPaths);
      
      // record the library paths used when this build was kicked off
      libPaths_ = module_context::getLibPaths();

      // prevent spurious cygwin warnings on windows
#ifdef _WIN32
      core::system::setenv(&childEnv, "CYGWIN", "nodosfilewarning");
#endif

      // set the not cran env var
      core::system::setenv(&childEnv, "NOT_CRAN", "true");

      // turn off external applications launching
      core::system::setenv(&childEnv, "R_BROWSER", "false");
      core::system::setenv(&childEnv, "R_PDFVIEWER", "false");

      // add r tools to path if necessary
      module_context::addRtoolsToPathIfNecessary(&childEnv, &buildToolsWarning_);

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
         module_context::RCommand rCmd(rBinDir);
         rCmd << "INSTALL";

         // get extra args
         std::string extraArgs = projectConfig().packageInstallArgs;

         // add --preclean if this is a rebuild all
         if (collectForcePackageRebuild() || (type == kRebuildAll))
         {
            if (!boost::algorithm::contains(extraArgs, "--preclean"))
               rCmd << "--preclean";
         }

         // remove --with-keep.source if this is R < 2.14
         if (!r::util::hasRequiredVersion("2.14"))
         {
            using namespace boost::algorithm;
            replace_all(extraArgs, "--with-keep.source", "");
            replace_all(extraArgs, "--without-keep.source", "");
         }

         // add extra args if provided
         rCmd << extraArgs;

         // add filename as a FilePath so it is escaped
         rCmd << FilePath(packagePath.getFilename());

         // show the user the command
         enqueCommandString(rCmd.commandString());

         // run R CMD INSTALL <package-dir>
         module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                        pkgOptions,
                                                        cb);
      }

      else if (type == kBuildSourcePackage)
      {
         if (useDevtools())
         {
            devtoolsBuildPackage(packagePath, false, pkgOptions, cb);
         }
         else
         {
            if (session::options().packageOutputInPackageFolder())
            {
               pkgOptions.workingDir = packagePath;
            }
            buildSourcePackage(rBinDir, packagePath, pkgOptions, cb);
         }
      }

      else if (type == kBuildBinaryPackage)
      {
         if (useDevtools())
         {
            devtoolsBuildPackage(packagePath, true, pkgOptions, cb);
         }
         else
         {
            if (session::options().packageOutputInPackageFolder())
            {
               pkgOptions.workingDir = packagePath;
            }
            buildBinaryPackage(rBinDir, packagePath, pkgOptions, cb);
         }
      }

      else if (type == kCheckPackage)
      {
         if (useDevtools())
         {
            devtoolsCheckPackage(packagePath, pkgOptions, cb);
         }
         else
         {
            if (session::options().packageOutputInPackageFolder())
            {
               pkgOptions.workingDir = packagePath;
            }
            checkPackage(rBinDir, packagePath, pkgOptions, cb);
         }
      }

      else if (type == kTestPackage)
      {

         if (useDevtools())
            devtoolsTestPackage(packagePath, pkgOptions, cb);
         else
            testPackage(packagePath, pkgOptions, cb);
      }

      else if (type == kTestFile)
      {
         testFile(packagePath, pkgOptions, cb);
      }
   }

   void buildSourcePackage(const FilePath& rBinDir,
                           const FilePath& packagePath,
                           const core::system::ProcessOptions& pkgOptions,
                           const core::system::ProcessCallbacks& cb)
   {
      // compose the build command
      module_context::RCommand rCmd(rBinDir);
      rCmd << "build";

      // add extra args if provided
      std::string extraArgs = projectConfig().packageBuildArgs;
      rCmd << extraArgs;

      // add filename as a FilePath so it is escaped
      if (session::options().packageOutputInPackageFolder())
         rCmd << FilePath(".");
      else
         rCmd << FilePath(packagePath.getFilename());

      // show the user the command
      enqueCommandString(rCmd.commandString());

      // set a success message
      successMessage_ = buildPackageSuccessMsg("Source");

      // run R CMD build <package-dir>
      module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                     pkgOptions,
                                                     cb);

   }


   void buildBinaryPackage(const FilePath& rBinDir,
                           const FilePath& packagePath,
                           const core::system::ProcessOptions& pkgOptions,
                           const core::system::ProcessCallbacks& cb)
   {
      // compose the INSTALL --binary
      module_context::RCommand rCmd(rBinDir);
      rCmd << "INSTALL";
      rCmd << "--build";
      rCmd << "--preclean";

      // add extra args if provided
      std::string extraArgs = projectConfig().packageBuildBinaryArgs;
      rCmd << extraArgs;

      // add filename as a FilePath so it is escaped
      if (session::options().packageOutputInPackageFolder())
         rCmd << FilePath(".");
      else
         rCmd << FilePath(packagePath.getFilename());

      // show the user the command
      enqueCommandString(rCmd.commandString());

      // set a success message
      successMessage_ = "\n" + buildPackageSuccessMsg("Binary");

      // run R CMD INSTALL --build <package-dir>
      module_context::processSupervisor().runCommand(rCmd.shellCommand(),
                                                     pkgOptions,
                                                     cb);
   }

   void checkPackage(const FilePath& rBinDir,
                     const FilePath& packagePath,
                     const core::system::ProcessOptions& pkgOptions,
                     const core::system::ProcessCallbacks& cb)
   {
      // first build then check

      // compose the build command
      module_context::RCommand rCmd(rBinDir);
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
      if (session::options().packageOutputInPackageFolder())
         rCmd << FilePath(".");
      else
         rCmd << FilePath(packagePath.getFilename());

      // compose the check command (will be executed by the onExit
      // handler of the build cmd)
      module_context::RCommand rCheckCmd(rBinDir);
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
      if (prefs::userPrefs().cleanupAfterRCmdCheck())
      {
         successFunction_ = boost::bind(&Build::cleanupAfterCheck,
                                        Build::shared_from_this(),
                                        pkgInfo_);
      }

      if (prefs::userPrefs().viewDirAfterRCmdCheck())
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

   bool rExecute(const std::string& command,
                 const FilePath& workingDir,
                 core::system::ProcessOptions pkgOptions,
                 bool vanilla,
                 const core::system::ProcessCallbacks& cb)
   {
      // Find the path to R
      FilePath rProgramPath;
      Error error = module_context::rScriptPath(&rProgramPath);
      if (error)
      {
         terminateWithError("attempting to locate R binary", error);
         return false;
      }

      // execute within the package directory
      pkgOptions.workingDir = workingDir;

      // build args
      std::vector<std::string> args;
      args.push_back("--slave");
      if (vanilla)
         args.push_back("--vanilla");
      args.push_back("-e");
      args.push_back(command);

      // run it
      module_context::processSupervisor().runProgram(
               string_utils::utf8ToSystem(rProgramPath.getAbsolutePath()),
               args,
               pkgOptions,
               cb);

      return true;
   }

   bool devtoolsExecute(const std::string& command,
                        const FilePath& packagePath,
                        core::system::ProcessOptions pkgOptions,
                        const core::system::ProcessCallbacks& cb)
   {
      if (!rExecute(command, packagePath, pkgOptions, true /* --vanilla */, cb))
         return false;

      usedDevtools_ = true;
      return true;
   }

   void devtoolsCheckPackage(const FilePath& packagePath,
                             const core::system::ProcessOptions& pkgOptions,
                             const core::system::ProcessCallbacks& cb)
   {
      // build the call to check
      std::ostringstream ostr;
      ostr << "devtools::check(";

      std::vector<std::string> args;

      if (projectConfig().packageRoxygenize.empty() ||
          !options_.autoRoxygenizeForCheck)
         args.push_back("document = FALSE");

      if (!prefs::userPrefs().cleanupAfterRCmdCheck())
         args.push_back("cleanup = FALSE");

      // optional extra check args
      if (!projectConfig().packageCheckArgs.empty())
      {
         args.push_back("args = " +
                        packageArgsVector(projectConfig().packageCheckArgs));
      }

      // optional extra build args
      if (!projectConfig().packageBuildArgs.empty())
      {
         // propagate check vignette args
         // add --no-manual and --no-build-vignettes if they are specified
         std::string buildArgs = projectConfig().packageBuildArgs;
         std::string checkArgs = projectConfig().packageCheckArgs;
         if (checkArgs.find("--no-manual") != std::string::npos)
            buildArgs.append(" --no-manual");
         if (checkArgs.find("--no-build-vignettes") != std::string::npos)
            buildArgs.append(" --no-build-vignettes");

         args.push_back("build_args = " + packageArgsVector(buildArgs));
      }

      // add the args
      ostr << boost::algorithm::join(args, ", ");

      // enque the command string without the check_dir
      enqueCommandString(ostr.str() + ")");

      // now complete the command
      if (session::options().packageOutputInPackageFolder())
         ostr << ", check_dir = getwd())";
      else
         ostr << ", check_dir = dirname(getwd()))";
      std::string command = ostr.str();

      // set a success message
      successMessage_ = "\nR CMD check succeeded\n";

      // bind a success function if appropriate
      if (prefs::userPrefs().cleanupAfterRCmdCheck())
      {
         successFunction_ = boost::bind(&Build::cleanupAfterCheck,
                                        Build::shared_from_this(),
                                        pkgInfo_);
      }

      if (prefs::userPrefs().viewDirAfterRCmdCheck())
      {
         failureFunction_ = boost::bind(&Build::viewDirAfterFailedCheck,
                                        Build::shared_from_this(),
                                        pkgInfo_);
      }

      // run it
      devtoolsExecute(command, packagePath, pkgOptions, cb);
   }

   void devtoolsTestPackage(const FilePath& packagePath,
                            const core::system::ProcessOptions& pkgOptions,
                            const core::system::ProcessCallbacks& cb)
   {
      std::string command = "devtools::test()";
      enqueCommandString(command);
      devtoolsExecute(command, packagePath, pkgOptions, cb);
   }

   void testPackage(const FilePath& packagePath,
                    core::system::ProcessOptions pkgOptions,
                    const core::system::ProcessCallbacks& cb)
   {
      FilePath rScriptPath;
      Error error = module_context::rScriptPath(&rScriptPath);
      if (error)
      {
         terminateWithError("Locating R script", error);
         return;
      }

      // navigate to the tests directory and source all R
      // scripts within
      FilePath testsPath = packagePath.completePath("tests");

      // construct a shell command to execute
      shell_utils::ShellCommand cmd(rScriptPath);
      cmd << "--slave";
      cmd << "--vanilla";
      cmd << "-e";
      std::vector<std::string> rSourceCommands;
      
      boost::format fmt(
         "setwd('%1%');"
         "files <- list.files(pattern = '[.][rR]$');"
         "invisible(lapply(files, function(x) {"
         "    system(paste(shQuote('%2%'), '--vanilla --slave -f', shQuote(x)))"
         "}))"
      );

      cmd << boost::str(fmt %
                           testsPath.getAbsolutePath() %
                        rScriptPath.getAbsolutePath());

      pkgOptions.workingDir = testsPath;
      enqueCommandString("Sourcing R files in 'tests' directory");
      successMessage_ = "\nTests complete";
      module_context::processSupervisor().runCommand(cmd,
                                                     pkgOptions,
                                                     cb);

   }

   void testFile(const FilePath& testPath,
                 core::system::ProcessOptions pkgOptions,
                 const core::system::ProcessCallbacks& cb)
   {
      FilePath rScriptPath;
      Error error = module_context::rScriptPath(&rScriptPath);
      if (error)
      {
         terminateWithError("Locating R script", error);
         return;
      }

      // construct a shell command to execute
      shell_utils::ShellCommand cmd(rScriptPath);
      cmd << "--slave";
      cmd << "--vanilla";
      cmd << "-e";
      std::vector<std::string> rSourceCommands;
      
      boost::format fmt(
         "if (nzchar('%1%')) devtools::load_all(dirname('%2%'));"
         "testthat::test_file('%2%')"
      );

      std::string testPathEscaped = 
         string_utils::singleQuotedStrEscape(string_utils::utf8ToSystem(
            testPath.getAbsolutePath()));

      cmd << boost::str(fmt %
                        pkgInfo_.name() %
                        testPathEscaped);

      enqueCommandString("Testing R file using 'testthat'");
      successMessage_ = "\nTest complete";
      module_context::processSupervisor().runCommand(cmd,
                                                     pkgOptions,
                                                     cb);

   }

   void testShiny(FilePath& shinyPath,
                  core::system::ProcessOptions testOptions,
                  const core::system::ProcessCallbacks& cb,
                  const std::string& type)
   {
      // normalize paths between all tests and single test
      std::string shinyTestName;
      if (type == kTestShinyFile) {
        shinyTestName = shinyPath.getFilename();
        shinyPath = shinyPath.getParent();
        if (shinyPath.getFilename() == "shinytests" ||
            shinyPath.getFilename() == "shinytest")
        {
           // In newer versions of shinytest, tests are stored in a "shinytest" or "shinytests"
           // folder under the "tests" folder.
           shinyPath = shinyPath.getParent();
        }
        if (shinyPath.getFilename() == "tests")
        {
           // Move up from the tests folder to the app folder.
           shinyPath = shinyPath.getParent();
        }
        else
        {
           // If this doesn't look like it's in a tests directory, bail out.
           terminateWithError("Could not find Shiny app for test in " + 
              shinyPath.getAbsolutePath());
        }
      }

      // get temp path to store rds results
      FilePath tempPath;
      Error error = FilePath::tempFilePath(tempPath);
      if (error)
      {
         terminateWithError("Find temp dir", error);
         return;
      }
      error = tempPath.ensureDirectory();
      if (error)
      {
         terminateWithError("Creating temp dir", error);
         return;
      }
      FilePath tempRdsFile = tempPath.completePath(core::system::generateUuid() + ".rds");

      // initialize parser
      CompileErrorParsers parsers;
      parsers.add(shinytestErrorParser(shinyPath, tempRdsFile));
      initErrorParser(shinyPath, parsers);

      FilePath rScriptPath;
      error = module_context::rScriptPath(&rScriptPath);
      if (error)
      {
         terminateWithError("Locating R script", error);
         return;
      }

      // construct a shell command to execute
      shell_utils::ShellCommand cmd(rScriptPath);
      cmd << "--slave";
      cmd << "--vanilla";
      cmd << "-e";
      std::vector<std::string> rSourceCommands;
      
      if (type == kTestShiny) {
        boost::format fmt(
           "result <- shinytest::testApp('%1%');"
           "saveRDS(result, '%2%')"
        );

        cmd << boost::str(fmt %
                             shinyPath.getAbsolutePath() %
                          tempRdsFile.getAbsolutePath());
      } else if (type == kTestShinyFile) {
        boost::format fmt(
           "result <- shinytest::testApp('%1%', '%2%');"
           "saveRDS(result, '%3%')"
        );

        cmd << boost::str(fmt %
                             shinyPath.getAbsolutePath() %
                          shinyTestName %
                          tempRdsFile.getAbsolutePath());
      } else {
        terminateWithError("Shiny test type is unsupported.");
      }

      enqueCommandString("Testing Shiny application using 'shinytest'");
      successMessage_ = "\nTest complete";
      module_context::processSupervisor().runCommand(cmd,
                                                     testOptions,
                                                     cb);

   }

   void devtoolsBuildPackage(const FilePath& packagePath,
                             bool binary,
                             const core::system::ProcessOptions& pkgOptions,
                             const core::system::ProcessCallbacks& cb)
   {
      // create the call to build
      std::ostringstream ostr;
      ostr << "devtools::build(";

      // args
      std::vector<std::string> args;

      // binary package?
      if (binary)
         args.push_back("binary = TRUE");

      if (session::options().packageOutputInPackageFolder())
         args.push_back("path = getwd()");

       // add R args
      std::string rArgs = binary ?  projectConfig().packageBuildBinaryArgs :
                                    projectConfig().packageBuildArgs;
      if (binary)
         rArgs.append(" --preclean");
      if (!rArgs.empty())
         args.push_back("args = " + packageArgsVector(rArgs));

      ostr << boost::algorithm::join(args, ", ");
      ostr << ")";

      // set a success message
      std::string type = binary ? "Binary" : "Source";
      successMessage_ = "\n" + buildPackageSuccessMsg(type);

      // execute it
      std::string command = ostr.str();
      enqueCommandString(command);
      devtoolsExecute(command, packagePath, pkgOptions, cb);
   }


   void onBuildForCheckCompleted(
                         int exitStatus,
                         const module_context::RCommand& checkCmd,
                         const core::system::ProcessOptions& checkOptions,
                         const core::system::ProcessCallbacks& checkCb)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         // show the user the build command
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
      FilePath buildPath = projects::projectContext().buildTargetPath();
      if (!session::options().packageOutputInPackageFolder())
         buildPath = buildPath.getParent();
      FilePath srcPkgPath = buildPath.completeChildPath(pkgInfo.sourcePackageFilename());
      FilePath chkDirPath = buildPath.completeChildPath(pkgInfo.name() + ".Rcheck");

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
      if (!terminationRequested_)
      {
         FilePath buildPath = projects::projectContext().buildTargetPath();
         if (!session::options().packageOutputInPackageFolder())
            buildPath = buildPath.getParent();
         FilePath chkDirPath = buildPath.completeChildPath(pkgInfo.name() + ".Rcheck");

         json::Object dataJson;
         dataJson["directory"] = module_context::createAliasedPath(chkDirPath);
         dataJson["activate"] = true;
         ClientEvent event(client_events::kDirectoryNavigate, dataJson);

         module_context::enqueClientEvent(event);
      }
   }

   void executeMakefileBuild(const std::string& type,
                             const FilePath& targetPath,
                             const core::system::ProcessOptions& options,
                             const core::system::ProcessCallbacks& cb)
   {
      // validate that there is a Makefile file
      FilePath makefilePath = targetPath.completeChildPath("Makefile");
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

   void executeCustomBuild(const std::string& /*type*/,
                           const FilePath& customScriptPath,
                           const core::system::ProcessOptions& options,
                           const core::system::ProcessCallbacks& cb)
   {
      module_context::processSupervisor().runCommand(
                           shell_utils::ShellCommand(customScriptPath),
                           options,
                           cb);
   }


   void executeWebsiteBuild(const std::string& type,
                            const std::string& subType,
                            const FilePath& websitePath,
                            const core::system::ProcessOptions& options,
                            const core::system::ProcessCallbacks& cb)
   {
      std::string command;

      if (type == "build-all")
      {
         if (options_.previewWebsite)
         {
            successFunction_ = boost::bind(&Build::showWebsitePreview,
                                           Build::shared_from_this(),
                                           websitePath);
         }

         // if there is a subType then use it to set the output format
         if (!subType.empty())
         {
            projects::projectContext().setWebsiteOutputFormat(subType);
            options_.websiteOutputFormat = subType;
         }

         boost::format fmt("rmarkdown::render_site(%1%)");
         std::string format;
         if (options_.websiteOutputFormat != "all")
            format = "output_format = '" + options_.websiteOutputFormat + "', ";

         format += ("encoding = '" +
                    projects::projectContext().defaultEncoding() +
                    "'");

         command = boost::str(fmt % format);
      }
      else if (type == "clean-all")
      {
         command = "rmarkdown::clean_site()";
      }

      // execute command
      enqueCommandString(command);
      rExecute(command, websitePath, options, false /* --vanilla */, cb);
   }

   void showWebsitePreview(const FilePath& websitePath)
   {
      // determine source file
      std::string output = outputAsText();
      FilePath sourceFile = websitePath.completeChildPath("index.Rmd");
      if (!sourceFile.exists())
         sourceFile = websitePath.completeChildPath("index.md");

      // look for Output created message
      FilePath outputFile = module_context::extractOutputFileCreated(sourceFile,
                                                                     output);
      if (!outputFile.isEmpty())
      {
         json::Object previewRmdJson;
         using namespace module_context;
         previewRmdJson["source_file"] = createAliasedPath(sourceFile);
         previewRmdJson["encoding"] = projects::projectContext().config().encoding;
         previewRmdJson["output_file"] = createAliasedPath(outputFile);
         ClientEvent event(client_events::kPreviewRmd, previewRmdJson);
         enqueClientEvent(event);
      }
   }

   void terminateWithErrorStatus(int exitStatus)
   {
      boost::format fmt("\nExited with status %1%.\n\n");
      enqueBuildOutput(module_context::kCompileOutputError,
                       boost::str(fmt % exitStatus));
      enqueBuildCompleted();
   }

   void terminateWithError(const std::string& context,
                           const Error& error)
   {
      std::string msg = "Error " + context + ": " + error.getSummary();
      terminateWithError(msg);
   }

   void terminateWithError(const std::string& msg)
   {
      enqueBuildOutput(module_context::kCompileOutputError, msg);
      enqueBuildCompleted();
   }

   bool useDevtools()
   {
      return projectConfig().packageUseDevtools &&
             module_context::isMinimumDevtoolsInstalled();
   }

public:
   virtual ~Build() = default;

   bool isRunning() const { return isRunning_; }

   const std::string& errorsBaseDir() const { return errorsBaseDir_; }
   const json::Array& errorsAsJson() const { return errorsJson_; }
   json::Array outputAsJson() const
   {
      json::Array outputJson;
      std::transform(output_.begin(),
                     output_.end(),
                     std::back_inserter(outputJson),
                     module_context::compileOutputAsJson);
      return outputJson;
   }
   const std::string type() const { return type_; }

   std::string outputAsText()
   {
      std::string output;
      for (const module_context::CompileOutput& compileOutput : output_)
      {
         output.append(compileOutput.output);
      }
      return output;
   }

   void terminate()
   {
      enqueBuildOutput(module_context::kCompileOutputNormal, "\n");
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
      size_t size = lines.size();
      for (size_t i = 0; i < size; i++)
      {
         // apply filter
         using namespace module_context;
         std::string line = lines.at(i);
         int type = errorOutputFilterFunction_(line) ?
                                 kCompileOutputError : kCompileOutputNormal;

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
         enqueBuildOutput(module_context::kCompileOutputNormal, output);
   }

   void onStandardError(const std::string& output)
   {
      if (errorOutputFilterFunction_)
         outputWithFilter(output);
      else
         enqueBuildOutput(module_context::kCompileOutputError, output);
   }

   void onCompleted(int exitStatus)
   {
      using namespace module_context;

      // call the error parser if one has been specified
      if (errorParser_)
      {
         std::vector<SourceMarker> errors = errorParser_(outputAsText());
         if (!errors.empty())
         {
            errorsJson_ = sourceMarkersAsJson(errors);
            enqueBuildErrors(errorsJson_);
         }
      }

      if (exitStatus != EXIT_SUCCESS)
      {
         boost::format fmt("\nExited with status %1%.\n\n");
         enqueBuildOutput(kCompileOutputError, boost::str(fmt % exitStatus));

         // if this is a package build then check for ability to
         // build C++ code at all
         if (!pkgInfo_.empty() && !module_context::canBuildCpp())
         {
            // prompted install of Rtools on Windows (but don't prompt if
            // we used devtools since it likely has it's own prompt)
#ifdef _WIN32
            if (!usedDevtools_)
               module_context::installRBuildTools("Building R packages");
#endif
         }

         // if this is a package build then try to clean up a left
         // behind 00LOCK directory. note that R uses the directory name
         // and not the actual package name for the lockfile (and these can
         // and do differ in some cases)
         if (!pkgInfo_.empty() && !libPaths_.empty())
         {
            std::string pkgFolder = projects::projectContext().buildTargetPath().getFilename();
            FilePath libPath = libPaths_[0];
            FilePath lockPath = libPath.completeChildPath("00LOCK-" + pkgFolder);
            lockPath.removeIfExists();
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
            enqueBuildOutput(kCompileOutputNormal, successMessage_ + "\n");

         if (successFunction_)
            successFunction_();
      }

      enqueBuildCompleted();
   }

   void enqueBuildOutput(int type, const std::string& output)
   {
      module_context::CompileOutput compileOutput(type, output);

      output_.push_back(compileOutput);

      ClientEvent event(client_events::kBuildOutput,
                        compileOutputAsJson(compileOutput));

      module_context::enqueClientEvent(event);
   }

   void enqueCommandString(const std::string& cmd)
   {
      enqueBuildOutput(module_context::kCompileOutputCommand,
                       "==> " + cmd + "\n\n");
   }

   void enqueBuildErrors(const json::Array& errors)
   {
      json::Object jsonData;
      jsonData["base_dir"] = errorsBaseDir_;
      jsonData["errors"] = errors;
      jsonData["open_error_list"] = openErrorList_;
      jsonData["type"] = type_;

      ClientEvent event(client_events::kBuildErrors, jsonData);
      module_context::enqueClientEvent(event);
   }

   std::string parseLibrarySwitchFromInstallArgs()
   {
      std::string libPath;

      std::string extraArgs = projectConfig().packageInstallArgs;
      std::size_t n = extraArgs.size();
      std::size_t index = extraArgs.find("--library=");

      if (index != std::string::npos &&
          index < n - 2) // ensure some space for path
      {
         std::size_t startIndex = index + std::string("--library=").length();
         std::size_t endIndex = startIndex + 1;

         // The library path can be specified with quotes + spaces, or without
         // quotes (but no spaces), so handle both cases.
         char firstChar = extraArgs[startIndex];
         if (firstChar == '\'' || firstChar == '\"')
         {
            while (++endIndex < n)
            {
               // skip escaped characters
               if (extraArgs[endIndex] == '\\')
               {
                  ++endIndex;
                  continue;
               }

               if (extraArgs[endIndex] == firstChar)
                  break;
            }

            libPath = extraArgs.substr(startIndex + 1, endIndex - startIndex - 1);
         }
         else
         {
            while (++endIndex < n)
            {
               if (isspace(extraArgs[endIndex]))
                  break;
            }
            libPath = extraArgs.substr(startIndex, endIndex - startIndex + 1);
         }
      }
      return libPath;
   }
   
   void enqueBuildCompleted()
   {
      isRunning_ = false;

      if (!buildToolsWarning_.empty())
      {
         enqueBuildOutput(module_context::kCompileOutputError,
                          buildToolsWarning_ + "\n\n");
      }

      // enque event
      std::string afterRestartCommand;
      if (restartR_)
      {
         afterRestartCommand = "library(" + pkgInfo_.name();
         
         // if --library="" was specified and we're not in devmode,
         // use it
         if (!(r::session::utils::isPackratModeOn() ||
               r::session::utils::isDevtoolsDevModeOn()))
         {
            std::string libPath = parseLibrarySwitchFromInstallArgs();
            if (!libPath.empty())
               afterRestartCommand += ", lib.loc = \"" + libPath + "\"";
         }
         
         afterRestartCommand += ")";
      }
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
      FilePath writtenPath = projects::projectContext().buildTargetPath();
      if (!session::options().packageOutputInPackageFolder())
         writtenPath = writtenPath.getParent();
      std::string written = module_context::createAliasedPath(writtenPath);
      if (written == "~")
         written = writtenPath.getAbsolutePath();

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
   std::vector<module_context::CompileOutput> output_;
   CompileErrorParser errorParser_;
   std::string errorsBaseDir_;
   json::Array errorsJson_;
   r_util::RPackageInfo pkgInfo_;
   projects::RProjectBuildOptions options_;
   std::vector<FilePath> libPaths_;
   std::string successMessage_;
   std::string buildToolsWarning_;
   boost::function<void()> successFunction_;
   boost::function<void()> failureFunction_;
   boost::function<bool(const std::string&)> errorOutputFilterFunction_;
   bool restartR_;
   bool usedDevtools_;
   bool openErrorList_;
   std::string type_;
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
   std::string type, subType;
   Error error = json::readParams(request.params, &type, &subType);
   if (error)
      return error;

   // if we have a build already running then just return false
   if (isBuildRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      s_pBuild = Build::create(type, subType);
      pResponse->setResult(true);
   }

   return Success();
}



Error terminateBuild(const json::JsonRpcRequest& /*request*/,
                     json::JsonRpcResponse* pResponse)
{
   if (isBuildRunning())
      s_pBuild->terminate();

   pResponse->setResult(true);

   return Success();
}

Error getCppCapabilities(const json::JsonRpcRequest& /*request*/,
                         json::JsonRpcResponse* pResponse)
{
   json::Object capsJson;
   capsJson["can_build"] = module_context::canBuildCpp();
   capsJson["can_source_cpp"] = module_context::haveRcppAttributes();
   pResponse->setResult(capsJson);

   return Success();
}

Error installBuildTools(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   // get param
   std::string action;
   Error error = json::readParam(request.params, 0, &action);
   if (error)
      return error;

   pResponse->setResult(module_context::installRBuildTools(action));

   return Success();
}

Error devtoolsLoadAllPath(const json::JsonRpcRequest& /*request*/,
                     json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(module_context::pathRelativeTo(
            module_context::safeCurrentPath(),
            projects::projectContext().buildTargetPath()));

   return Success();
}


struct BuildContext
{
   bool empty() const { return errors.isEmpty() && outputs.isEmpty(); }
   std::string errorsBaseDir;
   json::Array errors;
   json::Array outputs;
   std::string type;
};

BuildContext s_suspendBuildContext;


void writeBuildContext(const BuildContext& buildContext,
                       core::Settings* pSettings)
{
   pSettings->set("build-last-outputs", buildContext.outputs.write());
   pSettings->set("build-last-errors", buildContext.errors.write());
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
      buildContext.type = s_pBuild->type();
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
      if (!outputsJson.parse(buildLastOutputs) &&
          json::isType<json::Array>(outputsJson))
      {
         s_suspendBuildContext.outputs = outputsJson.getValue<json::Array>();
      }
   }

   s_suspendBuildContext.errorsBaseDir = settings.get("build-last-errors-base-dir");
   std::string buildLastErrors = settings.get("build-last-errors");
   if (!buildLastErrors.empty())
   {
      json::Value errorsJson;
      if (!errorsJson.parse(buildLastErrors) &&
          json::isType<json::Array>(errorsJson))
      {
         s_suspendBuildContext.errors = errorsJson.getValue<json::Array>();
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
    bool result = module_context::addRtoolsToPathIfNecessary(&newPath, &warningMsg);
    if (!warningMsg.empty())
       REprintf("%s\n", warningMsg.c_str());
    core::system::setenv("PATH", newPath);
    r::sexp::Protect protect;
    return r::sexp::create(result, &protect);
#endif
    return R_NilValue;
}

#ifdef _WIN32

SEXP rs_installBuildTools()
{
   Error error = installRtools();
   if (error)
      LOG_ERROR(error);

   return R_NilValue;
}

#elif __APPLE__

SEXP rs_installBuildTools()
{
   if (module_context::isMacOS())
   {
      if (!module_context::hasMacOSCommandLineTools())
      {
         core::system::ProcessResult result;
         Error error = core::system::runCommand(
                  "/usr/bin/xcode-select --install",
                  core::system::ProcessOptions(),
                  &result);
         if (error)
            LOG_ERROR(error);
      }
   }
   else
   {
      ClientEvent event = browseUrlEvent(
          "https://www.rstudio.org/links/install_osx_build_tools");
      module_context::enqueClientEvent(event);
   }
   return R_NilValue;
}

#else

SEXP rs_installBuildTools()
{
   return R_NilValue;
}

#endif


SEXP rs_installPackage(SEXP pkgPathSEXP, SEXP libPathSEXP)
{
   using namespace rstudio::r::sexp;
   Error error = module_context::installPackage(safeAsString(pkgPathSEXP),
                                                safeAsString(libPathSEXP));
   if (error)
   {
      std::string desc = error.getProperty("description");
      if (!desc.empty())
         module_context::consoleWriteError(desc + "\n");
      LOG_ERROR(error);
   }

   return R_NilValue;
}

Error getBookdownFormats(const json::JsonRpcRequest& /*request*/,
                         json::JsonRpcResponse* pResponse)
{
   json::Object responseJson;
   responseJson["output_format"] = projects::projectContext().buildOptions().websiteOutputFormat;
   responseJson["website_output_formats"] = projects::websiteOutputFormatsJson();
   pResponse->setResult(responseJson);
   return Success();
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
      stateJson["type"] = s_pBuild->type();
      stateJson["errors"] = s_pBuild->errorsAsJson();
      return std::move(stateJson);
   }
   else if (!s_suspendBuildContext.empty())
   {
      json::Object stateJson;
      stateJson["running"] = false;
      stateJson["outputs"] = s_suspendBuildContext.outputs;
      stateJson["errors_base_dir"] = s_suspendBuildContext.errorsBaseDir;
      stateJson["type"] = s_suspendBuildContext.type;
      stateJson["errors"] = s_suspendBuildContext.errors;
      return std::move(stateJson);
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
      FilePath makevarsPath = userHomePath().completeChildPath(".R/Makevars");
      if (isMacOS() && !makevarsPath.exists() && !canBuildCpp())
      {
         Error error = makevarsPath.getParent().ensureDirectory();
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
   // register .Call methods
   RS_REGISTER_CALL_METHOD(rs_canBuildCpp);
   RS_REGISTER_CALL_METHOD(rs_addRToolsToPath);
   RS_REGISTER_CALL_METHOD(rs_restorePreviousPath);
   RS_REGISTER_CALL_METHOD(rs_installPackage);
   RS_REGISTER_CALL_METHOD(rs_installBuildTools);

   // subscribe to deferredInit for build tools fixup
   module_context::events().onDeferredInit.connect(onDeferredInit);

   // subscribe to file monitor and source editor file saved so we
   // can tickle a flag to indicates when we should force an R
   // package rebuild
   session::projects::FileMonitorCallbacks cb;
   cb.onFilesChanged = onFilesChanged;
   projects::projectContext().subscribeToFileMonitor("", cb);
   module_context::events().onSourceEditorFileSaved.connect(onSourceEditorFileSaved);

   // add suspend handler
   addSuspendHandler(module_context::SuspendHandler(boost::bind(onSuspend, _2),
                                                    onResume));

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "start_build", startBuild))
      (bind(registerRpcMethod, "terminate_build", terminateBuild))
      (bind(registerRpcMethod, "get_cpp_capabilities", getCppCapabilities))
      (bind(registerRpcMethod, "install_build_tools", installBuildTools))
      (bind(registerRpcMethod, "devtools_load_all_path", devtoolsLoadAllPath))
      (bind(registerRpcMethod, "get_bookdown_formats", getBookdownFormats))
      (bind(sourceModuleRFile, "SessionBuild.R"))
      (bind(source_cpp::initialize));
   return initBlock.execute();
}


} // namespace build
} // namespace modules

namespace module_context {

#ifdef __APPLE__
namespace {

bool usingSystemMake()
{
   return findProgram("make").getAbsolutePath() == "/usr/bin/make";
}

} // anonymous namespace
#endif

bool haveRcppAttributes()
{
   return module_context::isPackageVersionInstalled("Rcpp", "0.10.1");
}

bool canBuildCpp()
{
   if (s_canBuildCpp)
      return true;

#ifdef __APPLE__
   // NOTE: on macOS, R normally requests user install and use its own
   // LLVM toolchain; however, that toolchain still needs to re-use
   // system headers provided by the default macOS toolchain, and so
   // we still want to check for macOS command line tools here
   if (isMacOS() &&
       usingSystemMake() &&
       !hasMacOSCommandLineTools())
   {
      return false;
   }
#endif

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
   rCmd << cppPath.getFilename();

   core::system::ProcessOptions options;
   options.workingDir = cppPath.getParent();
   core::system::Options childEnv;
   core::system::environment(&childEnv);
   std::string warningMsg;
   module_context::addRtoolsToPathIfNecessary(&childEnv, &warningMsg);
   options.environment = childEnv;

   core::system::ProcessResult result;
   error = core::system::runCommand(rCmd.shellCommand(), options, &result);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   if (result.exitStatus != EXIT_SUCCESS)
   {
      checkXcodeLicense();
      return false;
   }
   
   s_canBuildCpp = true;
   return true;
}

bool installRBuildTools(const std::string& action)
{
#if defined(_WIN32) || defined(__APPLE__)
   r::exec::RFunction check(".rs.installBuildTools", action);
   bool userConfirmed = false;
   Error error = check.call(&userConfirmed);
   if (error)
      LOG_ERROR(error);
   return userConfirmed;
#else
   return false;
#endif
}

}

} // namespace session
} // namespace rstudio

