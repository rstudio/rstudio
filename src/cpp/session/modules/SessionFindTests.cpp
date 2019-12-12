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

const std::string kLine("RStudio is great");
const std::string replaceString("awesome");
const size_t matchOn = 11;
const size_t matchOff = 16;

const std::string kRegexLine("aba OOOkkk okab AAOO aaabbb aa abab");
const size_t rMatchOn = 4;
const size_t rMatchOff = 10;

const std::string findRegex("\\([a-z]\\)\\1\\{2\\}\\([a-z]\\)\\2\\{2\\}");
const std::string replaceRegex("\\1\\2\\1\\2");

} // anonymous namespace

TEST_CASE("SessionFind")
{
   SECTION("Replace literal ignore case")
   {
      std::string line(kLine);
      size_t replaceMatchOff;

      Replacer replacer(true);
      replacer.replaceLiteral(matchOn, matchOff,
         replaceString, &line, &replaceMatchOff);
      CHECK(line.compare("RStudio is awesome") == 0);
      CHECK(replaceMatchOff == 18);
   }

   SECTION("Replace regex case sensitive")
   {
      std::string line(kRegexLine);
      size_t replaceMatchOff;

      Replacer replacer(false);
      replacer.replaceRegex(rMatchOn, rMatchOff,
         findRegex, replaceString, &line, &replaceMatchOff);
      CHECK(line.compare("aba OOOkkk okab AAOO awesome aa abab") == 0);
      CHECK(replaceMatchOff == 36);
   }

   SECTION("Replace regex ignore case")
   {
      std::string line(kRegexLine);
      size_t replaceMatchOff;

      Replacer replacer(true);
      replacer.replaceRegex(rMatchOn, rMatchOff, findRegex, replaceRegex, &line,
         &replaceMatchOff);
      CHECK(line.compare("aba OkOk okab AAOO aaabbb aa abab") == 0);
      CHECK(replaceMatchOff == 8);

      size_t matchOn = 18;
      size_t matchOff = 24;
      replacer.replaceRegex(matchOn, matchOff, findRegex, replaceRegex, &line,
         &replaceMatchOff);
      CHECK(line.compare("aba OkOk okab AAOO abab aa abab") == 0);
      CHECK(replaceMatchOff == 22);
   }

   SECTION("Replace regex case sensitive")
   {
      std::string line(kRegexLine);
      size_t replaceMatchOff;

      Replacer replacer(false);
      replacer.replaceRegex(rMatchOn, rMatchOff, findRegex, replaceRegex, &line,
         &replaceMatchOff);
      CHECK(line.compare("aba OOOkkk okab AAOO abab aa abab") == 0);
      CHECK(replaceMatchOff == 33);
   }

   SECTION("Attempt replace without valid match")
   {
      std::string line(kLine);
      size_t replaceMatchOff;

      Replacer replacer(true);
      Error error = replacer.replaceRegex(rMatchOn, rMatchOff, findRegex, replaceRegex,
         &line, &replaceMatchOff); 
      //CHECK(error);
      CHECK(line.compare(kLine) == 0);
   }
}

} // end namespace tests
} // end namespace modules
} // end namespace find
} // end namespace session
} // end namespace rstudio

