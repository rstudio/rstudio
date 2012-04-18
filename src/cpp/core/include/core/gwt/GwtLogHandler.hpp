/*
 * GwtLogHandler.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_GWT_LOG_HANDLER_HPP
#define CORE_GWT_LOG_HANDLER_HPP

#include <string>

namespace core {
   
namespace http {   
   class Request;
   class Response;
}
   
namespace gwt {
   
void handleLogRequest(const std::string& username,
                      const http::Request& request, 
                      http::Response* pResponse);
                           
} // namespace gwt
} // namespace core

#endif // CORE_GWT_LOG_HANDLER_HPP

