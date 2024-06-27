/*
 * EncryptionConfiguration.hpp
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

#ifndef SHARED_CORE_SYSTEM_ENCRYPTION_CONFIGURATION_HPP
#define SHARED_CORE_SYSTEM_ENCRYPTION_CONFIGURATION_HPP

namespace rstudio {
namespace core {

// Forward Declaration
class Error;

namespace system {
namespace crypto {

const int defaultMinimumEncryptionVersion = 0;
const int defaultMaximumEncryptionVersion = 0;

/**
 * @brief Helper function to check *minimum* allowed encryption version set by the
 * configuration and determine if a given version should be used
 *
 * @param version The encryption version to check if it's allowed for decryption
 *
 * @return Success if the configuration allows the supplied version, Error otherwise
 */
core::Error isDecryptionVersionAllowed(int version);

/**
 * @brief Gets the minimum encryption version, as set by the environment variable
 * POSIT_MINIMUM_ENCRYPTION_VERSION, or the default if the env var isn't set
 *
 * The higher the minimum encryption version, the stronger the encryption algorithm.
 * See EncryptionVersion.hpp for a table mapping versions to encryption algorithms.
 *
 * @return The minimum encryption version allowed to use.
 */
int getMinimumEncryptionVersion();

/**
 * @brief Gets the maximum encryption version, as set by the environment variable
 * POSIT_MAXIMUM_ENCRYPTION_VERSION, or the default if the env var isn't set
 *
 * The lower the maximum encryption version, the more backwards compatable the encryption
 * is with previous releases.
 * See EncryptionVersion.hpp for a table mapping versions to encryption algorithms.
 *
 * @return The maximum encryption version allowed to use.
 */
int getMaximumEncryptionVersion();

/**
 * @brief Sets the minimum encryption version. If used, this should be called at the
 * beginning of a process' startup (with core::system::crypto::encryption::initialize()),
 * and then not called again.
 *
 * Valid version values can be found in the table in EncryptionVersion.hpp
 *
 * @param min The minimum encryption version to use
 */
void setMinimumEncryptionVersion(int min);

/**
 * @brief Sets the maximum encryption version. If used, this should be called at the
 * beginning of a process' startup (with core::system::crypto::encryption::initialize()),
 * and then not called again.
 *
 * Valid version values can be found in the table in EncryptionVersion.hpp
 *
 * @param max The maximum encryption version to use
 */
void setMaximumEncryptionVersion(int max);

} // crypto
} // system
} // core
} // rstudio

#endif // SHARED_CORE_SYSTEM_ENCRYPTION_CONFIGURATION_HPP
