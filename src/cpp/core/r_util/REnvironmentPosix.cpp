/*
 * REnvironmentPosix.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/r_util/REnvironment.hpp>

#include <algorithm>

#include <boost/tokenizer.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/replace.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/ConfigUtils.hpp>
#include <core/system/System.hpp>

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
      if (rScriptPath.exists())
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
   LOG_ERROR_MESSAGE(*pErrMsg);
   return FilePath();
}

// MacOS X Specific
#ifdef __APPLE__

#define kLibRFileName            "libR.dylib"
#define kLibraryPathEnvVariable  "DYLD_LIBRARY_PATH"

// no extra paths on the mac
std::string extraLibraryPaths(const FilePath& ldPathsScript,
                              const std::string& rHome)
{
   return std::string();
}

FilePath systemDefaultRScript(std::string* pErrMsg)
{
   // define potential paths (use same order as in conventional osx PATH)
   std::vector<std::string> rScriptPaths;
   rScriptPaths.push_back("/opt/local/bin/R");
   rScriptPaths.push_back("/usr/bin/R");
   rScriptPaths.push_back("/usr/local/bin/R");
   return scanForRScript(rScriptPaths, pErrMsg);
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

      // get R lib path (probe subdiretories if necessary)
      FilePath libPath = FilePath(*pRHome).complete("lib");

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
                    "within R Home directory " + *pRHome;
         LOG_ERROR_MESSAGE(*pErrMsg);
         return false;
      }
   }
   else
   {
      *pErrMsg = "Unable to find R_HOME_DIR in " + rScriptPath.absolutePath();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
}

// Linux specific
#else

#define kLibRFileName            "libR.so"
#define kLibraryPathEnvVariable  "LD_LIBRARY_PATH"

// extra paths from R (for rjava) on linux
std::string extraLibraryPaths(const FilePath& ldPathsScript,
                              const std::string& rHome)
{
   // verify that script exists
   if (!ldPathsScript.exists())
   {
      LOG_WARNING_MESSAGE("r-ldpaths script not found at " +
                          ldPathsScript.absolutePath());
      return std::string();
   }

   // run script to capture paths
   std::string libraryPaths;
   std::string command = ldPathsScript.absolutePath() + " " + rHome;
   Error error = system::captureCommand(command, &libraryPaths);
   if (error)
      LOG_ERROR(error);
   boost::algorithm::trim(libraryPaths);
   return libraryPaths;
}

FilePath systemDefaultRScript(std::string* pErrMsg)
{
   // ask system which R to use
   std::string whichOutput;
   Error error = core::system::captureCommand("which R", &whichOutput);
   if (error)
   {
      *pErrMsg = "Error calling which R: " + error.summary();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return FilePath();
   }
   boost::algorithm::trim(whichOutput);

   // if we got no output then log and return false
   if (whichOutput.empty())
   {
      *pErrMsg = "Unable to find an installation of R on the system "
                 "(which R didn't return valid output)";
      LOG_ERROR_MESSAGE(*pErrMsg);
      return FilePath();
   }
   else
   {
      return FilePath(whichOutput);
   }
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
   std::string rHomeOutput;
   std::string command = rScriptPath.absolutePath() + " RHOME";
   Error error = core::system::captureCommand(command, &rHomeOutput);
   if (error)
   {
      *pErrMsg = "Error running R (" + rScriptPath.absolutePath() + "): " +
                 error.summary();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
   else
   {
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
   std::string output;
   std::string command = rScriptPath +
     " --slave --vanilla -e \"cat(paste("
            "R.home('home'),"
            "R.home('share'),"
            "R.home('include'),"
            "R.home('doc'),sep=':'))\"";
   Error error = core::system::captureCommand(command, &output);
   if (error)
   {
      LOG_ERROR(error);
      *pErrMsg = "Error calling R script (" + rScriptPath +
                 "), " + error.summary();
      LOG_ERROR_MESSAGE(*pErrMsg);
      return false;
   }
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

} // anonymous namespace


bool detectREnvironment(const FilePath& ldPathsScript,
                        EnvironmentVars* pVars,
                        std::string* pErrMsg)
{
   return detectREnvironment(FilePath(),
                             ldPathsScript,
                             std::string(),
                             pVars,
                             pErrMsg);
}

bool detectREnvironment(const FilePath& whichRScript,
                        const FilePath& ldPathsScript,
                        const std::string& ldLibraryPath,
                        EnvironmentVars* pVars,
                        std::string* pErrMsg)
{
   // if there is a which R script override then validate it
   std::string rScriptPath;
   if (!whichRScript.empty())
   {
      // validate
      if (!validateRScriptPath(whichRScript.absolutePath(), pErrMsg))
         return false;

      // set it
      rScriptPath = whichRScript.absolutePath();
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
      rScriptPath = sysRScript.absolutePath();
   }

   // detect R locations
   FilePath rHomePath, rLibPath;
   config_utils::Variables scriptVars;
#ifdef __APPLE__
   if (!detectRLocationsUsingScript(FilePath(rScriptPath),
                                    &rHomePath,
                                    &rLibPath,
                                    &scriptVars,
                                    pErrMsg))
   {
      return false;
   }
#else
   if (!detectRLocationsUsingR(rScriptPath,
                               &rHomePath,
                               &rLibPath,
                               &scriptVars,
                               pErrMsg))
   {
      return false;
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
   std::string libraryPath = core::system::getenv(kLibraryPathEnvVariable);
   if (!libraryPath.empty())
      libraryPath.append(":");
   libraryPath.append(ldLibraryPath);
   if (!libraryPath.empty())
      libraryPath.append(":");
   libraryPath.append(rLibPath.absolutePath());
   std::string extraPaths = extraLibraryPaths(ldPathsScript,
                                              rHomePath.absolutePath());
   if (!extraPaths.empty())
      libraryPath.append(":" + extraPaths);
   pVars->push_back(std::make_pair(kLibraryPathEnvVariable, libraryPath));

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

} // namespace r_util
} // namespace core 



