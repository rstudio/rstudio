/*
 * RToolsInfo.hpp
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

#ifndef CORE_R_UTIL_R_TOOLS_INFO_HPP
#define CORE_R_UTIL_R_TOOLS_INFO_HPP

#include <string>
#include <vector>
#include <iosfwd>

#include <core/FilePath.hpp>
#include <core/system/Environment.hpp>

namespace core {
namespace r_util {

class RToolsInfo
{
public:
   RToolsInfo() {}
   RToolsInfo(const std::string& name,
              const FilePath& installPath);

   bool empty() const { return name_.empty(); }

   bool isRecognized() const { return !versionPredicate().empty(); }

   const std::string& name() const { return name_; }
   const std::string& versionPredicate() const { return versionPredicate_; }
   const FilePath& installPath() const { return installPath_; }
   const std::vector<FilePath>& pathEntries() const { return pathEntries_; }

private:
   std::string name_;
   FilePath installPath_;
   std::string versionPredicate_;
   std::vector<FilePath> pathEntries_;
};

std::ostream& operator<<(std::ostream& os, const RToolsInfo& info);

Error discoverRTools(std::vector<RToolsInfo>* pRTools);

void prependToSystemPath(const RToolsInfo& toolsInfo,
                         core::system::Options* pEnvironment);


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_TOOLS_INFO_HPP

