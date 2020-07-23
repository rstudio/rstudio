/*
 * ServerSecureUriHandler.cpp
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
              bool refreshAuthCookies,
              bool requireUserListCookie)
   : handlerFunction_(handlerFunction),
     unauthorizedResponseFunction_(unauthorizedResponseFunction),
     refreshAuthCookies_(refreshAuthCookies),
     requireUserListCookie_(requireUserListCookie)
   {
   }
   
   // COPYING: via compiler (copyable members)
   
   // provide http::UriHandlerFunction concept
   void operator()(const http::Request& request, http::Response *pResponse)
   {
      std::string userIdentifier = handler::getUserIdentifier(request, requireUserListCookie_);
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
   bool requireUserListCookie_;
};

class AsyncUriHandler
{
public:
   AsyncUriHandler(
            SecureAsyncUriHandlerFunctionExVariant handlerFunction,
            http::AsyncUriHandlerFunction unauthorizedResponseFunction,
            bool refreshAuthCookies,
            bool requireUserListCookie)
   : handlerFunction_(handlerFunction),
     unauthorizedResponseFunction_(unauthorizedResponseFunction),
     refreshAuthCookies_(refreshAuthCookies),
     requireUserListCookie_(requireUserListCookie)
   {
   }

   // COPYING - ensure that none of the cached user variables are copied over
   // these values are cached so that form handlers can be invoked multiple times
   // without having to recalculate username, which is expensive (as it involves crypto)
   AsyncUriHandler(const AsyncUriHandler& other)
   {
      handlerFunction_ = other.handlerFunction_;
      unauthorizedResponseFunction_ = other.unauthorizedResponseFunction_;
      refreshAuthCookies_ = other.refreshAuthCookies_;
      requireUserListCookie_ = other.requireUserListCookie_;

      // do not copy cached user identifiers
   }

   // provide http::AsyncUriHandlerFunction concept
   void operator()(boost::shared_ptr<http::AsyncConnection> pConnection)
   {
      if (!getUser(pConnection))
         return;

      // call the handler
      invokeHandler(username_, userIdentifier_, pConnection);
   }

   // provide http::AsyncUriUploadHandlerFunction concept
   bool operator()(boost::shared_ptr<http::AsyncConnection> pConnection,
                   const std::string& formData,
                   bool keepGoing)
   {
      if (!getUser(pConnection))
         return false;

      // call the handler
      return invokeHandler(username_, userIdentifier_, pConnection, formData, keepGoing);
   }

private:
   class InvocationVisitor : public boost::static_visitor<bool>
   {
   public :
      InvocationVisitor(const std::string& username,
                        const std::string& userIdentifier,
                        boost::shared_ptr<http::AsyncConnection> pConnection)
         : username_(username),
           userIdentifier_(userIdentifier),
           pConnection_(pConnection)
      {
      }

      InvocationVisitor(const std::string& username,
                        const std::string& userIdentifier,
                        boost::shared_ptr<http::AsyncConnection> pConnection,
                        const std::string& formData,
                        bool keepGoing)
         : username_(username),
           userIdentifier_(userIdentifier),
           pConnection_(pConnection),
           formData_(formData),
           keepGoing_(keepGoing)
      {
      }

      bool operator()(const SecureAsyncUriHandlerFunctionEx& func) const
      {
         func(username_, userIdentifier_, pConnection_);

         // dummy return value
         return true;
      }

      bool operator()(const SecureAsyncUriUploadHandlerFunctionEx& func) const
      {
         return func(username_, userIdentifier_, pConnection_, formData_, keepGoing_);
      }

   private:
      std::string username_;
      std::string userIdentifier_;
      boost::shared_ptr<http::AsyncConnection> pConnection_;
      std::string formData_;
      bool keepGoing_;
   };

   bool getUser(boost::shared_ptr<http::AsyncConnection> pConnection)
   {
      // cache the username so we don't have to determine it every time this is invoked
      // looking up the username and performing the secure cookie decode is expensive
      // this optimization is important for large file uploads to prevent a lot of unnecessary work
      if (userIdentifier_.empty() || username_.empty())
      {
         userIdentifier_ = handler::getUserIdentifier(pConnection->request(),
                                                      requireUserListCookie_);
         if (userIdentifier_.empty())
         {
            unauthorizedResponseFunction_(pConnection);
            return false;
         }

         // convert to local username
         username_ = handler::userIdentifierToLocalUsername(userIdentifier_);
      }

      if (refreshAuthCookies_)
         handler::refreshAuthCookies(userIdentifier_, pConnection->request(), &pConnection->response());

      return true;
   }

   void invokeHandler(const std::string& username,
                      const std::string& userIdentifier,
                      boost::shared_ptr<http::AsyncConnection> pConnection)
   {
      boost::apply_visitor(InvocationVisitor(username, userIdentifier, pConnection), handlerFunction_);
   }

   bool invokeHandler(const std::string& username,
                      const std::string& userIdentifier,
                      boost::shared_ptr<http::AsyncConnection> pConnection,
                      const std::string& formData,
                      bool keepGoing)
   {
      return boost::apply_visitor(
               InvocationVisitor(username, userIdentifier, pConnection, formData, keepGoing),
               handlerFunction_);
   }

   SecureAsyncUriHandlerFunctionExVariant handlerFunction_;
   http::AsyncUriHandlerFunction unauthorizedResponseFunction_;
   bool refreshAuthCookies_;
   bool requireUserListCookie_;
   std::string username_;
   std::string userIdentifier_;
};
   
   
void setHttpError(const http::Request& request, http::Response* pResponse)
{
   pResponse->setError(http::status::Unauthorized, "Unauthorized");
}

void setJsonRpcError(const http::Request&, http::Response* pResponse)
{
   json::setJsonRpcError(Error(json::errc::Unauthorized, ERROR_LOCATION), pResponse);
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
                                           bool authenticate,
                                           bool requireUserListCookie)
{
   if (authenticate)
      return UriHandler(makeExtendedUriHandler(handler),
                        auth::handler::signInThenContinue,
                        true,
                        requireUserListCookie);
   else
      return UriHandler(makeExtendedUriHandler(handler),
                        setHttpError,
                        true,
                        requireUserListCookie);
}

http::UriHandlerFunction secureJsonRpcHandler(
                                       SecureUriHandlerFunction handler)
{
   // automatic auth refresh is dependant on the actual RPC being invoked
   // (since many of them are invoked automatically on timers)
   // therefore, auth refresh is handled in the session proxy, before the request
   // is forwarded to the actual session
   return UriHandler(makeExtendedUriHandler(handler), setJsonRpcError, false, true);
}

http::UriHandlerFunction secureJsonRpcHandlerEx(
                                       SecureUriHandlerFunctionEx handler)
{
   return UriHandler(handler, setJsonRpcError, false, true);
}

http::UriHandlerFunction secureUploadHandler(SecureUriHandlerFunction handler)
{
   return UriHandler(makeExtendedUriHandler(handler), setFileUploadError, true, true);
}   

http::AsyncUriHandlerFunction secureAsyncHttpHandler(
                                    SecureAsyncUriHandlerFunction handler,
                                    bool authenticate,
                                    bool refreshAuthCookies,
                                    bool requireUserListCookie)
{
   if (authenticate)
   {
       // try to recover from auth failure by silently refreshing credentials
       return AsyncUriHandler(makeExtendedAsyncUriHandler(handler),
                              auth::handler::refreshCredentialsThenContinue,
                              refreshAuthCookies,
                              requireUserListCookie);
   }
   else
   {
      return AsyncUriHandler(makeExtendedAsyncUriHandler(handler),
                             boost::bind(asyncSetError, setHttpError, _1),
                             refreshAuthCookies,
                             requireUserListCookie);
   }
}

http::AsyncUriHandlerFunction secureAsyncHttpHandler(
                                    SecureAsyncUriHandlerFunction handler,
                                    http::AsyncUriHandlerFunction unauthorizedResponseFunction,
                                    bool refreshAuthCookies,
                                    bool requireUserListCookie)
{
   return AsyncUriHandler(makeExtendedAsyncUriHandler(handler),
                          unauthorizedResponseFunction,
                          refreshAuthCookies,
                          requireUserListCookie);
}

http::AsyncUriHandlerFunction secureAsyncJsonRpcHandler(
                                    SecureAsyncUriHandlerFunction handler)
{
   return AsyncUriHandler(makeExtendedAsyncUriHandler(handler),
                          boost::bind(asyncSetError, setJsonRpcError, _1),
                          false,
                          true);
}

http::AsyncUriHandlerFunction secureAsyncJsonRpcHandlerEx(
                                    SecureAsyncUriHandlerFunctionEx handler)
{
   return AsyncUriHandler(handler,
                          boost::bind(asyncSetError, setJsonRpcError, _1),
                          false,
                          true);
}


http::AsyncUriUploadHandlerFunction secureAsyncUploadHandler(
                                    SecureAsyncUriUploadHandlerFunctionEx handler)
{
   return AsyncUriHandler(handler,
                          boost::bind(asyncSetError, setFileUploadError, _1),
                          true,
                          true);
}


} // namespace auth
} // namespace server
} // namespace rstudio



