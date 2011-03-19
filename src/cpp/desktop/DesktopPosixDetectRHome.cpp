/*
 * DesktopMacDetectRHome.cpp
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

#include "DesktopDetectRHome.hpp"

#include <vector>

#include <boost/algorithm/string/trim.hpp>

#include <QtCore>
#include <QMessageBox>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include "config.h"

using namespace core;

namespace desktop {

namespace {

// MacOS X Specific
#ifdef __APPLE__

#define kLibRFileName            "libR.dylib"
#define kLibraryPathEnvVariable  "DYLD_LIBRARY_PATH"

// define additional search paths for R. match shell search path behavior
// (including /opt/local/bin being first since that is where macports puts it)
void appendRPaths(std::vector<std::string>* pRPaths)
{
   pRPaths->push_back("/opt/local/bin/R");
   pRPaths->push_back("/usr/bin/R");
   pRPaths->push_back("/usr/local/bin/R");
}

// no extra paths on the mac
std::string extraLibraryPaths(const std::string& rHome)
{
 return std::string();
}


// Linux specific
#else

#define kLibRFileName            "libR.so"
#define kLibraryPathEnvVariable  "LD_LIBRARY_PATH"

// define additional search paths for R. match typical shell search path behavior
// (note differs from macox by having /usr/local/bin first)
void appendRPaths(std::vector<std::string>* pRPaths)
{
   pRPaths->push_back("/usr/local/bin/R");
   pRPaths->push_back("/usr/bin/R");
}

// extra paths from R (for rjava) on linux
std::string extraLibraryPaths(const std::string& rHome)
{
   std::string libraryPaths;

   FilePath supportingFilePath = desktop::options().supportingFilePath();
   FilePath scriptPath = supportingFilePath.complete("bin/r-ldpath");
   if (!scriptPath.exists())
      scriptPath = supportingFilePath.complete("session/r-ldpath");
   if (scriptPath.exists())
   {
      // run script
      std::string command = scriptPath.absolutePath() + " " + rHome;
      Error error = system::captureCommand(command, &libraryPaths);
      if (error)
         LOG_ERROR(error);
   }

   return libraryPaths;
}


#endif


FilePath systemDefaultR()
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

FilePath detectRHome()
{
   // scan possible locations for R (need these because on the mac
   // our path as a GUI app is very limited)
   FilePath rPath;
   std::vector<std::string> rPaths;

   // system default goes first (and will always be used if known)
   FilePath systemDefaultRPath = systemDefaultR();
   if (!systemDefaultRPath.empty())
      rPaths.push_back(systemDefaultRPath.absolutePath());

   // platfom specific additional paths to look for R binary
   appendRPaths(&rPaths);

   // scan the paths for a file that exists
   for(std::vector<std::string>::const_iterator it = rPaths.begin();
       it != rPaths.end(); ++it)
   {
      FilePath candidatePath(*it);
      if (candidatePath.exists())
      {
         rPath = candidatePath ;
         break;
      }
   }

   // if we didn't find one then bail
   if (rPath.empty())
   {
      LOG_ERROR_MESSAGE("Couldn't find R on known path");
      return FilePath();
   }

   // run R to detect R home
   std::string output;
   std::string command = rPath.absolutePath() + " RHOME";
   Error error = core::system::captureCommand(command, &output);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }
   boost::algorithm::trim(output);

   // return the home path if we got one
   if (!output.empty())
      return FilePath(output);
   else
      return FilePath();
}

void showRNotFoundError(const std::string& msg)
{
   QMessageBox::critical(NULL, "R Not Found", QString::fromStdString(msg));
}

} // anonymous namespace

bool prepareEnvironment(Options&)
{
   // declare home path and doc dir -- can get them from the environment
   // or by probing the system
   FilePath homePath, rDocDir;

   // first check environment
   std::string home = core::system::getenv("R_HOME");
   if (!home.empty())
      homePath = FilePath(home);
   std::string doc = core::system::getenv("R_DOC_DIR");
   if (!doc.empty())
      rDocDir = FilePath(doc);

   // probe for home path if necessary
   if (homePath.empty())
      homePath = detectRHome();

   // if that didn't work then try the configured path as a last ditch
   if (!homePath.exists())
      homePath = FilePath(CONFIG_R_HOME_PATH);

   // verify and set home path
   if (homePath.exists())
   {
      core::system::setenv("R_HOME", homePath.absolutePath());
   }
   else
   {
      showRNotFoundError("R home path (" + homePath.absolutePath() +
                         ") does not exist. Is R installed on "
                         "this system?");
      return false;
   }

   // complete doc dir if necesary
   if (rDocDir.empty())
      rDocDir = homePath.complete("doc");

   // doc dir may be in configured location (/usr/share) rather than
   // a subdirectory of R_HOME (this is in fact the case for the
   // debian r-base package). so check the configuired location as
   // a last ditch
   if (!rDocDir.exists())
      rDocDir = FilePath(CONFIG_R_DOC_PATH);

   // verify and set doc dir
   if (rDocDir.exists())
   {
       core::system::setenv("R_DOC_DIR", rDocDir.absolutePath());
   }
   else
   {
      showRNotFoundError("R doc directory (" + rDocDir.absolutePath() +
                         ") does not exist.");
      return false;
   }

   // verify and set library path
   FilePath rLibPath = homePath.complete("lib");
   if (rLibPath.exists())
   {
      // make sure the R lib was built
      FilePath libRpath = rLibPath.complete(kLibRFileName);
      if (libRpath.exists())
      {
         // initialize library path from existing value + any extras
         std::string libraryPath = core::system::getenv(kLibraryPathEnvVariable);
         if (!libraryPath.empty())
            libraryPath.append(":");
         libraryPath.append(rLibPath.absolutePath());
         std::string extraPaths = extraLibraryPaths(homePath.absolutePath());
         if (!extraPaths.empty())
            libraryPath.append(":" + extraPaths);

         // set it
         core::system::setenv(kLibraryPathEnvVariable, libraryPath);
      }
      else
      {
         showRNotFoundError(rLibPath.absolutePath() + " not found. "
                            "If this is a custom build of R, was it "
                            "built with the --enable-R-shlib option?");
         return false;
      }
   }
   else
   {
      showRNotFoundError("R library directory (" + rLibPath.absolutePath() +
                         ") does not exist.");
      return false;
   }

   return true;
}

} // namespace desktop
