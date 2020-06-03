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

Error getLastCryptoError(const ErrorLocation& in_location);

/**
 * @brief Base-64 decodes a string.
 *
 * @param in_data       The base-64 encoded data to be decoded.
 * @param out_decoded   The decoded data.
 *
 * @return Success if the data could be base-64 decoded; Error otherwise.
 */
Error base64Decode(const std::string in_data, std::vector<unsigned char>& out_decoded);

/**
 * @brief Base-64 decodes a string.
 *
 * @param in_data       The base-64 encoded data to be decoded.
 * @param out_decoded   The decoded data.
 *
 * @return Success if the data could be base-64 decoded; Error otherwise.
 */
Error base64Decode(const std::string in_data, std::string& out_decoded);

/**
 * @brief Base 64 encodes a string.
 *
 * @param in_data       The string data to be encoded.
 * @param out_encoded   The base 64 encoded string.
 *
 * @return Success if the data could be base 64 encoded; Error otherwise.
 */
Error base64Encode(const std::vector<unsigned char>& in_data, std::string& out_encoded);

/**
 * @brief Base-64 encodes a string.
 *
 * @param in_data       The string data to be encoded.
 * @param in_length     The length of in_data.
 * @param out_encoded   The base-64 encoded string.
 *
 * @return Success if the data could be base-64 encoded; Error otherwise.
 */
Error base64Encode(const unsigned char* in_data, int in_length, std::string& out_encoded);

/**
 * @brief Generates random bytes of the specified length.
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
