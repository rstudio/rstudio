/*
 * SessionFindTests.cpp
 *
 * Copyright (C) 2019 by RStudio, Inc.
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

#include "SessionFind.hpp"

#include <core/system/ShellUtils.hpp>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

 namespace rstudio {
 namespace session {
 namespace modules {
 namespace find {
 namespace tests {

 using namespace rstudio::core;
 using namespace modules::find;

 namespace {
      std::string line("<key>Green Component</key>");
      std::string newLine("<key>mean Component</key>");
      std::string replaceString("mean");
      int matchOn = 5;
      int matchOff = 10;
      int replaceMatchOff = 0;
 } // anonymous namespace

   TEST_CASE("SessionFind")
   {
      SECTION("Replace literal with literal ignore case")
      {
         Replacer replacer(true);
         replacer.replaceLiteralWithLiteral(matchOn, matchOff, &line,
            &replaceString, &replaceMatchOff);
         CHECK(line.compare(newLine) == 0);
         CHECK(replaceMatchOff == 9);
      }

      SECTION("Replace regex with literal ignore case")
      {
      }

      SECTION("Replace regex with literal case sensitive")
      {
      }

      SECTION("Replace regex with regex ignore case")
      {
      }

      SECTION("Replace regex with regex case sensitive")
      {
      }
   }
} // end namespace tests
} // end namespace modules
} // end namespace find
} // end namespace session
} // end namespace rstudio
