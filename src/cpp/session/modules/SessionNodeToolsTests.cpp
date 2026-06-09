/*
 * SessionNodeToolsTests.cpp
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

#include "SessionNodeTools.hpp"

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace modules {
namespace node_tools {
namespace tests {

TEST(SessionNodeToolsTest, AppendNodeOptionEmpty)
{
   EXPECT_EQ(appendNodeOption("", "--use-system-ca"), "--use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionPreservesExisting)
{
   EXPECT_EQ(appendNodeOption("--max-old-space-size=4096", "--use-system-ca"),
             "--max-old-space-size=4096 --use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionIdempotentSingle)
{
   EXPECT_EQ(appendNodeOption("--use-system-ca", "--use-system-ca"),
             "--use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionIdempotentFirstAndMiddle)
{
   EXPECT_EQ(appendNodeOption("--use-system-ca --enable-source-maps", "--use-system-ca"),
             "--use-system-ca --enable-source-maps");
   EXPECT_EQ(appendNodeOption("--a --use-system-ca --b", "--use-system-ca"),
             "--a --use-system-ca --b");
}

TEST(SessionNodeToolsTest, AppendNodeOptionIdempotentLast)
{
   EXPECT_EQ(appendNodeOption("--a --use-system-ca", "--use-system-ca"),
             "--a --use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionPreservesQuotedWhitespace)
{
   // Quoted values with intentional internal whitespace must not be rewritten.
   EXPECT_EQ(appendNodeOption("--require \"/my  app/x.js\"", "--use-system-ca"),
             "--require \"/my  app/x.js\" --use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionMatchesWholeTokenOnly)
{
   // A longer option that merely starts with the flag is not a match.
   EXPECT_EQ(appendNodeOption("--use-system-cafoo", "--use-system-ca"),
             "--use-system-cafoo --use-system-ca");
}

TEST(SessionNodeToolsTest, ParseNodeVersionTypical)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v22.22.2\n", &major, &minor));
   EXPECT_EQ(major, 22);
   EXPECT_EQ(minor, 22);
}

TEST(SessionNodeToolsTest, ParseNodeVersionBoundary)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v22.17.0", &major, &minor));
   EXPECT_EQ(major, 22);
   EXPECT_EQ(minor, 17);
}

TEST(SessionNodeToolsTest, ParseNodeVersionBelowGate)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v20.19.4", &major, &minor));
   EXPECT_EQ(major, 20);
   EXPECT_EQ(minor, 19);
}

TEST(SessionNodeToolsTest, ParseNodeVersionMalformed)
{
   int major = 0, minor = 0;
   EXPECT_FALSE(parseNodeVersion("", &major, &minor));
   EXPECT_FALSE(parseNodeVersion("not-a-version", &major, &minor));
}

} // namespace tests
} // namespace node_tools
} // namespace modules
} // namespace session
} // namespace rstudio
