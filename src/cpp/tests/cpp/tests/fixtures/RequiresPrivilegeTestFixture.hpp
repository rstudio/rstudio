/*
 * RequiresPrivilegeTestFixture.hpp
 *
 * Copyright (C) 2023 by Posit Software, PBC
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

#ifndef TESTS_FIXTURES_REQUIRES_PRIVILEGE_TEST_FIXTURE_HPP
#define TESTS_FIXTURES_REQUIRES_PRIVILEGE_TEST_FIXTURE_HPP

#include <gtest/gtest.h>
#include <core/system/System.hpp>

namespace rstudio {
namespace tests {
namespace fixtures {

/**
 * Base test fixture that requires root privileges.
 * Simply skips the test if not running as root.
 */
class RequiresPrivilegeTestFixture : public ::testing::Test
{
protected:
   void SetUp() override
   {
      // Skip tests if not running as root
      if (!core::system::effectiveUserIsRoot())
         GTEST_SKIP() << "Test requires root privileges";
   }
};

} // namespace fixtures
} // namespace tests
} // namespace rstudio

#endif // TESTS_FIXTURES_REQUIRES_PRIVILEGE_TEST_FIXTURE_HPP