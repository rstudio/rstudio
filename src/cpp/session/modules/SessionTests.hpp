/*
 * SessionTests.hpp
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

#ifndef SESSION_TESTS_HPP
#define SESSION_TESTS_HPP

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace tests {

enum TestsFileType 
{
   // Not a test file
   TestsNone,

   // A testthat file
   TestsTestThat,

   // A shinytest file
   TestsShinyTest
};

core::Error initialize();
                       
} // namespace tests
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_TESTS_HPP
