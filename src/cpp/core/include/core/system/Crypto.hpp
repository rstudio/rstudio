/*
 * Crypto.hpp
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

#ifndef CORE_SYSTEM_CRYPTO_HPP
#define CORE_SYSTEM_CRYPTO_HPP

#include <string>
#include <vector>

#include <core/Error.hpp>

namespace core {
namespace system {
namespace crypto {
      
void initialize();   

core::Error HMAC_SHA1(const std::string& data, 
                      const std::string& key,
                      std::vector<unsigned char>* pHMAC);

core::Error HMAC_SHA1(const std::string& data, 
                      const std::vector<unsigned char>& key,
                      std::vector<unsigned char>* pHMAC);   
   
core::Error base64Encode(const std::vector<unsigned char>& data, 
                         std::string* pEncoded);   
   
core::Error base64Encode(const unsigned char* pData, 
                         int len, 
                         std::string* pEncoded);
   
core::Error base64Decode(const std::string& data, 
                         std::vector<unsigned char>* pDecoded);

core::Error rsaInit();

void rsaPublicKey(std::string* pExponent, std::string* pModulo);

core::Error rsaPrivateDecrypt(const std::string& pCipherText, std::string* pPlainText);

         
} // namespace crypto
} // namespace system
} // namespace core

#endif // CORE_SYSTEM_CRYPTO_HPP

