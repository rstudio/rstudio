/*
 * RPackageInfo.hpp
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

#ifndef CORE_R_UTIL_R_PACKAGE_INFO_HPP
#define CORE_R_UTIL_R_PACKAGE_INFO_HPP

#include <string>

#include <shared_core/FilePath.hpp>

#define kPackageType "Package"

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace r_util {

class RPackageInfo
{
public:
   RPackageInfo()
   {
   }
   ~RPackageInfo()
   {
   }

   // COPYING: via compiler

public:
   Error read(const FilePath& packageDir);

   bool empty() const { return name().empty(); }

   const std::string& name() const { return name_; }
   const std::string& version() const { return version_; }
   const std::string& depends() const { return depends_; }
   const std::string& imports() const { return imports_; }
   const std::string& suggests() const { return suggests_; }
   const std::string& linkingTo() const { return linkingTo_; }
   const std::string& systemRequirements() const { return systemRequirements_; }
   const std::string& type() const { return type_; }
   const std::string& rdMacros() const { return rdMacros_; }

   std::string sourcePackageFilename() const;

private:
   std::string packageFilename(const std::string& extension) const;

private:
   std::string name_;
   std::string version_;
   std::string depends_;
   std::string imports_;
   std::string suggests_;
   std::string linkingTo_;
   std::string systemRequirements_;
   std::string type_;
   std::string rdMacros_;
};

bool isPackageDirectory(const FilePath& dir);

// Determine name of a package project in given directory. Returns empty string
// if not a package or any other error.
std::string packageNameFromDirectory(const FilePath& dir);

} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_PACKAGE_INFO_HPP

