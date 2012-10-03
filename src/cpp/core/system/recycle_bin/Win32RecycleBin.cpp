/*
 * Win32RecycleBin.cpp
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

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <windows.h>
#include <shellapi.h>

namespace core {
namespace system {
namespace recycle_bin {
      
Error sendTo(const FilePath& filePath)
{
   // get the path and double-null terminate
   std::wstring wPath = filePath.absolutePathW();
   std::vector<wchar_t> buffPath;
   std::copy(wPath.begin(), wPath.end(), std::back_inserter(buffPath));
   buffPath.push_back(L'\0');
   buffPath.push_back(L'\0');

   SHFILEOPSTRUCTW fileOp;
   fileOp.hwnd = NULL;
   fileOp.wFunc = FO_DELETE;
   fileOp.pFrom = &(buffPath[0]);
   fileOp.pTo = L"";
   fileOp.fFlags = FOF_ALLOWUNDO |
                   FOF_NOCONFIRMATION |
                   FOF_NOERRORUI |
                   FOF_SILENT;
   fileOp.fAnyOperationsAborted = FALSE;
   fileOp.hNameMappings = NULL;
   fileOp.lpszProgressTitle = L"";

   int result = ::SHFileOperationW(&fileOp);
   if (result != 0)
   {
      Error error = systemError(boost::system::errc::protocol_error,
                                ERROR_LOCATION);
      error.addProperty("result", result);
      error.addProperty("path", filePath);
      return error;
   }
   else
   {
      return Success();
   }
}

} // namespace recycle_bin
} // namespace system
} // namespace core

