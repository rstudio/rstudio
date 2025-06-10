/*
 * REnvironmentPosix.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <core/r_util/REnvironment.hpp>

#include <iostream>
#include <algorithm>

#include <boost/regex.hpp>
#include <boost/tokenizer.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/join.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/ConfigUtils.hpp>
#include <core/RegexUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>
#include <core/system/ShellUtils.hpp>

namespace rstudio {
namespace core {
namespace r_util {

namespace {

FilePath scanForRScript(const std::vector<std::string>& rScriptPaths,
                        std::string* pErrMsg)
{
   // iterate over paths
   for (std::vector<std::string>::const_iterator it = rScriptPaths.begin();
        it != rScriptPaths.end();
        ++it)
   {
      FilePath rScriptPath(*it);
      if (rScriptPath.exists() && !rScriptPath.isDirectory())
      {
         // verify that the alias points to a real version of R
         Error error = core::system::realPath(*it, &rScriptPath);
         if (error)
         {
            error.addProperty("script-path", *it);
            LOG_ERROR(error);
            continue;
         }

         return rScriptPath;
      }
   }

   // didn't find it
   *pErrMsg = "Unable to locate R binary by scanning standard locations";
   return FilePath();
}

// extra paths from R (for rJava)
std::string extraLibraryPaths(const FilePath& ldPathsScript,
                              const std::string& rHome)
{
   // no-op if no script is passed
   if (ldPathsScript.isEmpty())
      return std::string();

   // verify that script exists
   if (!ldPathsScript.exists())
   {
      LOG_WARNING_MESSAGE("r-ldpath script not found at " +
                             ldPathsScript.getAbsolutePath());
      return std::string();
   }

   // run script to capture paths
   std::string command = ldPathsScript.getAbsolutePath() + " " + rHome;
   system::ProcessResult result;
   Error error = runCommand(command, core::system::ProcessOptions(), &result);
   if (error)
      LOG_ERROR(error);
   std::string libraryPaths = result.stdOut;
   boost::algorithm::trim(libraryPaths);
   
   return libraryPaths;
}

// MacOS X Specific
#ifdef __APPLE__

#define kLibRFileName            "libR.dylib"
#define kLibraryPathEnvVariable  "DYLD_FALLBACK_LIBRARY_PATH"

FilePath systemDefaultRScript(std::string* pErrMsg)
{
   // check fallback paths
   std::vector<std::string> rScriptPaths = {
      "/usr/bin/R",
      "/usr/local/bin/R",
      "/opt/local/bin/R",
   #ifdef __APPLE__
      "/opt/homebrew/bin/R",
      "/Library/Frameworks/R.framework/Resources/bin/R",
   #endif
   };

   return scanForRScript(rScriptPaths, pErrMsg);
}

bool getLibPathFromRHome(const FilePath& rHomePath,
                         std::string* pRLibPath,
                         std::string* pErrMsg)
{
   // get R lib path (probe subdiretories if necessary)
   FilePath libPath = rHomePath.completePath("lib");

   // check for dylib in lib and lib/x86_64
   if (libPath.completePath(kLibRFileName).exists())
   {
      *pRLibPath = libPath.getAbsolutePath();
      return true;
   }
   else if (libPath.completePath("x86_64/" kLibRFileName).exists())
   {
      *pRLibPath = libPath.completePath("x86_64").getAbsolutePath();
      return true;
   }
   else
   {
      *pErrMsg = "Unable to find " kLibRFileName " in expected locations"
                 "within R Home directory " + rHomePath.getAbsolutePath();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
}

bool getRHomeAndLibPath(const FilePath& rScriptPath,
                        const config_utils::Variables& scriptVars,
                        std::string* pRHome,
                        std::string* pRLibPath,
                        std::string* pErrMsg,
                        const std::string& prelaunchScript = "")
{
   config_utils::Variables::const_iterator it = scriptVars.find("R_HOME_DIR");
   if (it != scriptVars.end())
   {
      // get R home
      *pRHome = it->second;

      // get lib path
      return getLibPathFromRHome(FilePath(*pRHome), pRLibPath, pErrMsg);
   }
   else
   {
      *pErrMsg = "Unable to find R_HOME_DIR in " + rScriptPath.getAbsolutePath();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
}

bool detectRLocationsUsingFramework(FilePath* pHomePath,
                                    FilePath* pLibPath,
                                    config_utils::Variables* pScriptVars,
                                    std::string* pErrMsg)
{
   // home path
   *pHomePath = FilePath("/Library/Frameworks/R.framework/Resources");

   // lib path
   std::string rLibPath;
   if (!getLibPathFromRHome(*pHomePath, &rLibPath, pErrMsg))
      return false;
   *pLibPath = FilePath(rLibPath);

   // other paths
   config_utils::Variables& scriptVars = *pScriptVars;
   scriptVars["R_HOME"] = pHomePath->getAbsolutePath();
   scriptVars["R_SHARE_DIR"] = pHomePath->completePath("share").getAbsolutePath();
   scriptVars["R_INCLUDE_DIR"] = pHomePath->completePath("include").getAbsolutePath();
   scriptVars["R_DOC_DIR"] = pHomePath->completePath("doc").getAbsolutePath();

   return true;
}

// Linux specific
#else

#define kLibRFileName            "libR.so"
#define kLibraryPathEnvVariable  "LD_LIBRARY_PATH"

FilePath systemDefaultRScript(std::string* pErrMsg)
{
   // ask system which R to use
   system::ProcessResult result;
   Error error = core::system::runCommand("which R",
                                          core::system::ProcessOptions(),
                                          &result);
   std::string whichR = result.stdOut;
   boost::algorithm::trim(whichR);
   if (error || whichR.empty())
   {
      // log error or failure to return output
      if (error)
      {
         *pErrMsg = "Error calling which R: " + error.getSummary();
      }
      else
      {
         *pErrMsg = "Unable to find an installation of R on the system "
                    "(which R didn't return valid output)";
      }

      // scan in standard locations as a fallback
      std::string scanErrMsg;
      std::vector<std::string> rScriptPaths;
      rScriptPaths.push_back("/usr/local/bin/R");
      rScriptPaths.push_back("/usr/bin/R");
      FilePath scriptPath = scanForRScript(rScriptPaths, &scanErrMsg);
      if (scriptPath.isEmpty())
      {
        pErrMsg->append("; " + scanErrMsg);
        return FilePath();
      }

      // set whichR
      whichR = scriptPath.getAbsolutePath();
   }


   // return path to R script
   return FilePath(whichR);
}

bool getRHomeAndLibPath(const FilePath& rScriptPath,
                        const config_utils::Variables& scriptVars,
                        std::string* pRHome,
                        std::string* pRLibPath,
                        std::string* pErrMsg,
                        const std::string& prelaunchScript = "")
{
   // eliminate a potentially conflicting R_HOME before calling R RHOME"
   // (the normal semantics of invoking the R script are that it overwrites
   // R_HOME and prints a warning -- this warning is co-mingled with the
   // output of "R RHOME" and messes up our parsing)
   core::system::setenv("R_HOME", "");

   // run R script to detect R home
   std::string command = rScriptPath.getAbsolutePath() + " RHOME";
   if (!prelaunchScript.empty())
   {
      command = prelaunchScript + " &> /dev/null && " + command;
   }

   system::ProcessResult result;
   Error error = runCommand(command, core::system::ProcessOptions(), &result);
   if (error)
   {
      *pErrMsg = "Error running R (" + rScriptPath.getAbsolutePath() + "): " +
                 error.getSummary();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   else
   {
      std::string rHomeOutput = result.stdOut;
      boost::algorithm::trim(rHomeOutput);
      *pRHome = rHomeOutput;
      *pRLibPath = FilePath(*pRHome).completePath("lib").getAbsolutePath();
      return true;
   }
}


#endif



bool validateRScriptPath(const std::string& rScriptPath,
                         std::string* pErrMsg)
{
   // get realpath
   FilePath rBinaryPath;
   Error error = core::system::realPath(rScriptPath, &rBinaryPath);
   if (error)
   {
      *pErrMsg = "Unable to determine real path of R script " +
                 rScriptPath + " (" + error.getSummary() + ")";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // check for real path doesn't exist
   else if (!rBinaryPath.exists())
   {
      *pErrMsg = "Real path of R script does not exist (" +
         rBinaryPath.getAbsolutePath() + ")";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // error if it is a directory
   else if (rBinaryPath.isDirectory())
   {
      *pErrMsg = "R script path (" + rBinaryPath.getAbsolutePath() +
                 ") is a directory rather than a file";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // looks good!
   else
   {
      return true;
   }
}


bool validateREnvironment(const EnvironmentVars& vars,
                          const FilePath& rLibPath,
                          std::string* pErrMsg)
{
   // first extract paths
   FilePath rHomePath, rSharePath, rIncludePath, rDocPath, rLibRPath;
   for (EnvironmentVars::const_iterator it = vars.begin();
        it != vars.end();
        ++it)
   {
      if (it->first == "R_HOME")
         rHomePath = FilePath(it->second);
      else if (it->first == "R_SHARE_DIR")
         rSharePath = FilePath(it->second);
      else if (it->first == "R_INCLUDE_DIR")
         rIncludePath = FilePath(it->second);
      else if (it->first == "R_DOC_DIR")
         rDocPath = FilePath(it->second);
   }

   // resolve libR path
   rLibRPath = rLibPath.completePath(kLibRFileName);

   // validate required paths (if these don't exist then rsession won't
   // be able start up)
   if (!rHomePath.exists())
   {
      *pErrMsg = "R Home path (" + rHomePath.getAbsolutePath() + ") not found";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   else if (!rLibPath.exists())
   {
      *pErrMsg = "R lib path (" + rLibPath.getAbsolutePath() + ") not found";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   else if (!rLibRPath.exists())
   {
      *pErrMsg = "R shared library (" + rLibRPath.getAbsolutePath() + ") "
                 "not found. If this is a custom build of R, was it "
                 "built with the --enable-R-shlib option?";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   else if (!rDocPath.exists())
   {
      *pErrMsg = "R doc dir (" + rDocPath.getAbsolutePath() + ") not found.";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // log warnings for other missing paths (rsession can still start but
   // won't be able to find these env variables)

   if (!rSharePath.exists())
   {
      LOG_WARNING_MESSAGE("R share path (" + rSharePath.getAbsolutePath() +
                          ") not found");
   }

   if (!rIncludePath.exists())
   {
      LOG_WARNING_MESSAGE("R include path (" + rIncludePath.getAbsolutePath() +
                          ") not found");
   }

   return true;
}

// resolve an R path which has been parsed from the R bash script. If
// R is running out of the source directory (and was thus never installed)
// then the values for R_DOC_DIR, etc. will contain unexpanded references
// to the R_HOME_DIR, so we expand these if they are present.
std::string resolveRPath(const FilePath& rHomePath, const std::string& path)
{
   std::string resolvedPath = path;
   boost::algorithm::replace_all(resolvedPath,
                                 "${R_HOME_DIR}",
                                 rHomePath.getAbsolutePath());
   return resolvedPath;
}

bool detectRLocationsUsingScript(const FilePath& rScriptPath,
                                 FilePath* pHomePath,
                                 FilePath* pLibPath,
                                 config_utils::Variables* pScriptVars,
                                 std::string* pErrMsg,
                                 const std::string& prelaunchScript = "")
{
   // scan R script for other locations and append them to our vars
   Error error = config_utils::extractVariables(rScriptPath, pScriptVars);
   if (error)
   {
      *pErrMsg = "Error reading R script (" + rScriptPath.getAbsolutePath() +
                 "), " + error.getSummary();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // get r home path
   std::string rHome, rLib;
   if (!getRHomeAndLibPath(rScriptPath, *pScriptVars, &rHome, &rLib, pErrMsg, prelaunchScript))
      return false;

   // validate: error if we got no output
   if (rHome.empty())
   {
      *pErrMsg = "Unable to determine R home directory";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // validate: error if `R RHOME` yields file that doesn't exist
   *pHomePath = FilePath(rHome);
   if (!pHomePath->exists())
   {
      *pErrMsg = "R home path (" + rHome + ") not found";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // get lib path
   *pLibPath = FilePath(rLib);

   return true;
}

std::string createSourcedCommand(const std::string& prelaunchScript,
                                 const std::string& module,
                                 const FilePath& moduleBinaryPath,
                                 const std::string& command)
{
   std::string fullCommand;

   if (!module.empty() && !moduleBinaryPath.isEmpty())
   {
      fullCommand += ". " + shell_utils::escape(moduleBinaryPath.getAbsolutePath()) +
                " > /dev/null 2>&1; module load " + shell_utils::escape(module) +
                " > /dev/null 2>&1; ";
   }

   // don't attempt to load the prelaunch script if it is a user script
   if (!prelaunchScript.empty())
   {
      fullCommand += ". " + shell_utils::escape(prelaunchScript) + " > /dev/null 2>&1; ";
   }

   return fullCommand + command;
}

#ifndef __APPLE__
bool detectRLocationsUsingR(const std::string& rScriptPath,
                            FilePath* pHomePath,
                            FilePath* pLibPath,
                            config_utils::Variables* pScriptVars,
                            std::string* pErrMsg,
                            const std::string& prelaunchScript = "",
                            const std::string& module = "",
                            const FilePath& moduleBinaryPath = FilePath())
{
   // eliminate a potentially conflicting R_HOME before calling R
   // (the normal semantics of invoking the R script are that it overwrites
   // R_HOME and prints a warning -- this warning is co-mingled with the
   // output of R and messes up our parsing)
   core::system::setenv("R_HOME", "");

   // if no R path was specified for a module, the module binary path MUST be specified
   // otherwise, we would have no way to load the module
   if (rScriptPath.empty() && moduleBinaryPath.isEmpty())
   {
      *pErrMsg = "Path to R not specified, and no module binary specified";
      return false;
   }

   // call R to determine the locations - if a path to R is not given
   // then just use the default on the command line - this should
   // only be the case when a module is specified
   std::string rCommand = !rScriptPath.empty() ? rScriptPath : "R";
   std::string command =  rCommand +
     " --vanilla -s -e \"cat(paste("
            "R.home('home'),"
            "R.home('share'),"
            "R.home('include'),"
            "R.home('doc'),sep=':'))\"";

   std::string fullCommand = createSourcedCommand(prelaunchScript,
                                                  module,
                                                  moduleBinaryPath,
                                                  command);

   system::ProcessResult result;
   Error error = runCommand(fullCommand, system::ProcessOptions(), &result);
   if (error)
   {
      LOG_ERROR(error);
      *pErrMsg = "Error calling R script (" + rScriptPath +
                 "), " + error.getSummary();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   std::string output = result.stdOut;
   boost::algorithm::trim(output);

   if (output.empty())
   {
      *pErrMsg = "R did not return any output when queried for "
                 "directory location information";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // extract the locations
   config_utils::Variables& scriptVars = *pScriptVars;
   using namespace boost;
   char_separator<char> sep(":");
   tokenizer<char_separator<char> > tokens(output, sep);
   tokenizer<char_separator<char> >::iterator tokenIter = tokens.begin();
   if (tokenIter != tokens.end())
      scriptVars["R_HOME"] = *tokenIter++;
   if (tokenIter != tokens.end())
      scriptVars["R_SHARE_DIR"] = *tokenIter++;
   if (tokenIter != tokens.end())
      scriptVars["R_INCLUDE_DIR"] = *tokenIter++;
   if (tokenIter != tokens.end())
      scriptVars["R_DOC_DIR"] = *tokenIter++;

   if (scriptVars.size() < 4)
   {
      *pErrMsg = "R did not return valid directory location information; "
                 "R output was: " + output;
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // get home and lib path
   config_utils::Variables::const_iterator it = pScriptVars->find("R_HOME");
   if (it != pScriptVars->end())
   {
      // get R home
      *pHomePath = FilePath(it->second);

      // get R lib path
      FilePath libPath = FilePath(*pHomePath).completePath("lib");

      // verify we can find libR
      if (libPath.completePath(kLibRFileName).exists())
      {
         *pLibPath = libPath;
      }

      // sometimes on the mac an architecture specific subdirectory is used
      else if (libPath.completePath("x86_64/" kLibRFileName).exists())
      {
         *pLibPath = libPath.completePath("x86_64");
      }

      // couldn't find libR
      else
      {
         *pErrMsg = "Unable to find " kLibRFileName " in expected locations "
                    "within R Home directory " + pHomePath->getAbsolutePath();
         LOG_ERROR_MESSAGE(*pErrMsg);
         return false;
      }
   }
   else
   {
      *pErrMsg = "Unable to find R_HOME via " + rScriptPath +
                 "; R output was: " + output;
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   return true;
}
#endif

} // anonymous namespace


bool detectREnvironment(const FilePath& whichRScript,
                        const FilePath& ldPathsScript,
                        const std::string& ldLibraryPath,
                        std::string* pRScriptPath,
                        std::string* pVersion,
                        EnvironmentVars* pVars,
                        std::string* pErrMsg,
                        const std::string& prelaunchScript,
                        const std::string& module,
                        const FilePath& modulesBinaryPath)
{
   // if there is a which R script override then validate it
   if (module.empty())
   {
      // get system default R path, but only if a module is not specified
      // if a module is specified, we will just use whatever R shows up on
      // the path when the module is loaded
      FilePath sysRScript = systemDefaultRScript(pErrMsg);
      *pRScriptPath = sysRScript.getAbsolutePath();
   }
   else
   {
      // using module - no R script path
      *pRScriptPath = std::string();
   }

   // detect R locations
   FilePath rHomePath, rLibPath;
   std::string scriptErrMsg;
   config_utils::Variables scriptVars;
   bool detected = false;

   // check RSTUDIO_WHICH_R override
   if (!detected)
   {
      std::string rstudioWhichR = core::system::getenv("RSTUDIO_WHICH_R");
      if (!rstudioWhichR.empty())
      {
         if (!validateRScriptPath(rstudioWhichR, pErrMsg))
            return false;

         detected = detectRLocationsUsingScript(
                  FilePath(rstudioWhichR),
                  &rHomePath,
                  &rLibPath,
                  &scriptVars,
                  pErrMsg);

         if (!detected)
            return false;

         *pRScriptPath = rstudioWhichR;
      }
   }

   // check whichRScript override
   if (!detected)
   {
      if (!whichRScript.isEmpty())
      {
         if (!validateRScriptPath(whichRScript.getAbsolutePath(), pErrMsg))
            return false;

         detected = detectRLocationsUsingScript(
                  whichRScript,
                  &rHomePath,
                  &rLibPath,
                  &scriptVars,
                  pErrMsg);

         if (!detected)
            return false;

         *pRScriptPath = whichRScript.getAbsolutePath();
      }
   }

#ifdef __APPLE__

   if (!detected)
   {
      // check with default R script path
      detected = detectRLocationsUsingScript(
               FilePath(*pRScriptPath),
               &rHomePath,
               &rLibPath,
               &scriptVars,
               pErrMsg);
   }

   if (!detected)
   {
      // fallback to detecting using Framework directory
      rHomePath = FilePath();
      rLibPath = FilePath();
      scriptVars.clear();
      *pRScriptPath = "/Library/Frameworks/R.framework/Resources/bin/R";

      detected = detectRLocationsUsingFramework(
               &rHomePath,
               &rLibPath,
               &scriptVars,
               &scriptErrMsg);
   }

#else

   if (!detected)
   {
      detected = detectRLocationsUsingR(
               *pRScriptPath,
               &rHomePath,
               &rLibPath,
               &scriptVars,
               pErrMsg,
               prelaunchScript,
               module,
               modulesBinaryPath);
   }

   if (!detected)
   {
      // fallback to detecting using script (sometimes we are unable to
      // call R successfully immediately after a system reboot)
      //
      // we do not attempt to do this if we were loading R via module
      // because we do not actually have a path to R to attempt these
      // workarounds
      if (pRScriptPath->empty())
      {
         pErrMsg->append("; Invalid R module (" + module + ")");
         return false;
      }

      rHomePath = FilePath();
      rLibPath = FilePath();
      scriptVars.clear();

      detected = detectRLocationsUsingScript(
               FilePath(*pRScriptPath),
               &rHomePath,
               &rLibPath,
               &scriptVars,
               &scriptErrMsg,
               prelaunchScript);
   }

#endif

   if (!detected)
   {
      pErrMsg->append("; " + scriptErrMsg);
      return false;
   }

   // set R home path
   pVars->push_back({"R_HOME", rHomePath.getAbsolutePath()});

   // set other environment values
   for (auto&& var : {"R_SHARE_DIR", "R_INCLUDE_DIR", "R_DOC_DIR"})
      pVars->push_back({var, resolveRPath(rHomePath, scriptVars[var])});

   // determine library path (existing + r lib dir + r extra lib dirs)
   std::string libraryPath = rLibraryPath(rHomePath,
                                          rLibPath,
                                          ldPathsScript,
                                          ldLibraryPath);
   
   pVars->push_back({kLibraryPathEnvVariable, libraryPath});

   Error error = rVersion(rHomePath,
                          FilePath(*pRScriptPath),
                          ldLibraryPath,
                          prelaunchScript,
                          module,
                          modulesBinaryPath,
                          pVersion);
   if (error)
      LOG_ERROR(error);

   return validateREnvironment(*pVars, rLibPath, pErrMsg);
}


void setREnvironmentVars(const EnvironmentVars& vars)
{
   for (EnvironmentVars::const_iterator it = vars.begin();
        it != vars.end();
        ++it)
   {
      core::system::setenv(it->first, it->second);
   }
}

void setREnvironmentVars(const EnvironmentVars& vars,
                         core::system::Options* pEnv)
{
   for (EnvironmentVars::const_iterator it = vars.begin();
        it != vars.end();
        ++it)
   {
      core::system::setenv(pEnv, it->first, it->second);
   }
}

std::string rLibraryPath(const FilePath& rHomePath,
                         const FilePath& rLibPath,
                         const FilePath& ldPathsScript,
                         const std::string& ldLibraryPath)
{
   std::vector<std::string> libraryPaths;
   
   // place R library path at front
   libraryPaths.push_back(rLibPath.getAbsolutePath());
   
   // pass along default (inherited) library paths
   std::string defaultLibraryPaths = core::system::getenv(kLibraryPathEnvVariable);
 
   // on macOS, we need to initialize with a default set of library paths
   // if the path was already empty
#ifdef __APPLE__
   if (defaultLibraryPaths.empty())
   {
      // if this isn't set explicitly then initialize it with the default
      // of $HOME/lib:/usr/local/lib:/usr/lib. See documentation here:
      // http://developer.apple.com/library/ios/#documentation/system/conceptual/manpages_iphoneos/man3/dlopen.3.html
      //
      // NOTE (kevin): the above documentation now appears to be old; 'man dyld' has:
      // 
      // DYLD_FALLBACK_LIBRARY_PATH This is a colon separated list of
      // directories that contain libraries. If a dylib is not found at its
      // install path, dyld uses this as a list of directories to search for
      // the dylib. By default, it is set to /usr/local/lib:/usr/lib.
      //
      // we keep $HOME/lib on the path for backwards compatibility, just in case
      boost::format fmt("%1%/lib:/usr/local/lib:/usr/lib");
      defaultLibraryPaths = boost::str(fmt % core::system::getenv("HOME"));
   }
#endif
   
   // now add our default library paths
   if (!defaultLibraryPaths.empty())
      libraryPaths.push_back(defaultLibraryPaths);
   
   // add LD_LIBRARY_PATH
   if (!ldLibraryPath.empty())
      libraryPaths.push_back(ldLibraryPath);
   
   // compute and add extra library paths (if any)
   std::string extraPaths = extraLibraryPaths(ldPathsScript, rHomePath.getAbsolutePath());
   if (!extraPaths.empty())
      libraryPaths.push_back(extraPaths);
   
   // join into path
   std::string fullPath = boost::algorithm::join(libraryPaths, ":");
   
   // remove duplicated colons
   boost::regex reColons("[:]+");
   boost::replace_all(fullPath, reColons, ":");
   
   return fullPath;
}

Error rVersion(const FilePath& rHomePath,
               const FilePath& rScriptPath,
               const std::string& ldLibraryPath,
               std::string* pVersion)
{
   return rVersion(rHomePath,
                   rScriptPath,
                   ldLibraryPath,
                   std::string(),
                   std::string(),
                   FilePath(),
                   pVersion);
}

Error rVersion(const FilePath& rHomePath,
               const FilePath& rScriptPath,
               const std::string& ldLibraryPath,
               const std::string& prelaunchScript,
               const std::string& module,
               const FilePath& moduleBinaryPath,
               std::string* pVersion)
{
   // set R_HOME as provided
   core::system::ProcessOptions options;
   core::system::Options env;
   core::system::environment(&env);
   core::system::setenv(&env, "R_HOME", rHomePath.getAbsolutePath());

   // if additional LD_LIBRARY_PATH paths were supplied, provide those as well
   if (!ldLibraryPath.empty())
   {
      std::string libPath = core::system::getenv(env, kLibraryPathEnvVariable);
      if (!libPath.empty())
         libPath += ":";
      libPath += ldLibraryPath;
      core::system::setenv(&env, kLibraryPathEnvVariable, libPath);
   }

   // determine the R version
   options.environment = env;
   core::system::ProcessResult result;

   std::string rCommand = !rScriptPath.isEmpty() ? rScriptPath.getAbsolutePath() : "R";
   std::string command = rCommand + " --vanilla -s -e 'cat(R.Version()$major,R.Version()$minor, sep=\".\")'";
   std::string fullCommand = createSourcedCommand(prelaunchScript,
                                                  module,
                                                  moduleBinaryPath,
                                                  command);

   Error error = core::system::runCommand(
      fullCommand,
      options,
      &result);
   if (error)
   {
      error.addProperty("r-script", rScriptPath);
      error.addProperty("command", fullCommand);
      if (!module.empty())
         error.addProperty("module", module);
      error.addProperty("modules-bin-path", moduleBinaryPath.getAbsolutePath());
      return error;
   }
   else
   {
      std::string versionInfo = boost::algorithm::trim_copy(result.stdOut);
      boost::regex re("^([\\d\\.]+)$");
      boost::smatch match;
      if (regex_utils::search(versionInfo, match, re))
      {
         *pVersion = match[1];
         LOG_DEBUG_MESSAGE("Found R version: " + *pVersion + " with: " + fullCommand);
         return Success();
      }
      else
      {
         Error error = systemError(boost::system::errc::protocol_error,
                                   "Unable to parse version from R",
                                   ERROR_LOCATION);

         // log the actual output returned by R
         error.addProperty("version-info", versionInfo);

         // log any errors emitted by R
         error.addProperty("r-error", result.stdErr);
         error.addProperty("command", fullCommand);
         if (!module.empty())
            error.addProperty("module", module);
         error.addProperty("modules-bin-path", moduleBinaryPath.getAbsolutePath());
         return error;
      }
   }
}

/*
We've observed that Ubuntu 14.10 no longer passes the LANG environment
variable to daemon processes so we lose the automatic inheritance of
LANG from the system default. For this case we'll do automatic detection
and setting of LANG.
*/
#if !defined(__APPLE__)
void ensureLang()
{
   // if no LANG environment variable is already defined
   if (core::system::getenv("LANG").empty())
   {
      // try to read the LANG from the various places it might be defined
      std::vector<std::pair<std::string,std::string> > langDefs;
      langDefs.push_back(std::make_pair("LANG", "/etc/default/locale"));
      langDefs.push_back(std::make_pair("LANG", "/etc/sysconfig/i18n"));
      langDefs.push_back(std::make_pair("LANG", "/etc/locale.conf"));
      langDefs.push_back(std::make_pair("RC_LANG", "/etc/sysconfig/language"));
      for (size_t i = 0; i<langDefs.size(); i++)
      {
         std::string var = langDefs[i].first;
         std::string file = langDefs[i].second;
         std::map<std::string,std::string> vars;
         Error error = config_utils::extractVariables(FilePath(file), &vars);
         if (error)
         {
            if (!core::isPathNotFoundError(error))
               LOG_ERROR(error);
            continue;
         }
         std::string value = vars[var];
         if (!value.empty())
         {
            core::system::setenv("LANG", value);
            break;
         }
      }

      // log a warning if it's still empty
      if (core::system::getenv("LANG").empty())
      {
         LOG_WARNING_MESSAGE(
            "Unable to determine LANG (proceeding with no LANG set");
      }
   }   
}
#else
void ensureLang()
{
}
#endif

} // namespace r_util
} // namespace core 
} // namespace rstudio



