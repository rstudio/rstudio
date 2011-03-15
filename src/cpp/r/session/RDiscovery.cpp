/*
 * RDiscovery.cpp
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

#include <r/session/RDiscovery.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include <r/RErrorCategory.hpp>

#ifdef _WIN32
#define Win32
#endif
#include <Rembedded.h>

#include "config.h"

using namespace core;

namespace r {
namespace session {

// probe registry for windows
#ifdef _WIN32

Error discoverR(RLocations* pLocations)
{
   // query R for the home path
   const char* lpszRHome = ::get_R_HOME();
   if (lpszRHome == NULL)
      return Error(errc::RHomeNotFound, ERROR_LOCATION);

   // set paths
   FilePath rHome(lpszRHome);
   pLocations->homePath = rHome.absolutePath();
   pLocations->docPath = rHome.complete("doc").absolutePath();

   return Success();
}

// Use configured / hard-coded paths for unix & osx
#else

Error discoverR(RLocations* pLocations)
{
   // use environment variables and default back to configured path
   // if they don't exists

   std::string rHome = core::system::getenv("R_HOME");
   if (!rHome.empty())
      pLocations->homePath = rHome;
   else
      pLocations->homePath = CONFIG_R_HOME_PATH;

   std::string rDocDir = core::system::getenv("R_DOC_DIR");
   if (!rDocDir.empty())
      pLocations->docPath = rDocDir;
   else
      pLocations->docPath = CONFIG_R_DOC_PATH;

   return Success();
}

#endif

} // namespace session
} // namespace r






