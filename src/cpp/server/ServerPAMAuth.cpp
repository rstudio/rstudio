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

#include "config.h"

namespace server {
namespace pam_auth {

namespace {


// TODO: verify that we never leak a file descriptor

// TODO: make sure inputs into pam helper are bounded
// TODO: block tty case in pam helper

// TODO: more restrictive startup profile

// TODO: make sure it works on redhat



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


bool pamLogin(const std::string& username, const std::string& password)
{
   // get path to pam helper
   FilePath pamHelperPath(server::options().authPamHelperPath());
   if (!pamHelperPath.exists())
   {
      LOG_ERROR_MESSAGE("PAM helper binary does not exist at " +
                        pamHelperPath.absolutePath());
      return false;
   }

   // create pipes
   const int READ = 0;
   const int WRITE = 1;
   int fdInput[2];
   if (posixcall(boost::bind(::pipe, fdInput), ERROR_LOCATION) == -1)
      return false;
   int fdOutput[2];
   if (posixcall(boost::bind(::pipe, fdOutput), ERROR_LOCATION) == -1)
      return false;

   // fork
   pid_t pid = posixcall(::fork, ERROR_LOCATION);
   if (pid == -1)
      return false;

   // child
   else if (pid == 0)
   {
      // NOTE: within the child we want to make sure in all cases that
      // we call ::execv to execute the pam helper. as a result if any
      // errors occur while we are setting up for the ::execv we log
      // and continue rather than calling ::exit (we do this to avoid
      // strange error conditions related to global c++ objects being
      // torn down in a non-standard sequence).

 #ifdef HAVE_PAM_REQUIRES_RESTORE_PRIV
      // RedHat 5 returns PAM_SYSTEM_ERR from pam_authenticate if we're
      // running with geteuid != getuid (as is the case when we temporarily
      // drop privileges). So restore privilliges in the child
      if (util::system::realUserIsRoot())
      {
         Error error = util::system::restorePriv();
         if (error)
         {
            LOG_ERROR(error);
            // intentionally fail forward (see note above)
         }
      }
#endif

      // close unused pipes
      posixcall(boost::bind(::close, fdInput[WRITE]), ERROR_LOCATION);
      posixcall(boost::bind(::close, fdOutput[READ]), ERROR_LOCATION);

      // clear the child signal mask
      Error error = core::system::clearSignalMask();
      if (error)
      {
         LOG_ERROR(error);
         // intentionally fail forward (see note above)
      }

      // wire standard streams
      posixcall(boost::bind(::dup2, fdInput[READ], STDIN_FILENO),
                ERROR_LOCATION);
      posixcall(boost::bind(::dup2, fdOutput[WRITE], STDOUT_FILENO),
                ERROR_LOCATION);

      // close all open file descriptors other than std streams
      error = core::system::closeNonStdFileDescriptors();
      if (error)
      {
         LOG_ERROR(error);
         // intentionally fail forward (see note above)
      }

      // build username args (on heap so they stay around after exec)
      // and execute pam helper
      using core::system::ProcessArgs;
      std::vector<std::string> args;
      args.push_back(pamHelperPath.absolutePath());
      args.push_back(username);
      ProcessArgs* pProcessArgs = new ProcessArgs(args);
      ::execv(pamHelperPath.absolutePath().c_str(), pProcessArgs->args()) ;

      // in the normal case control should never return from execv (it starts
      // anew at main of the process pointed to by path). therefore, if we get
      // here then there was an error
      LOG_ERROR(systemError(errno, ERROR_LOCATION)) ;
      ::exit(EXIT_FAILURE) ;
   }

   // parent
   else
   {
      // close unused pipes
      posixcall(boost::bind(::close, fdInput[READ]), ERROR_LOCATION);
      posixcall(boost::bind(::close, fdOutput[WRITE]), ERROR_LOCATION);

      // write the password to standard input then close the pipe
      std::string input = password ;
      std::size_t written = posixcall(boost::bind(::write,
                                                   fdInput[WRITE],
                                                   input.c_str(),
                                                   input.length()),
                                      ERROR_LOCATION);
      posixcall(boost::bind(::close, fdInput[WRITE]), ERROR_LOCATION);

      // check for correct bytes written
      if (written != static_cast<std::size_t>(input.length()))
      {
         // first close stdout pipe
         posixcall(boost::bind(::close, fdOutput[READ]), ERROR_LOCATION);

         // log error and return false
         LOG_ERROR_MESSAGE("Error writing to pam helper stdin");
         return false;
      }

      // read the response from standard output
      char buf;
      size_t bytesRead = posixcall(boost::bind(::read, fdOutput[READ], &buf, 1),
                                   ERROR_LOCATION);
      posixcall(boost::bind(::close, fdOutput[READ]), ERROR_LOCATION);

      // return true if bytes were read
      return bytesRead > 0;
   }

   return false;
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
