/*
 * ServerInit.hpp
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

#ifndef SERVER_INIT_HPP
#define SERVER_INIT_HPP

#include <string>

#include <core/http/Header.hpp>

namespace rstudio {
namespace core {
   class Error;
   namespace http {
      class AsyncServer;

   }
}
}

namespace rstudio {
namespace server {

core::http::AsyncServer* httpServerCreate(const core::http::Headers& additionalHeaders);
core::Error httpServerInit(core::http::AsyncServer* pAsyncServer);


} // namespace server
} // namespace rstudio

#endif // SERVER_INIT_HPP

