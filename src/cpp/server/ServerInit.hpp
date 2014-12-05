/*
 * ServerInit.hpp
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

#ifndef SERVER_INIT_HPP
#define SERVER_INIT_HPP

#include <string>

namespace rscore {
   class Error;
   namespace http {
      class AsyncServer;
   }
}

namespace server {

rscore::http::AsyncServer* httpServerCreate();
rscore::Error httpServerInit(rscore::http::AsyncServer* pAsyncServer);


} // namespace server

#endif // SERVER_INIT_HPP

