/*
 * SessionBootInit.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef SESSION_BOOT_INIT_HPP
#define SESSION_BOOT_INIT_HPP

#include <boost/function.hpp>

namespace rstudio {
namespace session {

class HttpConnection;

namespace boot_init {

void handleBootInit(boost::shared_ptr<HttpConnection> ptrConnection);

} // namespace client_init
} // namespace session
} // namespace rstudio

#endif
