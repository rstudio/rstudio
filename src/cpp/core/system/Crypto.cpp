/*
 * Crypto.cpp
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

#include <core/system/Crypto.hpp>

#include <gsl/gsl-lite.hpp>

#ifdef _MSC_VER
# include <io.h>
#endif

#include <fcntl.h>

#include <openssl/err.h>
#include <openssl/hmac.h>
#include <openssl/bio.h>
#include <openssl/buffer.h>
#include <openssl/evp.h>
#include <openssl/pem.h>
#include <openssl/rand.h>
#include <openssl/rsa.h>
#include <openssl/x509.h>

#include <algorithm>
#include <stdio.h>

#include <boost/utility.hpp>

#include <shared_core/Error.hpp>

#include <core/Log.hpp>

#include <memory>

using namespace rstudio::core;

namespace rstudio {
namespace core {
namespace system {
namespace crypto {

namespace {

template <typename T, typename Deleter>
std::unique_ptr<T, Deleter> make_unique_ptr(T* ptr, Deleter deleter) {
    return std::unique_ptr<T, Deleter>(ptr, deleter);
}

#if OPENSSL_VERSION_NUMBER < 0x30000000L

EVP_PKEY* logRsaKeygenError(const ErrorLocation& errorLocation)
{
    Error error = getLastCryptoError(errorLocation);
    LOG_ERROR(error);
    return nullptr;
}

// For forward-compatibility with OpenSSL >= 3.0.0
EVP_PKEY* EVP_RSA_gen(int bits)
{
   auto ctx = make_unique_ptr(
               EVP_PKEY_CTX_new_id(EVP_PKEY_RSA, nullptr),
               EVP_PKEY_CTX_free);

   if (ctx == nullptr)
       return logRsaKeygenError(ERROR_LOCATION);

   if (EVP_PKEY_keygen_init(ctx.get()) <= 0)
       return logRsaKeygenError(ERROR_LOCATION);

   if (EVP_PKEY_CTX_set_rsa_keygen_bits(ctx.get(), kRsaKeySizeBits) <= 0)
       return logRsaKeygenError(ERROR_LOCATION);

   EVP_PKEY* pKeyPair = nullptr;
   if (EVP_PKEY_keygen(ctx.get(), &pKeyPair) <= 0)
       return logRsaKeygenError(ERROR_LOCATION);

   return pKeyPair;
}

#endif

} // end anonymous namespace

void initialize()
{
#if OPENSSL_VERSION_NUMBER < 0x10100000L
   // load global error string table
   ::ERR_load_crypto_strings();
#endif
}
   
Error HMAC_SHA2(const std::string& data,
                const std::string& key,
                std::vector<unsigned char>* pHMAC)
{
   // copy data into vector
   std::vector<unsigned char> keyVector;
   std::copy(key.begin(), key.end(), std::back_inserter(keyVector));
   
   // call core
   return HMAC_SHA2(data, keyVector, pHMAC);
}
   
Error HMAC_SHA2(const std::string& data,
                const std::vector<unsigned char>& key,
                std::vector<unsigned char>* pHMAC)
{
   // copy data into data vector
   std::vector<unsigned char> dataVector;
   std::copy(data.begin(), data.end(), std::back_inserter(dataVector));
   
   // perform the hash
   unsigned int md_len = 0;
   pHMAC->resize(EVP_MAX_MD_SIZE);
   unsigned char* pResult = ::HMAC(EVP_sha256(),
                                   &(key[0]),
                                   gsl::narrow_cast<int>(key.size()),
                                   &(dataVector[0]),
                                   dataVector.size(),
                                   &(pHMAC->operator[](0)),
                                   &md_len);
   if (pResult != nullptr)
   {
      pHMAC->resize(md_len);
      return Success();
   }
   else
   {
      return getLastCryptoError(ERROR_LOCATION);
   }
}

Error sha256(const std::string& message,
             std::string* pHash)
{
   unsigned char hash[SHA256_DIGEST_LENGTH];
   if (SHA256(reinterpret_cast<const unsigned char*>(message.c_str()),
              message.size(), hash) == nullptr)
   {
      return getLastCryptoError(ERROR_LOCATION);
   }

   *pHash = std::string((const char*)hash, SHA256_DIGEST_LENGTH);
   return Success();
}

Error rsaSign(const std::string& message,
              const std::string& pemPrivateKey,
              std::string* pOutSignature)
{
   // create a sha256 hash of the message first which is what we will sign
   // this prevents attackers from being able to back into creating a valid message
   std::string hash;
   Error error = sha256(message, &hash);
   if (error)
      return error;

   // convert the key into an RSA structure
   BIO* buffer = BIO_new_mem_buf(
            const_cast<char*>(pemPrivateKey.c_str()),
            gsl::narrow_cast<int>(pemPrivateKey.size()));
         
   auto pKeyBuff = make_unique_ptr(buffer, BIO_free);
   if (!pKeyBuff)
      return systemError(boost::system::errc::not_enough_memory, "RSA sign", ERROR_LOCATION);


   auto pRsa = make_unique_ptr(
            PEM_read_bio_PrivateKey(pKeyBuff.get(), nullptr, nullptr, nullptr),
            EVP_PKEY_free);
   
   if (!pRsa)
      return systemError(boost::system::errc::not_enough_memory, "RSA private key", ERROR_LOCATION);


   // sign the message hash
   std::vector<unsigned char> pSignature(EVP_PKEY_size(pRsa.get()));
   size_t sigLen = pSignature.size();
   auto ctx = make_unique_ptr(EVP_PKEY_CTX_new(pRsa.get(), NULL), EVP_PKEY_CTX_free);
   if (!ctx)
   {
      return systemError(boost::system::errc::not_enough_memory,
                         "EVP_PKEY_CTX_new()", ERROR_LOCATION);
   }

   if (!EVP_PKEY_sign_init(ctx.get()) ||
       !EVP_PKEY_CTX_set_rsa_padding(ctx.get(), RSA_PKCS1_PADDING) ||
       !EVP_PKEY_CTX_set_signature_md(ctx.get(), EVP_sha256()))
   {
      return getLastCryptoError(ERROR_LOCATION);
   }

   if (!EVP_PKEY_sign(ctx.get(), &pSignature[0], &sigLen,
                      reinterpret_cast<const unsigned char*>(hash.c_str()),
                      hash.size()))
   {
      return getLastCryptoError(ERROR_LOCATION);
   }

   // store signature in output param
   pSignature.resize(sigLen);
   pOutSignature->assign(pSignature.begin(), pSignature.end());

   return Success();
}

Error rsaVerify(const std::string& message,
                const std::string& signature,
                const std::string& pemPublicKey)
{
   // create a sha256 hash of the message first which is what we will verify
   // this prevents attackers from being able to back into creating a valid message
   std::string hash;
   Error error = sha256(message, &hash);
   if (error)
      return error;

   // convert the key into an RSA structure
   BIO* buffer =  BIO_new_mem_buf(
            const_cast<char*>(pemPublicKey.c_str()),
            gsl::narrow_cast<int>(pemPublicKey.size()));
   
   auto pKeyBuff = make_unique_ptr(buffer, BIO_free);
   if (!pKeyBuff)
      return systemError(boost::system::errc::not_enough_memory, "RSA bio public key", ERROR_LOCATION);

   auto pRsa = make_unique_ptr(
            PEM_read_bio_PUBKEY(pKeyBuff.get(), nullptr, nullptr, nullptr),
            EVP_PKEY_free);
   
   if (!pRsa)
      return systemError(boost::system::errc::not_enough_memory, "RSA pub key", ERROR_LOCATION);

   // verify the message hash
   auto ctx = make_unique_ptr(EVP_PKEY_CTX_new(pRsa.get(), NULL), EVP_PKEY_CTX_free);
   if (!ctx)
   {
      return systemError(boost::system::errc::not_enough_memory,
                         "EVP_PKEY_CTX_new()", ERROR_LOCATION);
   }

   if (!EVP_PKEY_verify_init(ctx.get()) ||
       !EVP_PKEY_CTX_set_rsa_padding(ctx.get(), RSA_PKCS1_PADDING) ||
       !EVP_PKEY_CTX_set_signature_md(ctx.get(), EVP_sha256()))
   {
      return getLastCryptoError(ERROR_LOCATION);
   }

   if (!EVP_PKEY_verify(ctx.get(),
                        reinterpret_cast<const unsigned char*>(signature.c_str()),
                        signature.size(),
                        reinterpret_cast<const unsigned char*>(hash.c_str()),
                        hash.size()))
   {
      return getLastCryptoError(ERROR_LOCATION);
   }

   return Success();
}

namespace {

Error generateRsa(const std::unique_ptr<BIO, decltype(&BIO_free)>& pBioPub,
                  const std::unique_ptr<BIO, decltype(&BIO_free)>& pBioPem)
{
   // Generate RSA key
   auto pKey = make_unique_ptr(EVP_RSA_gen(kRsaKeySizeBits), EVP_PKEY_free);
   if (!pKey)
      return getLastCryptoError(ERROR_LOCATION);
   
   // Write public key in PEM format
   int status = PEM_write_bio_PUBKEY(pBioPub.get(), pKey.get());
   if (status != 1)
      return getLastCryptoError(ERROR_LOCATION);

   // Write private key in PEM format
   status = PEM_write_bio_PrivateKey(pBioPem.get(), pKey.get(), nullptr, nullptr, 0, nullptr, nullptr);
   if (status != 1)
      return getLastCryptoError(ERROR_LOCATION);

   return Success();
}

} // anonymous namespace

Error generateRsaKeyFiles(const FilePath& publicKeyPath,
                          const FilePath& privateKeyPath)
{
   auto pBioPub = make_unique_ptr(
            BIO_new_file(publicKeyPath.getAbsolutePath().c_str(), "w"),
            BIO_free);
   
   if (!pBioPub)
      return getLastCryptoError(ERROR_LOCATION);

   auto pBioPem = make_unique_ptr(
            BIO_new_file(privateKeyPath.getAbsolutePath().c_str(), "w"),
            BIO_free);
   
   if (!pBioPem)
      return getLastCryptoError(ERROR_LOCATION);

   return generateRsa(pBioPub, pBioPem);
}

Error generateRsaKeyPair(std::string* pOutPublicKey,
                         std::string* pOutPrivateKey)
{
   auto pBioPub = make_unique_ptr(BIO_new(BIO_s_mem()), BIO_free);
   if (!pBioPub)
      return systemError(boost::system::errc::not_enough_memory, "RSA bio key pair", ERROR_LOCATION);

   pOutPrivateKey->reserve(4096);
   auto pBioPem = make_unique_ptr(BIO_new(BIO_s_mem()), BIO_free);
   if (!pBioPem)
      return systemError(boost::system::errc::not_enough_memory, "RSA bio priv key", ERROR_LOCATION);

   Error error = generateRsa(pBioPub, pBioPem);
   if (error)
      return error;

   // extract the underlying character buffers from the memory BIOs
   // note - these will be freed automatically when the BIOs are freed
   BUF_MEM* pubPtr;
   BUF_MEM* pemPtr;
   BIO_get_mem_ptr(pBioPub.get(), &pubPtr);
   BIO_get_mem_ptr(pBioPem.get(), &pemPtr);

   *pOutPublicKey = std::string(pubPtr->data, pubPtr->length);
   *pOutPrivateKey = std::string(pemPtr->data, pemPtr->length);

   return Success();
}

namespace {

Error generateRsaCertAndKey(const std::string& certCommonName,
                            const std::unique_ptr<BIO, decltype(&BIO_free)>& pBioCert,
                            const std::unique_ptr<BIO, decltype(&BIO_free)>& pBioCertKey)
{
   // Generate RSA key
   auto pCertKey = make_unique_ptr(EVP_RSA_gen(kRsaKeySizeBits), EVP_PKEY_free);
   if (!pCertKey)
      return getLastCryptoError(ERROR_LOCATION);
   
   // Create certificate
   auto pCert = make_unique_ptr(X509_new(), X509_free);

   ASN1_INTEGER_set(X509_get_serialNumber(pCert.get()), 1);
   X509_gmtime_adj(X509_get_notBefore(pCert.get()), 0);
   X509_gmtime_adj(X509_get_notAfter(pCert.get()), 365 * 24 * 3600);

   X509_set_pubkey(pCert.get(), pCertKey.get());

   X509_name_st* name = X509_get_subject_name(pCert.get());

   X509_NAME_add_entry_by_txt(name, "C",  MBSTRING_ASC, (unsigned char *)"US", -1, -1, 0);
   X509_NAME_add_entry_by_txt(name, "O",  MBSTRING_ASC, (unsigned char *)"RStudio PBC", -1, -1, 0);
   if (certCommonName.size() > 0)
      X509_NAME_add_entry_by_txt(name, "CN", MBSTRING_ASC, (unsigned char *)certCommonName.c_str(), -1, -1, 0);

   X509_set_issuer_name(pCert.get(), name);

   X509_sign(pCert.get(), pCertKey.get(), EVP_sha256());

   // Write cert key in PEM format
   int status = PEM_write_bio_X509(pBioCert.get(), pCert.get());
   if (status != 1)
      return getLastCryptoError(ERROR_LOCATION);

   // Write private key in PEM format
   status = PEM_write_bio_PrivateKey(pBioCertKey.get(), pCertKey.get(), nullptr, nullptr, 0, nullptr, nullptr);
   if (status != 1)
      return getLastCryptoError(ERROR_LOCATION);

   return Success();
}

} // end private namespace

Error generateRsaCertAndKeyFiles(const std::string& in_certCommonName,
                                 const FilePath& in_certPath,
                                 const FilePath& in_certKeyPath)
{
   auto pBioCert = make_unique_ptr(
            BIO_new_file(in_certPath.getAbsolutePath().c_str(), "w"),
            BIO_free);
   if (!pBioCert)
      return getLastCryptoError(ERROR_LOCATION);

   auto pBioCertKey = make_unique_ptr(
            BIO_new_file(in_certKeyPath.getAbsolutePath().c_str(), "w"),
            BIO_free);
   if (!pBioCertKey)
      return getLastCryptoError(ERROR_LOCATION);

   Error res = generateRsaCertAndKey(in_certCommonName, pBioCert, pBioCertKey);
   if (res)
      return res;

#ifndef _WIN32
   // change modes so public key is readable by all
   Error error = in_certPath.changeFileMode(core::FileMode::USER_READ_WRITE_ALL_READ);
   if (error)
      return error;

   // change mode so private key is only readable by user
   error = in_certKeyPath.changeFileMode(core::FileMode::USER_READ_WRITE);
   if (error)
      return error;
#endif

   return Success();
}

Error generateRsaCertAndKeyPair(const std::string& in_certCommonName,
                                std::string& out_cert,
                                std::string& out_certKey)
{
   auto pBioCert = make_unique_ptr(BIO_new(BIO_s_mem()), BIO_free);
   if (!pBioCert)
      return systemError(boost::system::errc::not_enough_memory, "RSA cert buffer", ERROR_LOCATION);

   auto pBioCertKey = make_unique_ptr(BIO_new(BIO_s_mem()), BIO_free);
   if (!pBioCertKey)
      return systemError(boost::system::errc::not_enough_memory, "RSA cert key buffer", ERROR_LOCATION);

   Error error = generateRsaCertAndKey(in_certCommonName, pBioCert, pBioCertKey);
   if (error)
      return error;

   // extract the underlying character buffers from the memory BIOs
   // note - these will be freed automatically when the BIOs are freed
   BUF_MEM* certPtr;
   BUF_MEM* certKeyPtr;
   BIO_get_mem_ptr(pBioCert.get(), &certPtr);
   BIO_get_mem_ptr(pBioCertKey.get(), &certKeyPtr);

   out_cert = std::string(certPtr->data, certPtr->length);
   out_certKey = std::string(certKeyPtr->data, certKeyPtr->length);

   return Success();
}

namespace {
EVP_PKEY* s_pRSA;
std::string s_modulo;
std::string s_exponent;
}

core::Error rsaInit()
{
   // seed the random number generator
   RAND_poll();

   // validate that the RNG is happy
   if (RAND_status() != 1)
      return getLastCryptoError(ERROR_LOCATION);

   // generate an RSA key pair
   s_pRSA = EVP_RSA_gen(kRsaKeySizeBits);

#if OPENSSL_VERSION_NUMBER < 0x10100000L
   // extract exponent + modulus for future use
   const BIGNUM *bn, *be;
   RSA* pRsaKeyPair = EVP_PKEY_get1_RSA(s_pRSA);
   bn = pRsaKeyPair->n;
   be = pRsaKeyPair->e;
#elif OPENSSL_VERSION_NUMBER < 0x30000000L
   // extract exponent + modulus for future use
   const BIGNUM *bn, *be;
   RSA* pRsaKeyPair = EVP_PKEY_get0_RSA(s_pRSA);
   RSA_get0_key(pRsaKeyPair, &bn, &be, nullptr);
#else
   // extract exponent + modulus for future use
   BIGNUM* bn = nullptr;
   if (EVP_PKEY_get_bn_param(s_pRSA, "n", &bn) != 1)
      return getLastCryptoError(ERROR_LOCATION);
   
   BIGNUM* be = nullptr;
   if (EVP_PKEY_get_bn_param(s_pRSA, "e", &be) != 1)
      return getLastCryptoError(ERROR_LOCATION);
#endif

   char* n = BN_bn2hex(bn);
   s_modulo = n;
   OPENSSL_free(n);

   char* e = BN_bn2hex(be);
   s_exponent = e;
   OPENSSL_free(e);

#if OPENSSL_VERSION_NUMBER < 0x10100000L
   RSA_free(pRsaKeyPair);
#endif
   
   return Success();
}

void rsaPublicKey(std::string* pExponent, std::string* pModulo)
{
   pModulo->assign(s_modulo.begin(), s_modulo.end());
   pExponent->assign(s_exponent.begin(), s_exponent.end());
}

core::Error rsaPrivateDecrypt(const std::string& cipherText, std::string* pPlainText)
{
   int status = 0;
   
   std::vector<unsigned char> cipherTextBytes;
   Error error = base64Decode(cipherText, cipherTextBytes);
   if (error)
      return error;

   auto ctx = make_unique_ptr(
            EVP_PKEY_CTX_new(s_pRSA, nullptr),
            EVP_PKEY_CTX_free);
   
   if (ctx == nullptr)
      return getLastCryptoError(ERROR_LOCATION);
   
   status = EVP_PKEY_decrypt_init(ctx.get());
   if (status <= 0)
      return getLastCryptoError(ERROR_LOCATION);
   
   std::size_t size = 0;
   status = EVP_PKEY_decrypt(ctx.get(), nullptr, &size, cipherTextBytes.data(), cipherTextBytes.size());
   if (status <= 0)
      return getLastCryptoError(ERROR_LOCATION);
   
   unsigned char* buffer = (unsigned char*) OPENSSL_malloc(size);
   if (buffer == nullptr)
      return getLastCryptoError(ERROR_LOCATION);
   
   status = EVP_PKEY_decrypt(
            ctx.get(),
            buffer, &size,
            cipherTextBytes.data(), cipherTextBytes.size());
   
   if (status <= 0)
      return getLastCryptoError(ERROR_LOCATION);
   
   pPlainText->assign(buffer, buffer + size);
   return Success();
}

                      
} // namespace crypto
} // namespace system
} // namespace core
} // namespace rstudio

