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

#include <security/pam_appl.h>
#include <sys/wait.h>

#include <boost/regex.hpp>

#include <core/Error.hpp>
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

#include "ServerAppArmor.hpp"


// NOTE: Mac OS X supports PAM but ships with it in a locked-down config
// which will cause all passwords to be rejected. To make it work run:
//
//   sudo cp /etc/pam.d/ftpd /etc/pam.d/rstudio
//
// That configures PAM to send rstudio through the same authentication
// stack as ftpd uses, which is similar to us.

using namespace core;

namespace server {
namespace pam_auth {

namespace {

class MemoryPool : boost::noncopyable {

   typedef boost::function<void*(size_t)> Alloc;
   typedef boost::function<void(void*)> Free;

public:
   MemoryPool(Alloc allocFunc = ::malloc, Free freeFunc = ::free) :
      alloc_(allocFunc),
      free_(freeFunc)
   {}

   ~MemoryPool()
   {
      try
      {
         for (size_t i = 0; i < buffers_.size(); i++)
         {
            free_(buffers_.at(i));
         }
      }
      catch(...)
      {
      }
   }

   void* alloc(size_t size)
   {
      void* p = alloc_(size);
      if (p)
         buffers_.push_back(p);
      return p;
   }

   void relinquishOwnership()
   {
      buffers_.clear();
   }

private:
   std::vector<void*> buffers_;
   Alloc alloc_;
   Free free_;
};

int conv(int num_msg,
         const struct pam_message** msg,
         struct pam_response** resp,
         void * appdata_ptr)
{
   try
   {
      MemoryPool pool;

      // resp will be freed by the caller
      *resp = static_cast<pam_response*>(pool.alloc(sizeof(pam_response) * num_msg));
      if (!*resp)
         return PAM_BUF_ERR;

      ::memset(*resp, 0, sizeof(pam_response) * num_msg);

      for (int i = 0; i < num_msg; i++)
      {
         const pam_message* input = msg[i];
         std::string msgText = input->msg;

         switch (input->msg_style)
         {
         case PAM_PROMPT_ECHO_OFF:
         {
            boost::regex passwordRegex("\\bpassword:\\s*$",
                                       boost::regex_constants::icase);
            boost::smatch match;
            if (regex_search(msgText, match, passwordRegex))
            {
               resp[i]->resp_retcode = 0;
               char* password = static_cast<char*>(appdata_ptr);
               // respBuf will be freed by the caller
               char* respBuf = static_cast<char*>(pool.alloc(strlen(password) + 1));
               resp[i]->resp = ::strcpy(respBuf, password);
            }
            else
               return PAM_CONV_ERR;
            break;
         }
         case PAM_PROMPT_ECHO_ON:
         case PAM_ERROR_MSG:
         case PAM_TEXT_INFO:
         default:
            return PAM_CONV_ERR;
         }
      }

      // The caller will free all the memory we allocated
      pool.relinquishOwnership();

      return PAM_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   return PAM_CONV_ERR;
}

// Low-level C++ wrapper around PAM API.
class PAMAuth
{
public:
    explicit PAMAuth(bool silent) :
          defaultFlags_(silent ? PAM_SILENT : 0),
          pamh_(NULL),
          status_(PAM_SUCCESS)
    {
    }

    virtual ~PAMAuth()
    {
       if (pamh_)
       {
          ::pam_end(pamh_, status_ | (defaultFlags_ & PAM_SILENT));
       }
    }

    std::pair<int, const std::string> lastError()
    {
       return std::pair<int, const std::string>(
             status_,
             std::string(::pam_strerror(pamh_, status_)));
    }

    int login(const std::string& username,
              const std::string& password)
    {
       struct pam_conv myConv;
       myConv.conv = conv;
       myConv.appdata_ptr = const_cast<void*>(static_cast<const void*>(password.c_str()));
       status_ = ::pam_start("rstudio",
                             username.c_str(),
                             &myConv,
                             &pamh_);
       if (status_ != PAM_SUCCESS)
       {
          LOG_ERROR_MESSAGE("pam_start failed: " + lastError().second);
          return status_;
       }

       status_ = ::pam_authenticate(pamh_, defaultFlags_);
       if (status_ != PAM_SUCCESS)
       {
          if (status_ != PAM_AUTH_ERR)
             LOG_ERROR_MESSAGE("pam_authenticate failed: " + lastError().second);
          return status_;
       }

       status_ = ::pam_acct_mgmt(pamh_, defaultFlags_);
       if (status_ != PAM_SUCCESS)
       {
          LOG_ERROR_MESSAGE("pam_acct_mgmt failed: " + lastError().second);
          return status_;
       }

       return PAM_SUCCESS;
 }

private:
    int defaultFlags_;
    pam_handle_t* pamh_;
    int status_;
};

bool doPamLogin(const std::string& username, const std::string& password)
{
   return PAM_SUCCESS == PAMAuth(false).login(username, password);
}

// Handles error logging and EINTR retrying. Only for use with
// functions that return -1 on error and set errno.
int posixcall(boost::function<ssize_t()> func)
{
   while (true)
   {
      int result;
      if ((result = func()) == -1)
      {
         if (errno == EINTR)
            continue;
         LOG_ERROR(systemError(errno, ERROR_LOCATION));
         return result;
      }
      return result;
   }
}

bool pamLogin(const std::string& username, const std::string& password)
{
   // RedHat 5 returns PAM_SYSTEM_ERR from pam_authenticate if we're
   // running with geteuid != getuid, as is the case when we temporarily
   // drop privileges. Restoring privileges fixes the problem but we
   // don't want to do that in the (multithreaded) server process. Fork
   // a child instead.

   // We use an anonymous pipe to communicate the results because waitpid
   // doesn't go well with our child process cleanup strategy and signal
   // blocking of the background threads.

   // On successful login, one byte is written to the pipe--otherwise,
   // the pipe is closed without writing.

   int pfd[2];
   if (posixcall(boost::bind(::pipe, pfd)) == -1)
      return false;

   pid_t pid = posixcall(::fork);

   if (pid == -1)
   {
      return false;
   }
   else if (pid == 0)
   {
      // Forked child process

      // Close the unused reading end
      posixcall(boost::bind(::close, pfd[0]));

      // restore root privillege
      if (util::system::realUserIsRoot())
      {
         Error error = util::system::restorePriv();
         if (error)
            LOG_ERROR(error);
         // intentionally failing forward
      }

      // lift app-armor restrictions
      if (app_armor::isEnforcingRestricted())
      {
         Error error = app_armor::dropRestricted();
         if (error)
            LOG_ERROR(error);
         // intentionally failing forward
      }

      if (doPamLogin(username, password))
         posixcall(boost::bind(::write, pfd[1], "1", 1));
      posixcall(boost::bind(::close, pfd[1]));

      _exit(0);

      // should never get here, but need return stmt to satisfy compiler
      return false;
   }
   else
   {
      // Parent process

      // Close the unused writing end, very important--otherwise the ::read
      // call to come will block forever!
      posixcall(boost::bind(::close, pfd[1]));

      char buf;
      size_t bytesRead = posixcall(boost::bind(::read, pfd[0], &buf, 1));
      posixcall(boost::bind(::close, pfd[0]));
      return bytesRead > 0;
   }
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
