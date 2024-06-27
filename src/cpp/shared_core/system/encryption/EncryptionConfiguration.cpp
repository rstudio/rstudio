/*
 * EncryptionConfiguration.cpp
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

#include <shared_core/system/encryption/EncryptionConfiguration.hpp>
#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace crypto {

namespace {

int minimumEncryptionVersion = defaultMinimumEncryptionVersion;
int maximumEncryptionVersion = defaultMaximumEncryptionVersion;

} // anonymous namespace

Error isDecryptionVersionAllowed(int version)
{
   if (version < getMinimumEncryptionVersion())
      return Error(boost::system::errc::invalid_argument,
                   "Encryption version error: Attempted v" + std::to_string(version) + " decryption when minimum encryption version is: v" + std::to_string(getMinimumEncryptionVersion()),
                   ERROR_LOCATION);

   return Success();
}

int getMinimumEncryptionVersion()
{
   return minimumEncryptionVersion;
}

int getMaximumEncryptionVersion()
{
   return maximumEncryptionVersion;
}

void setMinimumEncryptionVersion(int min)
{
   minimumEncryptionVersion = min;
}

void setMaximumEncryptionVersion(int max)
{
   maximumEncryptionVersion = max;
}

} // crypto
} // system
} // core
} // rstudio
