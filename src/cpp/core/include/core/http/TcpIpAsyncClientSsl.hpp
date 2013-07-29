/*
 * TcpIpAsyncClientSsl.hpp
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

#ifndef CORE_HTTP_TCP_IP_ASYNC_CLIENT_SSL_HPP
#define CORE_HTTP_TCP_IP_ASYNC_CLIENT_SSL_HPP

#ifdef _WIN32
#error TcpIpAsyncClientSsl is not supported on Windows
#endif

#include <boost/scoped_ptr.hpp>

#include <boost/asio/ip/tcp.hpp>

#include "BoostAsioSsl.hpp"

#include <core/http/AsyncClient.hpp>
#include <core/http/TcpIpAsyncConnector.hpp>

namespace core {
namespace http {  

class TcpIpAsyncClientSsl
   : public AsyncClient<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> >
{
public:
   TcpIpAsyncClientSsl(boost::asio::io_service& ioService,
                       const std::string& address,
                       const std::string& port,
                       bool verify)
     : AsyncClient<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> >(ioService),
       sslContext_(ioService, boost::asio::ssl::context::sslv23_client),
       address_(address),
       port_(port),
       verify_(verify)
   {
      if (verify_)
      {
         sslContext_.set_default_verify_paths();
         sslContext_.set_verify_mode(boost::asio::ssl::context::verify_peer);
      }
      else
      {
         sslContext_.set_verify_mode(boost::asio::ssl::context::verify_none);
      }

      // use scoped ptr so we can call the constructor after we've configured
      // the ssl::context (immediately above)
      ptrSslStream_.reset(new boost::asio::ssl::stream<boost::asio::ip::tcp::socket>(ioService, sslContext_));
   }


protected:

   virtual boost::asio::ssl::stream<boost::asio::ip::tcp::socket>& socket()
   {
      return *(ptrSslStream_);
   }

   virtual void connectAndWriteRequest()
   {
      boost::shared_ptr<TcpIpAsyncConnector> pAsyncConnector(
                  new TcpIpAsyncConnector(ioService(),
                                          &(ptrSslStream_->next_layer())));

      pAsyncConnector->connect(
            address_,
            port_,
            boost::bind(&TcpIpAsyncClientSsl::performHandshake,
                        TcpIpAsyncClientSsl::sharedFromThis()),
            boost::bind(&TcpIpAsyncClientSsl::handleConnectionError,
                        TcpIpAsyncClientSsl::sharedFromThis(),
                        _1));
   }


private:

   void performHandshake()
   {
      if (verify_)
      {
         ptrSslStream_->set_verify_callback(
                            boost::asio::ssl::rfc2818_verification(address_));
      }
      ptrSslStream_->async_handshake(
            boost::asio::ssl::stream_base::client,
            boost::bind(&TcpIpAsyncClientSsl::handleHandshake,
                        sharedFromThis(),
                        boost::asio::placeholders::error));
   }

   void handleHandshake(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // finished handshake, commence with request
            writeRequest();
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   const boost::shared_ptr<TcpIpAsyncClientSsl> sharedFromThis()
   {
      boost::shared_ptr<AsyncClient<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> > > ptrShared
                                                 = shared_from_this();

      return boost::static_pointer_cast<TcpIpAsyncClientSsl>(ptrShared);
   }

   virtual bool isShutdownError(const boost::system::error_code& ec)
   {
      // boost returns "short_read" when the peer calls SSL_shutdown()
      if (ec.category() == boost::asio::error::get_ssl_category() &&
          ec.value() == ERR_PACK(ERR_LIB_SSL, 0, SSL_R_SHORT_READ))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

private:
   boost::asio::ssl::context sslContext_;
   boost::scoped_ptr<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> > ptrSslStream_;
   std::string address_;
   std::string port_;
   bool verify_;
};
   

} // namespace http
} // namespace core

#endif // CORE_HTTP_TCP_IP_ASYNC_CLIENT_SSL_HPP
