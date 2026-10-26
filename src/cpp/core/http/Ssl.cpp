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

#include <memory>

#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/split.hpp>

#include <openssl/bio.h>
#include <openssl/err.h>
#include <openssl/ssl.h>

#include <core/Log.hpp>
#include <core/FileUtils.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/system/Crypto.hpp>

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

bool initializeSslContext(boost::asio::ssl::context* pContext,
                          bool verify,
                          const std::string& certificateAuthority)
{
   bool res = true;
   if (verify)
   {
      boost::system::error_code ec;
      pContext->set_verify_mode(boost::asio::ssl::context::verify_peer, ec);
      if (ec)
      {
         LOG_ERROR(Error(ec, "Could not enable certificate verification on SSL context", ERROR_LOCATION));
         ec.clear();
         res = false;
      }

      if (certificateAuthority.empty())
      {
         pContext->set_default_verify_paths(ec);
         if (ec)
         {
            LOG_ERROR(Error(ec, "Could not set default certificate verification paths on SSL context", ERROR_LOCATION));
            ec.clear();
            res = false;
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
            res = false;
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
               res = false;
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
      {
         LOG_ERROR(Error(ec, "Could not disable certificate verification on SSL context", ERROR_LOCATION));
         res = false;
      }
   }
   return res;
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

namespace {

// the in-memory endpoints exchange the whole handshake through these buffers
const size_t kHandshakeBufferBytes = 64 * 1024;
const int kMaxHandshakeSteps = 32;

struct Protocol
{
   const char* name;
   int version;
};

// a version of 0 marks a name that nginx accepts but that current OpenSSL
// releases do not implement
const Protocol kProtocols[] = {
   { "SSLv2",   0 },
#ifdef SSL3_VERSION
   { "SSLv3",   SSL3_VERSION },
#else
   { "SSLv3",   0 },
#endif
   { "TLSv1",   TLS1_VERSION },
   { "TLSv1.1", TLS1_1_VERSION },
   { "TLSv1.2", TLS1_2_VERSION },
   { "TLSv1.3", TLS1_3_VERSION },
};

using SslContextPtr = std::unique_ptr<SSL_CTX, decltype(&SSL_CTX_free)>;
using SslPtr = std::unique_ptr<SSL, decltype(&SSL_free)>;

const Protocol* findProtocol(const std::string& name)
{
   for (const Protocol& protocol : kProtocols)
   {
      if (name == protocol.name)
         return &protocol;
   }
   return nullptr;
}

// stops OpenSSL from prompting on the terminal for the passphrase of an
// encrypted key; -1 tells OpenSSL that no passphrase is available
int refusePassphrase(char*, int, int, void*)
{
   return -1;
}

Error cryptoError(const std::string& description,
                  const FilePath& path,
                  const ErrorLocation& location)
{
   Error error = core::system::crypto::getLastCryptoError(location);
   error.addProperty("description", description);
   error.addProperty("path", path.getAbsolutePath());
   return error;
}

Error createServerContext(const FilePath& certificatePath,
                          const FilePath& keyPath,
                          const std::string& cipherList,
                          SslContextPtr* pContext)
{
   SslContextPtr context(SSL_CTX_new(TLS_server_method()), SSL_CTX_free);
   if (!context)
      return core::system::crypto::getLastCryptoError(ERROR_LOCATION);

   SSL_CTX_set_default_passwd_cb(context.get(), refusePassphrase);

   std::string certificate = certificatePath.getAbsolutePath();
   if (SSL_CTX_use_certificate_chain_file(context.get(), certificate.c_str()) != 1)
      return cryptoError("Could not load the certificate", certificatePath, ERROR_LOCATION);

   std::string key = keyPath.getAbsolutePath();
   if (SSL_CTX_use_PrivateKey_file(context.get(), key.c_str(), SSL_FILETYPE_PEM) != 1)
      return cryptoError("Could not load the certificate key", keyPath, ERROR_LOCATION);

   if (SSL_CTX_set_cipher_list(context.get(), cipherList.c_str()) != 1)
      return core::system::crypto::getLastCryptoError(ERROR_LOCATION);

   *pContext = std::move(context);
   return Success();
}

// the client accepts anything the server offers, so only the limits of the
// server side decide whether a protocol is usable
Error createClientContext(SslContextPtr* pContext)
{
   SslContextPtr context(SSL_CTX_new(TLS_client_method()), SSL_CTX_free);
   if (!context)
      return core::system::crypto::getLastCryptoError(ERROR_LOCATION);

   SSL_CTX_set_verify(context.get(), SSL_VERIFY_NONE, nullptr);
   SSL_CTX_set_security_level(context.get(), 0);
   if (SSL_CTX_set_cipher_list(context.get(), "ALL:@SECLEVEL=0") != 1)
      return core::system::crypto::getLastCryptoError(ERROR_LOCATION);

   *pContext = std::move(context);
   return Success();
}

// pinning the version on the SSL object replaces the minimum version from the
// system OpenSSL configuration, as nginx does for ssl_protocols
bool pinVersion(SSL* pSsl, int version)
{
   return SSL_set_min_proto_version(pSsl, version) == 1 &&
          SSL_set_max_proto_version(pSsl, version) == 1;
}

bool isHandshakeInProgress(SSL* pSsl, int result)
{
   if (result == 1)
      return true;

   int error = SSL_get_error(pSsl, result);
   return error == SSL_ERROR_WANT_READ || error == SSL_ERROR_WANT_WRITE;
}

bool runHandshake(SSL* pClient, SSL* pServer)
{
   for (int step = 0; step < kMaxHandshakeSteps; ++step)
   {
      int clientResult = SSL_do_handshake(pClient);
      int serverResult = SSL_do_handshake(pServer);
      if (clientResult == 1 && serverResult == 1)
         return true;

      if (!isHandshakeInProgress(pClient, clientResult) ||
          !isHandshakeInProgress(pServer, serverResult))
         return false;
   }
   return false;
}

bool canNegotiate(SSL_CTX* pServerContext, SSL_CTX* pClientContext, int version)
{
   if (version == 0)
      return false;

   SslPtr server(SSL_new(pServerContext), SSL_free);
   SslPtr client(SSL_new(pClientContext), SSL_free);
   if (!server || !client)
      return false;

   if (!pinVersion(server.get(), version) || !pinVersion(client.get(), version))
      return false;

   BIO* pClientBio = nullptr;
   BIO* pServerBio = nullptr;
   if (BIO_new_bio_pair(&pClientBio, kHandshakeBufferBytes,
                        &pServerBio, kHandshakeBufferBytes) != 1)
      return false;

   // each SSL object takes ownership of its end of the pair
   SSL_set_bio(client.get(), pClientBio, pClientBio);
   SSL_set_bio(server.get(), pServerBio, pServerBio);
   SSL_set_connect_state(client.get());
   SSL_set_accept_state(server.get());

   return runHandshake(client.get(), server.get());
}

Error sortProtocols(const std::string& protocolList,
                    const FilePath& certificatePath,
                    const FilePath& keyPath,
                    const std::string& cipherList,
                    ProtocolSupport* pSupport)
{
   SslContextPtr serverContext(nullptr, SSL_CTX_free);
   Error error = createServerContext(certificatePath, keyPath, cipherList, &serverContext);
   if (error)
      return error;

   SslContextPtr clientContext(nullptr, SSL_CTX_free);
   error = createClientContext(&clientContext);
   if (error)
      return error;

   std::vector<std::string> names;
   boost::split(names, protocolList, boost::is_any_of(" \t"), boost::token_compress_on);
   for (const std::string& name : names)
   {
      const Protocol* pProtocol = findProtocol(name);
      if (pProtocol == nullptr)
         continue;

      if (canNegotiate(serverContext.get(), clientContext.get(), pProtocol->version))
         pSupport->usable.push_back(name);
      else
         pSupport->unusable.push_back(name);
   }
   return Success();
}

} // anonymous namespace

Error checkProtocolSupport(const std::string& protocolList,
                           const FilePath& certificatePath,
                           const FilePath& keyPath,
                           const std::string& cipherList,
                           ProtocolSupport* pSupport)
{
   Error error = sortProtocols(protocolList, certificatePath, keyPath, cipherList, pSupport);

   // failed handshakes leave entries on the thread's OpenSSL error queue, and
   // later OpenSSL calls must not report them as their own errors
   ERR_clear_error();
   return error;
}

} // namespace ssl
} // namespace http
} // namespace core
} // namespace rstudio

