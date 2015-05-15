/*
 * SessionPosixHttpConnectionListener.cpp
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

#include <session/SessionHttpConnectionListener.hpp>

#include <core/system/Environment.hpp>
#include <core/system/FileMode.hpp>

#include <core/r_util/RSessionContext.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionLocalStreams.hpp>

#include "SessionTcpIpHttpConnectionListener.hpp"
#include "SessionLocalStreamHttpConnectionListener.hpp"

using namespace rstudio::core ;

namespace rstudio {
namespace session {

namespace {

// pointer to global connection listener singleton
HttpConnectionListener* s_pHttpConnectionListener = NULL;

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
                                           core::system::UserReadWriteMode,
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
         s_pHttpConnectionListener = new TcpIpHttpConnectionListener(
                                            options.wwwAddress(),
                                            options.wwwPort(),
                                            ""); // no shared secret
      }
      else
      {
         // create listener based on options
         r_util::SessionContext context = options.sessionContext();
         std::string streamFile = r_util::sessionContextToStreamFile(context);
         FilePath localStreamPath = local_streams::streamPath(streamFile);
         s_pHttpConnectionListener = new LocalStreamHttpConnectionListener(
                                          localStreamPath,
                                          core::system::EveryoneReadWriteMode,
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
