/*
 * AnsiCodeParserTests.cpp
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

#include <core/text/AnsiCodeParser.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace text {
namespace tests {

TEST(AnsiCodeParserTest, AnsiStrippingPreservesPlainText)
{
   std::string expect("abcd12345\nderkjdf\r\n234");
   std::string noAnsi = expect;
   stripAnsiCodes(&noAnsi);

   EXPECT_EQ(expect, noAnsi);
}

TEST(AnsiCodeParserTest, StripAnsiCodesRemovesComplexEscapes)
{
   std::string withAnsi = "abc\x1b[31mHello\x1b[39m World\nBye.";
   std::string expect = "abcHello World\nBye.";
   
   stripAnsiCodes(&withAnsi);
   EXPECT_EQ(expect, withAnsi);
}

} // namespace tests
} // namespace text
} // namespace core
} // namespace rstudio