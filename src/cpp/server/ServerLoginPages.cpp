/*
 * ServerLoginPages.cpp
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

#include "ServerLoginPages.hpp"

#include <boost/format.hpp>

#include <shared_core/SafeConvert.hpp>
#include <shared_core/FilePath.hpp>

#include <core/http/URL.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/text/TemplateFilter.hpp>

#include <server/ServerOptions.hpp>
#include <server/auth/ServerAuthHandler.hpp>

const char * const kStaySignedInDisplay = "staySignedInDisplay";
const char * const kAuthTimeoutMinutes = "authTimeoutMinutes";
const char * const kAuthTimeoutMinutesDisplay = "authTimeoutMinutesDisplay";
const char * const kIsRStudioProDesktop = "isRdp";
const char * const kLogoHtml = "logoHtml";

namespace rstudio {
namespace server {

std::string loginErrorMessage(ErrorType error)
{
   switch (error)
   {
      case kErrorNone:
         return "";
      case kErrorInvalidLogin: 
         return "Incorrect or invalid username/password";
      case kErrorServer:
         return "Temporary server error, please try again";
      case kErrorUserUnauthorized:
         return "Unauthorized user.";
      case kErrorUserLicenseLimitReached:
         return "The user limit for this license has been reached, or you are not allowed access.";
      case kErrorUserLicenseSystemUnavailable:
         return "The user licensing system is temporarily unavailable. Please try again later.";
   }
   return "";
}

void fillLoginFields(const core::http::Request& request,
                     const std::string& formAction,
                     std::map<std::string, std::string>& variables)
{
   variables["action"] = core::http::URL::uncomplete(request.uri(), formAction);

   // fill stay signed in
   variables[kStaySignedInDisplay] = auth::handler::overlay::canStaySignedIn() ? "block" : "none";
   int timeoutMinutes = server::options().authTimeoutMinutes();
   variables[kAuthTimeoutMinutesDisplay] = timeoutMinutes > 0 ? "block" : "none";
   variables[kAuthTimeoutMinutes] = core::safe_convert::numberToString(timeoutMinutes);

   // fill error
   std::string error = request.queryParamValue(kErrorParam);
   variables[kErrorMessage] = loginErrorMessage(static_cast<ErrorType>(
            core::safe_convert::stringTo<unsigned>(error, kErrorNone)));
   variables[kErrorDisplay] = error.empty() ? "none" : "block";

   // get the application uri the user was on the way to (default to
   // root location if it isn't specified)
   variables[kAppUri] = request.queryParamValue(kAppUri);

   // include custom login page html
   bool isRdp = request.queryParamValue(kIsRStudioProDesktop) == "1";
   boost::format logoImgHtmlFormat(R"DELIM(<img src="images/rstudio.png" width="78" height="27" alt="%1%"/>)DELIM");
   if (!isRdp)
   {
      variables[kLoginPageHtml] = server::options().authLoginPageHtml();

      // render logo with links
      std::string logoImgHtml = boost::str(logoImgHtmlFormat % "RStudio Logo (goes to external site)");
      variables[kLogoHtml] = R"DELIM(<a href="https://www.rstudio.com/">)DELIM" + logoImgHtml + "</a>";
   }
   else
   {
      variables[kLoginPageHtml] = server::options().authRdpLoginPageHtml();

      // render logo without links - user should not be able
      // to freely navigate in RDP
      std::string logoImgHtml = boost::str(logoImgHtmlFormat % "RStudio Logo");
      variables[kLogoHtml] = logoImgHtml;
   }
}

void loadLoginPage(const core::http::Request& request,
                   core::http::Response* pResponse,
                   const std::string& templatePath,
                   const std::string& formAction,
                   std::map<std::string,std::string> variables)
{
   // setup template variables
   fillLoginFields(request, formAction, variables);

   // get the path to the template file
   Options& options = server::options();
   core::FilePath wwwPath(options.wwwLocalPath());
   core::FilePath signInFilePath = wwwPath.completePath(templatePath);
   core::text::TemplateFilter templateFilter(variables);

   // don't allow sign-in page to be framed by other domains (clickjacking
   // defense)
   pResponse->setFrameOptionHeaders(options.wwwFrameOrigin());

   // return login (processing template)
   pResponse->setNoCacheHeaders();
   pResponse->setFile(signInFilePath, request, templateFilter);
   pResponse->setContentType("text/html");
}

std::string generateLoginPath(const core::http::Request& request,
                              const std::string& appUri,
                              ErrorType error /*= kErrorNone*/)
{
   // build fields
   core::http::Fields fields;
   fields.push_back(std::make_pair(kAppUri, appUri));
   if (error != kErrorNone)
     fields.push_back(std::make_pair(kErrorParam, core::safe_convert::numberToString(error)));

   // build query string
   std::string queryString;
   if (!fields.empty())
     core::http::util::buildQueryString(fields, &queryString);

   // generate url
   std::string signInPath = core::http::URL::uncomplete(request.baseUri(), auth::handler::kSignIn);
   if (!queryString.empty())
     signInPath += ("?" + queryString);
   return signInPath;
}

void redirectToLoginPage(const core::http::Request& request,
                         core::http::Response* pResponse,
                         const std::string& appUri,
                         ErrorType error /*= kErrorNone*/)
{
   pResponse->setMovedTemporarily(
                         request,
                         generateLoginPath(request, appUri, error));
}

} // namespace server
} // namespace rstudio
