/*
 * SessionHelpTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <gtest/gtest.h>

#include <string>
#include <vector>

#include "SessionHelp.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace help {
namespace {

// apply the production rewrite to a help page fragment; the filter operates
// on a character buffer, so round-trip through one here
std::string addArgumentsHeaderClass(const std::string& html)
{
   std::vector<char> contents(html.begin(), html.end());
   detail::addArgumentsHeaderClass(&contents);
   return std::string(contents.begin(), contents.end());
}

TEST(SessionHelpTest, ArgumentsHeaderGetsClass)
{
   EXPECT_EQ(
      "<h3 class=\"r-arguments-title\">Arguments</h3>",
      addArgumentsHeaderClass("<h3>Arguments</h3>"));
}

TEST(SessionHelpTest, ArgumentsHeaderWithAttributesPreservesThem)
{
   // R's dynamic help server may emit attributes on the header, e.g. an id
   // when generating a table of contents (R >= 4.6.0); regression test for
   // the match broadening in commit 852f68f
   EXPECT_EQ(
      "<h3 id='arguments' class=\"r-arguments-title\">Arguments</h3>",
      addArgumentsHeaderClass("<h3 id='arguments'>Arguments</h3>"));

   EXPECT_EQ(
      "<h3 id=\"_sec_arguments\" class=\"r-arguments-title\">Arguments</h3>",
      addArgumentsHeaderClass("<h3 id=\"_sec_arguments\">Arguments</h3>"));
}

TEST(SessionHelpTest, ArgumentsHeaderRewrittenWithinSurroundingContent)
{
   std::string input =
      "<h2>rnorm</h2>\n"
      "<h3 id=\"_sec_usage\">Usage</h3>\n"
      "<h3 id=\"_sec_arguments\">Arguments</h3>\n"
      "<table></table>\n";

   std::string expected =
      "<h2>rnorm</h2>\n"
      "<h3 id=\"_sec_usage\">Usage</h3>\n"
      "<h3 id=\"_sec_arguments\" class=\"r-arguments-title\">Arguments</h3>\n"
      "<table></table>\n";

   EXPECT_EQ(expected, addArgumentsHeaderClass(input));
}

TEST(SessionHelpTest, OtherHeadersUntouched)
{
   const std::string others[] = {
      "<h3>Usage</h3>",
      "<h3>Value</h3>",
      "<h2>Arguments</h2>",
      "<h3>Arguments and More</h3>",
   };

   for (const std::string& html : others)
      EXPECT_EQ(html, addArgumentsHeaderClass(html));
}

} // anonymous namespace
} // namespace help
} // namespace modules
} // namespace session
} // namespace rstudio
