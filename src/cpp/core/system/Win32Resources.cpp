/*
 * Win32Resources.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <windows.h>
#include <psapi.h>

#include <shared_core/Error.hpp>

#include <core/system/Resources.hpp>

namespace rstudio {
namespace core {
namespace system {

Error getTotalMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
{
   MEMORYSTATUSEX status;
   status.dwLength = sizeof(status);
   if (::GlobalMemoryStatusEx(&status) == 0)
   {
      return systemError(::GetLastError(), ERROR_LOCATION);
   }

   *pUsedKb = static_cast<long>((status.ullTotalPhys - status.ullAvailPhys) / 1024);
   *pProvider = MemoryProviderWindows;
   return Success();
}

Error getProcessMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
{
   PROCESS_MEMORY_COUNTERS info;
   if (::GetProcessMemoryInfo(GetCurrentProcess(), &info, sizeof(info)) == 0)
   {
      return systemError(::GetLastError(), ERROR_LOCATION);
   }
   *pUsedKb = static_cast<long>(info.WorkingSetSize / 1024);
   *pProvider = MemoryProviderWindows;
   return Success();
}

Error getTotalMemory(long *pTotalKb, MemoryProvider *pProvider)
{
   MEMORYSTATUSEX status;
   status.dwLength = sizeof(status);
   if (::GlobalMemoryStatusEx(&status) == 0)
   {
      return systemError(::GetLastError(), ERROR_LOCATION);
   }

   *pTotalKb = static_cast<long>(status.ullTotalPhys / 1024);
   *pProvider = MemoryProviderWindows;
   return Success();
}

} // namespace system
} // namespace core
} // namespace rstudio

