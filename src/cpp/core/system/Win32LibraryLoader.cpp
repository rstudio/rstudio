/*
 * Win32LibraryLoader.cpp
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

#include <core/system/LibraryLoader.hpp>

#include <windows.h>

#include <core/Error.hpp>

namespace core {
namespace system {

Error loadLibrary(const std::string& libPath, int options, void** ppLib)
{
   // use
   *ppLib = NULL;
   *ppLib = (void*)::LoadLibraryEx(libPath.c_str(), NULL, options);
   if (*ppLib == NULL)
   {
      Error error = systemError(::GetLastError(), ERROR_LOCATION);
      error.addProperty("lib-path", libPath);
      return error;
   }
   else
   {
      return Success();
   }
}

Error loadSymbol(void* pLib, const std::string& name, void** ppSymbol)
{
   *ppSymbol = NULL;
   *ppSymbol = (void*)::GetProcAddress((HINSTANCE)pLib, name.c_str());
   if (*ppSymbol == NULL)
   {
      Error error = systemError(::GetLastError(), ERROR_LOCATION);
      error.addProperty("symbol", name);
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
      return systemError(::GetLastError(), ERROR_LOCATION);
   else
      return Success();
}

} // namespace system
} // namespace core
