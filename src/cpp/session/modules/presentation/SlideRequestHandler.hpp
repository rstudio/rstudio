/*
 * SlideRequestHandler.hpp
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

#ifndef SESSION_PRESENTATION_SLIDE_REQUEST_HANDLER_HPP
#define SESSION_PRESENTATION_SLIDE_REQUEST_HANDLER_HPP

#include <string>

namespace core {
   namespace http {
      class Request;
      class Response;
   }
}
 
namespace session {
namespace modules { 
namespace presentation {
   
void handlePresentationPaneRequest(const core::http::Request& request,
                                  core::http::Response* pResponse);
                       

void handlePresentationHelpRequest(const core::http::Request& request,
                                   const std::string& jsCallbacks,
                                   core::http::Response* pResponse);

} // namespace presentation
} // namespace modules
} // namesapce session

#endif // SESSION_PRESENTATION_SLIDE_REQUEST_HANDLER_HPP
