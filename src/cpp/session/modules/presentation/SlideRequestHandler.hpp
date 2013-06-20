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
#include <core/http/Response.hpp>

namespace core {
   class Error;
   class FilePath;
   namespace http {
      class Request;
   }
}
 
namespace session {
namespace modules { 
namespace presentation {

struct ErrorResponse
{
   explicit ErrorResponse(const std::string& message = std::string())
      : statusCode(core::http::status::InternalServerError),
        contentType("text/plain"),
        message(message)
   {
   }

   ErrorResponse(const std::string& message,
                 const std::string& htmlMessage,
                 core::http::status::Code statusCode
                                   = core::http::status::InternalServerError)
      : statusCode(statusCode),
        contentType("text/html"),
        message(message),
        htmlMessage(htmlMessage)
   {
   }

   core::http::status::Code statusCode;
   std::string contentType;
   std::string message;
   std::string htmlMessage;
};

void setErrorResponse(const ErrorResponse& errorResponse,
                      core::http::Response* pResponse);


void handlePresentationPaneRequest(const core::http::Request& request,
                                  core::http::Response* pResponse);
                       

void handlePresentationHelpRequest(const core::http::Request& request,
                                   const std::string& jsCallbacks,
                                   core::http::Response* pResponse);

bool savePresentationAsStandalone(const core::FilePath& filePath,
                                  ErrorResponse* pErrorResponse);

bool savePresentationAsRpubsSource(const core::FilePath& filePath,
                                   ErrorResponse* pErrorResponse);

} // namespace presentation
} // namespace modules
} // namesapce session

#endif // SESSION_PRESENTATION_SLIDE_REQUEST_HANDLER_HPP
