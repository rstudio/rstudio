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
   // RStudio's private output grouping / highlighting markers (ESC G<n>;
   // ESC H<n>;, per AnsiEscapes.hpp) keep their numeric params attached:
   // a pattern stopping at the letter final would leave "1;" behind
   std::string withEsc = "\x1b" "G1;\x1b" "H1;Error\x1b" "h: not found\x1b" "g";
   std::string expect = "Error: not found";

   stripAnsiCodes(&withEsc);
   EXPECT_EQ(expect, withEsc);
}

TEST(AnsiCodeParserTest, PreservesDigitsAfterTwoByteEscapes)
{
   // the numeric parameter tail belongs only to RStudio's ESC G / ESC H
   // markers; after standard two-byte escapes (here NEL, ESC E) digits
   // are visible text
   std::string withEsc = "line1\x1b" "E2024 results";
   std::string expect = "line12024 results";

   stripAnsiCodes(&withEsc);
   EXPECT_EQ(expect, withEsc);
}

TEST(AnsiCodeParserTest, StripCsiWithColonParameters)
{
   // colon-separated SGR parameters (ITU T.416 form, e.g. from newer tools)
   std::string withCsi =
      "\x1b[38:5:196m"
      "red\x1b[0m";
   std::string expect = "red";

   stripAnsiCodes(&withCsi);
   EXPECT_EQ(expect, withCsi);
}

TEST(AnsiCodeParserTest, StripCsiWithIntermediateBytes)
{
   // DECSCUSR (cursor style) has a space intermediate before the final byte
   std::string withCsi = "a\x1b[1 qb";
   std::string expect = "ab";

   stripAnsiCodes(&withCsi);
   EXPECT_EQ(expect, withCsi);
}

TEST(AnsiCodeParserTest, StripCsiWithNonLetterFinals)
{
   // shells enable bracketed paste (ESC[?2004h) and wrap every pasted
   // command in ESC[200~ ... ESC[201~; CSI final bytes span the full
   // 0x40-0x7e range, not just letters
   std::string withCsi =
      "\x1b[?2004h"
      "\x1b[200~"
      "ls -l\x1b[201~"
      "\x1b[?2004l";
   std::string expect = "ls -l";

   stripAnsiCodes(&withCsi);
   EXPECT_EQ(expect, withCsi);
}

TEST(AnsiCodeParserTest, StripNonCsiEscapeForms)
{
   // nF charset designation (ESC ( B) and keypad modes (ESC =, ESC >)
   std::string withEsc =
      "\x1b(B"
      "abc\x1b="
      "def\x1b>"
      "ghi";
   std::string expect = "abcdefghi";

   stripAnsiCodes(&withEsc);
   EXPECT_EQ(expect, withEsc);
}

TEST(AnsiCodeParserTest, StripTwoByteEscapesWithHighFinals)
{
   // two-byte sequences with finals past 0x3f -- a bare ST (ESC \),
   // ESC @, ESC {, ESC ~ -- previously leaked a raw ESC byte
   std::string withEsc =
      "a\x1b\\"
      "b\x1b@"
      "c\x1b{"
      "d\x1b~"
      "e";
   std::string expect = "abcde";

   stripAnsiCodes(&withEsc);
   EXPECT_EQ(expect, withEsc);
}

TEST(AnsiCodeParserTest, StripOrphanAndTruncatedEscapes)
{
   // an ESC that cannot complete a sequence (followed by a newline, or
   // truncated at end of input) must still be removed
   std::string withEsc = "abc\x1b\ndef\x1b[";
   std::string expect = "abc\ndef";

   stripAnsiCodes(&withEsc);
   EXPECT_EQ(expect, withEsc);
}

TEST(AnsiCodeParserTest, DoubledEscapeKeepsFollowingText)
{
   // tmux and screen DCS passthrough double ESC bytes; stripping one
   // sequence must not pair the leftover ESC with the visible text that
   // follows (a multi-pass strip would delete the 9 and the 7 here)
   std::string withOsc = "\x1b\x1b]0;t\x07" "9 files";
   std::string expectOsc = "9 files";
   stripAnsiCodes(&withOsc);
   EXPECT_EQ(expectOsc, withOsc);

   std::string withCsi = "\x1b\x1b[31m" "7 items";
   std::string expectCsi = "7 items";
   stripAnsiCodes(&withCsi);
   EXPECT_EQ(expectCsi, withCsi);
}

TEST(AnsiCodeParserTest, PreservesUtf8ContinuationBytes)
{
   // 0x9b is the 8-bit CSI introducer in single-byte encodings, but in
   // UTF-8 it appears as a continuation byte (e.g. U+011B, 0xc4 0x9b);
   // it must not be treated as an escape introducer
   std::string text = "d\xc4\x9bm";
   std::string expect = text;

   stripAnsiCodes(&text);
   EXPECT_EQ(expect, text);
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

TEST(AnsiCodeParserTest, StripOscHyperlinksBelTerminated)
{
   // RStudio's own ANSI_HYPERLINK form (AnsiEscapes.hpp) terminates with
   // BEL rather than ST; this is the form on the console-actions path
   std::string withOsc =
      "See \x1b]8;;ide:help:base::print\x07"
      "print\x1b]8;;\x07"
      " for details";
   std::string expect = "See print for details";

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

TEST(AnsiCodeParserTest, StripSosPmApcSequences)
{
   // SOS (ESC X), PM (ESC ^), APC (ESC _) carry string payloads, like OSC
   std::string withSeqs =
      "a\x1bXsos-payload\x1b\\"
      "b\x1b^pm-payload\x07"
      "c\x1b_apc-payload\x1b\\"
      "d";
   std::string expect = "abcd";

   stripAnsiCodes(&withSeqs);
   EXPECT_EQ(expect, withSeqs);
}

TEST(AnsiCodeParserTest, StripScreenWindowTitleSequences)
{
   // GNU screen and tmux set the window title with ESC k <title> ST; the
   // title is a string payload, so it must go with the sequence rather
   // than glue onto the following text
   std::string withTitle =
      "\x1bk~/projects\x1b\\"
      "$ make";
   std::string expect = "$ make";

   stripAnsiCodes(&withTitle);
   EXPECT_EQ(expect, withTitle);
}

TEST(AnsiCodeParserTest, UnterminatedOscConfinedToLine)
{
   // an OSC sequence missing its terminator must not swallow following lines
   std::string withOsc = "\x1b]0;truncated title\nnext line";
   std::string expect = "\nnext line";

   stripAnsiCodes(&withOsc);
   EXPECT_EQ(expect, withOsc);
}

TEST(AnsiCodeParserTest, UnterminatedOscConsumesRestOfLine)
{
   // the flip side of the confinement above: with no terminator the
   // payload match runs to end of line, taking that line's visible text
   // with it -- accepted so a truncated sequence can never leak
   std::string withOsc = "\x1b]0;title README.md  src\nnext line";
   std::string expect = "\nnext line";

   stripAnsiCodes(&withOsc);
   EXPECT_EQ(expect, withOsc);
}

} // namespace tests
} // namespace text
} // namespace core
} // namespace rstudio