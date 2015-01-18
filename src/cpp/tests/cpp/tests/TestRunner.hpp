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
// custom built main.

#ifndef CORE_TESTRUNNER_HPP
#define CORE_TESTRUNNER_HPP

#ifdef RSTUDIO_ENABLE_TESTING

# define CATCH_CONFIG_RUNNER
# include "vendor/catch.hpp"

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

#else // not RSTUDIO_ENABLE_TESTING

namespace tests {

// no-op for running tests
bool enabled() { return false; }
int run(int argc, char* const argv[]) {}

} // end namespace tests

#endif // end RSTUDIO_ENABLE_TESTING

#endif

