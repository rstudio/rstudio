/*
 * Win32RecycleBin.cpp
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

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <windows.h>
#include <shellapi.h>

namespace rstudio {
namespace core {
namespace system {
namespace recycle_bin {
      
Error sendTo(const FilePath& filePath)
{
   // get the path and double-null terminate
   std::wstring wPath = filePath.getAbsolutePathW();
   std::vector<wchar_t> buffPath;
   std::copy(wPath.begin(), wPath.end(), std::back_inserter(buffPath));
   buffPath.push_back(L'\0');
   buffPath.push_back(L'\0');

   SHFILEOPSTRUCTW fileOp;
   fileOp.hwnd = nullptr;
   fileOp.wFunc = FO_DELETE;
   fileOp.pFrom = &(buffPath[0]);
   fileOp.pTo = L"";
   fileOp.fFlags = FOF_ALLOWUNDO |
                   FOF_NOCONFIRMATION |
                   FOF_NOERRORUI |
                   FOF_SILENT;
   fileOp.fAnyOperationsAborted = FALSE;
   fileOp.hNameMappings = nullptr;
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
} // namespace rstudio

