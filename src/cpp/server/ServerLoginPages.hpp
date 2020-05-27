/*
 * ServerLoginPages.hpp
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

#ifndef SERVER_LOGIN_PAGES_HPP
#define SERVER_LOGIN_PAGES_HPP

#include <map>
#include <string>

namespace core {
namespace http {
   class Request;
   class Response;
}
}


namespace rstudio {
namespace server {
   
const char * const kErrorParam = "error";
const char * const kErrorDisplay = "errorDisplay";
const char * const kErrorMessage = "errorMessage";

const char * const kLoginPageHtml = "loginPageHtml";

const char * const kAppUri = "appUri";
const char * const kStaySignedIn = "staySignedIn";

enum ErrorType 
{
   kErrorNone,
   kErrorInvalidLogin,
   kErrorServer,
   kErrorUserUnauthorized,
   kErrorUserLicenseLimitReached,
   kErrorUserLicenseSystemUnavailable,
};

void loadLoginPage(const core::http::Request& request,
                   core::http::Response* pResponse,
                   const std::string& templatePath,
                   const std::string& formAction,
                   std::map<std::string,std::string> variables = std::map<std::string,std::string>{});

std::string loginErrorMessage(ErrorType error);

void redirectToLoginPage(const core::http::Request& request,
                         core::http::Response* pResponse,
                         const std::string& appUri,
                         ErrorType error = kErrorNone);

} // namespace server
} // namespace rstudio

#endif
