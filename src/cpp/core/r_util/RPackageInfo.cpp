/*
 * RPackageInfo.cpp
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

#include <core/r_util/RPackageInfo.hpp>

#include <boost/format.hpp>

#include <core/Error.hpp>

#include <core/text/DcfParser.hpp>

namespace core {
namespace r_util {

namespace {

Error fieldNotFoundError(const FilePath& descFilePath,
                         const std::string& fieldName,
                         const ErrorLocation& location)
{
   return systemError(
            boost::system::errc::protocol_error,
            fieldName + " field not found in " + descFilePath.absolutePath(),
            location);
}

} // anonymous namespace


Error RPackageInfo::read(const FilePath& packageDir)
{
   // parse DCF file
   FilePath descFilePath = packageDir.childPath("DESCRIPTION");
   if (!descFilePath.exists())
      return core::fileNotFoundError(descFilePath, ERROR_LOCATION);
   std::string errMsg;
   std::map<std::string,std::string> fields;
   Error error = text::parseDcfFile(descFilePath, true, &fields, &errMsg);
   if (error)
      return error;

   // Package field
   std::map<std::string,std::string>::const_iterator it;
   it = fields.find("Package");
   if (it != fields.end())
      name_ = it->second;
   else
      return fieldNotFoundError(descFilePath, "Package", ERROR_LOCATION);

   //  Version field
   it = fields.find("Version");
   if (it != fields.end())
      version_ = it->second;
   else
      return fieldNotFoundError(descFilePath, "Version", ERROR_LOCATION);

   // Linking to field
   it = fields.find("LinkingTo");
   if (it != fields.end())
      linkingTo_ = it->second;

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

} // namespace r_util
} // namespace core 



