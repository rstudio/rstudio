/*
 * TestRunner.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

// Include this file if you want to use Catch inside a
// custom built main. Call with `tests::run(argc, argv)`.

#ifndef TESTS_TESTRUNNER_HPP
#define TESTS_TESTRUNNER_HPP

#ifdef RSTUDIO_UNIT_TESTS_ENABLED

# include <gtest/gtest.h>

#endif

namespace rstudio {
namespace tests {

#ifdef RSTUDIO_UNIT_TESTS_ENABLED

int run()
{
   // pass some dummy arguments to gtest
   int argc = 1;
   
   // avoid deprecation warnings by initializing as const char*
   const char* argv[1] = { "gtest-unit-tests" };
   
   // Initialize Google Test and run all tests
   testing::InitGoogleTest(&argc, const_cast<char**>(argv));
   return RUN_ALL_TESTS();
}

#else // not RSTUDIO_UNIT_TESTS_ENABLED

int run()
{
   return -1;
}

#endif // end RSTUDIO_UNIT_TESTS_ENABLED

} // namespace tests
} // namespace rstudio

#endif

