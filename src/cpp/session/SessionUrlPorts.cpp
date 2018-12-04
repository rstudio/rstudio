/*
 * SessionUrlPorts.hpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <session/SessionUrlPorts.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionOptions.hpp>
#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <server_core/UrlPorts.hpp>
#endif

namespace rstudio {
namespace session {
namespace url_ports {

// given a url, return a portmap path if applicable (i.e. we're in server
// mode and the path needs port mapping), and the unmodified url otherwise
std::string mapUrlPorts(const std::string& url)
{
#ifdef RSTUDIO_SERVER
   if (options().programMode() == kSessionProgramModeServer)
   {
      // see if we can form a portmap path for this url
      std::string path;
      if (server_core::portmapPathForLocalhostUrl(url, persistentState().portToken(), &path))
         return path;
   }
#endif
   return url;
}

}  // namespace url_ports
}  // namespace session
}  // namespace rstudio
