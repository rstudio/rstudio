/*
 * RVersions.hpp
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

#ifndef CORE_R_UTIL_R_VERSIONS_HPP
#define CORE_R_UTIL_R_VERSIONS_HPP

#define kRStudioRVersionsPath "RS_R_VERSIONS_PATH"

#include <vector>
#include <iosfwd>

#include <core/FilePath.hpp>

#include <core/json/Json.hpp>

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
   }

public:
   bool empty() const { return number_.empty(); }

   FilePath homeDir() const
   {
      return FilePath(core::system::getenv(environment_, "R_HOME"));
   }

   void setHomeDir(const FilePath& filePath)
   {
      core::system::setenv(&environment_, "R_HOME", filePath.absolutePath());
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

   bool operator<(const RVersion& other) const
   {
      RVersionNumber ver = RVersionNumber::parse(number());
      RVersionNumber otherVer = RVersionNumber::parse(other.number());

      if (ver == otherVer)
         return homeDir().absolutePath() < other.homeDir().absolutePath();
      else
         return ver < otherVer;
   }

   bool operator==(const RVersion& other) const
   {
      return number() == other.number() &&
             homeDir().absolutePath() == other.homeDir().absolutePath() &&
             (label() == other.label() || (label().empty() || other.label().empty()));
   }

private:
   std::string number_;
   core::system::Options environment_;
   std::string label_;
   std::string module_;
   std::string prelaunchScript_;
};

std::ostream& operator<<(std::ostream& os, const RVersion& version);

std::vector<RVersion> enumerateRVersions(
                              std::vector<FilePath> rHomePaths,
                              std::vector<r_util::RVersion> rEntries,
                              bool scanForOtherVersions,
                              const FilePath& ldPathsScript,
                              const std::string& ldLibraryPath);

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

