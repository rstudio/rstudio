/*
 * Ssl.cpp
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


#include <core/http/Ssl.hpp>

#include <core/Log.hpp>
#include <core/FileUtils.hpp>
#include <shared_core/FilePath.hpp>

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
namespace ssl {

namespace {

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
                error.addProperty("description",
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
#elif defined(__APPLE__)
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
            error.addProperty("description",
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
#else

Error loadCertificate(const std::string& certStr, X509** pCert)
{
   BIO* bio = BIO_new_mem_buf(certStr.c_str(), certStr.size());
   if (!bio)
   {
      LOG_ERROR_MESSAGE("Failed to load certificate: could not create BIO");
      return Error(boost::system::errc::io_error, ERROR_LOCATION);
   }

   *pCert = PEM_read_bio_X509(bio, nullptr, nullptr, nullptr);
   BIO_free(bio);

   if (!*pCert)
   {
      LOG_ERROR_MESSAGE("Failed to load certificate: could not read certificate data");
      return Error(boost::system::errc::io_error, ERROR_LOCATION);
   }

   return Success();
}

bool isSelfSigned(X509* pCert)
{
   // get the issuer and subject names
   X509_NAME* pIssuer = X509_get_issuer_name(pCert);
   X509_NAME* pSubject = X509_get_subject_name(pCert);

   // compare the issuer and subject names
   return X509_NAME_cmp(pIssuer, pSubject) == 0;
}

bool isCertTrustedLocally(X509* pCert)
{
   // Create a store and load the system CA certs.
   X509_STORE* store = X509_STORE_new();
   if (!store)
   {
      LOG_ERROR_MESSAGE("Failed to validate self signed certificate: could not create X509 store");
      return false;
   }

   if (X509_STORE_set_default_paths(store) != 1)
   {
      LOG_ERROR_MESSAGE("Failed to validate self signed certificate: could not load system CA store");
      X509_STORE_free(store);
      return false;
   }

   // Create a store context and validate the certificate.
   X509_STORE_CTX* ctx = X509_STORE_CTX_new();
   if (!ctx)
   {
      LOG_ERROR_MESSAGE("Failed to validate self signed certificate: could not create X509 store context");
      X509_STORE_free(store);
      return false;
   }

   if (X509_STORE_CTX_init(ctx, store, pCert, nullptr) != 1)
   {
      LOG_ERROR_MESSAGE("Failed to validate self signed certificate: could not initialize X509 store context");
      X509_STORE_CTX_free(ctx);
      X509_STORE_free(store);
      return false;
   }

   int result = X509_verify_cert(ctx);
   X509_STORE_CTX_free(ctx);
   X509_STORE_free(store);

   return result == 1;
}

#endif

} // namespace

void validateSelfSignedCertificate(const FilePath& certPath)
{
#if defined(_WIN32) || defined(__APPLE__)
   LOG_DEBUG_MESSAGE("Unable to validate self signed certificate: platform not supported");
#else
   // Read the certificate into an X509 object.
   std::string contents = file_utils::readFile(certPath);
   X509* cert = nullptr;
   Error error = loadCertificate(contents, &cert);
   if (error)
   {
      LOG_ERROR(error);
      if (cert)
         X509_free(cert);
      return;
   }

   // Check if the certificate is self-signed.
   if (isSelfSigned(cert))
   {
      if (!isCertTrustedLocally(cert))
      {
         LOG_WARNING_MESSAGE("Self-signed certificate is not in the system CA store: " + certPath.getAbsolutePath());
         X509_free(cert);
         return;
      }

      LOG_DEBUG_MESSAGE("Successfully validated self signed certificate: " + certPath.getAbsolutePath());
   }

   X509_free(cert);
#endif
}

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

      if (certificateAuthority.empty())
      {
         pContext->set_default_verify_paths(ec);
         if (ec)
         {
            LOG_ERROR(Error(ec, "Could not set default certificate verification paths on SSL context", ERROR_LOCATION));
            ec.clear();
         }
      }
      else
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

