/*
 * PosixShellUtilsTests.cpp
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

#ifndef _WIN32

#include <gtest/gtest.h>

#include <iostream>

#include <shared_core/Error.hpp>

#include <core/system/ShellUtils.hpp>

namespace rstudio {
namespace core {
namespace shell_utils {
namespace tests {

TEST(ShellTest, CommandsWithSpecialCharactersAreEscaped)
{
   std::string dollars = "$$$";
   std::string escaped  = escape(dollars);
   std::string expected = "'$$$'";
   EXPECT_EQ(escaped, expected);
}

TEST(ShellTest, CommandsWithBackslashesAreEscaped)
{
   std::string backslashes = R"(\\\)";
   std::string escaped = escape(backslashes);
   std::string expected = R"('\\\')";
   EXPECT_EQ(escaped, expected);
}

TEST(ShellTest, InnerQuotesAreProperlyHandled)
{
   std::string text = "Text with 'inner quotes'.";
   std::string escaped = escape(text);
   std::string expected = R"('Text with '"'"'inner quotes'"'"'.')";
   EXPECT_EQ(escaped, expected);
}

} // namespace tests
} // namespace shell_utils
} // namespace core
} // namespace rstudio

#endif // _WIN32
