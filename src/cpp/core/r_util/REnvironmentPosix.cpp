/*
 * REnvironmentPosix.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <core/r_util/REnvironment.hpp>

#include <iostream>
#include <algorithm>

#include <boost/regex.hpp>
#include <boost/tokenizer.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/ConfigUtils.hpp>
#include <core/RegexUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

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
         if (!error)
         {
            return rScriptPath;
         }
         else
         {
            error.addProperty("script-path", *it);
            LOG_ERROR(error);
            continue;
         }
      }
   }

   // didn't find it
   *pErrMsg = "Unable to locate R binary by scanning standard locations";
   return FilePath();
}

// MacOS X Specific
#ifdef __APPLE__

#define kLibRFileName            "libR.dylib"
#define kLibraryPathEnvVariable  "DYLD_FALLBACK_LIBRARY_PATH"

// no extra paths on the mac
std::string extraLibraryPaths(const FilePath& ldPathsScript,
                              const std::string& rHome)
{
   return std::string();
}

FilePath systemDefaultRScript(std::string* pErrMsg)
{
   // define potential paths
   std::vector<std::string> rScriptPaths;
   rScriptPaths.push_back("/usr/bin/R");
   rScriptPaths.push_back("/usr/local/bin/R");
   rScriptPaths.push_back("/opt/local/bin/R");
   rScriptPaths.push_back("/Library/Frameworks/R.framework/Resources/bin/R");
   return scanForRScript(rScriptPaths, pErrMsg);
}

bool getLibPathFromRHome(const FilePath& rHomePath,
                         std::string* pRLibPath,
                         std::string* pErrMsg)
{
   // get R lib path (probe subdiretories if necessary)
   FilePath libPath = rHomePath.complete("lib");

   // check for dylib in lib and lib/x86_64
   if (libPath.complete(kLibRFileName).exists())
   {
      *pRLibPath = libPath.absolutePath();
      return true;
   }
   else if (libPath.complete("x86_64/" kLibRFileName).exists())
   {
      *pRLibPath = libPath.complete("x86_64").absolutePath();
      return true;
   }
   else
   {
      *pErrMsg = "Unable to find " kLibRFileName " in expected locations"
                 "within R Home directory " + rHomePath.absolutePath();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
}

bool getRHomeAndLibPath(const FilePath& rScriptPath,
                        const config_utils::Variables& scriptVars,
                        std::string* pRHome,
                        std::string* pRLibPath,
                        std::string* pErrMsg)
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
      *pErrMsg = "Unable to find R_HOME_DIR in " + rScriptPath.absolutePath();
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
   scriptVars["R_HOME"] = pHomePath->absolutePath();
   scriptVars["R_SHARE_DIR"] = pHomePath->complete("share").absolutePath();
   scriptVars["R_INCLUDE_DIR"] = pHomePath->complete("include").absolutePath();
   scriptVars["R_DOC_DIR"] = pHomePath->complete("doc").absolutePath();

   return true;
}

// Linux specific
#else

#define kLibRFileName            "libR.so"
#define kLibraryPathEnvVariable  "LD_LIBRARY_PATH"

// extra paths from R (for rjava) on linux
std::string extraLibraryPaths(const FilePath& ldPathsScript,
                              const std::string& rHome)
{
   // no-op if no script is passed
   if (ldPathsScript.empty())
      return std::string();

   // verify that script exists
   if (!ldPathsScript.exists())
   {
      LOG_WARNING_MESSAGE("r-ldpaths script not found at " +
                          ldPathsScript.absolutePath());
      return std::string();
   }

   // run script to capture paths
   std::string command = ldPathsScript.absolutePath() + " " + rHome;
   system::ProcessResult result;
   Error error = runCommand(command, core::system::ProcessOptions(), &result);
   if (error)
      LOG_ERROR(error);
   std::string libraryPaths = result.stdOut;
   boost::algorithm::trim(libraryPaths);
   return libraryPaths;
}

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
         *pErrMsg = "Error calling which R: " + error.summary();
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
      if (scriptPath.empty())
      {
        pErrMsg->append("; " + scanErrMsg);
        return FilePath();
      }

      // set whichR
      whichR = scriptPath.absolutePath();
   }


   // return path to R script
   return FilePath(whichR);
}

bool getRHomeAndLibPath(const FilePath& rScriptPath,
                        const config_utils::Variables& scriptVars,
                        std::string* pRHome,
                        std::string* pRLibPath,
                        std::string* pErrMsg)
{
   // eliminate a potentially conflicting R_HOME before calling R RHOME"
   // (the normal semantics of invoking the R script are that it overwrites
   // R_HOME and prints a warning -- this warning is co-mingled with the
   // output of "R RHOME" and messes up our parsing)
   core::system::setenv("R_HOME", "");

   // run R script to detect R home
   std::string command = rScriptPath.absolutePath() + " RHOME";
   system::ProcessResult result;
   Error error = runCommand(command, core::system::ProcessOptions(), &result);
   if (error)
   {
      *pErrMsg = "Error running R (" + rScriptPath.absolutePath() + "): " +
                 error.summary();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   else
   {
      std::string rHomeOutput = result.stdOut;
      boost::algorithm::trim(rHomeOutput);
      *pRHome = rHomeOutput;
      *pRLibPath = FilePath(*pRHome).complete("lib").absolutePath();
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
                 rScriptPath + " (" + error.summary() + ")";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // check for real path doesn't exist
   else if (!rBinaryPath.exists())
   {
      *pErrMsg = "Real path of R script does not exist (" +
                  rBinaryPath.absolutePath() + ")";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // error if it is a directry
   else if (rBinaryPath.isDirectory())
   {
      *pErrMsg = "R script path (" + rBinaryPath.absolutePath() +
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
   rLibRPath = rLibPath.complete(kLibRFileName);

   // validate required paths (if these don't exist then rsession won't
   // be able start up)
   if (!rHomePath.exists())
   {
      *pErrMsg = "R Home path (" + rHomePath.absolutePath() + ") not found";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   else if (!rLibPath.exists())
   {
      *pErrMsg = "R lib path (" + rLibPath.absolutePath() + ") not found";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   else if (!rLibRPath.exists())
   {
      *pErrMsg = "R shared library (" + rLibRPath.absolutePath() + ") "
                 "not found. If this is a custom build of R, was it "
                 "built with the --enable-R-shlib option?";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   else if (!rDocPath.exists())
   {
      *pErrMsg = "R doc dir (" + rDocPath.absolutePath() + ") not found.";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // log warnings for other missing paths (rsession can still start but
   // won't be able to find these env variables)

   if (!rSharePath.exists())
   {
      LOG_WARNING_MESSAGE("R share path (" + rSharePath.absolutePath() +
                          ") not found");
   }

   if (!rIncludePath.exists())
   {
      LOG_WARNING_MESSAGE("R include path (" + rIncludePath.absolutePath() +
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
                                 rHomePath.absolutePath());
   return resolvedPath;
}

bool detectRLocationsUsingScript(const FilePath& rScriptPath,
                                 FilePath* pHomePath,
                                 FilePath* pLibPath,
                                 config_utils::Variables* pScriptVars,
                                 std::string* pErrMsg)
{
   // scan R script for other locations and append them to our vars
   Error error = config_utils::extractVariables(rScriptPath, pScriptVars);
   if (error)
   {
      *pErrMsg = "Error reading R script (" + rScriptPath.absolutePath() +
                 "), " + error.summary();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }

   // get r home path
   std::string rHome, rLib;
   if (!getRHomeAndLibPath(rScriptPath, *pScriptVars, &rHome, &rLib, pErrMsg))
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

#ifndef __APPLE__
bool detectRLocationsUsingR(const std::string& rScriptPath,
                            FilePath* pHomePath,
                            FilePath* pLibPath,
                            config_utils::Variables* pScriptVars,
                            std::string* pErrMsg)
{
   // eliminate a potentially conflicting R_HOME before calling R
   // (the normal semantics of invoking the R script are that it overwrites
   // R_HOME and prints a warning -- this warning is co-mingled with the
   // output of R and messes up our parsing)
   core::system::setenv("R_HOME", "");

   // call R to determine the locations
   std::string command = rScriptPath +
     " --slave --vanilla -e \"cat(paste("
            "R.home('home'),"
            "R.home('share'),"
            "R.home('include'),"
            "R.home('doc'),sep=':'))\"";
   system::ProcessResult result;
   Error error = runCommand(command, system::ProcessOptions(), &result);
   if (error)
   {
      LOG_ERROR(error);
      *pErrMsg = "Error calling R script (" + rScriptPath +
                 "), " + error.summary();
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
      FilePath libPath = FilePath(*pHomePath).complete("lib");

      // verify we can find libR
      if (libPath.complete(kLibRFileName).exists())
      {
         *pLibPath = libPath;
      }

      // sometimes on the mac an architecture specific subdirectory is used
      else if (libPath.complete("x86_64/" kLibRFileName).exists())
      {
         *pLibPath = libPath.complete("x86_64");
      }

      // couldn't find libR
      else
      {
         *pErrMsg = "Unable to find " kLibRFileName " in expected locations "
                    "within R Home directory " + pHomePath->absolutePath();
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
                        std::string* pErrMsg)
{
   // if there is a which R script override then validate it
   if (!whichRScript.empty())
   {
      // validate
      if (!validateRScriptPath(whichRScript.absolutePath(), pErrMsg))
         return false;

      // set it
      *pRScriptPath = whichRScript.absolutePath();
   }
   // otherwise use the system default (after validating it as well)
   else
   {
      // get system default
      FilePath sysRScript = systemDefaultRScript(pErrMsg);
      if (sysRScript.empty())
         return false;

      if (!validateRScriptPath(sysRScript.absolutePath(), pErrMsg))
         return false;

      // set it
      *pRScriptPath = sysRScript.absolutePath();
   }

   // detect R locations
   FilePath rHomePath, rLibPath;
   config_utils::Variables scriptVars;
#ifdef __APPLE__
   if (!detectRLocationsUsingScript(FilePath(*pRScriptPath),
                                    &rHomePath,
                                    &rLibPath,
                                    &scriptVars,
                                    pErrMsg))
   {
      // fallback to detecting using Framework directory
      rHomePath = FilePath();
      rLibPath = FilePath();
      scriptVars.clear();
      std::string scriptErrMsg;
      *pRScriptPath = "/Library/Frameworks/R.framework/Resources/bin/R";
      if (!detectRLocationsUsingFramework(&rHomePath,
                                          &rLibPath,
                                          &scriptVars,
                                          &scriptErrMsg))
      {
         pErrMsg->append("; " + scriptErrMsg);
         return false;
      }
   }
#else
   if (!detectRLocationsUsingR(*pRScriptPath,
                               &rHomePath,
                               &rLibPath,
                               &scriptVars,
                               pErrMsg))
   {
      // fallback to detecting using script (sometimes we are unable to
      // call R successfully immediately after a system reboot)
      rHomePath = FilePath();
      rLibPath = FilePath();
      scriptVars.clear();
      std::string scriptErrMsg;
      if (!detectRLocationsUsingScript(FilePath(*pRScriptPath),
                                       &rHomePath,
                                       &rLibPath,
                                       &scriptVars,
                                       &scriptErrMsg))
      {
         pErrMsg->append("; " + scriptErrMsg);
         return false;
      }
   }
#endif


   // set R home path
   pVars->push_back(std::make_pair("R_HOME", rHomePath.absolutePath()));

   // set other environment values
   pVars->push_back(std::make_pair("R_SHARE_DIR",
                                   resolveRPath(rHomePath,
                                                scriptVars["R_SHARE_DIR"])));
   pVars->push_back(std::make_pair("R_INCLUDE_DIR",
                                   resolveRPath(rHomePath,
                                                scriptVars["R_INCLUDE_DIR"])));
   pVars->push_back(std::make_pair("R_DOC_DIR",
                                   resolveRPath(rHomePath,
                                                scriptVars["R_DOC_DIR"])));

   // determine library path (existing + r lib dir + r extra lib dirs)
   std::string libraryPath = rLibraryPath(rHomePath,
                                          rLibPath,
                                          ldPathsScript,
                                          ldLibraryPath);
   pVars->push_back(std::make_pair(kLibraryPathEnvVariable, libraryPath));

   // set R_ARCH on the mac if we are running against CRAN R
#ifdef __APPLE__
   // if it starts with the standard prefix and an etc/x86_64 directory
   // exists then we set the R_ARCH
   if (boost::algorithm::starts_with(rHomePath.absolutePath(),
                                     "/Library/Frameworks/R.framework/") &&
       FilePath("/Library/Frameworks/R.framework/Resources/etc/x86_64")
                                                                   .exists())
   {
      pVars->push_back(std::make_pair("R_ARCH","/x86_64"));
   }
#endif

   Error error = rVersion(rHomePath, FilePath(*pRScriptPath), ldLibraryPath, 
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
   // determine library path (existing + r lib dir + r extra lib dirs)
   std::string libraryPath = core::system::getenv(kLibraryPathEnvVariable);
#ifdef __APPLE__
   // if this isn't set explicitly then initalize it with the default
   // of $HOME/lib:/usr/local/lib:/usr/lib. See documentation here:
   // http://developer.apple.com/library/ios/#documentation/system/conceptual/manpages_iphoneos/man3/dlopen.3.html
   if (libraryPath.empty())
   {
      boost::format fmt("%1%/lib:/usr/local/lib:/usr/lib");
      libraryPath = boost::str(fmt % core::system::getenv("HOME"));
   }
#endif
   if (!libraryPath.empty())
      libraryPath.append(":");
   libraryPath.append(ldLibraryPath);
   if (!libraryPath.empty())
      libraryPath.append(":");
   libraryPath = rLibPath.absolutePath() + ":" + libraryPath;
   std::string extraPaths = extraLibraryPaths(ldPathsScript,
                                              rHomePath.absolutePath());
   if (!extraPaths.empty())
      libraryPath.append(":" + extraPaths);
   return libraryPath;
}

Error rVersion(const FilePath& rHomePath,
               const FilePath& rScriptPath,
               const std::string& ldLibraryPath,
               std::string* pVersion)
{
   // set R_HOME as provided
   core::system::ProcessOptions options;
   core::system::Options env;
   core::system::environment(&env);
   core::system::setenv(&env, "R_HOME", rHomePath.absolutePath());

   // if additional LD_LIBRARY_PATH paths were supplied, provide those as well
   if (!ldLibraryPath.empty())
   {
      std::string libPath = core::system::getenv(env, "LD_LIBRARY_PATH");
      if (!libPath.empty())
         libPath += ":";
      libPath += ldLibraryPath;
      core::system::setenv(&env, "LD_LIBRARY_PATH", libPath);
   }

   // determine the R version
   options.environment = env;
   core::system::ProcessResult result;
   Error error = core::system::runCommand(
      rScriptPath.absolutePath() +
      " --slave --vanilla -e 'cat(R.Version()$major,R.Version()$minor, sep=\".\")'",
      options,
      &result);
   if (error)
   {
      error.addProperty("r-script", rScriptPath);
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
         return error;
      }
   }
}

/*
We've observed that Ubuntu 14.10 no longer passes the LANG environment
variable to daemon processes so we lose the automatic inheritence of
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



