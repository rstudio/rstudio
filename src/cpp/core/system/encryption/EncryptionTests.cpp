/*
 * EncryptionTests.cpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/system/Environment.hpp>
#include <core/system/encryption/Encryption.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/system/encryption/EncryptionConfiguration.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace system {
namespace crypto {
namespace encryption {
namespace tests {

void setToDefaults()
{
   crypto::setMinimumEncryptionVersion(0);
   crypto::setMaximumEncryptionVersion(0);
}

TEST(EncryptionTest, CanGetSetEncryptionMinMaxVersions)
{
   // Set to defaults
   setToDefaults();
   ASSERT_EQ(0, crypto::getMinimumEncryptionVersion());
   ASSERT_EQ(0, crypto::getMaximumEncryptionVersion());

   // Set to some value
   crypto::setMinimumEncryptionVersion(1);
   crypto::setMaximumEncryptionVersion(2);
   ASSERT_EQ(1, crypto::getMinimumEncryptionVersion());
   ASSERT_EQ(2, crypto::getMaximumEncryptionVersion());

   setToDefaults();
}



TEST(EncryptionTest, EnvVarsControlEncryptionMinMaxVersions)
{
   // Unset any environment variable values
   core::system::setenv(kEncryptionMinimumVersionEnvVar, std::string());
   core::system::setenv(kEncryptionMaximumVersionEnvVar, std::string());

   // Set to defaults
   setToDefaults();
   ASSERT_EQ(0, crypto::getMinimumEncryptionVersion());
   ASSERT_EQ(0, crypto::getMaximumEncryptionVersion());

   // Set environment variable values
   core::system::setenv(kEncryptionMinimumVersionEnvVar, "1");
   core::system::setenv(kEncryptionMaximumVersionEnvVar, "2");

   // Load env vars into config
   crypto::encryption::initialize();
   ASSERT_EQ(1, crypto::getMinimumEncryptionVersion());
   ASSERT_EQ(2, crypto::getMaximumEncryptionVersion());

   // Set back to defaults
   setToDefaults();
}

TEST(EncryptionTest, IsDecryptionAllowedRespectsMinEncryptionVersion)
{
   // Set to defaults
   setToDefaults();

   crypto::setMinimumEncryptionVersion(1);

   ASSERT_NE(crypto::isDecryptionVersionAllowed(0), Success());
   ASSERT_EQ(crypto::isDecryptionVersionAllowed(1), Success());
   ASSERT_EQ(crypto::isDecryptionVersionAllowed(2), Success());

   // Set back to defaults
   setToDefaults();
}

} // namespace tests
} // namespace encryption
} // namespace crypto
} // namespace system
} // namespace core
} // namespace rstudio
