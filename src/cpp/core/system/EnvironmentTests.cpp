/*
 * EnvironmentTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <tests/TestThat.hpp>

#include <shared_core/Error.hpp>

#include <core/system/Environment.hpp>
#include <core/system/Resources.hpp>

#define kLatexStyleLineCommentRegex ("^%+\\s*")

namespace rstudio {
namespace core {
namespace system {

test_context("Environment expansion")
{
   test_that("All instances of a variable are expanded")
   {
      // Simple example
      Options env;
      setenv(&env, "VAR1", "foo");
      setenv(&env, "VAR2", "bar");
      setenv(&env, "VAR3", "baz");

      std::string expanded = expandEnvVars(env,
         "Metasyntactic variables include $VAR1, $VAR2, and $VAR3, "
         "but $VAR1 is used most often.");

      expect_equal(expanded, "Metasyntactic variables include foo, bar, and baz, "
                   "but foo is used most often.");
   }

   test_that("Variables names are only replaced when fully matching")
   {
      Options env;
      setenv(&env, "VAR", "foo");

      std::string expanded = expandEnvVars(env,
          "I think $VAR is a nice name for a $VARIABLE.");

      expect_equal(expanded, "I think foo is a nice name for a $VARIABLE.");
   }

   test_that("Variables are expanded in braces")
   {
      Options env;
      setenv(&env, "VAR", "foo");

      std::string expanded = expandEnvVars(env, "Don't be ${VAR}lish or ${VAR}lhardy.");

      expect_equal(expanded, "Don't be foolish or foolhardy.");
   }
}

test_context("Resources")
{
   test_that("All resource usage metrics are nonzero")
   {
      // Used memory should be nonzero
      long kb = 0;
      MemoryProvider provider = MemoryProviderUnknown;
      Error error = getTotalMemoryUsed(&kb, &provider);
      expect_false(error);
      expect_true(kb > 0);
      expect_true(provider != MemoryProviderUnknown);

      // Process used memory should be nonzero
      kb = 0;
      provider = MemoryProviderUnknown;
      error = getProcessMemoryUsed(&kb, &provider);
      expect_false(error);
      expect_true(kb > 0);
      expect_true(provider != MemoryProviderUnknown);

      // Total memory should be nonzero
      kb = 0;
      provider = MemoryProviderUnknown;
      error = getTotalMemory(&kb, &provider);
      expect_false(error);
      expect_true(kb > 0);
      expect_true(provider != MemoryProviderUnknown);
   }

   test_that("Memory resource usage metrics are congruent")
   {
      long used, process, total;
      MemoryProvider provider;
      getTotalMemoryUsed(&used, &provider);
      getProcessMemoryUsed(&process, &provider);
      getTotalMemory(&total, &provider);

      // It'd be weird if there was more used memory than we had memory in the
      // first place
      expect_true(total > used);

      // It'd also be weird if the process used more memory than the total
      // amount of used memory
      expect_true(used > process);
   }
}

} // end namespace system
} // end namespace core
} // end namespace rstudio
