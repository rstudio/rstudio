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

#include <gsl/gsl-lite.hpp>

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

// Forward declare v1 low-level functions for use by v0
namespace v1 {
Error aesDecrypt(
    const unsigned char* const in_data, const int data_len,
    const std::vector<unsigned char>& in_key,
    const std::vector<unsigned char>& in_iv,
    std::vector<unsigned char>& out_decrypted);

Error aesEncrypt(
    const unsigned char* const data, const int data_length,
    const std::vector<unsigned char>& key,
    const std::vector<unsigned char>& iv,
    int& out_bytes_encrypted,
    unsigned char* out_encrypted);
}

namespace v0 {

/**
 * v0 decryption is the same as v1. Enforce compatability by calling v1 decryption
 */
Error aesDecrypt(
    const std::vector<unsigned char>& in_v0_data,
    const std::vector<unsigned char>& in_key,
    const std::vector<unsigned char>& in_iv,
    std::vector<unsigned char>& out_decrypted)
{
   // Key size mismatches can silently read more data than intended. Prevent that upfront.
   if (in_key.size() < KEY_LENGTH_BYTES )
      throw EncryptionVersionMismatchException();

   out_decrypted.resize(gsl::narrow_cast<int>(in_v0_data.size()));

   return v1::aesDecrypt(in_v0_data.data(),
                         gsl::narrow_cast<int>(in_v0_data.size()),
                         in_key,
                         in_iv,
                         out_decrypted);
}

/**
 * v0 encryption is the same as v1. Enforce compatability by calling v1 encryption
 */
Error aesEncrypt(
    const std::vector<unsigned char>& data,
    const std::vector<unsigned char>& key,
    const std::vector<unsigned char>& iv,
    std::vector<unsigned char>& out_v0_encrypted)
{
   // Key size mismatches can silently read more data than intended. Prevent that upfront.
   if (key.size() < KEY_LENGTH_BYTES )
      throw EncryptionVersionMismatchException();

   int encryptedBytes = 0;
   // allow enough space in output buffer for version byte, data, and potential additional block
   out_v0_encrypted.resize(data.size() + EVP_MAX_BLOCK_LENGTH);

   Error result = v1::aesEncrypt(data.data(),
                                 gsl::narrow_cast<int>(data.size()),
                                 key,
                                 iv,
                                 encryptedBytes,
                                 out_v0_encrypted.data());

   out_v0_encrypted.resize(encryptedBytes);
   return result;
}

} // namespace v0

namespace v1 {
Error aesDecrypt(
   const unsigned char* const in_data, const int data_len,
   const std::vector<unsigned char>& in_key,
   const std::vector<unsigned char>& in_iv,
   std::vector<unsigned char>& out_decrypted)
{
   int outLen = 0;
   int bytesDecrypted = 0;

   EVP_CIPHER_CTX *ctx;
   ctx = EVP_CIPHER_CTX_new();
   EVP_CipherInit_ex(ctx, EVP_aes_128_cbc(), nullptr, in_key.data(), in_iv.data(), s_decrypt);

   // perform the decryption
   if(!EVP_CipherUpdate(ctx, out_decrypted.data(), &outLen, in_data, data_len))
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
    const unsigned char* const data, const int data_length,
    const std::vector<unsigned char>& key,
    const std::vector<unsigned char>& iv,
    int& out_bytes_encrypted,
    unsigned char* out_encrypted)
{
   int outlen = 0;

   EVP_CIPHER_CTX *ctx;
   ctx = EVP_CIPHER_CTX_new();
   EVP_CipherInit_ex(ctx, EVP_aes_128_cbc(), nullptr, key.data(), iv.empty() ? nullptr : iv.data(), s_encrypt);

   // perform the encryption
   if(!EVP_CipherUpdate(ctx, out_encrypted, &outlen, data, data_length))
   {
      EVP_CIPHER_CTX_free(ctx);
      out_bytes_encrypted = 0;
      return getLastCryptoError(ERROR_LOCATION);
   }
   out_bytes_encrypted += outlen;

   // perform final flush including left-over padding
   if(!EVP_CipherFinal_ex(ctx, &out_encrypted[outlen], &outlen))
   {
      EVP_CIPHER_CTX_free(ctx);
      out_bytes_encrypted = 0;
      return getLastCryptoError(ERROR_LOCATION);
   }
   out_bytes_encrypted += outlen;

   EVP_CIPHER_CTX_free(ctx);

   return Success();
}

Error aesDecrypt(
   const std::vector<unsigned char>& in_v1_data,
   const std::vector<unsigned char>& in_key,
   const std::vector<unsigned char>& in_iv,
   std::vector<unsigned char>& out_decrypted)
{
   // Key size mismatches can silently read more data than intended. Prevent that upfront.
   if (in_key.size() < KEY_LENGTH_BYTES )
      throw EncryptionVersionMismatchException();

   // Extract v1 byte info and only decrypt actual encrypted data:
   // [ version byte ][ v1 encrypted data ]
   // Use std::max to prevent index math going negative.
   out_decrypted.resize(std::max(in_v1_data.size() - ENCRYPTION_VERSION_SIZE_BYTES, (size_t)0));

   return aesDecrypt(&in_v1_data[ENCRYPTION_VERSION_SIZE_BYTES],
                     gsl::narrow_cast<int>(std::max(in_v1_data.size() - ENCRYPTION_VERSION_SIZE_BYTES, (size_t)0)),
                     in_key,
                     in_iv,
                     out_decrypted);
}

Error aesEncrypt(
    const std::vector<unsigned char>& data,
    const std::vector<unsigned char>& key,
    const std::vector<unsigned char>& iv,
    std::vector<unsigned char>& out_v1_encrypted)
{
   // Key size mismatches can silently read more data than intended. Prevent that upfront.
   if (key.size() < KEY_LENGTH_BYTES )
      throw EncryptionVersionMismatchException();

   int bytesEncrypted = 0;

   // allow enough space in output buffer for version byte, data, and potential additional block
   out_v1_encrypted.resize(data.size() + EVP_MAX_BLOCK_LENGTH + ENCRYPTION_VERSION_SIZE_BYTES);

   Error result = aesEncrypt(
       data.data(),
       gsl::narrow_cast<int>(data.size()),
       key,
       iv,
       bytesEncrypted,
       &out_v1_encrypted[ENCRYPTION_VERSION_SIZE_BYTES]);

   // resize the container to the version byte and amount of actual bytes encrypted (including padding)
   out_v1_encrypted.resize(bytesEncrypted + ENCRYPTION_VERSION_SIZE_BYTES);
   out_v1_encrypted[VERSION_BYTE_INDEX] = VERSION_BYTE;

   return result;
}

} // namespace v1

namespace v2 {

Error aesDecrypt(
    const unsigned char* const in_data, const int data_length,
    const std::vector<unsigned char>& in_key,
    const std::vector<unsigned char>& in_iv,
    const unsigned char* const in_aad, const int aad_length,
    unsigned char* const in_mac, const int mac_length,
    std::vector<unsigned char>& out_decrypted)
{
   int outLen = 0;
   int bytesDecrypted = 0;

   EVP_CIPHER_CTX *ctx;
   ctx = EVP_CIPHER_CTX_new();
   EVP_DecryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, in_key.data(), in_iv.data());

   // provide any AAD data
   if(aad_length > 0)
   {
      if(!EVP_DecryptUpdate(ctx, nullptr, &outLen, in_aad, aad_length))
      {
         EVP_CIPHER_CTX_free(ctx);
         return getLastCryptoError(ERROR_LOCATION);
      }
   }

   // perform the decryption
   if(!EVP_DecryptUpdate(ctx, out_decrypted.data(), &outLen, in_data, data_length))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }
   bytesDecrypted += outLen;

   // Set expected tag value
   if(!EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_TAG, mac_length, in_mac))
   {
      EVP_CIPHER_CTX_free(ctx);
      return getLastCryptoError(ERROR_LOCATION);
   }

   // Finalize encryption. Additional encryption data is not written here for GCM
   // A positive return value indicates success,
   // anything else is a failure - the plaintext is not trustworthy.
   if(EVP_DecryptFinal_ex(ctx, &out_decrypted[outLen - 1], &outLen) <= 0)
   {
      EVP_CIPHER_CTX_free(ctx);
      out_decrypted.resize(0);
      return getLastCryptoError(ERROR_LOCATION);
   }
   bytesDecrypted += outLen;

   EVP_CIPHER_CTX_free(ctx);

   // resize the container to the amount of actual bytes decrypted (padding is removed)
   out_decrypted.resize(bytesDecrypted);

   return Success();
}

Error aesEncrypt(
    const unsigned char* const data, const int data_length,
    const std::vector<unsigned char>& key,
    const std::vector<unsigned char>& iv,
    const unsigned char* const aad, const int aad_length,
    std::vector<unsigned char>& out_mac,
    int& out_bytesEncrypted,
    unsigned char* out_encrypted)
{
   int outlen = 0;

   EVP_CIPHER_CTX *ctx;
   ctx = EVP_CIPHER_CTX_new();
   EVP_EncryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, key.data(), iv.empty() ? nullptr : iv.data());

   // provide any AAD data
   if (aad_length > 0)
   {
      if(!EVP_EncryptUpdate(ctx, NULL, &outlen, aad, gsl::narrow_cast<int>(aad_length)))
      {
         EVP_CIPHER_CTX_free(ctx);
         out_bytesEncrypted = 0;
         return getLastCryptoError(ERROR_LOCATION);
      }
   }

   // perform the encryption
   if(!EVP_EncryptUpdate(ctx, out_encrypted, &outlen, data, data_length))
   {
      EVP_CIPHER_CTX_free(ctx);
      out_bytesEncrypted = 0;
      return getLastCryptoError(ERROR_LOCATION);
   }
   out_bytesEncrypted += outlen;

   // perform final flush including left-over padding
   if(!EVP_EncryptFinal_ex(ctx, &out_encrypted[outlen], &outlen))
   {
      EVP_CIPHER_CTX_free(ctx);
      out_bytesEncrypted = 0;
      return getLastCryptoError(ERROR_LOCATION);
   }
   out_bytesEncrypted += outlen;

   // get the tag
   if (1 != EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_GET_TAG, MAC_SIZE_BYTES, out_mac.data()))
   {
      EVP_CIPHER_CTX_free(ctx);
      out_bytesEncrypted = 0;
      return getLastCryptoError(ERROR_LOCATION);
   }

   EVP_CIPHER_CTX_free(ctx);

   return Success();
}

Error aesDecrypt(
    const std::vector<unsigned char>& in_v2_data,
    const std::vector<unsigned char>& in_key,
    const std::vector<unsigned char>& in_iv,
    std::vector<unsigned char>& out_decrypted)
{
   // Key size mismatches can silently read more data than intended. Prevent that upfront.
   if (in_key.size() < KEY_LENGTH_BYTES )
      throw EncryptionVersionMismatchException();

   // Index maths. v2 buffer structure is: [ version byte ][ v2 encrypted data ][ mac ]
   // Use std::max to prevent index math going negative.
   auto dataLength = std::max(in_v2_data.size() - ENCRYPTION_VERSION_SIZE_BYTES - MAC_SIZE_BYTES, (size_t)0);
   auto macIndex = ENCRYPTION_VERSION_SIZE_BYTES + dataLength;

   out_decrypted.resize(dataLength);

   // For some reason the decryption doesn't like const data for the mac
   // Copy it out of the original v2 buffer to remove the const
   std::vector<unsigned char> mac(in_v2_data.begin() + macIndex, in_v2_data.end());

   // Decrypt just the data
   return aesDecrypt(&in_v2_data[ENCRYPTION_VERSION_SIZE_BYTES],
                     gsl::narrow_cast<int>(dataLength),
                     in_key,
                     in_iv,
                     &in_v2_data[VERSION_BYTE_INDEX],
                     ENCRYPTION_VERSION_SIZE_BYTES,
                     mac.data(),
                     MAC_SIZE_BYTES,
                     out_decrypted);
}

Error aesEncrypt(
    const std::vector<unsigned char>& data,
    const std::vector<unsigned char>& key,
    const std::vector<unsigned char>& iv,
    std::vector<unsigned char>& out_v2_encrypted)
{
   // Key size mismatches can silently read more data than intended. Prevent that upfront.
   if (key.size() < KEY_LENGTH_BYTES )
      throw EncryptionVersionMismatchException();

   int bytesEncrypted = 0;

   // allow enough space in output buffer for data and potential additional block
   out_v2_encrypted.resize(data.size() + EVP_MAX_BLOCK_LENGTH + ENCRYPTION_VERSION_SIZE_BYTES + MAC_SIZE_BYTES);
   std::vector <unsigned char> mac(MAC_SIZE_BYTES);

   // Encrypt the data
   Error result = aesEncrypt(
       data.data(),
       gsl::narrow_cast<int>(data.size()),
       key,
       iv,
       &VERSION_BYTE,
       ENCRYPTION_VERSION_SIZE_BYTES,
       mac,
       bytesEncrypted,
       &out_v2_encrypted[ENCRYPTION_VERSION_SIZE_BYTES]);

   // Add v2 metadata
   if (result == Success())
   {
      // resize the container to the amount of actual bytes encrypted (including padding)
      // plus any
      out_v2_encrypted.resize(ENCRYPTION_VERSION_SIZE_BYTES + bytesEncrypted + MAC_SIZE_BYTES);

      // Add the AAD
      out_v2_encrypted[VERSION_BYTE_INDEX] = VERSION_BYTE;

      // Add the MAC
      std::copy(mac.begin(), mac.end(), out_v2_encrypted.begin() + ENCRYPTION_VERSION_SIZE_BYTES + bytesEncrypted);
   }
   else
   {
      out_v2_encrypted.resize(0);
   }

   return result;

}

} // namespace v2

} // namespace crypto
} // namespace system
} // namespace core
} // namespace rstudio
