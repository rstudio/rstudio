/*
 * TcpIpAsyncClientSsl.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_HTTP_TCP_IP_ASYNC_CLIENT_SSL_HPP
#define CORE_HTTP_TCP_IP_ASYNC_CLIENT_SSL_HPP

#include <boost/scoped_ptr.hpp>

#include <boost/asio/ip/tcp.hpp>

#include "BoostAsioSsl.hpp"

#include <core/http/AsyncClient.hpp>
#include <core/http/Ssl.hpp>
#include <core/http/TcpIpAsyncConnector.hpp>
#include <core/http/ProxyUtils.hpp>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace http {

class TcpIpAsyncClientSsl
   : public AsyncClient<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> >
{
public:
   TcpIpAsyncClientSsl(boost::asio::io_context& ioContext,
                       const std::string& address,
                       const std::string& port,
                       bool verify,
                       const std::string& certificateAuthority = std::string(),
                       const boost::posix_time::time_duration& connectionTimeout =
                          boost::posix_time::time_duration(boost::posix_time::pos_infin),
                       const std::string& hostname = std::string(),
                       const std::string& verifyAddress = std::string())
     : AsyncClient<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> >(ioContext),
       sslContext_(boost::asio::ssl::context::sslv23_client),
       address_(address),
       port_(port),
       verify_(verify),
       certificateAuthority_(certificateAuthority),
       connectionTimeout_(connectionTimeout),
       verifyAddress_(verifyAddress)
   {
      ssl::initializeSslContext(&sslContext_, verify, certificateAuthority);

      // use scoped ptr so we can call the constructor after we've configured
      // the ssl::context (immediately above)
      ptrSslStream_.reset(new boost::asio::ssl::stream<boost::asio::ip::tcp::socket>(ioContext, sslContext_));

      ssl::initializeSslStream(ptrSslStream_.get(), (hostname.empty() ? address_.c_str() : hostname.c_str()));
   }


protected:

   virtual boost::asio::ssl::stream<boost::asio::ip::tcp::socket>& socket()
   {
      return *(ptrSslStream_);
   }

   virtual void connectAndWriteRequest()
   {
      boost::shared_ptr<TcpIpAsyncConnector> pAsyncConnector(
                  new TcpIpAsyncConnector(ioContext(),
                                          &(ptrSslStream_->next_layer())));

      auto connectAddress = address_;
      auto connectPort = port_;

      const auto proxyUrl = proxyUtils().httpsProxyUrl(address_, port_);

      if (proxyUrl.has_value())
      {
         connectAddress = proxyUrl->hostname();
         connectPort = std::to_string(proxyUrl->port());
         LOG_DEBUG_MESSAGE("Using proxy: " + connectAddress + ":" + connectPort);
      }

      pAsyncConnector->connect(
            connectAddress,
            connectPort,
            boost::asio::bind_executor(*pStrand_, boost::bind(&TcpIpAsyncClientSsl::handleConnect,
                                                            TcpIpAsyncClientSsl::sharedFromThis(),
                                                            proxyUrl)),
            boost::asio::bind_executor(*pStrand_, boost::bind(&TcpIpAsyncClientSsl::handleConnectionError,
                                                            TcpIpAsyncClientSsl::sharedFromThis(),
                                                            _1)),
            connectionTimeout_);
   }

   virtual std::string getDefaultHostHeader()
   {
      return address_ + ":" + port_;
   }


   virtual void addErrorProperties(Error& error)
   {
      AsyncClient::addErrorProperties(error);
      error.addProperty("address", address_);
      error.addProperty("port", port_);
   }

private:
  void handleConnect(const boost::optional<URL>& proxyUrl)
  {
     if (proxyUrl)
     {
        http::Request connectRequest;
        connectRequest.setMethod("CONNECT");
        connectRequest.setUri(address_ + ":" + port_);
        connectRequest.setHttpVersion(1, 1);
        connectRequest.setHeader("Host", address_ + ":" + port_);
        connectRequest.assign(connectRequest);

        boost::asio::async_write(
            socket().next_layer(),
            connectRequest.toBuffers(),
            boost::asio::bind_executor(
                *pStrand_,
                boost::bind(&TcpIpAsyncClientSsl::handleProxyConnectWrite,
                            sharedFromThis(),
                            boost::asio::placeholders::error)));
     }
     else
     {
        performHandshake();
     }
  }

  void handleProxyConnectWrite(const boost::system::error_code& ec)
  {

     if (!ec)
     {
        // Only read until the end of the response line, we just want to know if
        // the connection was successful
        boost::asio::async_read_until(
            socket().next_layer(),
            connectResponseBuffer_,
            "\r\n",
            boost::asio::bind_executor(
                *pStrand_,
                boost::bind(&TcpIpAsyncClientSsl::handleProxyConnectRead,
                            sharedFromThis(),
                            boost::asio::placeholders::error)));
     }
     else
     {
        handleErrorCode(ec, ERROR_LOCATION);
     }
  }

  void handleProxyConnectRead(const boost::system::error_code& ec)
  {

     if (!ec)
     {
        http::Response connectResponse;
        Error error = ResponseParser::parseStatusLine(&connectResponseBuffer_,
                                                      &connectResponse);
        if (error)
        {
           handleError(error);
        }
        else
        {
           // If the response is not a 200, we should close the connection
           if (connectResponse.statusCode() != 200)
           {
              Error error = systemError(boost::system::errc::connection_refused,
                                        "Proxy connection failed",
                                        ERROR_LOCATION);
              handleError(error);
           }
           else
           {
              performHandshake();
           }
        }
     }
     else
     {
        handleErrorCode(ec, ERROR_LOCATION);
     }
  }

  void performHandshake()
  {
     if (verify_)
     {
        ptrSslStream_->set_verify_callback(
            boost::asio::ssl::host_name_verification(
                verifyAddress_.empty() ? address_ : verifyAddress_));
     }

     ptrSslStream_->async_handshake(
         boost::asio::ssl::stream_base::client,
         boost::asio::bind_executor(
             *pStrand_,
             boost::bind(&TcpIpAsyncClientSsl::handleHandshake,
                         sharedFromThis(),
                         boost::asio::placeholders::error)));
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
      return util::isSslShutdownError(ec);
   }

private:
   boost::asio::ssl::context sslContext_;
   boost::scoped_ptr<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> > ptrSslStream_;
   std::string address_;
   std::string port_;
   bool verify_;
   std::string certificateAuthority_;
   boost::posix_time::time_duration connectionTimeout_;
   std::string verifyAddress_;
   http::Request connectRequest_;
   boost::asio::streambuf connectResponseBuffer_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_TCP_IP_ASYNC_CLIENT_SSL_HPP
