/*
 * PosixCrypto.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <core/system/Crypto.hpp>

#include <fcntl.h>

#include <openssl/err.h>
#include <openssl/hmac.h>
#include <openssl/bio.h>
#include <openssl/buffer.h>
#include <openssl/pem.h>
#include <openssl/rand.h>
#include <openssl/rsa.h>

#include <algorithm>

#include <boost/utility.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

// openssl calls on lion are are all marked as deprecated
#ifdef __APPLE__
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
#endif

using namespace core;

namespace core {
namespace system {
namespace crypto {

namespace {

// NOTE: we've never see the error codepath in spite of trying to get it to
// return an error by tweaking params -- NULL params caused a crash rather
// than returning an error). this is presumably because we are calling
// such low level functions. we may want to look at the src code to see
// if there is a way to test/engineer an error (or remove the error
// checking if there is no way to get one)

Error lastCryptoError(const ErrorLocation& location)
{
   // get the error code
   unsigned long ec = ::ERR_get_error();
   if (ec == 0)
   {
      LOG_WARNING_MESSAGE("lastCrytpoError called with no pending error");
      return systemError(boost::system::errc::not_supported,
                         "lastCrytpoError called with no pending error",
                         location);
   }
   
   // get the error message (docs say max len is 120)
   const int ERR_BUFF_SIZE = 250; 
   char errorBuffer[ERR_BUFF_SIZE];
   ::ERR_error_string_n(ec, errorBuffer, ERR_BUFF_SIZE);
   
   // return the error
   return systemError(boost::system::errc::bad_message,
                      errorBuffer,
                      location);
} 
   
class BIOFreeAllScope : boost::noncopyable
{
public:
   BIOFreeAllScope(BIO* pMem)
      : pMem_(pMem)
   {
   }
   
   ~BIOFreeAllScope()
   {
      try
      {
         ::BIO_free_all(pMem_);
      }
      catch(...)
      {
      }
   }
private:
   BIO* pMem_;
};
   
} // anonymous namespace
   
void initialize()
{
   // load global error string table
   ::ERR_load_crypto_strings();
}
   
Error HMAC_SHA1(const std::string& data, 
                const std::string& key,
                std::vector<unsigned char>* pHMAC)
{
   // copy data into vector
   std::vector<unsigned char> keyVector;
   std::copy(key.begin(), key.end(), std::back_inserter(keyVector));  
   
   // call core
   return HMAC_SHA1(data, keyVector, pHMAC);
}
   
Error HMAC_SHA1(const std::string& data, 
                const std::vector<unsigned char>& key,
                std::vector<unsigned char>* pHMAC)
{
   // copy data into data vector
   std::vector<unsigned char> dataVector;
   std::copy(data.begin(), data.end(), std::back_inserter(dataVector));
   
   // perform the hash
   unsigned int md_len = 0;
   pHMAC->resize(EVP_MAX_MD_SIZE);
   unsigned char* pResult = ::HMAC(EVP_sha1(),
                                   &(key[0]),
                                   key.size(),
                                   &(dataVector[0]),
                                   dataVector.size(),
                                   &(pHMAC->operator[](0)),
                                   &md_len);
   if (pResult != NULL)
   {
      pHMAC->resize(md_len);
      return Success();
   }
   else
   {
      return lastCryptoError(ERROR_LOCATION);
   }
}

Error base64Encode(const std::vector<unsigned char>& data, 
                   std::string* pEncoded)
{
   return base64Encode(&(data[0]), data.size(), pEncoded);
}
   
Error base64Encode(const unsigned char* pData, 
                   int len, 
                   std::string* pEncoded)
{
   // allocate BIO
   BIO* pB64 = ::BIO_new(BIO_f_base64());
   if (pB64 == NULL)
      return lastCryptoError(ERROR_LOCATION);
      
   // no newlines
   BIO_set_flags(pB64, BIO_FLAGS_BASE64_NO_NL);
   
   // make sure it is freed prior to exit from the function
   BIOFreeAllScope freeB64Scope(pB64);
   
   // allocate memory stream 
   BIO* pMem = ::BIO_new(BIO_s_mem());
   if (pMem == NULL)
      return lastCryptoError(ERROR_LOCATION);
   
   // tie the stream to the b64 stream
   pB64 = ::BIO_push(pB64, pMem);
   
   // perform the encoding
   int written = ::BIO_write(pB64, pData, len); 
   if (written != len)
      return lastCryptoError(ERROR_LOCATION);
      
   // flush all writes
   int result = BIO_flush(pB64);
   if (result <= 0)
      return lastCryptoError(ERROR_LOCATION);
   
   // seek to beginning of memory stream
   result = BIO_seek(pMem, 0);
   if (result == -1)
      return lastCryptoError(ERROR_LOCATION);

   // read the memory stream
   std::vector<char> buffer(len *2); // plenty more than len * 1.37 + padding
   int bytesRead = ::BIO_read(pMem, &(buffer[0]), buffer.capacity());
   if (bytesRead < 0 && ::ERR_get_error() != 0)
      return lastCryptoError(ERROR_LOCATION);

   // copy to out param
   buffer.resize(bytesRead);
   pEncoded->assign(buffer.begin(), buffer.end());

   // return success
   return Success();
}
   
   
Error base64Decode(const std::string& data, 
                   std::vector<unsigned char>* pDecoded)
{
   // allocate b64 BIO
   BIO* pB64 = ::BIO_new(BIO_f_base64());
   if (pB64 == NULL)
      return lastCryptoError(ERROR_LOCATION);
   
   // no newlines
   BIO_set_flags(pB64, BIO_FLAGS_BASE64_NO_NL);
   
   // make sure it is freed prior to exit from the function
   BIOFreeAllScope freeB64Scope(pB64);
   
   // allocate buffer 
   BIO* pMem = BIO_new_mem_buf((void*)data.data(), data.length());
   if (pMem == NULL)
      return lastCryptoError(ERROR_LOCATION);
   
   // tie the stream to the b64 stream
   pB64 = ::BIO_push(pB64, pMem);
   
   // reserve adequate memory in the decoded buffer and read into it
   pDecoded->clear();
   pDecoded->resize(data.length());
   int bytesRead = ::BIO_read(pB64, 
                              &(pDecoded->operator[](0)), 
                              pDecoded->size());
   if (bytesRead < 0)
      return lastCryptoError(ERROR_LOCATION);
   
   // resize the out buffer to the number of bytes actually read
   pDecoded->resize(bytesRead);
   
   // return success
   return Success();
     
}

namespace {
RSA* s_pRSA;
std::string s_modulo;
std::string s_exponent;
}

core::Error rsaInit()
{
   const int KEY_SIZE = 1024;
   const int ENTROPY_BYTES = 4096;

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

   s_pRSA = ::RSA_generate_key(KEY_SIZE, 0x10001, NULL, NULL);
   if (!s_pRSA)
      return lastCryptoError(ERROR_LOCATION);

   char* n = BN_bn2hex(s_pRSA->n);
   s_modulo = n;
   OPENSSL_free(n);
   char* e = BN_bn2hex(s_pRSA->e);
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
   Error error = base64Decode(cipherText, &cipherTextBytes);
   if (error)
      return error;

   int size = RSA_size(s_pRSA);
   std::vector<unsigned char> plainTextBytes(size);
   int bytesRead = RSA_private_decrypt(cipherTextBytes.size(),
                                       &cipherTextBytes[0],
                                       &plainTextBytes[0],
                                       s_pRSA,
                                       RSA_PKCS1_PADDING);
   if (bytesRead == -1)
      return lastCryptoError(ERROR_LOCATION);

   plainTextBytes.resize(bytesRead);
   pPlainText->assign(plainTextBytes.begin(), plainTextBytes.end());

   return Success();
}

                      
} // namespace crypto
} // namespace system
} // namespace core

