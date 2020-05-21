/*
 * RDiscovery.cpp
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

#include <r/session/RDiscovery.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <r/RErrorCategory.hpp>

#ifdef _WIN32
#define Win32
#endif
#include <Rembedded.h>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

// probe registry for windows
#ifdef _WIN32

Error discoverR(RLocations* pLocations)
{
   // query R for the home path
   const char* lpszRHome = ::get_R_HOME();
   if (lpszRHome == nullptr)
      return Error(errc::RHomeNotFound, ERROR_LOCATION);

   // set paths
   FilePath rHome(lpszRHome);
   pLocations->homePath = rHome.getAbsolutePath();
   pLocations->docPath = rHome.completePath("doc").getAbsolutePath();

   return Success();
}

// Use environment variables for unix & osx
#else

Error discoverR(RLocations* pLocations)
{
   // rhome
   std::string rHome = core::system::getenv("R_HOME");
   if (rHome.empty() || !FilePath(rHome).exists())
      return core::pathNotFoundError(rHome, ERROR_LOCATION);
   else
      pLocations->homePath = rHome;

   // rdocdir
   std::string rDocDir = core::system::getenv("R_DOC_DIR");
   if (rDocDir.empty() || !FilePath(rDocDir).exists())
      return core::pathNotFoundError(rDocDir, ERROR_LOCATION);
   else
      pLocations->docPath = rDocDir;

   return Success();
}

#endif

} // namespace session
} // namespace r
} // namespace rstudio






