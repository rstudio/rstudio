/*
 * StringUtils.cpp
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

#include <gtest/gtest.h>

#include <core/Algorithm.hpp>
#include <core/StringUtils.hpp>

#define kLatexStyleLineCommentRegex ("^%+\\s*")

namespace rstudio {
namespace core {
namespace string_utils {

using namespace algorithm;

TEST(StringTest, StringsCanBeSplitOnNULBytes)
{
   std::string text("a\0b\0c", 5);
   std::string delim("\0", 1);
   std::vector<std::string> splat = split(text, delim);
   EXPECT_EQ(3u, splat.size());
   EXPECT_EQ(std::string("a"), splat[0]);
   EXPECT_EQ(std::string("b"), splat[1]);
   EXPECT_EQ(std::string("c"), splat[2]);
}

TEST(StringTest, RawDelimitersContainingNullBytesCanBeSplit)
{
   std::string text("a\0b\0c", 5);
   std::vector<std::string> splat = split(text, "\0");
   EXPECT_EQ(3u, splat.size());
   EXPECT_EQ(std::string("a"), splat[0]);
   EXPECT_EQ(std::string("b"), splat[1]);
   EXPECT_EQ(std::string("c"), splat[2]);
}

TEST(StringTest, IsSubsequenceWorks)
{
   EXPECT_TRUE(isSubsequence("", ""));
   EXPECT_TRUE(isSubsequence("annnbnnnc", "abc"));
   EXPECT_FALSE(isSubsequence("abcdef", "abdcef"));
   EXPECT_TRUE(isSubsequence("abcdef", "AeF", true));
   EXPECT_TRUE(isSubsequence("a1d2", "12"));
}

TEST(StringTest, StrippedOfBackQuotesWorks)
{
   std::string string = "`abc`";
   std::string stripped = strippedOfBackQuotes(string);
   EXPECT_EQ(std::string("abc"), stripped);
   
   EXPECT_EQ(std::string("abc"), strippedOfBackQuotes("abc"));
}

TEST(StringTest, SubstringWorks)
{
   std::string string("  abc  ");
   EXPECT_EQ(std::string("abc"), substring(string, 2, 5));
}

TEST(StringTest, TrimWhitespaceWorks)
{
   std::string string("   abc   ");
   EXPECT_EQ(std::string("abc"), trimWhitespace(string));
   EXPECT_EQ(std::string("abc"), trimWhitespace("abc"));
   EXPECT_EQ(std::string(""), trimWhitespace(""));
}

TEST(StringTest, CountNewLinesWorksWithEmptyString)
{
   std::string str("");
   EXPECT_EQ(0u, countNewlines(str));
}

TEST(StringTest, CountNewLinesWorksWithLineFeed)
{
   std::string str("\n\n\n");
   EXPECT_EQ(3u, countNewlines(str));
}

TEST(StringTest, CountNewLinesWorksWithCarriageReturnLineFeed)
{
   std::string str("\r\n\r\n");
   EXPECT_EQ(2u, countNewlines(str));
}

TEST(StringTest, CommentHeadersCanBeExtracted)
{
   std::string text(
            "% This is a header.\n"
            "% Let's hope the text is extracted.\n"
            "\n"
            "This should be ignored.");
   
   std::string extracted;
   bool success = extractCommentHeader(text, kLatexStyleLineCommentRegex, &extracted);
   std::string expected(
            "This is a header.\n"
            "Let's hope the text is extracted.\n");
   
   EXPECT_TRUE(success);
   EXPECT_EQ(expected, extracted);
}

TEST(StringTest, CommentHeadersWithNoTrailingNewlineAreHandled)
{
   std::string text("% Hello\n% World");
   
   std::string extracted;
   bool success = extractCommentHeader(text, kLatexStyleLineCommentRegex, &extracted);
   std::string expected("Hello\nWorld\n");
   
   EXPECT_TRUE(success);
   EXPECT_EQ(expected, extracted);
}

TEST(StringTest, CommentHeaderEdgeCasesAreHandled)
{
   std::string extracted;
   EXPECT_FALSE(extractCommentHeader("", kLatexStyleLineCommentRegex, &extracted));
   
   std::string text(
            "There is a comment\n"
            "% but not at the start of the document.\n");
   EXPECT_FALSE(extractCommentHeader(text, kLatexStyleLineCommentRegex, &extracted));
}

TEST(StringTest, JavaScriptLiteralsAreEscaped)
{
   EXPECT_EQ(std::string("\\\"hello\\\""), jsLiteralEscape("\"hello\""));
   EXPECT_EQ(std::string("\\'goodbye\\'"), jsLiteralEscape("'goodbye'"));
   EXPECT_EQ(std::string("\\074/script>"), jsLiteralEscape("</script>"));
}

TEST(StringTest, HtmlTagsInJsonAreEscaped)
{
   EXPECT_EQ(std::string("\\u003ch1\\u003e"), jsonHtmlEscape("<h1>"));
   EXPECT_EQ(std::string("\\u003cscript\\u003ealert!"), jsonHtmlEscape("<script>alert!"));
}

TEST(StringTest, SomeSimpleStringsCanBeFormatted)
{
   std::string s;
   
   s = string_utils::sprintf("%s, %s!", "Hello", "world");
   EXPECT_EQ(std::string("Hello, world!"), s);
   
   s = string_utils::sprintf("%i + %i == %i", 2, 2, 2 + 2);
   EXPECT_EQ(std::string("2 + 2 == 4"), s);
}

TEST(StringTest, PositionInEmptyStringIsAlwaysZeroZero)
{
   std::string text("");
   collection::Position pos = offsetToPosition(text, 0);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(text, 100);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(0u, pos.column);
}

TEST(StringTest, PositionCanBeComputedForSingleLineString)
{
   std::string text("012345");
   collection::Position pos = offsetToPosition(text, 0);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(text, 4);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(4u, pos.column);

   pos = offsetToPosition(text, 100);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(6u, pos.column);
}

TEST(StringTest, PositionCanBeComputedForTwoLineString)
{
   std::string textPosix("0123456\n89");
   collection::Position pos = offsetToPosition(textPosix, 0);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textPosix, 5);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(5u, pos.column);

   pos = offsetToPosition(textPosix, 8);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(0u, pos.column);

   std::string textWindows("0123456\r\n90");
   pos = offsetToPosition(textWindows, 0);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textWindows, 5);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(5u, pos.column);

   pos = offsetToPosition(textWindows, 9);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textWindows, 10);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(1u, pos.column);

   std::string textOldMac("0123456\r89");
   pos = offsetToPosition(textOldMac, 0);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textOldMac, 5);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(5u, pos.column);

   pos = offsetToPosition(textOldMac, 8);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textOldMac, 9);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(1u, pos.column);
}

TEST(StringTest, PositionCanBeComputedForMultiLineString)
{
   std::string textPosix("01234\n67\n9012345\n");
   collection::Position pos = offsetToPosition(textPosix, 0);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textPosix, 4);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(4u, pos.column);

   pos = offsetToPosition(textPosix, 5);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(5u, pos.column);

   pos = offsetToPosition(textPosix, 6);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textPosix, 7);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(1u, pos.column);

   pos = offsetToPosition(textPosix, 13);
   EXPECT_EQ(2u, pos.row);
   EXPECT_EQ(4u, pos.column);

   pos = offsetToPosition(textPosix, 16);
   EXPECT_EQ(2u, pos.row);
   EXPECT_EQ(7u, pos.column);

   std::string textWindows("01234\r\n78\r\n1234567\r\n");
   pos = offsetToPosition(textWindows, 0);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textWindows, 4);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(4u, pos.column);

   pos = offsetToPosition(textWindows, 5);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(5u, pos.column);

   // ensure we can point at the \n in a \r\n sequence
   pos = offsetToPosition(textWindows, 6);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(6u, pos.column);

   pos = offsetToPosition(textWindows, 7);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textWindows, 13);
   EXPECT_EQ(2u, pos.row);
   EXPECT_EQ(2u, pos.column);

   pos = offsetToPosition(textWindows, 18);
   EXPECT_EQ(2u, pos.row);
   EXPECT_EQ(7u, pos.column);

   pos = offsetToPosition(textWindows, 19);
   EXPECT_EQ(2u, pos.row);
   EXPECT_EQ(8u, pos.column);

   std::string textOldMac("01234\r67\r9012345\r");
   pos = offsetToPosition(textOldMac, 0);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textOldMac, 4);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(4u, pos.column);

   pos = offsetToPosition(textOldMac, 5);
   EXPECT_EQ(0u, pos.row);
   EXPECT_EQ(5u, pos.column);

   pos = offsetToPosition(textOldMac, 6);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(0u, pos.column);

   pos = offsetToPosition(textOldMac, 7);
   EXPECT_EQ(1u, pos.row);
   EXPECT_EQ(1u, pos.column);

   pos = offsetToPosition(textOldMac, 13);
   EXPECT_EQ(2u, pos.row);
   EXPECT_EQ(4u, pos.column);

   pos = offsetToPosition(textOldMac, 16);
   EXPECT_EQ(2u, pos.row);
   EXPECT_EQ(7u, pos.column);
}

TEST(StringTest, HeredocStripsCommonIndent)
{
   std::string text = heredoc(R"(
      library(knitr)
      knit('example.Rmd')
   )");
   EXPECT_EQ(std::string("library(knitr)\nknit('example.Rmd')"), text);
}

TEST(StringTest, HeredocPreservesRelativeIndent)
{
   std::string text = heredoc(R"(
      if (x) {
         f()
      }
   )");
   EXPECT_EQ(std::string("if (x) {\n   f()\n}"), text);
}

TEST(StringTest, HeredocHandlesBlankInteriorLines)
{
   std::string text = heredoc(R"(
      a

      b
   )");
   EXPECT_EQ(std::string("a\n\nb"), text);
}

TEST(StringTest, HeredocLeavesUnindentedTextUnchanged)
{
   EXPECT_EQ(std::string("a\nb"), heredoc("a\nb"));
}

TEST(StringTest, HeredocHandlesEmptyInput)
{
   EXPECT_EQ(std::string(""), heredoc(""));
}

TEST(StringTest, SingleQuotedStrEscapeLeavesPlainTextUnchanged)
{
   EXPECT_EQ(std::string("hello"), singleQuotedStrEscape("hello"));
   EXPECT_EQ(std::string(""), singleQuotedStrEscape(""));
}

TEST(StringTest, SingleQuotedStrEscapeEscapesQuotesAndBackslashes)
{
   // a single quote is escaped so it cannot terminate the enclosing literal
   EXPECT_EQ(std::string("it\\'s"), singleQuotedStrEscape("it's"));

   // a backslash is doubled so it cannot escape the following character
   EXPECT_EQ(std::string("a\\\\b"), singleQuotedStrEscape("a\\b"));
}

TEST(StringTest, SingleQuotedStrEscapeNeutralizesInjectionPayload)
{
   // a filename crafted to break out of a single-quoted R literal and inject
   // code has every quote escaped, so the payload cannot close the literal
   EXPECT_EQ(std::string("\\'); system(\\'rm -rf /\\'); #"),
             singleQuotedStrEscape("'); system('rm -rf /'); #"));
}

TEST(StringTest, Utf8IncompleteSuffixLengthHandlesCompleteText)
{
   // empty, pure ASCII, and text ending on a character boundary are complete
   EXPECT_EQ(0u, utf8IncompleteSuffixLength(""));
   EXPECT_EQ(0u, utf8IncompleteSuffixLength("hello"));
   EXPECT_EQ(0u, utf8IncompleteSuffixLength("a\xE2\x94\x80"));       // a + U+2500
   EXPECT_EQ(0u, utf8IncompleteSuffixLength("\xC3\xA9"));            // U+00E9
   EXPECT_EQ(0u, utf8IncompleteSuffixLength("\xF0\x9F\x98\x80"));    // U+1F600
}

TEST(StringTest, Utf8IncompleteSuffixLengthDetectsTruncatedSequences)
{
   // 2-byte sequence missing its continuation byte
   EXPECT_EQ(1u, utf8IncompleteSuffixLength("abc\xC3"));

   // 3-byte sequence truncated after one and two bytes
   EXPECT_EQ(1u, utf8IncompleteSuffixLength("abc\xE2"));
   EXPECT_EQ(2u, utf8IncompleteSuffixLength("abc\xE2\x94"));

   // 4-byte sequence truncated after one, two, and three bytes
   EXPECT_EQ(1u, utf8IncompleteSuffixLength("abc\xF0"));
   EXPECT_EQ(2u, utf8IncompleteSuffixLength("abc\xF0\x9F"));
   EXPECT_EQ(3u, utf8IncompleteSuffixLength("abc\xF0\x9F\x98"));

   // a truncated sequence with no preceding text
   EXPECT_EQ(2u, utf8IncompleteSuffixLength("\xE2\x94"));
}

TEST(StringTest, Utf8IncompleteSuffixLengthIgnoresInvalidSequences)
{
   // a stray continuation byte is invalid, not truncated
   EXPECT_EQ(0u, utf8IncompleteSuffixLength("abc\x80"));

   // invalid lead bytes (0xF8-0xFF) cannot start a sequence
   EXPECT_EQ(0u, utf8IncompleteSuffixLength("abc\xFE"));
   EXPECT_EQ(0u, utf8IncompleteSuffixLength("abc\xFF\x80"));

   // continuation bytes with no lead byte in reach are invalid
   EXPECT_EQ(0u, utf8IncompleteSuffixLength("a\x80\x80\x80\x80"));

   // a complete sequence followed by nothing more is not truncated, even
   // though its last bytes are continuation bytes
   EXPECT_EQ(0u, utf8IncompleteSuffixLength("\xE2\x94\x80"));
}

} // end namespace string_utils
} // end namespace core
} // end namespace rstudio
