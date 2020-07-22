/*
 * Crypto.hpp
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

#ifndef CORE_SYSTEM_CRYPTO_HPP
#define CORE_SYSTEM_CRYPTO_HPP

#include <string>
#include <vector>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/system/Crypto.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace crypto {
      
void initialize();

core::Error HMAC_SHA2(const std::string& data,
                      const std::string& key,
                      std::vector<unsigned char>* pHMAC);

core::Error HMAC_SHA2(const std::string& data,
                      const std::vector<unsigned char>& key,
                      std::vector<unsigned char>* pHMAC);

core::Error sha256(const std::string& message,
                   std::string* pHash);

core::Error rsaInit();

core::Error rsaSign(const std::string& message,
                    const std::string& pemPrivateKey,
                    std::string* pOutSignature);

core::Error rsaVerify(const std::string& message,
                      const std::string& signature,
                      const std::string& pemPublicKey);

core::Error generateRsaKeyPair(std::string* pOutPublicKey,
                               std::string* pOutPrivateKey);

core::Error generateRsaKeyFiles(const FilePath& publicKeyPath,
                                const FilePath& privateKeyPath);

void rsaPublicKey(std::string* pExponent, std::string* pModulo);

core::Error rsaPrivateDecrypt(const std::string& pCipherText, std::string* pPlainText);
         
} // namespace crypto
} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_CRYPTO_HPP

