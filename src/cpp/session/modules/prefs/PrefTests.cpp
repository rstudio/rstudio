/*
 * UserPrefTests.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#include "UserStateDefaultLayer.hpp"
#include "UserPrefsDefaultLayer.hpp"
#include "UserState.hpp"
#include "UserPrefs.hpp"

#include <session/SessionOptions.hpp>

#include <tests/TestThat.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace tests {

test_context("default validation")
{
   test_that("user preference defaults are valid according to their schema")
   {
      UserPrefsDefaultLayer defaults;
      Error error = defaults.readPrefs();
      expect_true(error == Success());

      error = defaults.validatePrefsFromSchema(
         options().rResourcesPath().complete("schema").complete(kUserPrefsSchemaFile));
      INFO(error.description());
      expect_true(error == Success());
   }

   test_that("user state defaults are valid according to their schema")
   {
      UserStateDefaultLayer defaults;
      Error error = defaults.readPrefs();
      expect_true(error == Success());

      error = defaults.validatePrefsFromSchema(
         options().rResourcesPath().complete("schema").complete(kUserStateSchemaFile));
      INFO(error.description());
      expect_true(error == Success());
   }
}

} // namespace tests
} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

