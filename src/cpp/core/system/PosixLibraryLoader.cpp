/*
 * PosixLibraryLoader.cpp
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

#include <core/system/LibraryLoader.hpp>

#include <dlfcn.h>

#include <core/Error.hpp>

namespace core {
namespace system {

namespace {

void addLastDLErrorMessage(Error* pError)
{
   const char* msg = ::dlerror();
   if (msg != NULL)
      pError->addProperty("dlerror", std::string(msg));
}

} // anonymous namespace

Error loadLibrary(const std::string& libPath, int options, void** ppLib)
{
   *ppLib = NULL;
   *ppLib = ::dlopen(libPath.c_str(), options);
   if (*ppLib == NULL)
   {
      Error error = systemError(
                           boost::system::errc::no_such_file_or_directory,
                           ERROR_LOCATION);
      error.addProperty("lib-path", libPath);
      addLastDLErrorMessage(&error);
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
   *ppSymbol = ::dlsym(pLib, name.c_str());
   if (*ppSymbol == NULL)
   {
      Error error = systemError(boost::system::errc::not_supported,
                                ERROR_LOCATION);
      error.addProperty("symbol", name);
      addLastDLErrorMessage(&error);
      return error;
   }
   else
   {
      return Success();
   }
}

Error closeLibrary(void* pLib)
{
   if (::dlclose(pLib) != 0)
   {
      Error error = systemError(
                           boost::system::errc::no_such_file_or_directory,
                           ERROR_LOCATION);
      addLastDLErrorMessage(&error);
      return error;
   }
   else
   {
      return Success();
   }
}

} // namespace system
} // namespace core
