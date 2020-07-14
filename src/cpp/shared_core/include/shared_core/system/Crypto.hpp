/*
 * Crypto.hpp
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

#ifndef SHARED_CORE_SYSTEM_CRYPTO_HPP
#define SHARED_CORE_SYSTEM_CRYPTO_HPP

#include <string>
#include <vector>

namespace rstudio {
namespace core {

class Error;
class ErrorLocation;

} // namespace core
} // namespace rstudio

namespace rstudio {
namespace core {
namespace system {
namespace crypto {

/**
 * @file
 * Cryptographic Utilities.
 */

Error getLastCryptoError(const ErrorLocation& in_location);

/**
 * @brief AES decrypts the specified data using the specified initialization vector.
 *
 * This function is the inverse of aesEncrypt.
 *
 * @param in_data           The data to be decrypted.
 * @param in_key            The key with which to decrypt the data.
 * @param in_iv             The initialization vector that was used during encryption.
 * @param out_decrypted     The decrypted data.
 *
 * @return Success if the data could be AES decrypted; Error otherwise.
 */
Error aesDecrypt(
   const std::vector<unsigned char>& in_data,
   const std::vector<unsigned char>& in_key,
   const std::vector<unsigned char>& in_iv,
   std::vector<unsigned char>& out_decrypted);

/**
 * @brief AES encrypts the specified data using the specified initialization vector.
 *
 * This function is the inverse of aesDecrypt.
 *
 * @param in_data           The data to be encrypted.
 * @param in_key            The key with which to encrypt the data.
 * @param in_iv             The initialization vector to use during encryption.
 * @param out_encrypted     The encrypted data.
 *
 * @return Success if the data could be AES encrypted; Error otherwise.
 */
Error aesEncrypt(
   const std::vector<unsigned char>& in_data,
   const std::vector<unsigned char>& in_key,
   const std::vector<unsigned char>& in_iv,
   std::vector<unsigned char>& out_encrypted);

/**
 * @brief Base-64 decodes a string.
 *
 * This function is the inverse of base64Encode.
 *
 * @param in_data       The base-64 encoded data to be decoded.
 * @param out_decoded   The decoded data.
 *
 * @return Success if the data could be base-64 decoded; Error otherwise.
 */
Error base64Decode(const std::string& in_data, std::vector<unsigned char>& out_decoded);

/**
 * @brief Base-64 decodes a string.
 *
 * This function is the inverse of base64Encode.
 *
 * @param in_data       The base-64 encoded data to be decoded.
 * @param out_decoded   The decoded data.
 *
 * @return Success if the data could be base-64 decoded; Error otherwise.
 */
Error base64Decode(const std::string& in_data, std::string& out_decoded);

/**
 * @brief Base-64 encodes a string.
 *
 * This function is the inverse of base64Decode.
 *
 * @param in_data       The string data to be encoded.
 * @param out_encoded   The base-64 encoded string.
 *
 * @return Success if the data could be base-64 encoded; Error otherwise.
 */
Error base64Encode(const std::vector<unsigned char>& in_data, std::string& out_encoded);

/**
 * @brief Base-64 encodes a string.
 *
 * This function is the inverse of base64Decode.
 *
 * @param in_data       The string data to be encoded.
 * @param in_length     The length of in_data.
 * @param out_encoded   The base-64 encoded string.
 *
 * @return Success if the data could be base-64 encoded; Error otherwise.
 */
Error base64Encode(const unsigned char* in_data, int in_length, std::string& out_encoded);

/**
 * @brief Base-64 decodes and then decrypts an AES encrypted string with the specified initialization vector, which is
 *        also base-64 encoded.
 *
 * This function is the inverse of encryptAndBase64Encode.
 *
 * @param in_input          The base-64 encoded AES encrypted string.
 * @param in_key            The key with which to decrypt the string.
 * @param in_ivStr          The base-64 encrypted initialization vector.
 * @param out_decrypted     The base-64 decoded and decrypted string.
 *
 * @return Success if in_input could be base-64 decoded and decrypted; Error otherwise.
 */
Error decryptAndBase64Decode(
   const std::string& in_input,
   const std::string& in_key,
   const std::string& in_ivStr,
   std::string& out_decrypted);

/**
 * @brief AES encrypts and then base-64 encodes the specified string using the given key. Also generates and base-64
 *        encodes an initialization vector which is used in the encryption of the input.
 *
 * This function is the inverse of decryptAndBase64Decode.
 * 
 * @param in_input          The string to encrypt and base-64 encode.
 * @param in_key            The key with which to encrypt the string.
 * @param out_iv            The generated base-64 encoded initialization vector.
 * @param out_encrypted     The encrypted and base-64 encoded string.
 *
 * @return Success if the string could be encrypted and base-64 encoded; Error otherwise.
 */
Error encryptAndBase64Encode(
   const std::string& in_input,
   const std::string& in_key,
   std::string& out_iv,
   std::string& out_encrypted);

/**
 * @brief Generates random bytes of the specified length.
 *
 * This function uses openSSL to generate random data. Summarized from the openSSL documentation:
 * The bytes are generated using a cryptographically secure pseudo random generator. The quality of the randomness is
 * determined by the operating system's entropy source. If an entropy source fails or isn't available, an error will be
 * returned.
 *
 * @param in_length         The number of bytes of random data to generate.
 * @param out_randomData    The random data.
 *
 * @return Success if the random data could be generated; Error otherwise.
 */
Error random(uint32_t in_length, std::vector<unsigned char>& out_randomData);

} // namespace crypto
} // namespace system
} // namespace core
} // namespace rstudio

#endif
