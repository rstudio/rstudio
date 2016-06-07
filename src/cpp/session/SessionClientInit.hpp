/*
 * SessionClientInit.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#ifndef SESSION_CLIENT_INIT_HPP
#define SESSION_CLIENT_INIT_HPP

#include <boost/function.hpp>

namespace rstudio {
namespace session {

class HttpConnection;

namespace client_init {

void handleClientInit(const boost::function<void()>& initFunction,
                      boost::shared_ptr<HttpConnection> ptrConnection);

} // namespace client_init
} // namespace session
} // namespace rstudio

#endif
