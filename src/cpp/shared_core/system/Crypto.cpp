/*
 * Crypto.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <shared_core/system/Crypto.hpp>
#include <shared_core/system/encryption/EncryptionConfiguration.hpp>
#include <shared_core/system/encryption/EncryptionVersion.hpp>

#include <gsl/gsl-lite.hpp>

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

// Forward Declaration. Defined in EncryptionVersion.cpp
Error getLastCryptoError(const ErrorLocation& in_location);

Error aesDecrypt(
   const std::vector<unsigned char>& in_data,
   const std::vector<unsigned char>& in_key,
   const std::vector<unsigned char>& in_iv,
   std::vector<unsigned char>& out_decrypted)
{
   // Attempt versioned decryption. If unsuccessful, default to v0 decryption
   // Decrypting with incompatible buffer or key sizes can cause segfaults.
   // Wrap in a try/catch block to gracefully handle version decryption mismatches
   Error result;
   try
   {
      if (in_data.front() == v2::VERSION_BYTE)
      {
         if ((result = v2::aesDecrypt(in_data, in_key, in_iv, out_decrypted)) == Success())
            return result;
         else
            throw EncryptionVersionMismatchException();
      }
      else if (in_data.front() == v1::VERSION_BYTE)
      {
         if ((result = isDecryptionVersionAllowed(v1::VERSION_BYTE)) != Success())
            return result;
         if ((result = v1::aesDecrypt(in_data, in_key, in_iv, out_decrypted)) == Success())
            return result;
         else
            throw EncryptionVersionMismatchException();
      }
   }
   catch(...)
   {
      log::logDebugMessage("Failed to decrypt versioned encryption. Attempting v0 decryption.");
   }

   if ((result = isDecryptionVersionAllowed(v0::VERSION_BYTE)) != Success())
      return result;

   return v0::aesDecrypt(in_data, in_key, in_iv, out_decrypted);
}

Error aesEncrypt(
    const std::vector<unsigned char>& data,
    const std::vector<unsigned char>& key,
    std::vector<unsigned char>& out_encrypted)
{
   return aesEncrypt(data, key, {}, out_encrypted);
}

Error aesEncrypt(
   const std::vector<unsigned char>& data,
   const std::vector<unsigned char>& key,
   const std::vector<unsigned char>& iv,
   std::vector<unsigned char>& out_encrypted)
{
   // Encrypt with the highest version allowed
   switch (getMaximumEncryptionVersion())
   {
      case v0::VERSION_BYTE:
         return v0::aesEncrypt(data, key, iv, out_encrypted);
         break;
      case v1::VERSION_BYTE:
         return v1::aesEncrypt(data, key, iv, out_encrypted);
         break;
      case v2::VERSION_BYTE:
         return v2::aesEncrypt(data, key, iv, out_encrypted);
         break;
      default:
         return Error(boost::system::errc::invalid_argument,
                      "Encryption version error: Unrecognized maximum version value: " + std::to_string(getMaximumEncryptionVersion()),
                      ERROR_LOCATION);
         break;
   }
}

Error base64Encode(const std::vector<unsigned char>& in_data, std::string& out_encoded)
{
   return base64Encode(in_data.data(), gsl::narrow_cast<int>(in_data.size()), out_encoded);
}

Error base64Encode(const unsigned char* in_data, int in_length, std::string& out_encoded)
{
   // this crashes if passed an empty vector so avoid that altogether
   if (in_length == 0)
   {
      out_encoded = "";
      return Success();
   }
   
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
   // this crashes if passed an empty string so avoid that altogether
   if (in_data.length() == 0)
      return Success();

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

   // convert the decrypted bytes into the original string
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
