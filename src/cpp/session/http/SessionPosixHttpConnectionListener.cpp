/*
 * SessionPosixHttpConnectionListener.cpp
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

#include <session/SessionHttpConnectionListener.hpp>

#include <core/system/Environment.hpp>
#include <core/system/PosixSystem.hpp>

#include <core/r_util/RSessionContext.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionLocalStreams.hpp>

#include "SessionTcpIpHttpConnectionListener.hpp"
#include "SessionLocalStreamHttpConnectionListener.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace {

// pointer to global connection listener singleton
HttpConnectionListener* s_pHttpConnectionListener = nullptr;

}  // anonymouys namespace


void initializeHttpConnectionListener()
{
   // alias options
   session::Options& options = session::options();

   if (options.programMode() == kSessionProgramModeDesktop)
   {
      std::string localPeer = core::system::getenv("RS_LOCAL_PEER");
      if (!localPeer.empty())
      {
         FilePath streamPath(localPeer);
         s_pHttpConnectionListener = new LocalStreamHttpConnectionListener(
                                           streamPath,
                                           core::FileMode::USER_READ_WRITE,
                                           options.sharedSecret(),
                                           -1);
      }
      else
      {
         s_pHttpConnectionListener = new TcpIpHttpConnectionListener(
                                            options.wwwAddress(),
                                            options.wwwPort(),
                                            options.sharedSecret());
      }
   }
   else // mode == "server"
   {
      if (session::options().standalone())
      {
         std::string wwwAddress = options.wwwAddress();

         // if we are supposed to bind to the all address but there are no IPv4 addresses,
         // simply bind to all ipv6 interfaces. we prefer non-loopback ipv4 or non-link local ipv6
         if (wwwAddress == "0.0.0.0")
         {
            std::vector<core::system::posix::IpAddress> addrs;
            Error error = core::system::ipAddresses(&addrs, true);
            if (!error)
            {
               bool hasNonLocalIpv4 = false;
               bool hasNonLocalIpv6 = false;
               bool hasIpv4 = false;
               bool hasIpv6 = false;

               for (const core::system::posix::IpAddress& ip : addrs)
               {
                  boost::system::error_code ec;
                  boost::asio::ip::address addr = boost::asio::ip::address::from_string(ip.Address);

                  if (addr.is_v4())
                  {
                     hasIpv4 = true;
                     if (!addr.is_loopback())
                        hasNonLocalIpv4 =  true;
                  }
                  else if (addr.is_v6())
                  {
                     hasIpv6 = true;
                     if (!addr.is_loopback() && ip.Address.find("%") == std::string::npos)
                        hasNonLocalIpv6 = true;
                  }
               }

               if ((!hasNonLocalIpv4 && hasNonLocalIpv6) ||
                   (!hasIpv4 && hasIpv6))
               {
                  wwwAddress = "::";
               }
            }
         }

         s_pHttpConnectionListener = new TcpIpHttpConnectionListener(wwwAddress, options.wwwPort(), "");
      }
      else
      {
         // create listener based on options
         r_util::SessionContext context = options.sessionContext();
         std::string streamFile = r_util::sessionContextFile(context);
         FilePath localStreamPath = local_streams::streamPath(streamFile);
         s_pHttpConnectionListener = new LocalStreamHttpConnectionListener(
                                          localStreamPath,
                                          core::FileMode::ALL_READ_WRITE,
                                          "", // no shared secret
                                          options.limitRpcClientUid());
      }
   }
}

HttpConnectionListener& httpConnectionListener()
{
   return *s_pHttpConnectionListener;
}


} // namespace session
} // namespace rstudio
