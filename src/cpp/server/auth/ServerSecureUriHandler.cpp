/*
 * ServerSecureUriHandler.cpp
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

#include <server/auth/ServerSecureUriHandler.hpp>

#include <boost/function.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/URL.hpp>
#include <core/http/AsyncUriHandler.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/gwt/GwtFileHandler.hpp>

#include <server/ServerConstants.hpp>
#include <server/auth/ServerAuthHandler.hpp>
#include <server/auth/ServerValidateUser.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace auth {
   
namespace {  

class UriHandler
{
public:
   UriHandler(SecureUriHandlerFunctionEx handlerFunction,
              http::UriHandlerFunction unauthorizedResponseFunction,
              bool refreshAuthCookies)
   : handlerFunction_(handlerFunction),
     unauthorizedResponseFunction_(unauthorizedResponseFunction),
     refreshAuthCookies_(refreshAuthCookies)
   {
   }
   
   // COPYING: via compiler (copyable members)
   
   // provide http::UriHandlerFunction concept
   void operator()(const http::Request& request, http::Response *pResponse)
   {
      std::string userIdentifier = handler::getUserIdentifier(request, pResponse);
      if (userIdentifier.empty())
      {
         unauthorizedResponseFunction_(request, pResponse);
         return;
      }

      if (refreshAuthCookies_)
         handler::refreshAuthCookies(userIdentifier, request, pResponse);

      // convert to local username
      std::string username = handler::userIdentifierToLocalUsername(
                                                            userIdentifier);

      // call the handler
      handlerFunction_(username, userIdentifier, request, pResponse);
   }
   
private:
   SecureUriHandlerFunctionEx handlerFunction_;
   http::UriHandlerFunction unauthorizedResponseFunction_;
   bool refreshAuthCookies_;
};

class AsyncUriHandler
{
public:
   AsyncUriHandler(
            SecureAsyncUriHandlerFunctionEx handlerFunction,
            http::AsyncUriHandlerFunction unauthorizedResponseFunction,
            bool refreshAuthCookies)
   : handlerFunction_(handlerFunction),
     unauthorizedResponseFunction_(unauthorizedResponseFunction),
     refreshAuthCookies_(refreshAuthCookies)
   {
   }

   // COPYING: via compiler (copyable members)

   // provide http::AsyncUriHandlerFunction concept
   void operator()(boost::shared_ptr<http::AsyncConnection> pConnection)
   {
      std::string userIdentifier = handler::getUserIdentifier(
                                                   pConnection->request(),
                                                   &pConnection->response());
      if (userIdentifier.empty())
      {
         unauthorizedResponseFunction_(pConnection);
         return;
      }

      if (refreshAuthCookies_)
         handler::refreshAuthCookies(userIdentifier, pConnection->request(), &pConnection->response());

      // convert to local username
      std::string username = handler::userIdentifierToLocalUsername(
                                                            userIdentifier);

      // call the handler
      handlerFunction_(username, userIdentifier, pConnection);
   }

private:
   SecureAsyncUriHandlerFunctionEx handlerFunction_;
   http::AsyncUriHandlerFunction unauthorizedResponseFunction_;
   bool refreshAuthCookies_;
};
   
   
void setHttpError(const http::Request& request, http::Response* pResponse)
{
   pResponse->setError(http::status::Unauthorized, "Unauthorized");
}

void setJsonRpcError(const http::Request&, http::Response* pResponse)
{
   json::setJsonRpcError(json::errc::Unauthorized, pResponse);
}
   
void setFileUploadError(const http::Request& request, http::Response* pResponse)
{
   // response content type must always be text/html to be handled
   // properly by the browser/gwt on the client side

   pResponse->setContentType("text/html");
   setJsonRpcError(request, pResponse);
}

void asyncSetError(const http::UriHandlerFunction& syncSetErrorFunction,
                   boost::shared_ptr<http::AsyncConnection> pConnection)
{
   syncSetErrorFunction(pConnection->request(), &(pConnection->response()));
   pConnection->writeResponse();
}

SecureUriHandlerFunctionEx makeExtendedUriHandler(SecureUriHandlerFunction handler)
{
   return boost::bind(handler, _1, _3, _4);
}

SecureAsyncUriHandlerFunctionEx makeExtendedAsyncUriHandler(SecureAsyncUriHandlerFunction handler)
{
   return boost::bind(handler, _1, _3);
}
   
} // anonymous namespace
   
http::UriHandlerFunction secureHttpHandler(SecureUriHandlerFunction handler,
                                           bool authenticate)
{
   if (authenticate)
      return UriHandler(makeExtendedUriHandler(handler), auth::handler::signInThenContinue, true);
   else
      return UriHandler(makeExtendedUriHandler(handler), setHttpError, true);
}

http::UriHandlerFunction secureJsonRpcHandler(
                                       SecureUriHandlerFunction handler)
{
   // automatic auth refresh is dependant on the actual RPC being invoked
   // (since many of them are invoked automatically on timers)
   // therefore, auth refresh is handled in the session proxy, before the request
   // is forwarded to the actual session
   return UriHandler(makeExtendedUriHandler(handler), setJsonRpcError, false);
}

http::UriHandlerFunction secureJsonRpcHandlerEx(
                                       SecureUriHandlerFunctionEx handler)
{
   return UriHandler(handler, setJsonRpcError, false);
}

http::UriHandlerFunction secureUploadHandler(SecureUriHandlerFunction handler)
{
   return UriHandler(makeExtendedUriHandler(handler), setFileUploadError, true);
}   

http::AsyncUriHandlerFunction secureAsyncHttpHandler(
                                    SecureAsyncUriHandlerFunction handler,
                                    bool authenticate,
                                    bool refreshAuthCookies)
{
   if (authenticate)
   {
       // try to recover from auth failure by silently refreshing credentials
       return AsyncUriHandler(makeExtendedAsyncUriHandler(handler),
                              auth::handler::refreshCredentialsThenContinue,
                              refreshAuthCookies);
   }
   else
   {
      return AsyncUriHandler(makeExtendedAsyncUriHandler(handler),
                             boost::bind(asyncSetError, setHttpError, _1),
                             refreshAuthCookies);
   }
}

http::AsyncUriHandlerFunction secureAsyncHttpHandler(
                                    SecureAsyncUriHandlerFunction handler,
                                    http::AsyncUriHandlerFunction unauthorizedResponseFunction,
                                    bool refreshAuthCookies)
{
   return AsyncUriHandler(makeExtendedAsyncUriHandler(handler),
                          unauthorizedResponseFunction,
                          refreshAuthCookies);
}

http::AsyncUriHandlerFunction secureAsyncJsonRpcHandler(
                                    SecureAsyncUriHandlerFunction handler)
{
   return AsyncUriHandler(makeExtendedAsyncUriHandler(handler),
                          boost::bind(asyncSetError, setJsonRpcError, _1),
                          false);
}

http::AsyncUriHandlerFunction secureAsyncJsonRpcHandlerEx(
                                    SecureAsyncUriHandlerFunctionEx handler)
{
   return AsyncUriHandler(handler,
                          boost::bind(asyncSetError, setJsonRpcError, _1),
                          false);
}

http::AsyncUriHandlerFunction secureAsyncUploadHandler(
                                    SecureAsyncUriHandlerFunction handler)
{
   return AsyncUriHandler(makeExtendedAsyncUriHandler(handler),
                          boost::bind(asyncSetError, setFileUploadError, _1),
                          true);
}

} // namespace auth
} // namespace server
} // namespace rstudio



