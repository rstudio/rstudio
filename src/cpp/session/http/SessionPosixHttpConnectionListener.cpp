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

#include <session/SessionConstants.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionLocalStreams.hpp>

#include "SessionTcpIpHttpConnectionListener.hpp"
#include "SessionLocalStreamHttpConnectionListener.hpp"

using namespace core ;

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
      s_pHttpConnectionListener =
            new TcpIpHttpConnectionListener("127.0.0.1",
                                            options.wwwPort(),
                                            options.sharedSecret());
   }
   else // mode == "server"
   {
      // create listener based on options
      std::string userIdentity = options.userIdentity();
      FilePath localStreamPath = local_streams::streamPath(userIdentity);
      s_pHttpConnectionListener = new LocalStreamHttpConnectionListener(
                                              localStreamPath,
                                              options.limitRpcClientUid());
   }
}

HttpConnectionListener& httpConnectionListener()
{
   return *s_pHttpConnectionListener;
}


} // namespace session
