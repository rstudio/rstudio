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

#include <tests/TestThat.hpp>

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

test_context("EncryptionConfigurationTests")
{
   test_that("Can get/set encryption min/max versions")
   {
      // Set to defaults
      setToDefaults();
      REQUIRE(crypto::getMinimumEncryptionVersion() == 0);
      REQUIRE(crypto::getMaximumEncryptionVersion() == 0);

      // Set to some value
      crypto::setMinimumEncryptionVersion(1);
      crypto::setMaximumEncryptionVersion(2);
      REQUIRE(crypto::getMinimumEncryptionVersion() == 1);
      REQUIRE(crypto::getMaximumEncryptionVersion() == 2);

      setToDefaults();
   }

   test_that("Env vars control encryption min/max versions")
   {
      // Unset any environment variable values
      core::system::setenv(kEncryptionMinimumVersionEnvVar, std::string());
      core::system::setenv(kEncryptionMaximumVersionEnvVar, std::string());

      // Set to defaults
      setToDefaults();
      REQUIRE(crypto::getMinimumEncryptionVersion() == 0);
      REQUIRE(crypto::getMaximumEncryptionVersion() == 0);

      // Set environment variable values
      core::system::setenv(kEncryptionMinimumVersionEnvVar, "1");
      core::system::setenv(kEncryptionMaximumVersionEnvVar, "2");

      // Load env vars into config
      crypto::encryption::initialize();
      REQUIRE(crypto::getMinimumEncryptionVersion() == 1);
      REQUIRE(crypto::getMaximumEncryptionVersion() == 2);

      // Set back to defaults
      setToDefaults();
   }

   test_that("isDecryptionAllowed respects min encryption version")
   {
      // Set to defaults
      setToDefaults();

      crypto::setMinimumEncryptionVersion(1);

      REQUIRE(crypto::isDecryptionVersionAllowed(0) != Success());
      REQUIRE(crypto::isDecryptionVersionAllowed(1) == Success());
      REQUIRE(crypto::isDecryptionVersionAllowed(2) == Success());

      // Set back to defaults
      setToDefaults();
   }
}

} // end namespace tests
} // end namespace encryption
} // end namespace crypto
} // end namespace system
} // end namespace core
} // end namespace rstudio
