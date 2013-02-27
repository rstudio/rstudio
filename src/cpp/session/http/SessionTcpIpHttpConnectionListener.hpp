/*
 * SessionTcpIpHttpConnectionListener.hpp
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

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>

#include <core/http/TcpIpSocketUtils.hpp>

#include "SessionHttpConnectionListenerImpl.hpp"

using namespace core ;

namespace session {

// implementation of local stream http connection listener
class TcpIpHttpConnectionListener :
    public HttpConnectionListenerImpl<boost::asio::ip::tcp>
{
public:
   TcpIpHttpConnectionListener(const std::string& address,
                               const std::string& port,
                               const std::string& sharedSecret)

      : address_(address), port_(port), secret_(sharedSecret)
   {
   }

protected:

   bool authenticate(boost::shared_ptr<HttpConnection> ptrConnection)
   {
      // allow all requests if no secret
      if (secret_.empty())
         return true;

      // Allow /help, /custom, and /session urls -- this is because the creators
      // of custom http apps for R (either using tools:::http.handlers.env
      // directly or using Rack) will often instruct their users to paste
      // the url e.g. http://localhost:34302/custom/appname into their browser
      // address bar. This of course won't work with our shared secret scheme.
      // We allow this exception to our security policy because doing
      // so makes us no less secure than standard CRAN desktop R. We also
      // allow help as a convenience to the user (since the same security
      // logic applies)
      std::string uri = ptrConnection->request().uri();
      if (boost::algorithm::starts_with(uri, "/custom/") ||
          boost::algorithm::starts_with(uri, "/session") ||
          boost::algorithm::starts_with(uri, "/help/"))
      {
         return true;
      }

      // validate against shared secret
      return secret_ == ptrConnection->request().headerValue("X-Shared-Secret");
   }

private:

   virtual Error initializeAcceptor(
      http::SocketAcceptorService<boost::asio::ip::tcp>* pAcceptor)
   {
      return http::initTcpIpAcceptor(*pAcceptor, address_, port_);
   }

   virtual bool validateConnection(
      boost::shared_ptr<HttpConnectionImpl<boost::asio::ip::tcp> > ptrConnection)
   {
      return true;
   }


   virtual Error cleanup()
   {
      return Success();
   }


private:
   std::string address_;
   std::string port_;
   std::string secret_;
};

} // namespace session
