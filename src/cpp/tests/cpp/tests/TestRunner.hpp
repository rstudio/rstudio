/*
 * TestRunner.hpp
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

// Include this file if you want to use Catch inside a
// custom built main. Call with `tests::run(argc, argv)`.

#ifndef TESTS_TESTRUNNER_HPP
#define TESTS_TESTRUNNER_HPP

#ifdef RSTUDIO_UNIT_TESTS_ENABLED

# define CATCH_CONFIG_RUNNER
# include "vendor/catch.hpp"

#endif

namespace rstudio {
namespace tests {

#ifdef RSTUDIO_UNIT_TESTS_ENABLED

int run()
{
   int argc = 1;
   char* argv[1];
   argv[0] = "tests";
   return Catch::Session().run(argc, argv);
}

#else // not RSTUDIO_UNIT_TESTS_ENABLED

int run(int argc, char* const argv[])
{
}

#endif // end RSTUDIO_UNIT_TESTS_ENABLED

} // namespace tests
} // namespace rstudio

#endif

