/*
 * Crypto.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the
 * terms of a commercial license agreement with RStudio, then this program is
 * licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

#include <shared_core/system/Crypto.hpp>

#include <gsl/gsl>

#include <boost/noncopyable.hpp>

#include <openssl/bio.h>
#include <openssl/buffer.h>
#include <openssl/err.h>
#include <openssl/evp.h>
#include <openssl/rand.h>

#include <shared_core/Error.hpp>
#include <shared_core/Logger.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace crypto {

namespace {

// openssl encrypt/decrypt constants
constexpr int s_encrypt = 1;
constexpr int s_decrypt = 0;

class BIOFreeAllScope : boost::noncopyable
{
public:
   explicit BIOFreeAllScope(BIO* pMem)
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

Error getLastCryptoError(const ErrorLocation& in_location)
{
   // get the error code
   unsigned long ec = ::ERR_get_error();
   if (ec == 0)
   {
      log::logWarningMessage("getLastCryptoError called with no pending error");
      return systemError(
         boost::system::errc::not_supported,
         "getLastCryptoError called with no pending error",
         in_location);
   }

   // get the error message (docs say max len is 120)
   const int ERR_BUFF_SIZE = 250;
   char errorBuffer[ERR_BUFF_SIZE];
   ::ERR_error_string_n(ec, errorBuffer, ERR_BUFF_SIZE);

   // return the error
   return systemError(
      boost::system::errc::bad_message,
      errorBuffer,
      in_location);
}

Error aesDecrypt(
   const std::vector<unsigned char>& in_data,
   const std::vector<unsigned char>& in_key,
   const std::vector<unsigned char>& in_iv,
   std::vector<unsigned char>& out_decrypted)
{
   out_decrypted.resize(in_data.size());
   int outLen = 0;
   int bytesDecrypted = 0;

   EVP_CIPHER_CTX *ctx;
   ctx = EVP_CIPHER_CTX_new();
   EVP_CipherInit_ex(ctx, EVP_aes_128_cbc(), nullptr, &in_key[0], &in_iv[0], s_decrypt);

   // perform the decryption
   if(!EVP_CipherUpdate(ctx, &out_decrypted[0], &outLen, &in_data[0], gsl::narrow_cast<int>(in_data.size())))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }
   bytesDecrypted += outLen;

   // perform final flush
   if(!EVP_CipherFinal_ex(ctx, &out_decrypted[outLen], &outLen))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }
   bytesDecrypted += outLen;

   EVP_CIPHER_CTX_free(ctx);

   // resize the container to the amount of actual bytes decrypted (padding is removed)
   out_decrypted.resize(bytesDecrypted);

   return Success();
}

Error aesEncrypt(
   const std::vector<unsigned char>& data,
   const std::vector<unsigned char>& key,
   const std::vector<unsigned char>& iv,
   std::vector<unsigned char>& out_encrypted)
{
   // allow enough space in output buffer for additional block
   out_encrypted.resize(data.size() + EVP_MAX_BLOCK_LENGTH);
   int outlen = 0;
   int bytesEncrypted = 0;

   EVP_CIPHER_CTX *ctx;
   ctx = EVP_CIPHER_CTX_new();
   EVP_CipherInit_ex(ctx, EVP_aes_128_cbc(), nullptr, &key[0], &iv[0], s_encrypt);

   // perform the encryption
   if(!EVP_CipherUpdate(ctx, &out_encrypted[0], &outlen, &data[0], gsl::narrow_cast<int>(data.size())))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }
   bytesEncrypted += outlen;

   // perform final flush including left-over padding
   if(!EVP_CipherFinal_ex(ctx, &out_encrypted[outlen], &outlen))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }
   bytesEncrypted += outlen;

   EVP_CIPHER_CTX_free(ctx);

   // resize the container to the amount of actual bytes encrypted (including padding)
   out_encrypted.resize(bytesEncrypted);

   return Success();
}

Error base64Encode(const std::vector<unsigned char>& in_data, std::string& out_encoded)
{
   return base64Encode(&in_data[0], gsl::narrow_cast<int>(in_data.size()), out_encoded);
}

Error base64Encode(const unsigned char* in_data, int in_length, std::string& out_encoded)
{
   // allocate BIO
   BIO* pB64 = ::BIO_new(BIO_f_base64());
   if (pB64 == nullptr)
      return getLastCryptoError(ERROR_LOCATION);

   // no newlines
   BIO_set_flags(pB64, BIO_FLAGS_BASE64_NO_NL);

   // make sure it is freed prior to exit from the function
   BIOFreeAllScope freeB64Scope(pB64);

   // allocate memory stream
   BIO* pMem = ::BIO_new(BIO_s_mem());
   if (pMem == nullptr)
      return getLastCryptoError(ERROR_LOCATION);

   // tie the stream to the b64 stream
   pB64 = ::BIO_push(pB64, pMem);

   // perform the encoding
   int written = ::BIO_write(pB64, in_data, in_length);
   if (written != in_length)
      return getLastCryptoError(ERROR_LOCATION);

   // flush all writes
   int result = BIO_flush(pB64);
   if (result <= 0)
      return getLastCryptoError(ERROR_LOCATION);

   // seek to beginning of memory stream
   result = BIO_seek(pMem, 0);
   if (result == -1)
      return getLastCryptoError(ERROR_LOCATION);

   // read the memory stream
   std::vector<char> buffer(in_length * 2); // plenty more than len * 1.37 + padding
   int bytesRead = ::BIO_read(pMem, &buffer[0], gsl::narrow_cast<int>(buffer.capacity()));
   if (bytesRead < 0 && ::ERR_get_error() != 0)
      return getLastCryptoError(ERROR_LOCATION);

   // copy to out param
   buffer.resize(bytesRead);
   out_encoded.assign(buffer.begin(), buffer.end());

   // return success
   return Success();
}

Error base64Decode(const std::string& in_data, std::vector<unsigned char>& out_decoded)
{
   // allocate b64 BIO
   BIO* pB64 = ::BIO_new(BIO_f_base64());
   if (pB64 == nullptr)
      return getLastCryptoError(ERROR_LOCATION);

   // no newlines
   BIO_set_flags(pB64, BIO_FLAGS_BASE64_NO_NL);

   // make sure it is freed prior to exit from the function
   BIOFreeAllScope freeB64Scope(pB64);

   // allocate buffer
   BIO* pMem = BIO_new_mem_buf((void*)in_data.data(), gsl::narrow_cast<int>(in_data.length()));
   if (pMem == nullptr)
      return getLastCryptoError(ERROR_LOCATION);

   // tie the stream to the b64 stream
   pB64 = ::BIO_push(pB64, pMem);

   // reserve adequate memory in the decoded buffer and read into it
   out_decoded.clear();
   out_decoded.resize(in_data.length());
   int bytesRead = ::BIO_read(pB64,
                              &(out_decoded[0]),
                              gsl::narrow_cast<int>(out_decoded.size()));
   if (bytesRead < 0)
      return getLastCryptoError(ERROR_LOCATION);

   // resize the out buffer to the number of bytes actually read
   out_decoded.resize(bytesRead);

   // return success
   return Success();
}

Error base64Decode(const std::string& in_data, std::string& out_decoded)
{
   std::vector<unsigned char> decoded;
   Error error = base64Decode(in_data, decoded);
   if (error)
      return error;

   out_decoded.reserve(decoded.size());
   std::copy(decoded.begin(), decoded.end(), std::back_inserter(out_decoded));
   return Success();
}

Error decryptAndBase64Decode(
   const std::string& input,
   const std::string& keyStr,
   const std::string& ivStr,
   std::string& out_decrypted)
{
   // copy key into vector
   std::vector<unsigned char> key;
   std::copy(keyStr.begin(), keyStr.end(), std::back_inserter(key));

   // decode initialization vector
   std::vector<unsigned char> iv;
   Error error = core::system::crypto::base64Decode(ivStr, iv);
   if (error)
      return error;

   // decode encrypted input
   std::vector<unsigned char> decoded;
   error = core::system::crypto::base64Decode(input, decoded);
   if (error)
      return error;

   // decrypt decoded input
   std::vector<unsigned char> decrypted;
   error = core::system::crypto::aesDecrypt(decoded, key, iv, decrypted);
   if (error)
      return error;

   // covert the decrypted bytes into the original string
   out_decrypted.reserve(decrypted.size());
   std::copy(decrypted.begin(), decrypted.end(), std::back_inserter(out_decrypted));

   return Success();
}


Error encryptAndBase64Encode(
   const std::string& input,
   const std::string& keyStr,
   std::string& out_iv,
   std::string& out_encrypted)
{
   // copy data into vector
   std::vector<unsigned char> data;
   std::copy(input.begin(), input.end(), std::back_inserter(data));

   // copy key into vector
   std::vector<unsigned char> key;
   std::copy(keyStr.begin(), keyStr.end(), std::back_inserter(key));

   // create a random initialization vector for a little added security
   std::vector<unsigned char> iv;
   Error error = core::system::crypto::random(256, iv);
   if (error)
      return error;

   // encrypt the input
   std::vector<unsigned char> encrypted;
   error = core::system::crypto::aesEncrypt(data, key, iv, encrypted);
   if (error)
      return error;

   // base 64 encode the IV used for encryption
   error = core::system::crypto::base64Encode(iv, out_iv);
   if (error)
      return error;

   // base 64 encode encrypted result
   return core::system::crypto::base64Encode(encrypted, out_encrypted);
}

Error random(uint32_t in_length, std::vector<unsigned char>& out_randomData)
{
   out_randomData.resize(in_length);

   if (!RAND_bytes(&out_randomData[0], in_length))
      return getLastCryptoError(ERROR_LOCATION);

   return Success();
}

} // namespace crypto
} // namespace system
} // namespace core
} // namespace rstudio
