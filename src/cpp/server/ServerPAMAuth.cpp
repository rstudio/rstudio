/*
 * ServerPAMAuth.cpp
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
#include "ServerPAMAuth.hpp"
#include "ServerPAMAuthOverlay.hpp"

#include <core/Thread.hpp>
#include <core/system/Process.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/PosixUser.hpp>

#include <core/http/URL.hpp>

#include <shared_core/Error.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>
#include <server/ServerSessionProxy.hpp>

#include <server/auth/ServerAuthHandler.hpp>
#include <server/auth/ServerAuthCommon.hpp>

#include "ServerLoginPages.hpp"

namespace rstudio {
namespace server {
namespace pam_auth {

using namespace rstudio::core;

namespace {

void assumeRootPriv()
{
    // RedHat 5 returns PAM_SYSTEM_ERR from pam_authenticate if we're
    // running with geteuid != getuid (as is the case when we temporarily
    // drop privileges). We've also seen kerberos on Ubuntu require
    // priv to work correctly -- so, restore privilliges in the child
    if (core::system::realUserIsRoot())
    {
       Error error = core::system::restorePriv();
       if (error)
       {
          LOG_ERROR(error);
          // intentionally fail forward (see note above)
       }
    }
}

// It's important that URIs be in the root directory, so the cookie
// gets set/unset at the correct scope!
const char * const kDoSignIn = "/auth-do-sign-in";
const char * const kPublicKey = "/auth-public-key";

const char * const kFormAction = "formAction";

std::string getUserIdentifier(const core::http::Request& request)
{
   return auth::common::getUserIdentifier(request);
}

std::string userIdentifierToLocalUsername(const std::string& userIdentifier)
{
   static core::thread::ThreadsafeMap<std::string, std::string> cache;
   std::string username = userIdentifier;

   if (cache.contains(userIdentifier)) 
   {
      username = cache.get(userIdentifier);
   }
   else
   {
      // The username returned from this function is eventually used to create
      // a local stream path, so it's important that it agree with the system
      // view of the username (as that's what the session uses to form the
      // stream path), which is why we do a username => username transform
      // here. See case 5413 for details.
      core::system::User user;
      Error error = core::system::User::getUserFromIdentifier(userIdentifier, user);
      if (error)
      {
         // log the error and return the PAM user identifier as a fallback
         LOG_ERROR(error);
      }
      else
      {
         username = user.getUsername();
      }

      // cache the username -- we do this even if the lookup fails since
      // otherwise we're likely to keep hitting (and logging) the error on
      // every request
      cache.set(userIdentifier, username);
   }

   return username;
}

void signIn(const http::Request& request,
            http::Response* pResponse)
{
   if (server::options().authNone())
   {
      auth::handler::setSignInCookies(request, core::system::username(), false, pResponse);
      pResponse->setMovedTemporarily(request, "./");
      return;
   }

   std::map<std::string,std::string> variables;
   variables["publicKeyUrl"] = http::URL::uncomplete(request.uri(), kPublicKey);
   if (server::options().authEncryptPassword())
      variables[kFormAction] = "action=\"javascript:void\" "
                               "onsubmit=\"submitRealForm();return false\"";
   else
      variables[kFormAction] = "action=\"" + core::http::URL::uncomplete(request.uri(), kDoSignIn) + "\" "
                               "onsubmit=\"return verifyMe()\"";
   const std::string& templatePath = "templates/encrypted-sign-in.htm";
   auth::common::signIn(request, pResponse, templatePath, kDoSignIn, variables);
}

void publicKey(const http::Request&,
               http::Response* pResponse)
{
   std::string exp, mod;
   core::system::crypto::rsaPublicKey(&exp, &mod);
   pResponse->setNoCacheHeaders();
   pResponse->setBody(exp + ":" + mod);
   pResponse->setContentType("text/plain");
}

void doSignIn(const http::Request& request,
              http::Response* pResponse)
{
   std::string appUri = request.formFieldValue(kAppUri);
   if (!auth::common::validateSignIn(request, pResponse))
   {
      redirectToLoginPage(request, pResponse, kAppUri, kErrorServer);
      return;
   }

   bool persist = false;
   std::string username, password;

   if (server::options().authEncryptPassword())
   {
      std::string encryptedValue = request.formFieldValue("v");
      std::string plainText;
      Error error = core::system::crypto::rsaPrivateDecrypt(encryptedValue,
                                                            &plainText);
      if (error)
      {
         LOG_ERROR(error);
         redirectToLoginPage(request, pResponse, appUri, kErrorServer);
         return;
      }

      size_t splitAt = plainText.find('\n');
      if (splitAt == std::string::npos)
      {
         LOG_ERROR_MESSAGE("Didn't find newline in plaintext");
         redirectToLoginPage(request, pResponse, appUri, kErrorServer);
         return;
      }

      persist = request.formFieldValue("persist") == "1";
      username = plainText.substr(0, splitAt);
      password = plainText.substr(splitAt + 1, plainText.size());
   }
   else
   {
      persist = request.formFieldValue("staySignedIn") == "1";
      username = request.formFieldValue("username");
      password = request.formFieldValue("password");
   }

   // tranform to local username
   username = auth::handler::userIdentifierToLocalUsername(username);

   overlay::onUserPasswordUnavailable(username);

   bool authenticated = pamLogin(username, password);
   if (!auth::common::doSignIn(request,
                               pResponse,
                               username,
                               appUri,
                               persist,
                               authenticated))
   {
      return;
   }
   overlay::onUserPasswordAvailable(username, password);
}

void signOut(const http::Request& request,
             http::Response* pResponse)
{
   std::string username = auth::common::signOut(request, pResponse, getUserIdentifier, auth::handler::kSignIn);
   if (!username.empty())
   {
      overlay::onUserPasswordUnavailable(username, true);
   }
}

} // anonymous namespace


bool pamLogin(const std::string& username, const std::string& password)
{
   // get path to pam helper
   FilePath pamHelperPath(server::options().authPamHelperPath());
   if (!pamHelperPath.exists())
   {
      LOG_ERROR_MESSAGE("PAM helper binary does not exist at " +
                           pamHelperPath.getAbsolutePath());
      return false;
   }

   // form args
   std::vector<std::string> args;
   args.push_back(username);
   args.push_back("rstudio");
   args.push_back(server::options().authPamRequirePasswordPrompt() ? "1" : "0");

   // don't try to login with an empty password (this hangs PAM as it waits for input)
   if (password.empty())
   {
      LOG_WARNING_MESSAGE("No PAM password provided for user '" + username + "'; refusing login");
      return false;
   }

   // options (assume priv after fork)
   core::system::ProcessOptions options;
   options.onAfterFork = assumeRootPriv;

   // run pam helper
   core::system::ProcessResult result;
   Error error = core::system::runProgram(
      pamHelperPath.getAbsolutePath(),
      args,
      password,
      options,
      &result);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // check for success
   return result.exitStatus == 0;
}

Error initialize()
{
   // register ourselves as the auth handler
   server::auth::handler::Handler pamHandler;
   auth::common::prepareHandler(pamHandler,
                                signIn,
                                "", // defined below
                                userIdentifierToLocalUsername,
                                getUserIdentifier);
   pamHandler.signOut = signOut;
   if (overlay::canSetSignInCookies())
      pamHandler.setSignInCookies = boost::bind(auth::common::setSignInCookies, _1, _2, _3, _4);
   auth::handler::registerHandler(pamHandler);

   // add pam-specific auth handlers
   uri_handlers::addBlocking(kDoSignIn, doSignIn);
   uri_handlers::addBlocking(kPublicKey, publicKey);

   // initialize overlay
   Error error = overlay::initialize();
   if (error)
      return error;

   // initialize crypto
   return core::system::crypto::rsaInit();
}

} // namespace pam_auth
} // namespace server
} // namespace rstudio
