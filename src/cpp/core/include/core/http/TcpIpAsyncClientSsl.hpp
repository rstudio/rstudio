/*
 * TcpIpAsyncClientSsl.hpp
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

#ifndef CORE_HTTP_TCP_IP_ASYNC_CLIENT_SSL_HPP
#define CORE_HTTP_TCP_IP_ASYNC_CLIENT_SSL_HPP

#include <boost/scoped_ptr.hpp>

#include <boost/asio/ip/tcp.hpp>

#include "BoostAsioSsl.hpp"

#include <core/http/AsyncClient.hpp>
#include <core/http/TcpIpAsyncConnector.hpp>

#ifdef _WIN32
#include <windows.h>
#include <wincrypt.h>
#include <cryptuiapi.h>
#include <openssl/x509.h>
#endif

#ifdef __APPLE__
#include <core/http/Keychain.hpp>
#include <openssl/x509.h>
#endif

namespace rstudio {
namespace core {
namespace http {

class TcpIpAsyncClientSsl
   : public AsyncClient<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> >
{
public:
   TcpIpAsyncClientSsl(boost::asio::io_service& ioService,
                       const std::string& address,
                       const std::string& port,
                       bool verify,
                       const std::string& certificateAuthority = std::string(),
                       const boost::posix_time::time_duration& connectionTimeout =
                          boost::posix_time::time_duration(boost::posix_time::pos_infin),
                       const std::string& hostname = std::string() )
     : AsyncClient<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> >(ioService),
       sslContext_(boost::asio::ssl::context::sslv23_client),
       address_(address),
       port_(port),
       verify_(verify),
       certificateAuthority_(certificateAuthority),
       connectionTimeout_(connectionTimeout)
   {
      if (verify_)
      {
         sslContext_.set_default_verify_paths();
         sslContext_.set_verify_mode(boost::asio::ssl::context::verify_peer);

         if (!certificateAuthority_.empty())
         {
            boost::asio::const_buffer buff(certificateAuthority_.data(), certificateAuthority_.size());
            boost::system::error_code ec;
            sslContext_.add_certificate_authority(buff, ec);
            if (ec)
               LOG_ERROR(Error(ec, ERROR_LOCATION));
         }

      #ifdef _WIN32
         // on Windows, OpenSSL does not support loading certificates from the Windows certificate store
         // because of this, each time we need to verify certificates, we initialize
         // all certificates individually with OpenSSL
         const ICertStore& certStore = getCertificateStore();
      #endif

      #ifdef __APPLE__
         // on OSX, OpenSSL does not support loading certificates from the Keychain
         // because of this, each time we need to verify certificates, we initialize
         // all certificates individually with OpenSSL, similar to what is done above for Windows
         const ICertStore& certStore = getKeychain();
      #endif

      #if defined(__APPLE__) || defined(_WIN32)
         for (const auto& cert : certStore.getCertificates())
         {
            if (X509_STORE* store = SSL_CTX_get_cert_store(sslContext_.native_handle()))
            {
               if (::X509_STORE_add_cert(store, cert) != 1)
               {
                  char* subjectName = X509_NAME_oneline(X509_get_subject_name(cert), nullptr, 0);
                  std::string subjectNameStr(subjectName);
                  OPENSSL_free(subjectName);

                  boost::system::error_code ec = boost::system::error_code(
                              static_cast<int>(::ERR_get_error()),
                              boost::asio::error::get_ssl_category());
                  Error error(ec, ERROR_LOCATION);
                  error.addProperty("Description", "Could not add certificate to OpenSSL cert store: " + subjectNameStr);

                  LOG_ERROR(error);
               }
            }
         }
      #endif
      }
      else
      {
         sslContext_.set_verify_mode(boost::asio::ssl::context::verify_none);
      }

      // use scoped ptr so we can call the constructor after we've configured
      // the ssl::context (immediately above)
      ptrSslStream_.reset(new boost::asio::ssl::stream<boost::asio::ip::tcp::socket>(ioService, sslContext_));

      // TLS v1.3 requires that the SNI be set, so set the SNI.
      if (!SSL_set_tlsext_host_name(
            ptrSslStream_->native_handle(),
            (hostname.empty() ? address_.c_str() : hostname.c_str())))
      {
         boost::system::error_code ec{ static_cast<int>(::ERR_get_error()), boost::asio::error::get_ssl_category() };
         LOG_ERROR(Error(ec, ERROR_LOCATION));
      }
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
                        _1),
            connectionTimeout_);
   }

   virtual std::string getDefaultHostHeader()
   {
      return address_ + ":" + port_;
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

#if defined(_WIN32) || defined(__APPLE__)
   class ICertStore
   {
   public:
      virtual std::vector<X509*> getCertificates() const = 0;
      virtual ~ICertStore() {}
   };
#endif

#ifdef _WIN32
   class WindowsCertificateStore : public ICertStore
   {
   public:
      WindowsCertificateStore()
      {
         // load certificates from important stores
         LPCSTR stores[] = {"ROOT", "CA"};
         for (const LPCSTR& store : stores)
         {
             HCERTSTORE hStore = CertOpenSystemStore(NULL, store);
             if (!hStore)
             {
                LOG_ERROR_MESSAGE("Could not open certificate store");
                return;
             }

             PCCERT_CONTEXT pContext = nullptr;
             while (pContext = CertEnumCertificatesInStore(hStore, pContext))
             {
                // convert the certificate returned from the Windows store into a
                // format that OpenSSL can understand
                const BYTE* certPtr = pContext->pbCertEncoded;
                X509* x509 = d2i_X509(nullptr, &certPtr, pContext->cbCertEncoded);
                if (x509)
                {
                   certificates.push_back(x509);
                }
                else
                {
                   boost::system::error_code ec = boost::system::error_code(
                               static_cast<int>(::ERR_get_error()),
                               boost::asio::error::get_ssl_category());
                   Error error(ec, ERROR_LOCATION);
                   error.addProperty("Description",
                                     "Could not create OpenSSL certificate object from Windows certificate data");
                   LOG_DEBUG_MESSAGE(error.asString());
                }
             }

             CertCloseStore(hStore, 0);
         }
      }

      // note to caller - the certificate data (X509 pointers) here must not be freed as the data
      // must persist for the lifetime of the process
      std::vector<X509*> getCertificates() const
      {
         return certificates;
      }

   private:
      // certificate pointers - these are intentionally leaked
      // as they need to be available for the entire run of the program
      std::vector<X509*> certificates;
   };

   static const ICertStore& getCertificateStore()
   {
       // Meyer's singleton - guarantees this is thread safe
       // and will be initialized exactly once by the first caller
       static WindowsCertificateStore instance;
       return instance;
   }
#endif

#ifdef __APPLE__
   class Keychain : public ICertStore
   {
   public:
      Keychain()
      {
         // load all certs from the keychain
         std::vector<KeychainCertificateData> certs = getKeychainCertificates();
         for (const auto& cert : certs)
         {
            // convert the raw bytes from the keychain into a format that OpenSSL can understand
            const unsigned char* bytePtr = cert.data.get();
            X509* x509 = d2i_X509(nullptr, &bytePtr, cert.size);
            if (x509)
            {
               certificates.push_back(x509);
            }
            else
            {
               boost::system::error_code ec = boost::system::error_code(
                           static_cast<int>(::ERR_get_error()),
                           boost::asio::error::get_ssl_category());
               Error error(ec, ERROR_LOCATION);
               error.addProperty("Description",
                                 "Could not create OpenSSL certificate object from Keychain certificate data");
               LOG_DEBUG_MESSAGE(error.asString());
            }
         }
      }

      // note to caller - the certificate data (X509 pointers) here must not be freed as the data
      // must persist for the lifetime of the process
      std::vector<X509*> getCertificates() const
      {
         return certificates;
      }

   private:
      // certificate pointers - these are intentionally leaked
      // as they need to be available for the entire run of the program
      std::vector<X509*> certificates;
   };

   static const ICertStore& getKeychain()
   {
      // Meyer's singleton - guarantees this is thread safe
      // and will be initialized exactly once by the first caller
      static Keychain instance;
      return instance;
   }
#endif
};


} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_TCP_IP_ASYNC_CLIENT_SSL_HPP
