/*
 * SessionWin32HttpConnectionListener.cpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// Necessary to avoid compile error on Win x64
#include <winsock2.h>

#include <session/SessionHttpConnectionListener.hpp>

#include <boost/scoped_ptr.hpp>

#include <core/system/System.hpp>

#include "SessionTcpIpHttpConnectionListener.hpp"

#ifdef DEBUG
# undef DEBUG
#endif

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace {

// pointer to global connection listener singleton
HttpConnectionListener* s_pHttpConnectionListener = nullptr;

}  // anonymous namespace


void initializeHttpConnectionListener()
{
   session::Options& options = session::options();

   if (log::isLogLevel(log::LogLevel::DEBUG))
   {
      const char* fmt =
            "Initializing HTTP connection listener [address=%s; port=%s; secret=%s]";

      std::string sharedSecret;
#ifdef RSTUDIO_PACKAGE_BUILD
      sharedSecret = "<redacted>";
#else
      sharedSecret = options.sharedSecret();
#endif

      std::string msg = core::string_utils::sprintf(
               fmt,
               options.wwwAddress().c_str(),
               options.wwwPort().c_str(),
               sharedSecret.c_str());

      LOG_DEBUG_MESSAGE(msg);
   }

   s_pHttpConnectionListener = new TcpIpHttpConnectionListener(
                                      options.wwwAddress(),
                                      options.wwwPort(),
                                      options.sharedSecret());

}

HttpConnectionListener& httpConnectionListener()
{
   return *s_pHttpConnectionListener;
}


} // namespace session
} // namespace rstudio
