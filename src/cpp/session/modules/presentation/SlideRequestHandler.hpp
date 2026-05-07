/*
 * SlideRequestHandler.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
   namespace http {
      class Request;
   }
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

struct ErrorResponse
{
   explicit ErrorResponse(const std::string& message = std::string(),
                          core::http::status::Code statusCode
                                  = core::http::status::InternalServerError)
      : message(message), statusCode(statusCode)
   {
   }

   std::string message;
   core::http::status::Code statusCode;
};

bool clearKnitrCache(ErrorResponse* pErrorResponse);

void handlePresentationPaneRequest(const core::http::Request& request,
                                  core::http::Response* pResponse);
                       

void handlePresentationHelpRequest(const core::http::Request& request,
                                   const std::string& jsCallbacks,
                                   core::http::Response* pResponse);

// Returns true if `filePath` canonically resolves to a location within
// `dirPath`. Both paths must exist on disk; returns false if either is
// missing. Canonicalization defeats symlink and ".." escapes.
bool isPathWithin(const core::FilePath& filePath,
                  const core::FilePath& dirPath);

// Returns true if `fetchSite` (a Sec-Fetch-Site header value) does not
// indicate a cross-site request. Empty values (older browsers without
// Fetch Metadata) are treated as allowed; in that case the path
// constraint is the security boundary.
bool isPresentationHelpFetchSiteAllowed(const std::string& fetchSite);

bool savePresentationAsStandalone(const core::FilePath& filePath,
                                  ErrorResponse* pErrorResponse);

bool savePresentationAsRpubsSource(const core::FilePath& filePath,
                                   ErrorResponse* pErrorResponse);

} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PRESENTATION_SLIDE_REQUEST_HANDLER_HPP
