/*
 * ServerAppArmor.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

bool isAvailable()
{
   return false;
}

bool isEnforcingRestricted()
{
   return false;
}

Error enforceRestricted()
{
   return systemError(boost::system::errc::not_supported, ERROR_LOCATION);
}

core::Error dropRestricted()
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

// aa_change_hat
typedef int (*PtrAAChangeHat)(const char*, unsigned long);
PtrAAChangeHat s_pChangeHat = NULL;

// magic token used to get out of restriected mode
unsigned long s_magicToken = 0L;

} // anonymous namespace

bool isAvailable()
{
   return FilePath("/etc/apparmor.d/rstudio-server").exists();
}

bool isEnforcingRestricted()
{
   return s_magicToken != 0;
}

Error enforceRestricted()
{
   // verify we aren't already enforcing restricted
   if (isEnforcingRestricted())
   {
      return systemError(boost::system::errc::permission_denied,
                         ERROR_LOCATION);
   }

   // create magic token
   unsigned int magicToken = core::random::uniformRandomInteger<unsigned long>();

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
   s_pChangeHat = (PtrAAChangeHat)::dlsym(pLibAA, "aa_change_hat");
   if (s_pChangeHat == NULL)
   {
      Error error = systemError(boost::system::errc::not_supported,
                                ERROR_LOCATION);
      addLastDLErrorMessage(&error);
      return error;
   }

   // change to restricted
   if (s_pChangeHat("restricted", magicToken) == -1)
      return systemError(errno, ERROR_LOCATION);

   s_magicToken = magicToken;
   return Success();
}

core::Error dropRestricted()
{
   // verify we are currently enforcing restricted
   if (!isEnforcingRestricted())
   {
      return systemError(boost::system::errc::permission_denied,
                         ERROR_LOCATION);
   }

   // drop restricted. note we leave the magic token alone so
   // that if a caller subsequently attempts to call enforceRestricted it
   // will fail
   if (s_pChangeHat(NULL, s_magicToken) == -1)
      return systemError(errno, ERROR_LOCATION);
   else
      return Success();
}


#endif


} // namespace app_aprmor
} // namespace server

