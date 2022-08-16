/*
 * SessionTcpIpHttpConnectionListener.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_TCP_IP_HTTP_CONNECTION_LISTENER_HPP
#define SESSION_TCP_IP_HTTP_CONNECTION_LISTENER_HPP

#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>

#include <core/http/TcpIpSocketUtils.hpp>

#include "SessionHttpConnectionUtils.hpp"
#include "SessionHttpConnectionListenerImpl.hpp"

using namespace rstudio::core;

namespace rstudio {
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

   TcpIpHttpConnectionListener(const std::string& address,
                               const std::string& port,
                               const std::string& sharedSecret,
                               FilePath certFile,
                               FilePath keyFile)
      : address_(address), port_(port), secret_(sharedSecret),
        certFile_(certFile), keyFile_(keyFile)
   {
   }

   boost::asio::ip::tcp::endpoint getLocalEndpoint() const
   {
      return localEndpoint_;
   }

   bool isSsl() const
   {
      return !certFile_.isEmpty();
   }

protected:

   bool authenticate(boost::shared_ptr<HttpConnection> ptrConnection)
   {
      bool res = connection::authenticate(ptrConnection, secret_);
      if (!res)
         return false;
      return HttpConnectionListenerImpl::authenticate(ptrConnection);
   }

private:

   virtual Error initializeAcceptor(
      http::SocketAcceptorService<boost::asio::ip::tcp>* pAcceptor)
   {
      if (isSsl())
      {
         if (!certFile_.exists())
         {
            return systemError(boost::system::errc::no_such_file_or_directory,
                               "Session http certificate file does not exist: " + certFile_.getAbsolutePath(),
                               ERROR_LOCATION);
         }
         if (keyFile_.isEmpty())
         {
            return systemError(boost::system::errc::no_such_file_or_directory,
                               "Session http missing key file",
                               ERROR_LOCATION);
         }
         if (!keyFile_.exists())
         {
            return systemError(boost::system::errc::no_such_file_or_directory,
                               "Session http certificate key file does not exist: " + keyFile_.getAbsolutePath(),
                               ERROR_LOCATION);
         }

         boost::shared_ptr<boost::asio::ssl::context> context(
                  new boost::asio::ssl::context(boost::asio::ssl::context::sslv23));
         context->set_options(boost::asio::ssl::context::default_workarounds |
                              boost::asio::ssl::context::no_sslv2 |
                              boost::asio::ssl::context::single_dh_use);

         boost::system::error_code ec;
         context->use_certificate_chain_file(certFile_.getAbsolutePath(), ec);
         if (ec)
            return Error(ec, "Invalid cert with path: " + certFile_.getAbsolutePath(), ERROR_LOCATION);

         context->use_private_key_file(keyFile_.getAbsolutePath(), boost::asio::ssl::context::pem, ec);
         if (ec)
            return Error(ec, "Invalid cert key file with path: " + keyFile_.getAbsolutePath(), ERROR_LOCATION);

         setSslContext(context);
      }
      else if (!keyFile_.isEmpty())
      {
         return systemError(boost::system::errc::no_such_file_or_directory,
                            "Session http cert key specified without a cert",
                            ERROR_LOCATION);
      }
      

      Error error = http::initTcpIpAcceptor(*pAcceptor, address_, port_);
      if (error)
         return error;

      localEndpoint_ = pAcceptor->acceptor().local_endpoint();
      return Success();
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
   boost::asio::ip::tcp::endpoint localEndpoint_;
   FilePath certFile_;
   FilePath keyFile_;
};

} // namespace session
} // namespace rstudio

#endif /* SESSION_TCP_IP_HTTP_CONNECTION_LISTENER_HPP */
