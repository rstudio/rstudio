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

#include <boost/algorithm/string/trim.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/ConfigUtils.hpp>
#include <core/system/System.hpp>

namespace core {
namespace r_util {

namespace {

// MacOS X Specific
#ifdef __APPLE__

#define kLibRFileName            "libR.dylib"
#define kLibraryPathEnvVariable  "DYLD_LIBRARY_PATH"

// no extra paths on the mac
std::string extraLibraryPaths(const std::string& rHome)
{
 return std::string();
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

#endif

FilePath systemDefaultRScript()
{
   // ask system which R to use
   std::string whichOutput;
   Error error = core::system::captureCommand("which R", &whichOutput);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }
   boost::algorithm::trim(whichOutput);

   // check for nothing returned
   if (whichOutput.empty())
      return FilePath();

   // verify that the alias points to a real version of R
   FilePath rBinaryPath;
   error = core::system::realPath(whichOutput, &rBinaryPath);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   // check for real path doesn't exist
   if (!rBinaryPath.exists())
      return FilePath();

   // got a valid R binary
   return rBinaryPath;
}

bool validateREnvironment(const EnvironmentVars& vars, std::string* pErrMsg)
{
   // first extract paths
   FilePath rHomePath, rSharePath, rIncludePath, rDocPath, rLibPath, rLibRPath;
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

   // resolve derivitive paths
   rLibPath = rHomePath.complete("lib");
   rLibRPath = rLibPath.complete(kLibRFileName);

   // validate required paths (if these don't exist then rsession won't
   // be able start up)
   if (!rHomePath.exists())
   {
      *pErrMsg = "R Home path (" + rHomePath.absolutePath() + ") not found";
      return false;
   }
   else if (!rLibPath.exists())
   {
      *pErrMsg = "R lib path (" + rLibPath.absolutePath() + ") not found";
      return false;
   }
   else if (!rLibRPath.exists())
   {
      *pErrMsg = "R shared library (" + rLibRPath.absolutePath() + ") "
                 "not found. If this is a custom build of R, was it "
                 "built with the --enable-R-shlib option?";
      return false;
   }

   // log warnings for other missing paths (rsession can still start but
   // won't be able to find these paths)

   if (!rSharePath.exists())
   {
      LOG_WARNING_MESSAGE("R share path (" + rSharePath.absolutePath() +
                          ") not found");
   }

   // merely log warnings for other missing paths
   if (!rIncludePath.exists())
   {
      LOG_WARNING_MESSAGE("R include path (" + rIncludePath.absolutePath() +
                          ") not found");
   }

   // merely log warnings for other missing paths
   if (!rDocPath.exists())
   {
      LOG_WARNING_MESSAGE("R doc path (" + rDocPath.absolutePath() +
                          ") not found");
   }

   return true;
}

} // anonymous namespace


bool detectREnvironment(const FilePath& ldPathsScript,
                        EnvironmentVars* pVars,
                        std::string* pErrMsg)
{
   return detectREnvironment(FilePath(), ldPathsScript, pVars, pErrMsg);
}

bool detectREnvironment(const FilePath& whichRScript,
                        const FilePath& ldPathsScript,
                        EnvironmentVars* pVars,
                        std::string* pErrMsg)
{
   // determine R script path. use either system default or user override
   FilePath rScriptPath = whichRScript;
   if (rScriptPath.empty())
      rScriptPath = systemDefaultRScript();

   // bail if not found
   if (!rScriptPath.exists())
   {
      LOG_ERROR(pathNotFoundError(rScriptPath.absolutePath(), ERROR_LOCATION));
      *pErrMsg = "R binary (" + rScriptPath.absolutePath() + ") not found";
      return false;
   }

   // run R script to detect R home
   std::string rHomeOutput;
   std::string command = rScriptPath.absolutePath() + " RHOME";
   Error error = core::system::captureCommand(command, &rHomeOutput);
   if (error)
   {
      LOG_ERROR(error);
      *pErrMsg = "Error running R (" + rScriptPath.absolutePath() + "): " +
                 error.summary();
      return false;
   }
   boost::algorithm::trim(rHomeOutput);

   // error if we got no output
   if (rHomeOutput.empty())
   {
      *pErrMsg = "Unable to determine R home directory";
      LOG_ERROR(systemError(boost::system::errc::not_supported,
                            *pErrMsg,
                            ERROR_LOCATION));
      return false;
   }

   // set r home path
   FilePath rHomePath(rHomeOutput);
   pVars->push_back(std::make_pair("R_HOME", rHomePath.absolutePath()));

   // scan R script for other locations and append them to our vars
   config_utils::Variables locationVars;
   locationVars.push_back(std::make_pair("R_SHARE_DIR", ""));
   locationVars.push_back(std::make_pair("R_INCLUDE_DIR", ""));
   locationVars.push_back(std::make_pair("R_DOC_DIR", ""));
   error = config_utils::extractVariables(rScriptPath, &locationVars);
   if (error)
   {
      LOG_ERROR(error);
      *pErrMsg = "Error reading R script (" + rScriptPath.absolutePath() +
                 "), " + error.summary();
      return false;
   }
   pVars->insert(pVars->end(), locationVars.begin(), locationVars.end());

   // determine library path (existing + r lib dir + r extra lib dirs)
   std::string libraryPath = core::system::getenv(kLibraryPathEnvVariable);
   if (!libraryPath.empty())
      libraryPath.append(":");
   libraryPath.append(rHomePath.complete("lib").absolutePath());
   std::string extraPaths = extraLibraryPaths(ldPathsScript,
                                              rHomePath.absolutePath());
   if (!extraPaths.empty())
      libraryPath.append(":" + extraPaths);
   pVars->push_back(std::make_pair(kLibraryPathEnvVariable, libraryPath));

   return validateREnvironment(*pVars, pErrMsg);
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



