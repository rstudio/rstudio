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
      std::string line("RStudio is great");
      std::string regexLine("aba OOOkkk okab AAOO aaabbb aa abab");
      const std::string regexOriginalLine("aba OOOkkk okab AAOO aaabbb aa abab");

      std::string findRegex("\\([a-z]\\)\\1\\{2\\}\\([a-z]\\)\\2\\{2\\}");
      std::string replaceString("awesome");
      std::string replaceRegex("\\1\\1\\1");

      size_t matchOn = 11;
      size_t matchOff = 16;

      size_t rMatchOn = 4;
      size_t rMatchOff = 10;

      size_t replaceMatchOff = 0;
 } // anonymous namespace

   TEST_CASE("SessionFind")
   {
      SECTION("Replace literal with literal ignore case")
      {
         Replacer replacer(true);
         replacer.replaceLiteralWithLiteral(matchOn, matchOff, &line,
            &replaceString, &replaceMatchOff);
         CHECK(line.compare("RStudio is awesome") == 0);
         CHECK(replaceMatchOff == 18);
      }

      SECTION("Replace regex with literal ignore case")
      {
         Replacer replacer(true);
         replacer.replaceRegexWithLiteral(rMatchOn, rMatchOff, &regexLine,
            &findRegex, &replaceString, &replaceMatchOff);
         CHECK(regexLine.compare("aba awesome okab AAOO awesome aa abab") == 0);
         CHECK(replaceMatchOff == 37);
         regexLine = regexOriginalLine;
      }

      SECTION("Replace regex with literal case sensitive")
      {
         std::string regexOriginalLine = regexLine;
         Replacer replacer(false);
         replacer.replaceRegexWithLiteral(rMatchOn, rMatchOff, &regexLine,
            &findRegex, &replaceString, &replaceMatchOff);
         CHECK(regexLine.compare("aba OOOkkk okab AAOO awesome aa abab") == 0);
         CHECK(replaceMatchOff == 36);
         regexLine = regexOriginalLine;
      }

      SECTION("Replace regex with regex ignore case")
      {
         Replacer replacer(true);
         replacer.replaceRegexWithRegex(rMatchOn, rMatchOff, &regexLine,
            &findRegex, &replaceRegex, &replaceMatchOff);
         CHECK(regexLine.compare("aba OOO okab AAOO aaa aa abab") == 0);
         CHECK(replaceMatchOff == 29);
         regexLine = regexOriginalLine;
      }

      SECTION("Replace regex with regex case sensitive")
      {
         Replacer replacer(false);
         replacer.replaceRegexWithRegex(rMatchOn, rMatchOff, &regexLine,
            &findRegex, &replaceRegex, &replaceMatchOff);
         CHECK(regexLine.compare("aba OOOkkk okab AAOO aaa aa abab") == 0);
         CHECK(replaceMatchOff == 32);
         regexLine = regexOriginalLine;
      }
   }
} // end namespace tests
} // end namespace modules
} // end namespace find
} // end namespace session
} // end namespace rstudio
