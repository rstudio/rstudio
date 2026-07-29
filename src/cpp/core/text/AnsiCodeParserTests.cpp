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

TEST(AnsiCodeParserTest, StripSimpleEscapeSequences)
{
   // ESC + letter without params (e.g., closing tags)
   std::string withEsc = "Hello\x1b" "g World";
   std::string expect = "Hello World";

   stripAnsiCodes(&withEsc);
   EXPECT_EQ(expect, withEsc);
}

TEST(AnsiCodeParserTest, StripSimpleEscapeSequencesWithParams)
{
   // ESC + letter + numeric params (e.g., ESC G1; ESC H2;)
   // Used by RStudio for clickable error links
   std::string withEsc = "\x1b" "G1;\x1b" "H1;Error\x1b" "h: not found\x1b" "g";
   std::string expect = "Error: not found";

   stripAnsiCodes(&withEsc);
   EXPECT_EQ(expect, withEsc);
}

TEST(AnsiCodeParserTest, StripSimpleEscapeBeforeAnsiMatch)
{
   // This test verifies the order of regex application matters.
   // ESC G and ESC H are in the kAnsiMatch character range (A-PR),
   // so if kAnsiMatch runs first, it would strip just "ESC G" and leave "1;" behind.
   // By running kSimpleEscapeMatch first, we strip "ESC G1;" entirely.
   std::string withEsc = "\x1b" "G1;Error";
   std::string expect = "Error";

   stripAnsiCodes(&withEsc);
   EXPECT_EQ(expect, withEsc);
}

TEST(AnsiCodeParserTest, StripOscSequencesWithStTerminator)
{
   // systemd >= 258 wraps every shell prompt in OSC 3008 "context" sequences
   // (https://systemd.io/OSC_CONTEXT/) whose payload carries machine id, user,
   // cwd, etc.; the payload must not leak into the stripped text (#18194)
   std::string withOsc =
      "\x1b]3008;start=4cc46ab2;machineid=ab12cd34;user=kevin;cwd=/home/kevin\x1b\\"
      "2\n"
      "\x1b]3008;end=4cc46ab2;exit=success\x1b\\"
      "$ ";
   std::string expect = "2\n$ ";

   stripAnsiCodes(&withOsc);
   EXPECT_EQ(expect, withOsc);
}

TEST(AnsiCodeParserTest, StripOscSequencesWithBelTerminator)
{
   // xterm window title (OSC 0) and cwd report (OSC 7), BEL-terminated
   std::string withOsc =
      "\x1b]0;user@host: ~\x07"
      "\x1b]7;file://host/home/user\x07"
      "$ echo hi";
   std::string expect = "$ echo hi";

   stripAnsiCodes(&withOsc);
   EXPECT_EQ(expect, withOsc);
}

TEST(AnsiCodeParserTest, StripOscHyperlinks)
{
   // OSC 8 hyperlinks (e.g. emitted by the cli R package): the link targets
   // go, the link text stays
   std::string withOsc =
      "See \x1b]8;;https://example.com\x1b\\"
      "docs\x1b]8;;\x1b\\"
      " for details";
   std::string expect = "See docs for details";

   stripAnsiCodes(&withOsc);
   EXPECT_EQ(expect, withOsc);
}

TEST(AnsiCodeParserTest, StripDcsSequences)
{
   std::string withDcs =
      "before\x1bP+q544e\x1b\\"
      "after";
   std::string expect = "beforeafter";

   stripAnsiCodes(&withDcs);
   EXPECT_EQ(expect, withDcs);
}

TEST(AnsiCodeParserTest, UnterminatedOscConfinedToLine)
{
   // an OSC sequence missing its terminator must not swallow following lines
   std::string withOsc = "\x1b]0;truncated title\nnext line";
   std::string expect = "\nnext line";

   stripAnsiCodes(&withOsc);
   EXPECT_EQ(expect, withOsc);
}

} // namespace tests
} // namespace text
} // namespace core
} // namespace rstudio