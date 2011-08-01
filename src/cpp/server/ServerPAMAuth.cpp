/*
 * ServerPAMAuth.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
#include "ServerPAMAuth.hpp"

#include <sys/wait.h>
#include <security/pam_appl.h>

#include <core/Error.hpp>
#include <core/system/System.hpp>
#include <core/system/ProcessArgs.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/URL.hpp>
#include <core/http/AsyncUriHandler.hpp>
#include <core/text/TemplateFilter.hpp>

#include <server/util/system/Crypto.hpp>
#include <server/util/system/System.hpp>

#include <server/auth/ServerValidateUser.hpp>
#include <server/auth/ServerSecureUriHandler.hpp>
#include <server/auth/ServerAuthHandler.hpp>
#include <server/auth/ServerSecureCookie.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>

namespace server {
namespace pam_auth {

namespace {

std::map<std::string, pam_handle_t *> pam_handles;

// Handles error logging and EINTR retrying. Only for use with
// functions that return -1 on error and set errno.
int posixcall(boost::function<ssize_t()> func, const ErrorLocation& location)
{
   while (true)
   {
      int result;
      if ((result = func()) == -1)
      {
         if (errno == EINTR)
            continue;
         LOG_ERROR(systemError(errno, location));
         return result;
      }
      return result;
   }
}

int pam_conversation(int num_msg,
                     const struct pam_message **msg,
                     struct pam_response **resp,
                     void *appdata_ptr)
{
  assert(num_msg == 1);
  char *response = strdup((char *)appdata_ptr);
  struct pam_response *pam_resp = (struct pam_response *)malloc(sizeof(struct pam_response));
  pam_resp->resp = response;
  pam_resp->resp_retcode = 0;
  *resp = pam_resp;
  return PAM_SUCCESS;
}

const char *service_name = "rstudio";

bool pamLogin(const std::string& username, const std::string& password)
{
  struct pam_conv *conversation = (struct pam_conv*)malloc(sizeof(struct pam_conv));;
  conversation->conv = pam_conversation;
  conversation->appdata_ptr = (void *)password.c_str();
  pam_handle_t *pamh = NULL;
  int start_code = pam_start(service_name,
                           username.c_str(),
                           conversation,
                           &pamh);
  bool granted = start_code == PAM_SUCCESS &&
                 pam_authenticate(pamh, 0) == PAM_SUCCESS &&
                 pam_setcred(pamh, PAM_ESTABLISH_CRED) == PAM_SUCCESS &&
                 pam_open_session(pamh, 0) == PAM_SUCCESS;
  if(granted)
    pam_handles[username] = pamh;
  return granted;
}


const char * const kUserId = "user-id";

// It's important that URIs be in the root directory, so the cookie
// gets set/unset at the correct scope!
const char * const kDoSignIn = "/auth-do-sign-in";
const char * const kPublicKey = "/auth-public-key";

const char * const kAppUri = "appUri";

const char * const kErrorParam = "error";
const char * const kErrorDisplay = "errorDisplay";
const char * const kErrorMessage = "errorMessage";


std::string applicationURL(const http::Request& request,
                           const std::string& path = std::string())
{
   return http::URL::uncomplete(
         request.uri(),
         path);
}

std::string applicationSignInURL(const http::Request& request,
                                 const std::string& appUri,
                                 const std::string& errorMessage=std::string())
{
   // build fields
   http::Fields fields ;
   if (appUri != "/")
      fields.push_back(std::make_pair(kAppUri, appUri));
   if (!errorMessage.empty())
     fields.push_back(std::make_pair(kErrorParam, errorMessage));

   // build query string
   std::string queryString ;
   if (!fields.empty())
     http::util::buildQueryString(fields, &queryString);

   // generate url
   std::string signInURL = applicationURL(request, auth::handler::kSignIn);
   if (!queryString.empty())
     signInURL += ("?" + queryString);
   return signInURL;
}

std::string getUserIdentifier(const core::http::Request& request)
{
   return auth::secure_cookie::readSecureCookie(request, kUserId);
}

std::string userIdentifierToLocalUsername(const std::string& userIdentifier)
{
   return userIdentifier;
}

bool mainPageFilter(const http::Request& request,
                    http::Response* pResponse)
{
   // check for user identity, if we have one then allow the request to proceed
   std::string userIdentifier = getUserIdentifier(request);
   if (userIdentifier.empty())
   {
      // otherwise redirect to sign-in
      pResponse->setMovedTemporarily(request, applicationSignInURL(request, request.uri()));
      return false;
   }
   else
   {
      return true;
   }
}

void signInThenContinue(const core::http::Request& request,
                        core::http::Response* pResponse)
{
   pResponse->setMovedTemporarily(request, applicationSignInURL(request, request.uri()));
}

void refreshCredentialsThenContinue(
            boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   // no silent refresh possible so delegate to sign-in and continue
   signInThenContinue(pConnection->request(),
                      &(pConnection->response()));

   // write response
   pConnection->writeResponse();
}

void signIn(const http::Request& request,
            http::Response* pResponse)
{
   auth::secure_cookie::remove(request, kUserId, "", pResponse);

   std::map<std::string,std::string> variables;
   variables["action"] = applicationURL(request, kDoSignIn);
   variables["publicKeyUrl"] = applicationURL(request, kPublicKey);

   // setup template variables
   std::string error = request.queryParamValue(kErrorParam);
   variables[kErrorMessage] = error;
   variables[kErrorDisplay] = error.empty() ? "none" : "block";

   variables[kAppUri] = request.queryParamValue(kAppUri);

   // get the path to the JS file
   Options& options = server::options();
   FilePath wwwPath(options.wwwLocalPath());
   FilePath signInPath = wwwPath.complete("templates/encrypted-sign-in.htm");

   text::TemplateFilter filter(variables);

   pResponse->setFile(signInPath, request, filter);
   pResponse->setContentType("text/html");
}

void publicKey(const http::Request&,
               http::Response* pResponse)
{
   std::string exp, mod;
   util::system::crypto::rsaPublicKey(&exp, &mod);
   pResponse->setNoCacheHeaders();
   pResponse->setBody(exp + ":" + mod);
   pResponse->setContentType("text/plain");
}

void doSignIn(const http::Request& request,
              http::Response* pResponse)
{
   std::string appUri = request.formFieldValue(kAppUri);
   if (appUri.empty())
      appUri = "/";

   std::string encryptedValue = request.formFieldValue("v");
   bool persist = request.formFieldValue("persist") == "1";
   std::string plainText;
   Error error = util::system::crypto::rsaPrivateDecrypt(encryptedValue,
                                                         &plainText);
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setMovedTemporarily(
            request,
            applicationSignInURL(request,
                                 appUri,
                                 "Temporary server error,"
                                 " please try again"));
      return;
   }

   size_t splitAt = plainText.find('\n');
   if (splitAt < 0)
   {
      LOG_ERROR_MESSAGE("Didn't find newline in plaintext");
      pResponse->setMovedTemporarily(
            request,
            applicationSignInURL(request,
                                 appUri,
                                 "Temporary server error,"
                                 " please try again"));
      return;
   }

   std::string username = plainText.substr(0, splitAt);
   std::string password = plainText.substr(splitAt + 1, plainText.size());

   if ( pamLogin(username, password) &&
        server::auth::validateUser(username))
   {
      if (appUri.size() > 0 && appUri[0] != '/')
         appUri = "/" + appUri;

      boost::optional<boost::gregorian::days> expiry;
      if (persist)
         expiry = boost::gregorian::days(3652);
      else
         expiry = boost::none;

      auth::secure_cookie::set(kUserId,
                               username,
                               request,
                               boost::posix_time::time_duration(24*3652,
                                                                0,
                                                                0,
                                                                0),
                               expiry,
                               std::string(),
                               pResponse);
      pResponse->setMovedTemporarily(request, appUri);
   }
   else
   {
      pResponse->setMovedTemporarily(
            request,
            applicationSignInURL(request,
                                 appUri,
                                 "Incorrect or invalid username/password"));
   }
}

void signOut(const std::string&,
             const http::Request& request,
             http::Response* pResponse)
{
   std::string username = auth::secure_cookie::readSecureCookie(request, kUserId);
   if(!username.empty()) {
      pam_handle_t *pamh = pam_handles[username];
      if(pamh) {
         pam_close_session(pam_handles[username], 0);
         pam_end(pam_handles[username], PAM_SUCCESS);
         pam_handles.erase(username);
      }
   }
   auth::secure_cookie::remove(request, kUserId, "", pResponse);
   pResponse->setMovedTemporarily(request, auth::handler::kSignIn);
}

} // anonymous namespace

Error initialize()
{
   // register ourselves as the auth handler
   server::auth::handler::Handler pamHandler;
   pamHandler.getUserIdentifier = getUserIdentifier;
   pamHandler.userIdentifierToLocalUsername = userIdentifierToLocalUsername;
   pamHandler.mainPageFilter = mainPageFilter;
   pamHandler.signInThenContinue = signInThenContinue;
   pamHandler.refreshCredentialsThenContinue = refreshCredentialsThenContinue;
   pamHandler.signIn = signIn;
   pamHandler.signOut = signOut;
   auth::handler::registerHandler(pamHandler);

   // add pam-specific auth handlers
   uri_handlers::addBlocking(kDoSignIn, doSignIn);
   uri_handlers::addBlocking(kPublicKey, publicKey);

   // initialize crypto
   return util::system::crypto::rsaInit();
}


} // namespace pam_auth
} // namespace server
