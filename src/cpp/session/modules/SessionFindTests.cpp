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
      std::string regexLine("aba oookkk okab aaoo aa abab");
      const std::string regexOriginalLine("aba oookkk okab aaoo aa abab");

      std::string newLine("<key>mean Component</key>");
      std::string regexNewLine("aba mean okab aaoo aa abab");
      //std::string regexRegexNewLine("aba oOooOo okab aaooo");
      std::string regexRegexNewLine("aba ooo okab aaoo aa abab");

      //std::string findRegex("([a-z])\1{2}([a-z])\2{2})");
      std::string findRegex("\\([a-z]\\)\\1\\{2\\}\\([a-z]\\)\\2\\{2\\}");

      std::string replaceString("mean");
      //std::string replaceRegex("\\1\\{2\\}");
      std::string replaceRegex("\\1\\1\\1");

      int matchOn = 5;
      int matchOff = 10;

      int rMatchOn = 4;
      int rMatchOff = 10;

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
         Replacer replacer(true);
         replacer.replaceRegexWithLiteral(rMatchOn, rMatchOff, &regexLine,
            &findRegex, &replaceString, &replaceMatchOff);
         CHECK(regexLine.compare(regexNewLine) == 0);
         CHECK(replaceMatchOff == 8);
         regexLine = regexOriginalLine;
      }

      SECTION("Replace regex with literal case sensitive")
      {
         std::string regexOriginalLine = regexLine;
         Replacer replacer(false);
         replacer.replaceRegexWithLiteral(rMatchOn, rMatchOff, &regexLine,
            &findRegex, &replaceString, &replaceMatchOff);
         CHECK(regexLine.compare(regexNewLine) == 0);
         CHECK(replaceMatchOff == 8);
         regexLine = regexOriginalLine;
      }

      SECTION("Replace regex with regex ignore case")
      {
         Replacer replacer(true);
         replacer.replaceRegexWithRegex(rMatchOn, rMatchOff, &regexLine,
            &findRegex, &replaceRegex, &replaceMatchOff);

         CHECK(regexLine.compare(regexRegexNewLine) == 0);
         CHECK(replaceMatchOff == 7);
         regexLine = regexOriginalLine;
      }

      SECTION("Replace regex with regex case sensitive")
      {
         std::string regexOriginalLine = regexLine;
         Replacer replacer(false);
         replacer.replaceRegexWithRegex(rMatchOn, rMatchOff, &regexLine,
            &findRegex, &replaceRegex, &replaceMatchOff);

         LOG_DEBUG_MESSAGE("regexLine: " + regexLine);
         LOG_DEBUG_MESSAGE("regexRegexNewLine: " + regexRegexNewLine);
         LOG_DEBUG_MESSAGE("replaceRegex: " + replaceRegex);
         CHECK(regexLine.compare(regexRegexNewLine) == 0);
         CHECK(replaceMatchOff == 7);
         regexLine = regexOriginalLine;
      }
   }
} // end namespace tests
} // end namespace modules
} // end namespace find
} // end namespace session
} // end namespace rstudio
