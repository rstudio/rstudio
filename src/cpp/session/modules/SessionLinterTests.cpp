/*
 * SessionLinterTests.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include "SessionLinter.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace linter {

// We use macros so that the test output gives
// meaningful line numbers.
#define EXPECT_HAS_NO_LINT(__STRING__)                                         \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__);                                \
      expect_true(results.second.get().empty());                               \
   } while (0)

#define EXPECT_HAS_LINT(__STRING__)                                            \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__);                                \
      expect_false(results.second.get().empty());                              \
   } while (0)


context("Linter")
{
   test_that("valid expressions generate no lint")
   {
      EXPECT_HAS_NO_LINT("print(1)");
      EXPECT_HAS_NO_LINT("1 + 1");
      EXPECT_HAS_NO_LINT("{{{}}}");
      EXPECT_HAS_NO_LINT("for (i in 1) 1");
      EXPECT_HAS_NO_LINT("(for (i in 1:10) i)");
      EXPECT_HAS_NO_LINT("for (i in 10) {}");
      EXPECT_HAS_NO_LINT("while ((for (i in 10) {})) 1");
      EXPECT_HAS_NO_LINT("while (for (i in 0) 0) 0");
      EXPECT_HAS_NO_LINT("1; 2; 3; 4; 5");
      
      EXPECT_HAS_LINT("{)");
      
   }
}

} // namespace linter
} // namespace modules
} // namespace session
} // namespace rstudio
