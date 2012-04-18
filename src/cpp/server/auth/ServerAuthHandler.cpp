/*
 * ServerAuthHandler.cpp
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

#ifndef SERVER_AUTH_HANDLER_CPP
#define SERVER_AUTH_HANDLER_CPP

#include <server/auth/ServerAuthHandler.hpp>

#include <core/json/JsonRpc.hpp>

#include <server/ServerUriHandlers.hpp>

#include <server/auth/ServerSecureUriHandler.hpp>

using namespace core;

namespace server {
namespace auth {
namespace handler {

namespace {

// global auth handler
Handler s_handler;

void updateCredentialsNotSupported(
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   // alias response
   http::Response* pResponse = &(pConnection->response());

   // gwt form panel requires text/html content type
   pResponse->setContentType("text/html");

   // set method not found error
   Error methodNotFoundError(json::errc::MethodNotFound, ERROR_LOCATION);
   json::setJsonRpcError(methodNotFoundError, pResponse);

   // write response
   pConnection->writeResponse();
}

} // anonymous namespace

// uri constants
const char * const kSignIn = "/auth-sign-in";
const char * const kSignOut = "/auth-sign-out";
const char * const kRefreshCredentialsAndContinue = "/auth-refresh-credentials";


std::string getUserIdentifier(const core::http::Request& request)
{
   return s_handler.getUserIdentifier(request);
}

std::string userIdentifierToLocalUsername(const std::string& userIdentifier)
{
   return s_handler.userIdentifierToLocalUsername(userIdentifier);
}

bool mainPageFilter(const core::http::Request& request,
                    core::http::Response* pResponse)
{
   return s_handler.mainPageFilter(request, pResponse);
}

void signInThenContinue(const core::http::Request& request,
                        core::http::Response* pResponse)
{
   s_handler.signInThenContinue(request, pResponse);
}

void refreshCredentialsThenContinue(
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   s_handler.refreshCredentialsThenContinue(pConnection);
}


// register the auth handler
void registerHandler(const Handler& handler)
{
   // set handler functions
   s_handler = handler;

   // register uri handlers
   uri_handlers::addBlocking(kSignIn, s_handler.signIn);

   uri_handlers::addBlocking(kSignOut,
                             auth::secureHttpHandler(s_handler.signOut));

   uri_handlers::add(kRefreshCredentialsAndContinue,
                     s_handler.refreshCredentialsThenContinue);

   uri_handlers::add("/auth-update-credentials",
                     s_handler.updateCredentials ?
                        s_handler.updateCredentials :
                        updateCredentialsNotSupported);
}

// is there a handler already registered?
bool isRegistered()
{
   return ! s_handler.getUserIdentifier.empty();
}

} // namespace handler
} // namespace auth
} // namespace server

#endif // SERVER_AUTH_HANDLER_CPP


