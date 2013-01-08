/*
 * ServerAppArmor.cpp
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

#include "ServerAppArmor.hpp"

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/Random.hpp>

#ifndef __APPLE__
#include <dlfcn.h>
#endif

using namespace core;

namespace server {
namespace app_armor {
  
#ifdef __APPLE__

Error enforceRestricted()
{
   return systemError(boost::system::errc::not_supported, ERROR_LOCATION);
}

#else


namespace {

void addLastDLErrorMessage(Error* pError)
{
   const char* msg = ::dlerror();
   if (msg != NULL)
      pError->addProperty("dlerror", std::string(msg));
}

} // anonymous namespace

Error enforceRestricted()
{
   // dynamically load libapparmor
   void* pLibAA = ::dlopen("libapparmor.so.1", RTLD_NOW);
   if (pLibAA == NULL)
   {
      Error error = systemError(boost::system::errc::no_such_file_or_directory,
                                ERROR_LOCATION);
      addLastDLErrorMessage(&error);
      return error;
   }

   // lookup the change hat function
   typedef int (*PtrAAChangeHat)(const char*, unsigned long);
   PtrAAChangeHat pChangeHat = (PtrAAChangeHat)::dlsym(pLibAA,
                                                       "aa_change_hat");
   if (pChangeHat == NULL)
   {
      Error error = systemError(boost::system::errc::not_supported,
                                ERROR_LOCATION);
      addLastDLErrorMessage(&error);
      return error;
   }

   // change to restricted
   if (pChangeHat("restricted", 0) == -1)
   {
      // if this is operation not permitted then simply log a warning
      // (this occurs when the app armor profile is disabled)
      if (errno == EPERM)
      {
         LOG_WARNING_MESSAGE("Unable to change rserver into app armor "
                             "restricted hat (profile may be disabled)");
         return Success();
      }
      else
      {
         return systemError(errno, ERROR_LOCATION);
      }
   }
   else
   {
      return Success();
   }
}

#endif


} // namespace app_aprmor
} // namespace server

