/*
 * SessionFindTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
const std::string kReplaceString("awesome");
const size_t matchOn = 11;
const size_t matchOff = 16;

const std::string kRegexLine("aba OOOkkk okab AAOO aaabbb aa abab");
const size_t rMatchOn = 4;
const size_t rMatchOff = 10;
const size_t caseMatchOn = 21;
const size_t caseMatchOff = 27;

const std::string kFindRegex("\\([a-z]\\)\\1\\{2\\}\\([a-z]\\)\\2\\{2\\}");
const std::string kReplaceRegex("\\1\\2\\1\\2");

const std::string kGrepPattern("aba \033[01m\033[KOOOkkk\033[m\033[K okab AAOO awesome aa abab");
const std::string kGitGrepPattern("aba \033[1;31mOOOkkk\033[m okab AAOO awesome aa abab");
} // anonymous namespace

TEST_CASE("SessionFind")
{
   SECTION("Replace literal ignore case")
   {
      std::string line(kLine);
      size_t replaceMatchOff;

      Replacer replacer(true);
      replacer.replaceLiteral(matchOn, matchOff,
         kReplaceString, &line, &replaceMatchOff);
      CHECK(line.compare("RStudio is awesome") == 0);
      CHECK(replaceMatchOff == 18);
   }

   SECTION("Replace regex case sensitive")
   {
      std::string line(kRegexLine);
      size_t replaceMatchOff;

      Replacer replacer(false);
      replacer.replaceRegex(caseMatchOn, caseMatchOff,
         kFindRegex, kReplaceString, &line, &replaceMatchOff);
      CHECK(line.compare("aba OOOkkk okab AAOO awesome aa abab") == 0);
      CHECK(replaceMatchOff == 28);
   }

   SECTION("Replace regex ignore case")
   {
      std::string line(kRegexLine);
      size_t replaceMatchOff;

      Replacer replacer(true);
      replacer.replaceRegex(rMatchOn, rMatchOff, kFindRegex, kReplaceRegex, &line,
         &replaceMatchOff);
      CHECK(line.compare("aba OkOk okab AAOO aaabbb aa abab") == 0);
      CHECK(replaceMatchOff == 8);

      size_t matchOn = 18;
      size_t matchOff = 24;
      replacer.replaceRegex(matchOn, matchOff, kFindRegex, kReplaceRegex, &line,
         &replaceMatchOff);
      CHECK(line.compare("aba OkOk okab AAOO abab aa abab") == 0);
      CHECK(replaceMatchOff == 22);
   }

   SECTION("Replace regex case sensitive")
   {
      std::string line(kRegexLine);
      size_t replaceMatchOff;

      Replacer replacer(false);
      replacer.replaceRegex(caseMatchOn, caseMatchOff, kFindRegex, kReplaceRegex, &line,
         &replaceMatchOff);
      CHECK(line.compare("aba OOOkkk okab AAOO abab aa abab") == 0);
      CHECK(replaceMatchOff == 25);
   }

   SECTION("Replace ASCII encoding")
   {
      std::string line("äSCII ìs ƒun");
      std::string find("ƒun");
      std::string replace("Ök");

      size_t matchOn = 11;
      size_t matchOff = 15;
      size_t replaceMatchOff;

      Replacer replacer(false, "ASCII");
      replacer.replaceRegex(matchOn, matchOff, find, replace, &line, &replaceMatchOff);
      CHECK(line.compare("äSCII ìs Ök") == 0);
      CHECK(replaceMatchOff == 14);
   }

   SECTION("Replace BIG5 encoding")
   {
      std::string line("´sπƒ∆GƒßµM");
      std::string find("∆G");
      std::string replace("…@");

      size_t matchOn = 7;
      size_t matchOff = 11;
      size_t replaceMatchOff;

      Replacer replacer(false, "BIG5");
      replacer.replaceRegex(matchOn, matchOff, find, replace, &line, &replaceMatchOff);
      CHECK(line.compare("´sπƒ…@ƒßµM") == 0);
      CHECK(replaceMatchOff == 11);

   }

   SECTION("Attempt replace without valid match")
   {
      std::string line(kLine);
      size_t replaceMatchOff = 99;

      Replacer replacer(true);
      Error error = replacer.replaceRegex(rMatchOn, rMatchOff, kFindRegex, kReplaceRegex,
         &line, &replaceMatchOff);
      CHECK(line.compare(kLine) == 0);
   }

   SECTION("Attempt replace with consecutive matches")
   {
      std::string line("hellohellohello");
      std::string replacePattern("hello world");
      size_t replaceMatchOff;
      size_t on = 10;
      size_t off = 15;

      Replacer replacer(true);
      replacer.replaceLiteral(on, off, replacePattern, &line, &replaceMatchOff);
      CHECK(line.compare("hellohellohello world") == 0);
      CHECK(replaceMatchOff == 21);

      on = 5;
      off = 10;
      replacer.replaceRegex(on, off, "hello", replacePattern, &line, &replaceMatchOff);
      CHECK(line.compare("hellohello worldhello world") == 0);
      CHECK(replaceMatchOff == 16);

      on = 0;
      off = 5;
      replacer.replaceRegex(on, off, "hello", replacePattern, &line, &replaceMatchOff);
      CHECK(line.compare("hello worldhello worldhello world") == 0);
      CHECK(replaceMatchOff == 11);
   }

   SECTION("Attempt regex replace with nested results")
   {
      std::string line("hehello worldllo");
      std::string findPattern("he[^ ].*llo");
      std::string replacePattern("hello world");
      size_t replaceMatchOff;
      size_t on = 0;
      size_t off = 16;

      Replacer replacer(false);
      replacer.replaceRegex(on, off, findPattern, replacePattern, &line, &replaceMatchOff);
      CHECK(line.compare("hello world") == 0);
      CHECK(replaceMatchOff == 11);
   }

   SECTION("Grep get file, line number, and contents")
   {
      boost::regex regex = getGrepOutputRegex(/*isGitGrep*/ false);
      std::string contents(
         "case.test:2:aba \033[01m\033[KOOOkkk\033[m\033[K okab AAOO awesome aa abab");

      boost::smatch match;
      CHECK(regex_utils::match(contents, match, regex));
      CHECK(match[1].str().compare("case.test") == 0);
      CHECK(match[2].str().compare("2") == 0);
      CHECK(match[3].str().compare(kGrepPattern) == 0);
   }

   SECTION("Grep get color encoding regex")
   {
      boost::regex regex = getColorEncodingRegex(/*isGitGrep*/ false);
      boost::cmatch match;
      CHECK(regex_utils::search(kGrepPattern.c_str(), match, regex));
      CHECK(match[1].str().compare("01") == 0);
   }

   SECTION("Git grep get file, line number, and contents")
   {
      boost::regex regex = getGrepOutputRegex(/*isGitGrep*/ true);
      std::string contents(
   "case.test\033[36m:\033[m2\033[36m:\033[maba \033[1;31mOOOkkk\033[m okab AAOO awesome aa abab");

      boost::smatch match;
      CHECK(regex_utils::match(contents, match, regex));
      CHECK(match[1].str().compare("case.test") == 0);
      CHECK(match[2].str().compare("2") == 0);
      CHECK(match[3].str().compare(kGitGrepPattern) == 0);
   }

   SECTION("Git grep with colon get file, line number, and contents")
   {
      boost::regex regex = getGrepOutputRegex(/*isGitGrep*/ true);
      std::string contents(
         "file.test\033[36m:\033[m9\033[36m:\033[m  - \033[1;31mr:\033[m devel");

      boost::smatch match;
      CHECK(regex_utils::match(contents, match, regex));
      CHECK(match[1].str().compare("file.test") == 0);
      CHECK(match[2].str().compare("9") == 0);
      CHECK(match[3].str().compare("  - \033[1;31mr:\033[m devel") == 0);
   }

   SECTION("Git grep get color encoding regex")
   {
      boost::regex regex = getColorEncodingRegex(/*isGitGrep*/ true);
      boost::cmatch match;
      CHECK(regex_utils::search(kGitGrepPattern.c_str(), match, regex));
      CHECK(match[2].str().compare("1") == 0);
   }

}

} // end namespace tests
} // end namespace modules
} // end namespace find
} // end namespace session
} // end namespace rstudio

