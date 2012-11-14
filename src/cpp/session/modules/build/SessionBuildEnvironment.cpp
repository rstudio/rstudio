/*
 * SessionBuildEnvironment.cpp
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

#include "SessionBuildEnvironment.hpp"

#include <string>
#include <vector>

#include <boost/regex.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/r_util/RToolsInfo.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {  
namespace modules {
namespace build {

#ifdef _WIN32

namespace {

bool isRtoolsCompatible(const r_util::RToolsInfo& rTools)
{
   bool isCompatible = false;
   Error error = r::exec::evaluateString(rTools.versionPredicate(),
                                         &isCompatible);
   if (error)
      LOG_ERROR(error);
   return isCompatible;
}

r_util::RToolsInfo scanPathForRTools()
{
   // first confirm ls.exe is in Rtools
   r_util::RToolsInfo noToolsFound;
   FilePath lsPath = module_context::findProgram("ls.exe");
   if (lsPath.empty())
      return noToolsFound;

   // we have a candidate installPath
   FilePath installPath = lsPath.parent().parent();
   core::system::ensureLongPath(&installPath);
   if (!installPath.childPath("Rtools.txt").exists())
      return noToolsFound;

   // find the version path
   FilePath versionPath = installPath.childPath("VERSION.txt");
   if (!versionPath.exists())
      return noToolsFound;

   // further verify that gcc is in Rtools
   FilePath gccPath = module_context::findProgram("gcc.exe");
   if (!gccPath.exists())
      return noToolsFound;
   if (!gccPath.parent().parent().parent().childPath("Rtools.txt").exists())
      return noToolsFound;

   // Rtools is in the path -- now crack the VERSION file
   std::string contents;
   Error error = core::readStringFromFile(versionPath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return noToolsFound;
   }

   // extract the version
   boost::algorithm::trim(contents);
   boost::regex pattern("Rtools version (\\d\\.\\d\\d)[\\d\\.]+$");
   boost::smatch match;
   if (boost::regex_search(contents, match, pattern))
      return r_util::RToolsInfo(match[1], installPath);
   else
      return noToolsFound;
}

std::string formatPath(const FilePath& filePath)
{
   FilePath displayPath = filePath;
   core::system::ensureLongPath(&displayPath);
   return boost::algorithm::replace_all_copy(
                                 displayPath.absolutePath(), "/", "\\");
}

template <typename T>
bool doAddRtoolsToPathIfNecessary(T* pTarget, std::string* pWarningMessage)
{
    // can we find ls.exe and gcc.exe on the path? if so then
    // we assume Rtools are already there (this is the same test
    // used by devtools)
    bool rToolsOnPath = false;
    Error error = r::exec::RFunction(".rs.isRtoolsOnPath").call(&rToolsOnPath);
    if (error)
       LOG_ERROR(error);
    if (rToolsOnPath)
    {
       // perform an extra check to see if the version on the path is not
       // compatible with the currenly running version of R
       r_util::RToolsInfo rTools = scanPathForRTools();
       if (!rTools.empty())
       {
          if (!isRtoolsCompatible(rTools))
          {
            boost::format fmt(
             "WARNING: Rtools version %1% is on the PATH (intalled at %2%) "
             "however is "
             "not compatible with the version of R you are currently running."
             "\n\nPlease download and install the appropriate version of "
             "Rtools to ensure that packages are built correctly:"
             "\n\nhttp://cran.r-project.org/bin/windows/Rtools/");
            *pWarningMessage = boost::str(
               fmt % rTools.name() % formatPath(rTools.installPath()));
          }
       }

       return false;
    }

    // ok so scan for R tools
    std::vector<r_util::RToolsInfo> rTools;
    error = core::r_util::scanRegistryForRTools(&rTools);
    if (error)
    {
       LOG_ERROR(error);
       return false;
    }

    // enumerate them to see if we have a compatible version
    // (go in reverse order for most recent first)
    std::vector<r_util::RToolsInfo>::const_reverse_iterator it = rTools.rbegin();
    for ( ; it != rTools.rend(); ++it)
    {
       if (isRtoolsCompatible(*it))
       {
          r_util::prependToSystemPath(*it, pTarget);
          return true;
       }
    }

    // if we found no version of rtools whatsoever then print warning and return
    if (rTools.empty())
    {
       *pWarningMessage =
           "WARNING: Rtools is required to build R packages however is not "
           "currently installed. "
           "Please download and install the appropriate "
           "version of Rtools before proceeding:\n\n"
           "http://cran.r-project.org/bin/windows/Rtools/";
    }
    else
    {
       // Rtools installed but no compatible version, print a suitable warning
       pWarningMessage->append(
          "WARNING: Rtools is required to build R packages however no version "
          "of Rtools compatible with the version of R you are currently "
          "running was found. Note that the following incompatible version(s) "
          "of Rtools were found:\n\n");

       std::vector<r_util::RToolsInfo>::const_iterator fwdIt = rTools.begin();
       for (; fwdIt != rTools.end(); ++fwdIt)
       {
          std::string path = formatPath(fwdIt->installPath());
          boost::format fmt("  - Rtools %1% (installed at %2%)\n");
          pWarningMessage->append(boost::str(fmt % fwdIt->name() % path));
       }

       pWarningMessage->append(
          "\nPlease download and install the appropriate "
          "version of Rtools before proceeding:\n\n"
          "http://cran.r-project.org/bin/windows/Rtools/");
    }

    return false;
}


} // anonymous namespace


bool addRtoolsToPathIfNecessary(std::string* pPath,
                                std::string* pWarningMessage)
{
   return doAddRtoolsToPathIfNecessary(pPath, pWarningMessage);
}

bool addRtoolsToPathIfNecessary(core::system::Options* pEnvironment,
                                std::string* pWarningMessage)
{
   return doAddRtoolsToPathIfNecessary(pEnvironment, pWarningMessage);
}


#else

bool addRtoolsToPathIfNecessary(std::string* pPath,
                                std::string* pWarningMessage)
{
   return false;
}

bool addRtoolsToPathIfNecessary(core::system::Options* pEnvironment,
                                std::string* pWarningMessage)
{
   return false;
}

#endif


} // namespace build
} // namespace modules
} // namespace session
