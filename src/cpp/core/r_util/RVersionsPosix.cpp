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
#include <core/FileSerializer.hpp>
#include <core/r_util/REnvironment.hpp>
#include <core/json/JsonRpc.hpp>

#include <core/system/Environment.hpp>
#include <core/system/System.hpp>

#ifdef __APPLE__
#define kRFrameworkVersions "/Library/Frameworks/R.framework/Versions"
#define kRScriptPath "Resources/bin/R"
#endif

namespace rstudio {
namespace core {
namespace r_util {

namespace {

std::vector<FilePath> realPaths(const std::vector<FilePath>& paths)
{
   std::vector<FilePath> realPaths;
   BOOST_FOREACH(const FilePath& path, paths)
   {
      FilePath realPath;
      Error error = core::system::realPath(path.absolutePath(), &realPath);
      if (!error)
         realPaths.push_back(realPath);
      else
         LOG_ERROR(error);
   }
   return realPaths;
}

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

void scanForRHomePaths(const core::FilePath& rootDir,
                       std::vector<FilePath>* pHomePaths)
{
   if (rootDir.exists())
   {
      std::vector<FilePath> rDirs;
      Error error = rootDir.children(&rDirs);
      if (error)
         LOG_ERROR(error);
      BOOST_FOREACH(const FilePath& rDir, rDirs)
      {
         if (rDir.childPath("bin/R").exists())
            pHomePaths->push_back(rDir);
      }
   }
}


} // anonymous namespace

std::ostream& operator<<(std::ostream& os, const RVersion& version)
{
   os << version.number();
   os << std::endl;
   os << version.homeDir() << std::endl;
   BOOST_FOREACH(const core::system::Option& option, version.environment())
   {
      os << option.first << "=" << option.second << std::endl;
   }
   os << std::endl;

   return os;
}

std::vector<RVersion> enumerateRVersions(
                              std::vector<FilePath> rHomePaths,
                              bool scanForOtherVersions,
                              const FilePath& ldPathsScript,
                              const std::string& ldLibraryPath)
{
   std::vector<RVersion> rVersions;

   // scan if requested
   if (scanForOtherVersions)
   {
      // start with all of the typical script locations
      rHomePaths.push_back(FilePath("/usr/lib/R"));
      rHomePaths.push_back(FilePath("/usr/lib64/R"));
      rHomePaths.push_back(FilePath("/usr/local/lib/R"));
      rHomePaths.push_back(FilePath("/usr/local/lib64/R"));
      rHomePaths.push_back(FilePath("/opt/local/lib/R"));
      rHomePaths.push_back(FilePath("/opt/local/lib64/R"));

      // scan /opt/R and /opt/local/R
      scanForRHomePaths(FilePath("/opt/R"), &rHomePaths);
      scanForRHomePaths(FilePath("/opt/local/R"), &rHomePaths);
   }

   // filter on existence, capture real paths, and eliminate duplicates
   rHomePaths = removeNonExistent(rHomePaths);
   rHomePaths = realPaths(rHomePaths);
   std::sort(rHomePaths.begin(), rHomePaths.end());
   rHomePaths.erase(std::unique(rHomePaths.begin(), rHomePaths.end()),
                    rHomePaths.end());

   // probe versions
   BOOST_FOREACH(const FilePath& rHomePath, rHomePaths)
   {
      // compute R script path
      FilePath rScriptPath = rHomePath.childPath("bin/R");
      if (!rScriptPath.exists())
         continue;

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
         RVersion version(rVersion, env);
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

         RVersion version(versionPath.filename(), env);

         // improve on the version by asking R for it's version
         FilePath rBinaryPath = rHomePath.childPath("bin/exec/R");
         if (!rBinaryPath.exists())
            rBinaryPath = rHomePath.childPath("bin/exec/x86_64/R");
         if (rBinaryPath.exists())
         {
            std::string versionNumber = version.number();
            Error error = rVersion(rHomePath,
                                   rBinaryPath,
                                   ldLibraryPath,
                                   &versionNumber);
            if (error)
               LOG_ERROR(error);
            version = RVersion(versionNumber, version.environment());
         }

         rVersions.push_back(version);
      }
   }
#endif

   // sort the versions
   std::sort(rVersions.begin(), rVersions.end());

   // remove duplicates
   rVersions.erase(std::unique(rVersions.begin(), rVersions.end()),
                   rVersions.end());

   // reverse the order so more recent versions come first
   std::reverse(rVersions.begin(), rVersions.end());

   // return the versions
   return rVersions;
}

namespace {

bool isVersion(const RVersionNumber& number,
               const std::string& rHomeDir,
               const RVersion& item)
{
   return number == RVersionNumber::parse(item.number()) &&
          rHomeDir == item.homeDir().absolutePath();
}

bool isMajorMinorVersion(const RVersionNumber& test, const RVersion& item)
{
   RVersionNumber itemNumber = RVersionNumber::parse(item.number());
   return (test.versionMajor() == itemNumber.versionMajor() &&
           test.versionMinor() == itemNumber.versionMinor());
}


bool compareVersionInfo(const RVersionNumber& versionNumber,
                        const RVersion& version)
{
   return versionNumber < RVersionNumber::parse(version.number());
}

RVersion findClosest(const RVersionNumber& versionNumber,
                     std::vector<RVersion> versions)
{
   // sort so algorithms work correctly
   std::sort(versions.begin(), versions.end());

   // first look for an upper_bound
   std::vector<RVersion>::const_iterator it;
   it = std::upper_bound(versions.begin(),
                         versions.end(),
                         versionNumber,
                         compareVersionInfo);
   if (it != versions.end())
      return *it;

   // can't find a greater version, use the newest version
   return *std::max_element(versions.begin(), versions.end());
}

}


RVersion selectVersion(const std::string& number,
                       const std::string& rHomeDir,
                       std::vector<RVersion> versions)
{
   // check for empty
   if (versions.empty())
      return RVersion();

   // version we are seeking
   RVersionNumber matchNumber = RVersionNumber::parse(number);

   // order correctly for algorithms
   std::sort(versions.begin(), versions.end());

   // first seek an exact match
   std::vector<RVersion>::const_iterator it;
   it = std::find_if(versions.begin(),
                     versions.end(),
                     boost::bind(isVersion, matchNumber, rHomeDir, _1));
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
      return findClosest(matchNumber, seriesVersions);
   }
   // otherwise find the closest match in the whole list
   else
   {
      return findClosest(matchNumber, versions);
   }
}

json::Object rVersionToJson(const RVersion& version)
{
   json::Object versionJson;
   versionJson["number"] = version.number();
   versionJson["environment"] = json::toJsonObject(version.environment());
   return versionJson;
}

Error rVersionFromJson(const json::Object& versionJson,
                       r_util::RVersion* pVersion)
{
   std::string number;
   json::Object environmentJson;
   Error error = json::readObject(versionJson,
                                  "number", &number,
                                  "environment", &environmentJson);
   if (error)
      return error;

   *pVersion = RVersion(number,
                        json::optionsFromJson(environmentJson));
   return Success();
}

json::Array versionsToJson(const std::vector<RVersion>& versions)
{
   json::Array versionsJson;
   std::transform(versions.begin(),
                  versions.end(),
                  std::back_inserter(versionsJson),
                  rVersionToJson);
   return versionsJson;
}

Error rVersionsFromJson(const json::Array& versionsJson,
                        std::vector<RVersion>* pVersions)
{
   BOOST_FOREACH(const json::Value& versionJson, versionsJson)
   {
      if (!json::isType<json::Object>(versionJson))
         return systemError(boost::system::errc::bad_message, ERROR_LOCATION);

      r_util::RVersion rVersion;
      Error error = rVersionFromJson(versionJson.get_obj(), &rVersion);
      if (error)
          return error;

      pVersions->push_back(rVersion);
   }

   return Success();
}


Error writeRVersionsToFile(const FilePath& filePath,
                           const std::vector<r_util::RVersion>& versions)
{
   std::ostringstream ostr;
   json::writeFormatted(versionsToJson(versions), ostr);
   return core::writeStringToFile(filePath, ostr.str());
}

Error readRVersionsFromFile(const FilePath& filePath,
                            std::vector<r_util::RVersion>* pVersions)
{
   // read file contents
   std::string contents;
   Error error = core::readStringFromFile(filePath, &contents);
   if (error)
      return error;

   // parse json
   using namespace json;
   json::Value jsonValue;
   if (!parse(contents, &jsonValue) || !isType<json::Array>(jsonValue))
   {
      Error error = systemError(boost::system::errc::bad_message,
                                ERROR_LOCATION);
      error.addProperty("contents", contents);
      return error;
   }

   return rVersionsFromJson(jsonValue.get_array(), pVersions);
}

Error validatedReadRVersionsFromFile(const FilePath& filePath,
                                     std::vector<r_util::RVersion>* pVersions)
{
   std::vector<r_util::RVersion> versions;
   Error error = readRVersionsFromFile(filePath, &versions);
   if (error)
      return error;

   // ensure the home path exists before returning
   BOOST_FOREACH(const r_util::RVersion& version, versions)
   {
      if (version.homeDir().exists())
      {
         pVersions->push_back(version);
      }
      else
      {
         LOG_WARNING_MESSAGE("R version home directory not found: " +
                             version.homeDir().absolutePath());
      }
   }

   return Success();
}


} // namespace r_util
} // namespace core
} // namespace rstudio



