/*
 * Win32LibraryLoader.cpp
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

#include <core/system/LibraryLoader.hpp>

#include <windows.h>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {
  
std::string getLastErrorMessage()
{
   LPVOID lpMsgBuf;
   auto lastErr = ::GetLastError();

   DWORD length = ::FormatMessage(
       FORMAT_MESSAGE_ALLOCATE_BUFFER |
       FORMAT_MESSAGE_FROM_SYSTEM |
       FORMAT_MESSAGE_IGNORE_INSERTS,
       nullptr,
       lastErr,
       MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
       (LPTSTR) &lpMsgBuf,
       0, nullptr );

   if (length != 0)
   {
      std::string msg((LPTSTR)lpMsgBuf);
      LocalFree(lpMsgBuf);
      return msg;
   }
   else
   {
     return "Unknown error";
   }
}
  
} // anonymous namespace

Error loadLibrary(const std::string& libPath, void** ppLib)
{
   // use
   *ppLib = nullptr;
   *ppLib = (void*)::LoadLibraryEx(libPath.c_str(), nullptr, 0);
   if (*ppLib == nullptr)
   {
      Error error = LAST_SYSTEM_ERROR();
      error.addProperty("dlerror", libPath + " - " + getLastErrorMessage());
      return error;
   }
   else
   {
      return Success();
   }
}

Error loadSymbol(void* pLib, const std::string& name, void** ppSymbol)
{
   *ppSymbol = nullptr;
   *ppSymbol = (void*)::GetProcAddress((HINSTANCE)pLib, name.c_str());
   if (*ppSymbol == nullptr)
   {
      Error error = LAST_SYSTEM_ERROR();
      error.addProperty("dlerror", name + " - " + getLastErrorMessage());
      return error;
   }
   else
   {
      return Success();
   }
}

Error closeLibrary(void* pLib)
{
   if (!::FreeLibrary((HMODULE)pLib))
   {
      Error error = LAST_SYSTEM_ERROR();
      error.addProperty("dlerror", getLastErrorMessage());
      return error;
   }
   else 
   {
      return Success();
   }
}

} // namespace system
} // namespace core
} // namespace rstudio
