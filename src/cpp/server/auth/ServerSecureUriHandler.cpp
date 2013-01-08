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

#include <server/auth/ServerAuthHandler.hpp>
#include <server/auth/ServerValidateUser.hpp>

using namespace core;

namespace server {
namespace auth {
   
namespace {  

class UriHandler
{
public:
   UriHandler(SecureUriHandlerFunction handlerFunction,
              http::UriHandlerFunction unauthorizedResponseFunction)
   : handlerFunction_(handlerFunction),
     unauthorizedResponseFunction_(unauthorizedResponseFunction)
   {
   }
   
   // COPYING: via compiler (copyable members)
   
   // provide http::UriHandlerFunction concept
   void operator()(const http::Request& request, http::Response *pResponse)
   {
      std::string userIdentifier = handler::getUserIdentifier(request);
      if (userIdentifier.empty())
      {
         unauthorizedResponseFunction_(request, pResponse);
         return;
      }

      // convert to local username
      std::string username = handler::userIdentifierToLocalUsername(
                                                            userIdentifier);

      // call the handler
      handlerFunction_(username, request, pResponse);
   }
   
private:
   SecureUriHandlerFunction handlerFunction_;
   http::UriHandlerFunction unauthorizedResponseFunction_;
};

class AsyncUriHandler
{
public:
   AsyncUriHandler(
            SecureAsyncUriHandlerFunction handlerFunction,
            http::AsyncUriHandlerFunction unauthorizedResponseFunction)
   : handlerFunction_(handlerFunction),
     unauthorizedResponseFunction_(unauthorizedResponseFunction)
   {
   }

   // COPYING: via compiler (copyable members)

   // provide http::AsyncUriHandlerFunction concept
   void operator()(boost::shared_ptr<http::AsyncConnection> pConnection)
   {
      std::string userIdentifier = handler::getUserIdentifier(
                                                   pConnection->request());
      if (userIdentifier.empty())
      {
         unauthorizedResponseFunction_(pConnection);
         return;
      }

      // convert to local username
      std::string username = handler::userIdentifierToLocalUsername(
                                                            userIdentifier);

      // call the handler
      handlerFunction_(username, pConnection);
   }

private:
   SecureAsyncUriHandlerFunction handlerFunction_;
   http::AsyncUriHandlerFunction unauthorizedResponseFunction_;
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
   
} // anonymous namespace
   
http::UriHandlerFunction secureHttpHandler(SecureUriHandlerFunction handler,
                                           bool authenticate)
{
   if (authenticate)
      return UriHandler(handler, auth::handler::signInThenContinue);
   else
      return UriHandler(handler, setHttpError);
}

http::UriHandlerFunction secureJsonRpcHandler(
                                       SecureUriHandlerFunction handler)
{
   return UriHandler(handler, setJsonRpcError); 
}

http::UriHandlerFunction secureUploadHandler(SecureUriHandlerFunction handler)
{
   return UriHandler(handler, setFileUploadError); 
}   

http::AsyncUriHandlerFunction secureAsyncHttpHandler(
                                    SecureAsyncUriHandlerFunction handler,
                                    bool authenticate)
{
   if (authenticate)
   {
       // try to recover from auth failure by silently refreshing credentials
       return AsyncUriHandler(handler,
                              auth::handler::refreshCredentialsThenContinue);
   }
   else
   {
      return AsyncUriHandler(handler,
                             boost::bind(asyncSetError, setHttpError, _1));
   }
}

http::AsyncUriHandlerFunction secureAsyncJsonRpcHandler(
                                    SecureAsyncUriHandlerFunction handler)
{
   return AsyncUriHandler(handler,
                          boost::bind(asyncSetError, setJsonRpcError, _1));
}

http::AsyncUriHandlerFunction secureAsyncUploadHandler(
                                    SecureAsyncUriHandlerFunction handler)
{
   return AsyncUriHandler(handler,
                          boost::bind(asyncSetError, setFileUploadError, _1));
}

} // namespace auth
} // namespace server



