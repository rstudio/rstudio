/*
 * EnvironmentTests.cpp
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

#include <tests/TestThat.hpp>

#include <core/system/Environment.hpp>

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

} // end namespace system
} // end namespace core
} // end namespace rstudio
