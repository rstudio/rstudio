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

#include <server_core/RVersionsScanner.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace r_environment {
  
namespace {

// R version detected during initialization (either the system
// R version or the provided fallback)
core::r_util::RVersion s_rVersion;

boost::shared_ptr<RVersionsScanner> s_scanner;

} // anonymous namespace

bool initialize(std::string* pErrMsg)
{
   s_scanner.reset(
            new RVersionsScanner(true,
                                 options().rsessionWhichR(),
                                 options().rldpathPath(),
                                 options().rsessionLdLibraryPath()));

   // if we already have a cached version (such as multi version setting it)
   // then simply return success
   if (!s_rVersion.empty())
   {
      return true;
   }

   // otherwise, we have no cached version (no multi version)
   // detect it ourselves
   bool detected = s_scanner->detectSystemRVersion(&s_rVersion, pErrMsg);
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

void setRVersion(const r_util::RVersion& version)
{
   s_rVersion = version;
}

bool detectRVersion(const core::FilePath& rScriptPath,
                    core::r_util::RVersion* pVersion,
                    std::string* pErrMsg)
{
   return s_scanner->detectRVersion(rScriptPath, pVersion, pErrMsg);
}

} // namespace r_environment
} // namespace server
} // namespace rstudio

