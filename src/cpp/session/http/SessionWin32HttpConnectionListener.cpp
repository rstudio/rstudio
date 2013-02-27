/*
 * SessionWin32HttpConnectionListener.cpp
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

#include <winsock2.h>

#include <session/SessionHttpConnectionListener.hpp>

#include <boost/scoped_ptr.hpp>

#include "SessionTcpIpHttpConnectionListener.hpp"

using namespace core ;

namespace session {

namespace {

// pointer to global connection listener singleton
HttpConnectionListener* s_pHttpConnectionListener = NULL ;

}  // anonymouys namespace


void initializeHttpConnectionListener()
{
   session::Options& options = session::options();
   s_pHttpConnectionListener =
         new TcpIpHttpConnectionListener("127.0.0.1",
                                         options.wwwPort(),
                                         options.sharedSecret());
}

HttpConnectionListener& httpConnectionListener()
{
   return *s_pHttpConnectionListener;
}


} // namespace session
