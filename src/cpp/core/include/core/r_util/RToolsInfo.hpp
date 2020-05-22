/*
 * RToolsInfo.hpp
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

#ifndef CORE_R_UTIL_R_TOOLS_INFO_HPP
#define CORE_R_UTIL_R_TOOLS_INFO_HPP

#include <string>
#include <vector>
#include <iosfwd>

#include <boost/algorithm/string/replace.hpp>

#include <shared_core/FilePath.hpp>
#include <core/system/Environment.hpp>

namespace rstudio {
namespace core {
namespace r_util {

class RToolsInfo
{
public:
   RToolsInfo() {}
   RToolsInfo(const std::string& name,
              const FilePath& installPath,
              bool usingMingwGcc49);

   bool empty() const { return name_.empty(); }

   bool isRecognized() const { return !versionPredicate().empty(); }

   bool isStillInstalled() const { return installPath_.exists(); }

   const std::string& name() const { return name_; }
   std::string url(const std::string& repos) const;
   const std::string& versionPredicate() const { return versionPredicate_; }
   const FilePath& installPath() const { return installPath_; }
   const std::vector<std::string> clangArgs() const { return clangArgs_; }
   const std::vector<FilePath>& pathEntries() const { return pathEntries_; }
   const std::vector<core::system::Option> environmentVars() const
   {
      return environmentVars_;
   }

private:
   std::string name_;
   FilePath installPath_;
   std::vector<std::string> clangArgs_;
   std::string versionPredicate_;
   std::vector<FilePath> pathEntries_;
   std::vector<core::system::Option> environmentVars_;
};

std::ostream& operator<<(std::ostream& os, const RToolsInfo& info);

void scanForRTools(bool usingMingwGcc49,
                   const std::string& rtoolsHomeEnvVar,
                   std::vector<RToolsInfo>* pRTools);

template <typename T>
void prependToSystemPath(const RToolsInfo& toolsInfo, T* pTarget)
{
   // prepend in reverse order
   std::vector<FilePath>::const_reverse_iterator it
                                          = toolsInfo.pathEntries().rbegin();
   for ( ; it != toolsInfo.pathEntries().rend(); ++it)
   {
      std::string path = it->getAbsolutePath();
      boost::algorithm::replace_all(path, "/", "\\");
      core::system::addToPath(pTarget, path, true);
   }
}


} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_TOOLS_INFO_HPP

