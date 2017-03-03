/*
 * Win32SystemTests.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifdef _WIN32

#include <core/system/System.hpp>
#include <boost/algorithm/string/predicate.hpp>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

TEST_CASE("Win32SystemTests")
{
   SECTION("Test Vista or Later")
   {
      CHECK(isVistaOrLater());
   }

   SECTION("Test Win7 or Later")
   {
      CHECK(isWin7OrLater());
   }

   SECTION("Expand Empty Environment Variable")
   {
      std::string orig;
      std::string result;
      Error err = expandEnvironmentVariables(orig, &result);
      CHECK(!err);
      CHECK(result.empty());
   }

   SECTION("Expand Bogus Env Variable")
   {
      std::string orig = "%oncetherewasafakevariable374732%";
      std::string result;
      Error err = expandEnvironmentVariables(orig, &result);
      CHECK(!err);
      CHECK_FALSE(result.compare(orig));
   }

   SECTION("Expand Real Environment Variable")
   {
      std::string first = "RoadOftenTravelled=";
      std::string orig = first + "%path%";
      std::string result;
      Error err = expandEnvironmentVariables(orig, &result);
      CHECK(!err);
      CHECK(boost::algorithm::starts_with(result, first));
      // assume non-empty path, seems safe
      CHECK(result.length() > first.length());
   }

   SECTION("ComSpec")
   {
      FilePath command = expandComSpec();
      CHECK_FALSE(command.empty());
      CHECK(command.exists());
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
