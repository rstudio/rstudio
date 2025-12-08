/*
 * SessionFindTests.cpp
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

#include "SessionFind.hpp"

#include <core/system/ShellUtils.hpp>

#include <gtest/gtest.h>

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

const std::string kFindRegex("([a-z])\\1{2}([a-z])\\2{2}");
const std::string kReplaceRegex("\\1\\2\\1\\2");

const std::string kGrepPattern("aba \033[01m\033[KOOOkkk\033[m\033[K okab AAOO awesome aa abab");
const std::string kGitGrepPattern("aba \033[1;31mOOOkkk\033[m okab AAOO awesome aa abab");

const std::string kWordBoundaryRegex("\\bgreat\\b");
const size_t wMatchOn = 15;
const size_t wMatchOff = 20;
const std::string kLineNoMatch("RStudio is the greatest");
} // anonymous namespace

TEST(SessionFindTest, ReplaceLiteralIgnoreCase) {
   std::string line(kLine);
   size_t replaceMatchOff;

   Replacer replacer(true);
   replacer.replaceLiteral(matchOn, matchOff,
      kReplaceString, &line, &replaceMatchOff);
   EXPECT_EQ("RStudio is awesome", line);
   EXPECT_EQ(18u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceRegexCaseSensitiveSimple) {
   std::string line(kRegexLine);
   size_t replaceMatchOff;

   Replacer replacer(false);
   replacer.replaceRegex(caseMatchOn, caseMatchOff,
      kFindRegex, kReplaceString, &line, &replaceMatchOff);
   EXPECT_EQ("aba OOOkkk okab AAOO awesome aa abab", line);
   EXPECT_EQ(28u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceRegexIgnoreCase) {
   std::string line(kRegexLine);
   size_t replaceMatchOff;

   Replacer replacer(true);
   replacer.replaceRegex(rMatchOn, rMatchOff, kFindRegex, kReplaceRegex, &line,
      &replaceMatchOff);
   EXPECT_EQ("aba OkOk okab AAOO aaabbb aa abab", line);
   EXPECT_EQ(8u, replaceMatchOff);

   size_t matchOn = 18;
   size_t matchOff = 24;
   replacer.replaceRegex(matchOn, matchOff, kFindRegex, kReplaceRegex, &line,
      &replaceMatchOff);
   EXPECT_EQ("aba OkOk okab AAOO abab aa abab", line);
   EXPECT_EQ(22u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceRegexCaseSensitive) {
   std::string line(kRegexLine);
   size_t replaceMatchOff;

   Replacer replacer(false);
   replacer.replaceRegex(caseMatchOn, caseMatchOff, kFindRegex, kReplaceRegex, &line,
      &replaceMatchOff);
   EXPECT_EQ("aba OOOkkk okab AAOO abab aa abab", line);
   EXPECT_EQ(25u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceRegexWordBoundaries) {
   std::string line(kLine);
   size_t replaceMatchOff;

   Replacer replacer(true);
   replacer.replaceRegex(matchOn, matchOff, kWordBoundaryRegex, kReplaceString,
                         &line, &replaceMatchOff);
   EXPECT_EQ("RStudio is awesome", line);
   EXPECT_EQ(18u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceRegexWordBoundariesNoMatch) {
   std::string line(kLineNoMatch);
   size_t replaceMatchOff;

   Replacer replacer(true);
   replacer.replaceRegex(wMatchOn, wMatchOff, kWordBoundaryRegex, kReplaceString,
                         &line, &replaceMatchOff);
   EXPECT_EQ("RStudio is the greatest", line);
   EXPECT_EQ(wMatchOff, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceASCIIEncoding) {
   std::string line("äSCII ìs ƒun");
   std::string find("ƒun");
   std::string replace("Ök");

   size_t matchOn = 11;
   size_t matchOff = 15;
   size_t replaceMatchOff;

   Replacer replacer(false, "ASCII");
   replacer.replaceRegex(matchOn, matchOff, find, replace, &line, &replaceMatchOff);
   EXPECT_EQ("äSCII ìs Ök", line);
   EXPECT_EQ(14u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceBIG5Encoding) {
   std::string line("´sπƒ∆GƒßµM");
   std::string find("∆G");
   std::string replace("…@");

   size_t matchOn = 7;
   size_t matchOff = 11;
   size_t replaceMatchOff;

   Replacer replacer(false, "BIG5");
   replacer.replaceRegex(matchOn, matchOff, find, replace, &line, &replaceMatchOff);
   EXPECT_EQ("´sπƒ…@ƒßµM", line);
   EXPECT_EQ(11u, replaceMatchOff);

}

TEST(SessionFindTest, ReplaceWithoutValidMatch) {
   std::string line(kLine);
   size_t replaceMatchOff = 99;

   Replacer replacer(true);
   Error error = replacer.replaceRegex(rMatchOn, rMatchOff, kFindRegex, kReplaceRegex,
      &line, &replaceMatchOff);
   EXPECT_EQ(kLine, line);
}

TEST(SessionFindTest, ReplaceWithConsecutiveMatches) {
   std::string line("hellohellohello");
   std::string replacePattern("hello world");
   size_t replaceMatchOff;
   size_t on = 10;
   size_t off = 15;

   Replacer replacer(true);
   replacer.replaceLiteral(on, off, replacePattern, &line, &replaceMatchOff);
   EXPECT_EQ("hellohellohello world", line);
   EXPECT_EQ(21u, replaceMatchOff);

   on = 5;
   off = 10;
   replacer.replaceRegex(on, off, "hello", replacePattern, &line, &replaceMatchOff);
   EXPECT_EQ("hellohello worldhello world", line);
   EXPECT_EQ(16u, replaceMatchOff);

   on = 0;
   off = 5;
   replacer.replaceRegex(on, off, "hello", replacePattern, &line, &replaceMatchOff);
   EXPECT_EQ("hello worldhello worldhello world", line);
   EXPECT_EQ(11u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceRegexWithNestedResults) {
   std::string line("hehello worldllo");
   std::string findPattern("he[^ ].*llo");
   std::string replacePattern("hello world");
   size_t replaceMatchOff;
   size_t on = 0;
   size_t off = 16;

   Replacer replacer(false);
   replacer.replaceRegex(on, off, findPattern, replacePattern, &line, &replaceMatchOff);
   EXPECT_EQ("hello world", line);
   EXPECT_EQ(11u, replaceMatchOff);
}

TEST(SessionFindTest, GrepGetFileLineNumberAndContents) {
   boost::regex regex = getGrepOutputRegex(/*isGitGrep*/ false);
   std::string contents(
      "case.test:2:aba \033[01m\033[KOOOkkk\033[m\033[K okab AAOO awesome aa abab");

   boost::smatch match;
   EXPECT_TRUE(regex_utils::match(contents, match, regex));
   EXPECT_EQ("case.test", match[1].str());
   EXPECT_EQ("2", match[2].str());
   EXPECT_EQ(kGrepPattern, match[3].str());
}

TEST(SessionFindTest, GrepGetColorEncodingRegex) {
   boost::regex regex = getColorEncodingRegex(/*isGitGrep*/ false);
   boost::cmatch match;
   EXPECT_TRUE(regex_utils::search(kGrepPattern.c_str(), match, regex));
   EXPECT_EQ("01", match[1].str());
}

TEST(SessionFindTest, GitGrepGetFileLineNumberAndContents) {
   boost::regex regex = getGrepOutputRegex(/*isGitGrep*/ true);
   std::string contents(
"case.test\033[36m:\033[m2\033[36m:\033[maba \033[1;31mOOOkkk\033[m okab AAOO awesome aa abab");

   boost::smatch match;
   EXPECT_TRUE(regex_utils::match(contents, match, regex));
   EXPECT_EQ("case.test", match[1].str());
   EXPECT_EQ("2", match[2].str());
   EXPECT_EQ(kGitGrepPattern, match[3].str());
}

TEST(SessionFindTest, GitGrepWithColonGetFileLineNumberAndContents) {
   boost::regex regex = getGrepOutputRegex(/*isGitGrep*/ true);
   std::string contents(
      "file.test\033[36m:\033[m9\033[36m:\033[m  - \033[1;31mr:\033[m devel");

   boost::smatch match;
   EXPECT_TRUE(regex_utils::match(contents, match, regex));
   EXPECT_EQ("file.test", match[1].str());
   EXPECT_EQ("9", match[2].str());
   EXPECT_EQ("  - \033[1;31mr:\033[m devel", match[3].str());
}

TEST(SessionFindTest, GitGrepGetColorEncodingRegex) {
   boost::regex regex = getColorEncodingRegex(/*isGitGrep*/ true);
   boost::cmatch match;
   EXPECT_TRUE(regex_utils::search(kGitGrepPattern.c_str(), match, regex));
   EXPECT_EQ("1", match[2].str());
}

TEST(SessionFindTest, ReplaceRegexWithQuantifiers) {
   std::string line("helllooo");
   const std::string regex("l+o+X?");
   const std::string replacement("LO!");
   Replacer replacer(false);
   const size_t matchOn = 2;
   const size_t matchOff = 7;
   size_t replaceMatchOff;

   replacer.replaceRegex(matchOn, matchOff, regex, replacement, &line, &replaceMatchOff);
   EXPECT_EQ("heLO!", line);
   EXPECT_EQ(4u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceRegexWithWordCharsAndBackrefs) {
   std::string line("good, !@$ good and more!");
   const std::string regex("^(\\w+)(\\W+)\\1");
   const std::string replacement("\\1");
   Replacer replacer(false);
   const size_t matchOn = 0;
   const size_t matchOff = 13;
   size_t replaceMatchOff;

   replacer.replaceRegex(matchOn, matchOff, regex, replacement, &line, &replaceMatchOff);
   EXPECT_EQ("good and more!", line);
   EXPECT_EQ(3u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceRegexWithBoundedRepeatAndAlternation) {
   std::string line("11 cats");
   const std::string regex("\\d{2} (cat|dog)");
   const std::string replacement("x");
   Replacer replacer(false);
   const size_t matchOn = 0;
   const size_t matchOff = 5;
   size_t replaceMatchOff;

   replacer.replaceRegex(matchOn, matchOff, regex, replacement, &line, &replaceMatchOff);
   EXPECT_EQ("xs", line);
   EXPECT_EQ(0u, replaceMatchOff);
}

TEST(SessionFindTest, ReplaceRegexWithBracketsAndSpecialChars) {
   std::string line("How are you? Mr. (x)");
   size_t matchOn = 11;  // include '?'
   size_t matchOff = 20; // include ')'
   size_t replaceMatchOff;
   
   Replacer replacer(false);
   replacer.replaceRegex(matchOn, matchOff, "\\? [A-Z][a-z]{0,2}\\. \\(\\w\\)", "?!", &line, &replaceMatchOff);
   EXPECT_EQ("How are you?!", line);
   EXPECT_EQ(13u, replaceMatchOff);
}

} // namespace tests
} // namespace find
} // namespace modules
} // namespace session
} // namespace rstudio
