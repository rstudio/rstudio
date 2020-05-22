/*
 * ServerREnvironment.cpp
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

#include "ServerREnvironment.hpp"

#include <shared_core/Error.hpp>
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

boost::mutex s_versionMutex;

// R version detected during initialization (either the system
// R version or the provided fallback)
core::r_util::RVersion s_rVersion;

boost::shared_ptr<RVersionsScanner> s_scanner;

} // anonymous namespace

void initializeScanner()
{
   s_scanner.reset(
            new RVersionsScanner(true,
                                 options().rsessionWhichR(),
                                 options().rldpathPath(),
                                 options().rsessionLdLibraryPath()));
}

bool initialize(std::string* pErrMsg)
{
   if (!s_scanner)
      initializeScanner();

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
   LOCK_MUTEX(s_versionMutex)
   {
      s_rVersion = version;
   }
   END_LOCK_MUTEX
}

bool detectRVersion(const core::FilePath& rScriptPath,
                    core::r_util::RVersion* pVersion,
                    std::string* pErrMsg)
{
   if (!s_scanner)
      initializeScanner();

   return s_scanner->detectRVersion(rScriptPath, pVersion, pErrMsg);
}

} // namespace r_environment
} // namespace server
} // namespace rstudio

