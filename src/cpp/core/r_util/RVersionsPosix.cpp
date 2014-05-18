/*
 * RVersionsPosix.cpp
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

std::vector<RVersion> enumerateRVersionsPosix(
                              const std::string& arch,
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

   // scan /opt/R
   FilePath optRDir("/opt/R");
   if (optRDir.exists())
   {
      std::vector<FilePath> optRDirs;
      Error error = optRDir.children(&optRDirs);
      if (error)
         LOG_ERROR(error);
      BOOST_FOREACH(const FilePath& optRDir, optRDirs)
      {
         FilePath rScriptPath = optRDir.childPath("bin/R");
         if (rScriptPath.exists())
            rScriptPaths.push_back(rScriptPath);
      }
   }

   // add the additional R homes
   BOOST_FOREACH(const FilePath& otherRHome, otherRHomes)
   {
      FilePath rScriptPath = otherRHome.childPath("bin/R");
      if (rScriptPath.exists())
         rScriptPaths.push_back(rScriptPath);
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
         version.arch = arch;
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

   return rVersions;
}

} // namespace r_util
} // namespace core 



