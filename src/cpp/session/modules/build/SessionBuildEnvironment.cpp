/*
 * SessionBuildEnvironment.cpp
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

#include <string>
#include <vector>

#include <boost/regex.hpp>
#include <boost/format.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/r_util/RToolsInfo.hpp>

#include <r/RExec.hpp>
#include <r/RVersionInfo.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

#ifdef _WIN32

namespace {

r_util::RToolsInfo scanPathForRTools()
{
   // first confirm ls.exe is in Rtools
   r_util::RToolsInfo noToolsFound;
   FilePath lsPath = module_context::findProgram("ls.exe");
   if (lsPath.isEmpty())
      return noToolsFound;

   // we have a candidate installPath
   FilePath installPath = lsPath.getParent().getParent();
   core::system::ensureLongPath(&installPath);
   if (!installPath.completeChildPath("Rtools.txt").exists())
      return noToolsFound;

   // find the version path
   FilePath versionPath = installPath.completeChildPath("VERSION.txt");
   if (!versionPath.exists())
      return noToolsFound;

   // further verify that gcc is in Rtools
   FilePath gccPath = module_context::findProgram("gcc.exe");
   if (!gccPath.exists())
      return noToolsFound;
   if (!gccPath.getParent().getParent().getParent().completeChildPath("Rtools.txt").exists())
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
   if (regex_utils::search(contents, match, pattern))
      return r_util::RToolsInfo(match[1],
                                installPath,
                                module_context::usingMingwGcc49());
   else
      return noToolsFound;
}

std::string formatPath(const FilePath& filePath)
{
   FilePath displayPath = filePath;
   core::system::ensureLongPath(&displayPath);
   return boost::algorithm::replace_all_copy(
                                 displayPath.getAbsolutePath(), "/", "\\");
}

template <typename T>
bool doAddRtoolsToPathIfNecessary(T* pTarget,
                                  std::vector<core::system::Option>* pEnvironmentVars,
                                  std::string* pWarningMessage)
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
          if (!module_context::isRtoolsCompatible(rTools))
          {
            boost::format fmt(
             "WARNING: Rtools version %1% is on the PATH (installed at %2%) "
             "but is "
             "not compatible with the currently running version of R."
             "\n\nPlease download and install the appropriate version of "
             "Rtools to ensure that packages are built correctly:"
             "\n\nhttps://cran.rstudio.com/bin/windows/Rtools/"
             "\n\nNote that in addition to installing a compatible version you "
             "also need to remove the incompatible version from your PATH");
            *pWarningMessage = boost::str(
               fmt % rTools.name() % formatPath(rTools.installPath()));
          }
       }

       return false;
    }

    std::string rtoolsHomeEnvVar;

    // Rtools 4.0 will set RTOOLS40_HOME
    auto rVersion = r::version_info::currentRVersion();
    if (rVersion.versionMajor() == 4)
       rtoolsHomeEnvVar = "RTOOLS40_HOME";

    // ok so scan for R tools
    bool usingGcc49 = module_context::usingMingwGcc49();
    std::vector<r_util::RToolsInfo> rTools;
    core::r_util::scanForRTools(usingGcc49, rtoolsHomeEnvVar, &rTools);

    // enumerate them to see if we have a compatible version
    // (go in reverse order for most recent first)
    std::vector<r_util::RToolsInfo>::const_reverse_iterator it = rTools.rbegin();
    for ( ; it != rTools.rend(); ++it)
    {
       if (module_context::isRtoolsCompatible(*it))
       {
          r_util::prependToSystemPath(*it, pTarget);
          *pEnvironmentVars = it->environmentVars();
          return true;
       }
    }

    // if we found no version of rtools whatsoever then print warning and return
    if (rTools.empty())
    {
       *pWarningMessage =
           "WARNING: Rtools is required to build R packages but is not "
           "currently installed. "
           "Please download and install the appropriate "
           "version of Rtools before proceeding:\n\n"
           "https://cran.rstudio.com/bin/windows/Rtools/";
    }
    else
    {
       // Rtools installed but no compatible version, print a suitable warning
       pWarningMessage->append(
          "WARNING: Rtools is required to build R packages but no version "
          "of Rtools compatible with the currently running version of R "
          "was found. Note that the following incompatible version(s) "
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
          "https://cran.rstudio.com/bin/windows/Rtools/");
    }

    return false;
}


} // anonymous namespace

bool isRtoolsCompatible(const r_util::RToolsInfo& rTools)
{
   bool isCompatible = false;
   Error error = r::exec::evaluateString(rTools.versionPredicate(),
                                         &isCompatible);
   if (error)
      LOG_ERROR(error);
   return isCompatible;
}

bool addRtoolsToPathIfNecessary(std::string* pPath,
                                std::string* pWarningMessage)
{
   std::vector<core::system::Option> environmentVars;
   if (doAddRtoolsToPathIfNecessary(pPath,
                                    &environmentVars,
                                    pWarningMessage))
   {
      for (const core::system::Option& var : environmentVars)
      {
         core::system::setenv(var.first, var.second);
      }
      return true;
   }
   else
   {
      return false;
   }
}

bool addRtoolsToPathIfNecessary(core::system::Options* pEnvironment,
                                std::string* pWarningMessage)
{
   std::vector<core::system::Option> environmentVars;
   if (doAddRtoolsToPathIfNecessary(pEnvironment,
                                    &environmentVars,
                                    pWarningMessage))
   {
      for (const core::system::Option& var : environmentVars)
      {
         core::system::setenv(pEnvironment, var.first, var.second);
      }
      return true;
   }
   else
   {
      return false;
   }
}


#else

bool isRtoolsCompatible(const r_util::RToolsInfo& rTools)
{
   return false;
}

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

} // namespace module_context
} // namespace session
} // namespace rstudio
