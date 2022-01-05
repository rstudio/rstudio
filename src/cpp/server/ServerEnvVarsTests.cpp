/*
 * ServerEnvVars.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include "ServerEnvVars.hpp"

#include <core/system/Environment.hpp>
#include <core/FileSerializer.hpp>

#include <shared_core/FilePath.hpp>

#include <tests/TestThat.hpp>


using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace env_vars {

test_context("environment vars")
{
   test_that("environment variables are read")
   {
      // Create a directory to host temporary configuration
      FilePath configDir;
      FilePath::tempFilePath(configDir);
      configDir.ensureDirectory();

      // Set the directory as our config directory
      core::system::setenv("RSTUDIO_CONFIG_DIR", configDir.getAbsolutePath());

      // Create the env-vars file inside the directory
      appendToFile(configDir.completePath("env-vars"), R"(
# We are in a unit test
RSTUDIO_UNIT_TESTS=1

# Pineapple does not belong on pizza
PINEAPPLE_ON_PIZZA=no
)");

      // Initialize env vars (reads from file)
      initialize();

      // Ensure we got the values we expected
      expect_equal(core::system::getenv("RSTUDIO_UNIT_TESTS"), "1");
      expect_equal(core::system::getenv("PINEAPPLE_ON_PIZZA"), "no");

      // Clean up
      configDir.remove();
      core::system::unsetenv("RSTUDIO_CONFIG_DIR");
   }
}

} // namespace env_vars
} // namespace server
} // namespace rstudio
