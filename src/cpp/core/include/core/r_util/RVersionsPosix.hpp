/*
 * RVersions.hpp
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

#ifndef CORE_R_UTIL_R_VERSIONS_HPP
#define CORE_R_UTIL_R_VERSIONS_HPP

#define kRStudioRVersionsPath "RS_R_VERSIONS_PATH"

#include <vector>
#include <iosfwd>

#include <shared_core/FilePath.hpp>

#include <shared_core/json/Json.hpp>

#include <core/system/Environment.hpp>

#include <core/r_util/RVersionInfo.hpp>

namespace rstudio {
namespace core {
namespace r_util {

class RVersion
{
public:
   RVersion() {}
   RVersion(const std::string& number, const core::system::Options& environment)
      : number_(number), environment_(environment)
   {
      setLabel(std::string());
      setModule(std::string());
      setPrelaunchScript(std::string());
      setRepo(std::string());
      setLibrary(std::string());
   }

public:
   bool empty() const { return number_.empty(); }

   FilePath homeDir() const
   {
      return FilePath(core::system::getenv(environment_, "R_HOME"));
   }

   void setHomeDir(const FilePath& filePath)
   {
      core::system::setenv(&environment_, "R_HOME", filePath.getAbsolutePath());
   }

   const std::string& number() const { return number_; }
   void setNumber(const std::string& number) { number_ = number; }

   const core::system::Options& environment() const { return environment_; }
   void setEnvironment(const core::system::Options& environment) { environment_ = environment; }

   const std::string& label() const { return label_; }
   void setLabel(const std::string& label)
   {
      label_ = label;
      core::system::setenv(&environment_, "RSTUDIO_R_VERSION_LABEL", label);
   }

   const std::string& module() const { return module_; }
   void setModule(const std::string& module)
   {
      module_ = module;
      core::system::setenv(&environment_, "RSTUDIO_R_MODULE", module);
   }

   const std::string& prelaunchScript() const { return prelaunchScript_; }
   void setPrelaunchScript(const std::string& prelaunchScript)
   {
      prelaunchScript_ = prelaunchScript;
      core::system::setenv(&environment_, "RSTUDIO_R_PRELAUNCH_SCRIPT", prelaunchScript);
   }

   const std::string& repo() const { return repo_; }
   void setRepo(const std::string& repo)
   {
      repo_ = repo;
      core::system::setenv(&environment_, "RSTUDIO_R_REPO", repo);
   }

   const std::string& library() const { return library_; }
   void setLibrary(const std::string& library)
   {
      library_ = library;

      // only set R_LIBS_SITE env var if it is non-empty
      // setting an empty site library will cause the default system configuration to be lost
      if (!library.empty())
         core::system::setenv(&environment_, "R_LIBS_SITE", library);
   }

   bool operator<(const RVersion& other) const
   {
      RVersionNumber ver = RVersionNumber::parse(number());
      RVersionNumber otherVer = RVersionNumber::parse(other.number());

      if (ver == otherVer)
         return homeDir().getAbsolutePath() < other.homeDir().getAbsolutePath();
      else
         return ver < otherVer;
   }

   bool operator==(const RVersion& other) const
   {
      return number() == other.number() &&
         homeDir().getAbsolutePath() == other.homeDir().getAbsolutePath() &&
             (label() == other.label() || (label().empty() || other.label().empty()));
   }

private:
   std::string number_;
   core::system::Options environment_;
   std::string label_;
   std::string module_;
   std::string prelaunchScript_;
   std::string repo_;
   std::string library_;
};

std::ostream& operator<<(std::ostream& os, const RVersion& version);

std::vector<RVersion> enumerateRVersions(
                              std::vector<FilePath> rHomePaths,
                              std::vector<r_util::RVersion> rEntries,
                              bool scanForOtherVersions,
                              const FilePath& ldPathsScript,
                              const std::string& ldLibraryPath,
                              const FilePath& modulesBinaryPath);

RVersion selectVersion(const std::string& number,
                       const std::string& rHomeDir,
                       const std::string& label,
                       std::vector<RVersion> versions);

json::Object rVersionToJson(const r_util::RVersion& version);

r_util::RVersion rVersionFromJson(const json::Object& versionJson);

Error rVersionsFromJson(const json::Array& versionsJson,
                        std::vector<RVersion>* pVersions);

json::Array versionsToJson(const std::vector<r_util::RVersion>& versions);

Error writeRVersionsToFile(const FilePath& filePath,
                          const std::vector<r_util::RVersion>& versions);

Error readRVersionsFromFile(const FilePath& filePath,
                            std::vector<r_util::RVersion>* pVersions);

Error validatedReadRVersionsFromFile(const FilePath& filePath,
                                     std::vector<r_util::RVersion>* pVersions);

} // namespace r_util
} // namespace core 
} // namespace rstudio

#endif // CORE_R_UTIL_R_VERSIONS_HPP

