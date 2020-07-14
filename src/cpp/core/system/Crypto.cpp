/*
 * Crypto.cpp
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


#include <core/system/Crypto.hpp>

#include <gsl/gsl>

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

#include <algorithm>
#include <stdio.h>

#include <boost/utility.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>

#include <memory>

// openssl calls on lion are are all marked as deprecated
#ifdef __clang__
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
#endif

using namespace rstudio::core;

namespace rstudio {
namespace core {
namespace system {
namespace crypto {
   
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
   SHA256_CTX shaCtx;
   int ret = SHA256_Init(&shaCtx);
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

   ret = SHA256_Update(&shaCtx, message.c_str(), message.size());
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

   unsigned char hash[SHA256_DIGEST_LENGTH];
   ret = SHA256_Final(hash, &shaCtx);
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

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
   std::unique_ptr<BIO, decltype(&BIO_free)> pKeyBuff(BIO_new_mem_buf(const_cast<char*>(pemPrivateKey.c_str()),
                                                      gsl::narrow_cast<int>(pemPrivateKey.size())),
                                                      BIO_free);
   if (!pKeyBuff)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);


   std::unique_ptr<RSA, decltype(&RSA_free)> pRsa(PEM_read_bio_RSAPrivateKey(pKeyBuff.get(), nullptr, nullptr, nullptr),
                                                  RSA_free);
   if (!pRsa)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);


   // sign the message hash
   std::unique_ptr<unsigned char, decltype(&free)> pSignature((unsigned char*)malloc(RSA_size(pRsa.get())),
                                                              free);
   if (!pSignature)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   unsigned int sigLen = 0;
   int ret = RSA_sign(NID_sha256, (const unsigned char*)hash.c_str(),
                      static_cast<unsigned int>(hash.size()), pSignature.get(), &sigLen, pRsa.get());
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

   // store signature in output param
   *pOutSignature = std::string((const char*)pSignature.get(), sigLen);

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
   std::unique_ptr<BIO, decltype(&BIO_free)> pKeyBuff(
            BIO_new_mem_buf(const_cast<char*>(pemPublicKey.c_str()),
            gsl::narrow_cast<int>(pemPublicKey.size())),
            BIO_free);
   if (!pKeyBuff)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   std::unique_ptr<RSA, decltype(&RSA_free)> pRsa(PEM_read_bio_RSA_PUBKEY(pKeyBuff.get(), nullptr, nullptr, nullptr),
                                                  RSA_free);
   if (!pRsa)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   // verify the message hash
   int ret = RSA_verify(NID_sha256, (const unsigned char*)hash.c_str(), static_cast<unsigned int>(hash.size()),
                       (const unsigned char*)signature.c_str(), static_cast<unsigned int>(signature.size()), pRsa.get());
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

   return Success();
}

namespace {

Error generateRsa(const std::unique_ptr<BIO, decltype(&BIO_free)>& pBioPub,
                  const std::unique_ptr<BIO, decltype(&BIO_free)>& pBioPem)
{
   std::unique_ptr<RSA, decltype(&RSA_free)> pRsa(RSA_new(), RSA_free);
   if (!pRsa)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   std::unique_ptr<BIGNUM, decltype(&BN_free)> pBigNum(BN_new(), BN_free);
   if (!pBigNum)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   int ret = BN_set_word(pBigNum.get(), RSA_F4);
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

   ret = RSA_generate_key_ex(pRsa.get(), 2048, pBigNum.get(), nullptr);
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

   // Convert RSA to PKEY
   std::unique_ptr<EVP_PKEY, decltype(&EVP_PKEY_free)> pKey(EVP_PKEY_new(), EVP_PKEY_free);
   if (!pKey)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   ret = EVP_PKEY_set1_RSA(pKey.get(), pRsa.get());
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

   // Write public key in PEM format
   ret = PEM_write_bio_PUBKEY(pBioPub.get(), pKey.get());
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

   // Write private key in PEM format
   ret = PEM_write_bio_PrivateKey(pBioPem.get(), pKey.get(), nullptr, nullptr, 0, nullptr, nullptr);
   if (ret != 1)
      return getLastCryptoError(ERROR_LOCATION);

   return Success();
}

} // anonymous namespace

Error generateRsaKeyFiles(const FilePath& publicKeyPath,
                          const FilePath& privateKeyPath)
{
   std::unique_ptr<BIO, decltype(&BIO_free)> pBioPub(BIO_new_file(publicKeyPath.getAbsolutePath().c_str(), "w"),
                                                     BIO_free);
   if (!pBioPub)
      return getLastCryptoError(ERROR_LOCATION);

   std::unique_ptr<BIO, decltype(&BIO_free)> pBioPem(BIO_new_file(privateKeyPath.getAbsolutePath().c_str(), "w"),
                                                     BIO_free);
   if (!pBioPem)
      return getLastCryptoError(ERROR_LOCATION);

   return generateRsa(pBioPub, pBioPem);
}

Error generateRsaKeyPair(std::string* pOutPublicKey,
                         std::string* pOutPrivateKey)
{
   std::unique_ptr<BIO, decltype(&BIO_free)> pBioPub(BIO_new(BIO_s_mem()),
                                                     BIO_free);
   if (!pBioPub)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   pOutPrivateKey->reserve(4096);
   std::unique_ptr<BIO, decltype(&BIO_free)> pBioPem(BIO_new(BIO_s_mem()),
                                                     BIO_free);
   if (!pBioPem)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

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
RSA* s_pRSA;
std::string s_modulo;
std::string s_exponent;
}

core::Error rsaInit()
{
   const int KEY_SIZE = 2048;
   const int ENTROPY_BYTES = 4096;

   const BIGNUM *bn_n;
   const BIGNUM *bn_e;

   int rnd = ::open("/dev/urandom", O_RDONLY);
   if (rnd == -1)
      return systemError(errno, ERROR_LOCATION);

   char entropy[ENTROPY_BYTES];
   if (-1 == ::read(rnd, entropy, ENTROPY_BYTES))
   {
      ::close(rnd);
      return systemError(errno, ERROR_LOCATION);
   }
   ::close(rnd);

   RAND_seed(entropy, ENTROPY_BYTES);

   #if OPENSSL_VERSION_NUMBER < 0x10100000L
      s_pRSA = ::RSA_generate_key(KEY_SIZE, 0x10001, nullptr, nullptr);
      if (!s_pRSA)
         return getLastCryptoError(ERROR_LOCATION);

      bn_n = s_pRSA->n;
      bn_e = s_pRSA->e;
   #else
      BIGNUM *bn = BN_new();
      BN_set_word(bn, RSA_F4);
 
      s_pRSA = RSA_new();
      int rc = ::RSA_generate_key_ex(s_pRSA, KEY_SIZE, bn, nullptr);
      BN_clear_free(bn);
      if (rc != 1) {
        RSA_free(s_pRSA);
        return getLastCryptoError(ERROR_LOCATION);
      }
   
      RSA_get0_key(s_pRSA, &bn_n, &bn_e, nullptr);
   #endif

   char* n = BN_bn2hex(bn_n);
   s_modulo = n;
   OPENSSL_free(n);
   char* e = BN_bn2hex(bn_e);
   s_exponent = e;
   OPENSSL_free(e);

   return Success();
}

void rsaPublicKey(std::string* pExponent, std::string* pModulo)
{
   pModulo->assign(s_modulo.begin(), s_modulo.end());
   pExponent->assign(s_exponent.begin(), s_exponent.end());
}

core::Error rsaPrivateDecrypt(const std::string& cipherText, std::string* pPlainText)
{
   std::vector<unsigned char> cipherTextBytes;
   Error error = base64Decode(cipherText, cipherTextBytes);
   if (error)
      return error;

   int size = RSA_size(s_pRSA);
   std::vector<unsigned char> plainTextBytes(size);
   int bytesRead = RSA_private_decrypt(gsl::narrow_cast<int>(cipherTextBytes.size()),
                                       &cipherTextBytes[0],
                                       &plainTextBytes[0],
                                       s_pRSA,
                                       RSA_PKCS1_PADDING);
   if (bytesRead == -1)
      return getLastCryptoError(ERROR_LOCATION);

   plainTextBytes.resize(bytesRead);
   pPlainText->assign(plainTextBytes.begin(), plainTextBytes.end());

   return Success();
}

                      
} // namespace crypto
} // namespace system
} // namespace core
} // namespace rstudio

