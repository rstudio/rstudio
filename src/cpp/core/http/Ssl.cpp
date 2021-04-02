/*
 * Ssl.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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


#include <core/http/Ssl.hpp>

#include <core/Log.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace ssl {

void initializeSslContext(boost::asio::ssl::context* pContext,
                                 bool verify,
                                 const std::string& certificateAuthority)
{
   if (verify)
   {
      boost::system::error_code ec;
      pContext->set_verify_mode(boost::asio::ssl::context::verify_peer, ec);
      if (ec)
      {
         LOG_ERROR(Error(ec, "Could not enable certificate verification on SSL context", ERROR_LOCATION));
         ec.clear();
      }

      pContext->set_default_verify_paths(ec);
      if (ec)
      {
         LOG_ERROR(Error(ec, "Could not set default certificate verification paths on SSL context", ERROR_LOCATION));
         ec.clear();
      }

      if (!certificateAuthority.empty())
      {
         boost::asio::const_buffer buff(certificateAuthority.data(), certificateAuthority.size());
         pContext->add_certificate_authority(buff, ec);
         if (ec)
         {
            LOG_ERROR(Error(ec, "Could not add certificate authority to SSL context - is it valid?", ERROR_LOCATION));
            ec.clear();
         }
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
         if (X509_STORE* store = SSL_CTX_get_cert_store(pContext->native_handle()))
         {
            if (::X509_STORE_add_cert(store, cert) != 1)
            {
               char* subjectName = X509_NAME_oneline(X509_get_subject_name(cert), nullptr, 0);
               std::string subjectNameStr(subjectName);
               OPENSSL_free(subjectName);

               ec = boost::system::error_code(
                           static_cast<int>(::ERR_get_error()),
                           boost::asio::error::get_ssl_category());
               Error error(ec, ERROR_LOCATION);
               error.addProperty("description", "Could not add certificate to OpenSSL cert store: " + subjectNameStr);

               LOG_ERROR(error);
            }
         }
      }
   #endif
   }
   else
   {
      boost::system::error_code ec;
      pContext->set_verify_mode(boost::asio::ssl::context::verify_none, ec);
      if (ec)
         LOG_ERROR(Error(ec, "Could not disable certificate verification on SSL context", ERROR_LOCATION));
   }
}

void initializeSslStream(boost::asio::ssl::stream<boost::asio::ip::tcp::socket>* pSslStream,
                                const std::string& host)
{
   // TLS v1.3 requires that the SNI be set, so set the SNI.
   if (!SSL_set_tlsext_host_name(pSslStream->native_handle(), host.c_str()))
   {
      boost::system::error_code ec{ static_cast<int>(::ERR_get_error()), boost::asio::error::get_ssl_category() };
      LOG_ERROR(Error(ec, ERROR_LOCATION));
   }
}

} // namespace ssl
} // namespace http
} // namespace core
} // namespace rstudio

