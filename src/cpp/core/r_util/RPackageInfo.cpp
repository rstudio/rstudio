/*
 * RPackageInfo.cpp
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

#include <core/r_util/RPackageInfo.hpp>

#include <boost/format.hpp>

#include <core/Log.hpp>
#include <core/text/DcfParser.hpp>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace r_util {

namespace {

Error fieldNotFoundError(const FilePath& descFilePath,
                         const std::string& fieldName,
                         const ErrorLocation& location)
{
   return systemError(
            boost::system::errc::protocol_error,
            fieldName + " field not found in " + descFilePath.getAbsolutePath(),
            location);
}

template <typename Container>
Error readField(const Container& container,
                const std::string& fieldName,
                std::string* pField,
                const FilePath& descFilePath,
                const ErrorLocation& errorLocation)
{
   typename Container::const_iterator it = container.find(fieldName);
   if (it == container.end())
      return fieldNotFoundError(descFilePath, fieldName, errorLocation);
   
   *pField = it->second;
   return Success();
}

template <typename Container>
void readField(const Container& container,
               const std::string& fieldName,
               std::string* pField,
               const std::string& defaultValue = std::string())
{
   typename Container::const_iterator it = container.find(fieldName);
   if (it != container.end())
      *pField = it->second;
   else if (!defaultValue.empty())
      *pField = defaultValue;
}

} // anonymous namespace


Error RPackageInfo::read(const FilePath& packageDir)
{
   // parse DCF file
   FilePath descFilePath = packageDir.completeChildPath("DESCRIPTION");
   if (!descFilePath.exists())
      return core::fileNotFoundError(descFilePath, ERROR_LOCATION);
   std::string errMsg;
   std::map<std::string,std::string> fields;
   Error error = text::parseDcfFile(descFilePath, true, &fields, &errMsg);
   if (error)
      return error;

   error = readField(fields, "Package", &name_, descFilePath, ERROR_LOCATION);
   if (error) return error;
   
   error = readField(fields, "Version", &version_, descFilePath, ERROR_LOCATION);
   if (error) return error;
   
   readField(fields, "Depends", &depends_);
   readField(fields, "Imports", &imports_);
   readField(fields, "Suggests", &suggests_);
   readField(fields, "LinkingTo", &linkingTo_);
   readField(fields, "SystemRequirements", &systemRequirements_);
   readField(fields, "Type", &type_, kPackageType);
   readField(fields, "RdMacros", &rdMacros_);

   return Success();
}


std::string RPackageInfo::sourcePackageFilename() const
{
   return packageFilename("tar.gz");
}

std::string RPackageInfo::packageFilename(const std::string& extension) const
{
   boost::format fmt("%1%_%2%.%3%");
   return boost::str(fmt % name() % version() % extension);
}

bool isPackageDirectory(const FilePath& dir)
{
   if (dir.completeChildPath("DESCRIPTION").exists())
   {
      RPackageInfo pkgInfo;
      Error error = pkgInfo.read(dir);
      if (error)
         return false;

      return pkgInfo.type() == kPackageType;
   }
   else
   {
      return false;
   }
}

std::string packageNameFromDirectory(const FilePath& dir)
{
   if (dir.completeChildPath("DESCRIPTION").exists())
   {
      RPackageInfo pkgInfo;
      Error error = pkgInfo.read(dir);
      if (error)
      {
         LOG_ERROR(error);
         return "";
      }

      return pkgInfo.name();
   }
   else
   {
      return "";
   }
}


} // namespace r_util
} // namespace core 
} // namespace rstudio



