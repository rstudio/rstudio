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

namespace rstudio {
namespace tests {

bool enabled(int argc, char* const argv[])
{
   return argc > 1 && strcmp(argv[1], "--test") == 0;
}

// use Catch to run tests -- check for the '--test'
// flag and run if that's applied
int run(int argc, char* const argv[])
{
    if (argc > 1 && strcmp(argv[1], "--test") == 0)
    {
        int testArgc = argc - 1;
        char* testArgv[testArgc];

        testArgv[0] = argv[0];
        for (int i = 2; i < argc; ++i)
            testArgv[i - 1] = argv[i];

        return Catch::Session().run(testArgc, testArgv);
    }

    return 1;
}

} // end namespace tests

#else // not RSTUDIO_UNIT_TESTS_ENABLED

namespace tests {

bool enabled(int argc, char* const argv[])
{
   return false;
}

int run(int argc, char* const argv[])
{
   // no-op -- unit tests disabled
}

} // namespace tests
} // namespace rstudio

#endif // end RSTUDIO_UNIT_TESTS_ENABLED

#endif

