/*
 * EncryptionVersion.cpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the
 * terms of a commercial license agreement with Posit Software, then this program is
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

#include <shared_core/system/encryption/EncryptionVersion.hpp>

#include <gsl/gsl>

#include <openssl/err.h>
#include <openssl/evp.h>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace crypto {

// openssl encrypt/decrypt constants
constexpr int s_encrypt = 1;
constexpr int s_decrypt = 0;

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

namespace v0 {

/**
 * v0 decryption is the same as v1. Enforce compatability by calling v1 decryption
 */
Error aesDecrypt(
    const std::vector<unsigned char>& in_data,
    const std::vector<unsigned char>& in_key,
    const std::vector<unsigned char>& in_iv,
    std::vector<unsigned char>& out_decrypted)
{
   return v1::aesDecrypt(in_data, in_key, in_iv, out_decrypted);
}

/**
 * v0 encryption is the same as v1. Enforce compatability by calling v1 encryption
 */
Error aesEncrypt(
    const std::vector<unsigned char>& data,
    const std::vector<unsigned char>& key,
    const std::vector<unsigned char>& iv,
    std::vector<unsigned char>& out_encrypted)
{
   return v1::aesEncrypt(data, key, iv, out_encrypted);
}

} // namespace v0

namespace v1 {

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
   EVP_CipherInit_ex(ctx, EVP_aes_128_cbc(), nullptr, &key[0], iv.empty() ? nullptr : &iv[0], s_encrypt);

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

} // namespace v1

namespace v2 {

Error aesDecrypt(
    const std::vector<unsigned char>& in_data,
    const std::vector<unsigned char>& in_key,
    const std::vector<unsigned char>& in_iv,
    const std::vector<unsigned char>& in_aad,
    std::vector<unsigned char>& in_mac,
    std::vector<unsigned char>& out_decrypted)
{
   out_decrypted.resize(in_data.size());
   int outLen = 0;
   int bytesDecrypted = 0;

   EVP_CIPHER_CTX *ctx;
   ctx = EVP_CIPHER_CTX_new();
   EVP_DecryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, &in_key[0], &in_iv[0]);

   // provide any AAD data
   if(!in_aad.empty())
   {
      if(!EVP_DecryptUpdate(ctx, nullptr, &outLen, &in_aad[0], gsl::narrow_cast<int>(in_aad.size())))
      {
         EVP_CIPHER_CTX_free(ctx);
         return getLastCryptoError(ERROR_LOCATION);
      }
   }

   // perform the decryption
   if(!EVP_DecryptUpdate(ctx, &out_decrypted[0], &outLen, &in_data[0], gsl::narrow_cast<int>(in_data.size())))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }
   bytesDecrypted += outLen;

   // Set expected tag value
   if(!EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_TAG, 16, &in_mac[0]))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }

   // perform final flush
   // A positive return value indicates success,
   // anything else is a failure - the plaintext is not trustworthy.
   if(EVP_DecryptFinal_ex(ctx, &out_decrypted[outLen], &outLen) < 0)
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
    const std::vector<unsigned char>& aad,
    std::vector<unsigned char>& out_mac,
    std::vector<unsigned char>& out_encrypted)
{
   // allow enough space in output buffer for additional block
   out_encrypted.resize(data.size() + EVP_MAX_BLOCK_LENGTH);

   // ensure enough room in mac buffer
   if (out_mac.size() != 2)
      out_mac.resize(2);

   int outlen = 0;
   int bytesEncrypted = 0;

   EVP_CIPHER_CTX *ctx;
   ctx = EVP_CIPHER_CTX_new();
   EVP_EncryptInit_ex(ctx, EVP_aes_128_cbc(), nullptr, &key[0], iv.empty() ? nullptr : &iv[0]);

   // provide any AAD data
   if (!aad.empty())
   {
      if(!EVP_EncryptUpdate(ctx, NULL, &outlen, &aad[0], gsl::narrow_cast<int>(aad.size())))
      {
         EVP_CIPHER_CTX_free(ctx);
         return getLastCryptoError(ERROR_LOCATION);
      }
   }

   // perform the encryption
   if(!EVP_EncryptUpdate(ctx, &out_encrypted[0], &outlen, &data[0], gsl::narrow_cast<int>(data.size())))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }
   bytesEncrypted += outlen;

   // perform final flush including left-over padding
   if(!EVP_EncryptFinal_ex(ctx, &out_encrypted[outlen], &outlen))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }
   bytesEncrypted += outlen;

   // get the tag
   if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_GET_TAG, 16, &out_mac[0]))

   EVP_CIPHER_CTX_free(ctx);

   // resize the container to the amount of actual bytes encrypted (including padding)
   out_encrypted.resize(bytesEncrypted);

   return Success();
}

} // namespace v2

} // namespace crypto
} // namespace system
} // namespace core
} // namespace rstudio
