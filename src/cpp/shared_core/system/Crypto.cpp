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

Error getLastCryptoError(const ErrorLocation& in_location)
{
   // get the error code
   unsigned long ec = ::ERR_get_error();
   if (ec == 0)
   {
      log::logWarningMessage("getLastCrytpoError called with no pending error");
      return systemError(
         boost::system::errc::not_supported,
         "lastCrytpoError called with no pending error",
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

Error base64Encode(const std::vector<unsigned char>& in_data, std::string& out_encoded)
{
   return base64Encode(&in_data[0], static_cast<int>(in_data.size()), out_encoded);
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
   int bytesRead = ::BIO_read(pMem, &buffer[0], static_cast<int>(buffer.capacity()));
   if (bytesRead < 0 && ::ERR_get_error() != 0)
      return getLastCryptoError(ERROR_LOCATION);

   // copy to out param
   buffer.resize(bytesRead);
   out_encoded.assign(buffer.begin(), buffer.end());

   // return success
   return Success();
}


Error base64Decode(const std::string in_data, std::vector<unsigned char>& out_decoded)
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
   BIO* pMem = BIO_new_mem_buf((void*)in_data.data(), static_cast<int>(in_data.length()));
   if (pMem == nullptr)
      return getLastCryptoError(ERROR_LOCATION);

   // tie the stream to the b64 stream
   pB64 = ::BIO_push(pB64, pMem);

   // reserve adequate memory in the decoded buffer and read into it
   out_decoded.clear();
   out_decoded.resize(in_data.length());
   int bytesRead = ::BIO_read(pB64,
                              &(out_decoded[0]),
                              static_cast<int>(out_decoded.size()));
   if (bytesRead < 0)
      return getLastCryptoError(ERROR_LOCATION);

   // resize the out buffer to the number of bytes actually read
   out_decoded.resize(bytesRead);

   // return success
   return Success();

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
