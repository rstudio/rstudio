/*
 * RVersions.cpp
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

#include <core/r_util/RVersions.hpp>

#include <iostream>
#include <algorithm>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <core/Algorithm.hpp>
#include <core/r_util/REnvironment.hpp>

#include <core/system/Environment.hpp>

#define kRFrameworkVersions "/Library/Frameworks/R.framework/Versions"
#define kRScriptPath "Resources/bin/R"

namespace core {
namespace r_util {

namespace {

std::vector<FilePath> removeNonExistent(const std::vector<FilePath>& paths)
{
   std::vector<FilePath> filteredPaths;
   BOOST_FOREACH(const FilePath& path, paths)
   {
      if (path.exists())
         filteredPaths.push_back(path);
   }
   return filteredPaths;
}

} // anonymous namespace

std::vector<RVersion> enumerateRVersions(
                              const std::vector<FilePath>& otherRHomes,
                              const FilePath& ldPathsScript,
                              const std::string& ldLibraryPath)
{
   std::vector<RVersion> rVersions;

   // start with all of the typical script locations
   std::vector<FilePath> rScriptPaths;
   const char* const kDefaultVersion = "/usr/bin/R";
   rScriptPaths.push_back(FilePath(kDefaultVersion));
   rScriptPaths.push_back(FilePath("/usr/local/bin/R"));
   rScriptPaths.push_back(FilePath("/opt/local/bin/R"));

   // add the additional R homes
   BOOST_FOREACH(const FilePath& otherRHome, otherRHomes)
   {
      rScriptPaths.push_back(otherRHome.childPath("bin/R"));
   }

   // filter on existence and eliminate duplicates
   rScriptPaths = removeNonExistent(rScriptPaths);
   std::sort(rScriptPaths.begin(), rScriptPaths.end());
   std::unique(rScriptPaths.begin(), rScriptPaths.end());

   // probe versions
   BOOST_FOREACH(const FilePath& rScriptPath, rScriptPaths)
   {
      std::string rDiscoveredScriptPath, rVersion, errMsg;
      core::system::Options env;
      if (detectREnvironment(rScriptPath,
                             ldPathsScript,
                             ldLibraryPath,
                             &rDiscoveredScriptPath,
                             &rVersion,
                             &env,
                             &errMsg))
      {
         RVersion version;
         version.isDefault = (rScriptPath.absolutePath() == kDefaultVersion);
         version.number = rVersion;
         version.arch = "64";
         version.environment = env;
         rVersions.push_back(version);
      }
      else
      {
         LOG_ERROR_MESSAGE("Error scanning R version at " +
                           rScriptPath.absolutePath() + ": " +
                           errMsg);
      }
   }

   // scan the R frameworks directory
   FilePath rFrameworkVersions(kRFrameworkVersions);
   std::vector<FilePath> versionPaths;
   Error error = rFrameworkVersions.children(&versionPaths);
   if (error)
      LOG_ERROR(error);
   BOOST_FOREACH(const FilePath& versionPath, versionPaths)
   {
      if (!versionPath.isHidden() && (versionPath.filename() != "Current"))
      {
         using namespace core::system;
         core::system::Options env;
         FilePath rHomePath = versionPath.childPath("Resources");
         FilePath rLibPath = rHomePath.childPath("lib");
         core::system::setenv(&env, "R_HOME", rHomePath.absolutePath());
         core::system::setenv(&env,
                              "R_SHARE_DIR",
                              rHomePath.childPath("share").absolutePath());
         core::system::setenv(&env,
                              "R_INCLUDE_DIR",
                               rHomePath.childPath("include").absolutePath());
         core::system::setenv(&env,
                              "R_DOC_DIR",
                               rHomePath.childPath("doc").absolutePath());
         core::system::setenv(&env,
                              "DYLD_FALLBACK_LIBRARY_PATH",
                              r_util::rLibraryPath(rHomePath,
                                                   rLibPath,
                                                   ldPathsScript,
                                                   ldLibraryPath));
         core::system::setenv(&env, "R_ARCH", "/x86_64");

         RVersion version;
         version.number = versionPath.filename();
         version.arch = "64";
         version.environment = env;

         // improve on the version by asking R for it's version
         FilePath rBinaryPath = rHomePath.childPath("bin/exec/R");
         if (!rBinaryPath.exists())
            rBinaryPath = rHomePath.childPath("bin/exec/x86_64/R");
         if (rBinaryPath.exists())
         {
            Error error = rVersion(rHomePath,
                                   rBinaryPath,
                                   &version.number);
            if (error)
               LOG_ERROR(error);
         }

         rVersions.push_back(version);
      }
   }


   return rVersions;
}

} // namespace r_util
} // namespace core 



