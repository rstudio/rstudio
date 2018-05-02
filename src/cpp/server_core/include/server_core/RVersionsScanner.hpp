/*
 * RVersionsScanner.hpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#ifndef SERVER_CORE_R_VERSIONS_SCANNER_HPP
#define SERVER_CORE_R_VERSIONS_SCANNER_HPP

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/r_util/RVersionsPosix.hpp>

namespace rstudio {
namespace core {

class RVersionsScanner
{
public:
   RVersionsScanner();

   RVersionsScanner(bool checkCommonRLocations,
                    const std::string& whichROverride,
                    const std::string& rLdScriptPath,
                    const std::string& ldLibraryPath);

   RVersionsScanner(bool checkCommonRLocations,
                    const std::string& whichROverride,
                    const std::string& rLdScriptPath,
                    const std::string& ldLibraryPath,
                    const r_util::RVersion& profileDefaultR,
                    const std::vector<core::FilePath>& profileRHomeDirs);

   // scans for r versions and returns any that were found
   // subsequent calls return cached versions found in initial scan
   std::vector<r_util::RVersion> getRVersions();

   bool detectRVersion(const core::FilePath& rScriptPath,
                       core::r_util::RVersion* pVersion,
                       std::string* pErrMsg);

   bool detectSystemRVersion(core::r_util::RVersion* pVersion,
                             std::string* pErrMsg);

private:
   bool checkCommonRLocations_;
   std::string whichROverride_;
   core::FilePath rLdScriptPath_;
   std::string rLdLibraryPath_;
   core::r_util::RVersion profileDefaultR_;
   std::vector<FilePath> profileRHomeDirs_;

   // cached versions
   core::r_util::RVersion systemVersion_;
   std::vector<r_util::RVersion> cachedVersions_;

   void parseRVersionsFile(const std::string& contents,
                           std::vector<FilePath> *pRPaths,
                           std::vector<r_util::RVersion> *pREntries);

   boost::shared_ptr<r_util::RVersion> parseREntry(const std::string& rEntryStr);

   void setFallbackVersion();
};

} // namespace core
} // namespace rstudio

#endif // SERVER_CORE_R_VERSIONS_SCANNER_HPP

