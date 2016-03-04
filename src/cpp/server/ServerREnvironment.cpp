/*
 * ServerREnvironment.cpp
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

#include "ServerREnvironment.hpp"

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Thread.hpp>
#include <core/r_util/REnvironment.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace r_environment {
  
namespace {

// system R version detected during intialization
core::r_util::RVersion s_systemVersion;

// fallback version to use if no R environment is detected
core::r_util::RVersion s_fallbackVersion;

// R version detected during initialization (either the system
// R version or the provided fallback)
core::r_util::RVersion s_rVersion;


}

bool initialize(std::string* pErrMsg)
{
   // attempt to detect system R version
   bool detected = detectSystemRVersion(&s_rVersion, pErrMsg);

   // if we didn't detect then use fallback if possible
   if (!detected && hasFallbackVersion())
   {
      s_rVersion = s_fallbackVersion;
      detected = true;
   }

   // return status
   return detected;
}

core::r_util::RVersion rVersion()
{
   // make a copy protected by a mutex just to be on the safest
   // possible side (the copy is cheap and we're not sure what
   // universal guarantees about multi-threaded read access to
   // std::vector are)
   static boost::mutex s_versionMutex ;
   LOCK_MUTEX(s_versionMutex)
   {
      return s_rVersion;
   }
   END_LOCK_MUTEX

   // mutex related error
   return r_util::RVersion();
}

bool hasFallbackVersion()
{
   return !s_fallbackVersion.empty();
}

void setFallbackVersion(const core::r_util::RVersion& version)
{
   s_fallbackVersion = version;
}

bool detectSystemRVersion(core::r_util::RVersion* pVersion,
                          std::string* pErrMsg)
{
   // return cached version if we have it
   if (!s_systemVersion.empty())
   {
      *pVersion = s_systemVersion;
      return true;
   }

   // check for which R override
   FilePath rWhichRPath;
   std::string whichROverride = server::options().rsessionWhichR();
   if (!whichROverride.empty())
      rWhichRPath = FilePath(whichROverride);

   // if it's a directory then see if we can find the script
   if (rWhichRPath.isDirectory())
   {
      FilePath rScriptPath = rWhichRPath.childPath("bin/R");
      if (rScriptPath.exists())
         rWhichRPath = rScriptPath;
   }

   // attempt to detect R version
   bool result = detectRVersion(rWhichRPath, pVersion, pErrMsg);

   // if we detected it then cache it
   if (result)
      s_systemVersion = *pVersion;

   // return result
   return result;
}

bool detectRVersion(const core::FilePath& rScriptPath,
                    core::r_util::RVersion* pVersion,
                    std::string* pErrMsg)
{
   // determine rLdPaths script location
   FilePath rLdScriptPath(server::options().rldpathPath());
   std::string ldLibraryPath = server::options().rsessionLdLibraryPath();

   std::string rDetectedScriptPath;
   std::string rVersion;
   core::r_util::EnvironmentVars environment;
   bool result = r_util::detectREnvironment(
                                     rScriptPath,
                                     rLdScriptPath,
                                     ldLibraryPath,
                                     &rDetectedScriptPath,
                                     &rVersion,
                                     &environment,
                                     pErrMsg);
   if (result)
   {
      *pVersion = core::r_util::RVersion(rVersion, environment);
   }

   return result;
}

} // namespace r_environment
} // namespace server
} // namespace rstudio

