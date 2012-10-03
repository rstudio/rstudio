/*
 * MacRecycleBin.cpp
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

#include <CoreServices/CoreServices.h>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/StringUtils.hpp>

namespace core {
namespace system {
namespace recycle_bin {
      
namespace {

Error errorForStatus(OSStatus status,
                     const FilePath& filePath,
                     const ErrorLocation& location)
{
   Error error = systemError(boost::system::errc::protocol_error,
                             ::GetMacOSStatusCommentString(status),
                             location);
   error.addProperty("OSStatus", status);
   error.addProperty("path", filePath);
   return error;
}

} // anonymous namespace

Error sendTo(const FilePath& filePath)
{
   FSRef ref;
   std::string sysPath = string_utils::utf8ToSystem(filePath.absolutePath());
   OSStatus status = ::FSPathMakeRefWithOptions(
                                        (const UInt8*)sysPath.c_str(),
                                        kFSPathMakeRefDoNotFollowLeafSymlink,
                                        &ref,
                                        NULL);
   if (status != 0)
      return errorForStatus(status, filePath, ERROR_LOCATION);

   status = ::FSMoveObjectToTrashSync(&ref,
                                      NULL,
                                      kFSFileOperationDefaultOptions);
   if (status != 0)
      return errorForStatus(status, filePath, ERROR_LOCATION);
   else
      return Success();
}

} // namespace recycle_bin
} // namespace system
} // namespace core

