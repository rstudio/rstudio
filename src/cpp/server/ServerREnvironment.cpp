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

using namespace core;

namespace server {
namespace r_environment {
  
namespace {

// static R environment vars detected during initialization
r_util::EnvironmentVars s_rEnvironmentVars;

}

bool initialize(std::string* pErrMsg)
{
   // check for which R override
   FilePath rWhichRPath;
   std::string whichROverride = server::options().rsessionWhichR();
   if (!whichROverride.empty())
      rWhichRPath = FilePath(whichROverride);

   // attempt to detect R environment
   return detectREnvironment(rWhichRPath,
                             &s_rEnvironmentVars,
                             pErrMsg);
}

std::vector<std::pair<std::string,std::string> > variables()
{
   // make a copy protected by a mutex just to be on the safest
   // possible side (the copy is cheap and we're not sure what
   // universal guarantees about multi-threaded read access to
   // std::vector are)
   static boost::mutex s_variablesMutex ;
   LOCK_MUTEX(s_variablesMutex)
   {
      return s_rEnvironmentVars;
   }
   END_LOCK_MUTEX

   // mutex related error
   return r_util::EnvironmentVars();
}

bool detectREnvironment(const core::FilePath& rScriptPath,
                        core::r_util::EnvironmentVars* pVars,
                        std::string* pErrMsg)
{
   // determine rLdPaths script location
   FilePath rLdScriptPath(server::options().rldpathPath());
   std::string ldLibraryPath = server::options().rsessionLdLibraryPath();

   std::string rScriptPathOut, rVersion;
   return r_util::detectREnvironment(rScriptPath,
                                     rLdScriptPath,
                                     ldLibraryPath,
                                     &rScriptPathOut,
                                     &rVersion,
                                     pVars,
                                     pErrMsg);
}

} // namespace r_environment
} // namespace server

