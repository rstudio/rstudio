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

// static R version detected during initialization
core::r_util::RVersion s_rVersion;

// fallback version to use if no R environment is detected
core::r_util::RVersion s_fallbackVersion;

}

bool initialize(std::string* pErrMsg)
{
   // check for which R override
   FilePath rWhichRPath;
   std::string whichROverride = server::options().rsessionWhichR();
   if (!whichROverride.empty())
      rWhichRPath = FilePath(whichROverride);

   // attempt to detect R environment
   std::string rVersion;
   r_util::EnvironmentVars environment;
   bool detected = detectREnvironment(rWhichRPath,
                                      &rVersion,
                                      &environment,
                                      pErrMsg);

   // populate the R version if we successfully detected
   if (detected)
   {
      s_rVersion = r_util::RVersion(rVersion, environment);
   }

   // use fallback if possible
   else if (!detected && hasFallbackVersion())
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

bool detectREnvironment(const core::FilePath& rScriptPath,
                        std::string* pVersion,
                        core::r_util::EnvironmentVars* pVars,
                        std::string* pErrMsg)
{
   // determine rLdPaths script location
   FilePath rLdScriptPath(server::options().rldpathPath());
   std::string ldLibraryPath = server::options().rsessionLdLibraryPath();

   std::string rDetectedScriptPath;
   return r_util::detectREnvironment(rScriptPath,
                                     rLdScriptPath,
                                     ldLibraryPath,
                                     &rDetectedScriptPath,
                                     pVersion,
                                     pVars,
                                     pErrMsg);
}

} // namespace r_environment
} // namespace server
} // namespace rstudio

