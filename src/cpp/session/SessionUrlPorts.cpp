/*
 * SessionUrlPorts.hpp
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

#include <session/SessionUrlPorts.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionOptions.hpp>
#include "session-config.h"

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#ifdef RSTUDIO_SERVER
#include <server_core/UrlPorts.hpp>
#endif

namespace rstudio {
namespace session {
namespace url_ports {
namespace {

// API method for translating local URLs into externally accessible URLs, for use in R packages and
// user code that need direct access to the URL (vs. the implicit transformation we do in some
// places)
SEXP rs_translateLocalUrl(SEXP url, SEXP absolute)
{
   if (options().programMode() == kSessionProgramModeDesktop)
   {
      // Return the URL, unchanged, in desktop mode
      return url;
   }

   // Transform the URL
   auto localUrl = r::sexp::safeAsString(url);
   auto transformed = mapUrlPorts(localUrl);
   if (transformed == localUrl)
   {
      // No transformation was necessary
      return url;
   }

   // The URL was transformed. mapUrlPorts takes an absolute URL and returns a relative URL like
   // "p/08afc455", so make it absolute again if requested by prefixing it with the URL of the
   // connected client.
   if (r::sexp::asLogical(absolute))
   {
      auto prefix = persistentState().activeClientUrl();
      if (!prefix.empty())
      {
         // Ensure trailing slash before we stick the strings, since mapUrlPorts doesn't return one
         if (prefix.back() != '/' && transformed.front() != '/')
            prefix += "/";

         // Prepend to the transformed URL
         transformed = prefix + transformed;
      }
   }

   // Return the transformed URL
   r::sexp::Protect protect;
   return r::sexp::create(transformed, &protect);
}

} // anonymous namespace

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

core::Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_translateLocalUrl);
   return core::Success();
}

}  // namespace url_ports
}  // namespace session
}  // namespace rstudio
