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

#include <core/r_util/RVersionsPosix.hpp>

#include <iostream>
#include <algorithm>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <core/Algorithm.hpp>
#include <core/r_util/REnvironment.hpp>

#include <core/system/Environment.hpp>

#ifdef __APPLE__
#define kRFrameworkVersions "/Library/Frameworks/R.framework/Versions"
#define kRScriptPath "Resources/bin/R"
#endif

namespace rstudio {
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

std::ostream& operator<<(std::ostream& os, const RVersion& version)
{
   os << version.number;
   if (version.isDefault)
      os << " [default]";
   os << std::endl;
   os << version.homeDir() << std::endl;
   BOOST_FOREACH(const core::system::Option& option, version.environment)
   {
      os << option.first << "=" << option.second << std::endl;
   }
   os << std::endl;

   return os;
}

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

#ifdef __APPLE__
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
         using namespace rstudio::core::system;
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
#endif

   return rVersions;
}

namespace {

bool isVersion(const RVersionNumber& test, const RVersion& item)
{
   return test == RVersionNumber::parse(item.number);
}

bool isMajorMinorVersion(RVersionNumber& test, const RVersion& item)
{
   RVersionNumber itemNumber = RVersionNumber::parse(item.number);
   return (test.major() == itemNumber.major() &&
           test.minor() == itemNumber.minor());
}

bool compareVersion(const RVersion& a, const RVersion& b)
{
   return RVersionNumber::parse(a.number) <
          RVersionNumber::parse(b.number);
}

bool compareVersionInfo(const RVersionInfo& versionInfo,
                        const RVersion& version)
{
   return RVersionNumber::parse(versionInfo.number) <
          RVersionNumber::parse(version.number);
}

RVersion findClosest(const RVersionInfo& matchVersion,
                     std::vector<RVersion> versions)
{
   // sort so algorithms work correctly
   std::sort(versions.begin(), versions.end(), compareVersion);

   // first look for an upper_bound
   std::vector<RVersion>::const_iterator it;
   it = std::upper_bound(versions.begin(),
                         versions.end(),
                         matchVersion,
                         compareVersionInfo);
   if (it != versions.end())
      return *it;

   // can't find a greater version, use the newest version
   return *std::max_element(versions.begin(), versions.end(), compareVersion);
}

}


RVersion selectVersion(const RVersionInfo& matchVersion,
                       std::vector<RVersion> versions)
{
   // check for empty
   if (versions.empty())
      return RVersion();

   // version we are seeking
   RVersionNumber matchNumber = RVersionNumber::parse(matchVersion.number);

   // order correctly for algorithms
   std::sort(versions.begin(), versions.end(), compareVersion);

   // first seek an exact match
   std::vector<RVersion>::const_iterator it;
   it = std::find_if(versions.begin(),
                     versions.end(),
                     boost::bind(isVersion, matchNumber, _1));
   if (it != versions.end())
      return *it;

   // now look for versions that match major and minor (same series)
   std::vector<RVersion> seriesVersions;
   algorithm::copy_if(versions.begin(),
                      versions.end(),
                      std::back_inserter(seriesVersions),
                      boost::bind(isMajorMinorVersion, matchNumber, _1));

   // find the closest match in the series
   if (seriesVersions.size() > 0)
   {
      return findClosest(matchVersion, seriesVersions);
   }
   // otherwise find the closest match in the whole list
   else
   {
      return findClosest(matchVersion, versions);
   }
}


} // namespace r_util
} // namespace core 
} // namespace rstudio



