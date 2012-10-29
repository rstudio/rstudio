/*
 * RPackageInfo.hpp
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

#ifndef CORE_R_UTIL_R_PACKAGE_INFO_HPP
#define CORE_R_UTIL_R_PACKAGE_INFO_HPP

#include <string>

#include <core/FilePath.hpp>

namespace core {

class Error;

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
   const std::string& linkingTo() const { return linkingTo_; }

   std::string sourcePackageFilename() const;

private:
   std::string packageFilename(const std::string& extension) const;

private:
   std::string name_;
   std::string version_;
   std::string linkingTo_;
};


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_PACKAGE_INFO_HPP

