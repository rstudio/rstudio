/*
 * MacRecycleBin.cpp
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

#include <CoreServices/CoreServices.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/StringUtils.hpp>

// GetMacOSStatusCommentString and FSMoveObjectToTrashSync deprecated
// as of Mac OS X 10.8
#ifdef __clang__
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
#endif

namespace rstudio {
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
   std::string sysPath = string_utils::utf8ToSystem(filePath.getAbsolutePath());
   OSStatus status = ::FSPathMakeRefWithOptions(
                                        (const UInt8*)sysPath.c_str(),
                                        kFSPathMakeRefDoNotFollowLeafSymlink,
                                        &ref,
                                        nullptr);
   if (status != 0)
      return errorForStatus(status, filePath, ERROR_LOCATION);

   status = ::FSMoveObjectToTrashSync(&ref,
                                      nullptr,
                                      kFSFileOperationDefaultOptions);
   if (status != 0)
      return errorForStatus(status, filePath, ERROR_LOCATION);
   else
      return Success();
}

} // namespace recycle_bin
} // namespace system
} // namespace core
} // namespace rstudio

