/*
 * TermBufferParserTests.cpp
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

#include <core/text/TermBufferParser.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace tests {

const char *pStart1 = "\033[?1049h";
const char *pStart2 = "\033[?1047h";
const char *pStart3 = "\033[?47h";
const char *pEnd1 =   "\033[?1049l";
const char *pEnd2 =   "\033[?1047l";
const char *pEnd3 =   "\033[?47l";

TEST(TermBufferParseTest, EmptyInput)
{
   std::string input;
   std::string newStr = core::text::stripSecondaryBuffer(input, nullptr);
   ASSERT_TRUE(newStr.empty());
}

TEST(TermBufferParseTest, EmptyInputPreservesTrueAltmode)
{
   std::string input;
   bool altMode = true;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_TRUE(altMode == true);
}

TEST(TermBufferParseTest, EmptyInputPreservesFalseAltmode)
{
   std::string input;
   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_TRUE(altMode == false);
}

TEST(TermBufferParseTest, NoEscapeCodes)
{
   std::string input = "Hello World!\nHow are you today?";
   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(input, newStr);
}

TEST(TermBufferParseTest, EscapeCharacterAtStart)
{
   std::string input = "\033";
   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(input, newStr);
}

TEST(TermBufferParseTest, MultipleEscapeCharacters)
{
   std::string input = "\033\033\033\033\033";
   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(input, newStr);
}

TEST(TermBufferParseTest, OtherESCEscapeCode)
{
   std::string input = "Hello World!\nHow \033[000l are you today?";
   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(input, newStr);
}

TEST(TermBufferParseTest, OtherESCQuestionEscapeCode)
{
   std::string input = "Hello. How\033[?000l are you today?";
   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(input, newStr);
}

TEST(TermBufferParseTest, OtherESCQuestionXEscapeCode)
{
   std::string input = "Hello. How\033[?x047h are you today?";
   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(input, newStr);
}

TEST(TermBufferParseTest, ESCEscapeCodePositions)
{
   std::string input = "\033[?Hello World!\nHow \033[000l are you today?\033 Great!";
   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(input, newStr);
}

TEST(TermBufferParseTest, AltBufferModeReturnsEmptyString)
{
   std::string input = "None of this should be returned!";
   bool altMode = true;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_TRUE(newStr.empty());
   ASSERT_TRUE(altMode == true);
}

TEST(TermBufferParseTest, AltBufferWithMultipleStarts)
{
   std::string input = "None of ";
   input.append(pStart1);
   input.append(" this should \nbe in the output. ");
   input.append(pStart2);
   input.append(pStart3);
   input.append("The end.");
   bool altMode = true;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_TRUE(newStr.empty());
   ASSERT_TRUE(altMode == true);
}

TEST(TermBufferParseTest, EmptyAltModeInMiddle)
{
   std::string input = "Once upon ";
   input.append(pStart1);
   input.append(pEnd1);
   input.append("a time.");
   std::string expect("Once upon a time.");

   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(newStr, expect);
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, EmptyAltModeInMiddleVar2)
{
   std::string input = "Once upon ";
   input.append(pStart2);
   input.append(pEnd2);
   input.append("a time.");
   std::string expect("Once upon a time.");

   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(newStr, expect);
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, EmptyAltModeInMiddleVar3)
{
   std::string input = "Once upon ";
   input.append(pStart3);
   input.append(pEnd3);
   input.append("a time.");
   std::string expect("Once upon a time.");

   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(newStr, expect);
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, NonEmptyAltModeInMiddle)
{
   std::string input = "Once upon ";
   input.append(pStart1);
   input.append("a bunch of random stuff \033[?freddy the Wonder Llama");
   input.append(pEnd1);
   input.append("a time.");
   std::string expect("Once upon a time.");

   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(newStr, expect);
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, NonEmptyAltModeWithMultipleStarts)
{
   std::string input = "Once upon ";
   input.append(pStart3);
   input.append("a bunch of random stuff \033[?freddy the Wonder Llama");
   input.append(pStart1);
   input.append("zoom");
   input.append(pEnd1);
   input.append("a time.");
   std::string expect("Once upon a time.");

   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(newStr, expect);
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, NoStartsMultipleEnds)
{
   std::string input;
   input.append(pEnd1);
   input.append("Once upon ");
   input.append(pEnd2);
   input.append("a time.");
   input.append(pEnd3);
   std::string expect("Once upon a time.");

   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      ASSERT_EQ(newStr, expect);
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, StartsAltModeEndsAtEnd)
{
   std::string input = "Once upon a time.";
   input.append(pEnd3);
   bool altMode = true;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_TRUE(newStr.empty());
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, StartsAltModeEndsAtStart)
{
   std::string input;
   input.append(pEnd1);
   input.append("Once upon a time.");
   std::string expect("Once upon a time.");
   bool altMode = true;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(expect.compare(newStr), 0);
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, StartsAltModeEndsInMiddle)
{
   std::string input;
   input.append("Once upon a time.");
   input.append(pEnd1);
   input.append("There was a cat.");
   std::string expect("There was a cat.");

   bool altMode = true;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(expect.compare(newStr), 0);
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, StartsAltModeEndsRestarts)
{
   std::string input;
   input.append("Once upon a time.");
   input.append(pEnd1);
   input.append("There was a cat.");
   input.append(pStart1);
   input.append("Meow");
   input.append(pEnd1);
   input.append(" The end.");
   std::string expect("There was a cat. The end.");

   bool altMode = true;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(expect.compare(newStr), 0);
   ASSERT_FALSE(altMode);
}

TEST(TermBufferParseTest, MultipleAltModeRuns)
{
   std::string input;
   input.append("Once upon a time.");
   input.append(pStart1);
   input.append("There was a dog.");
   input.append(pEnd1);
   input.append(" There was a cat.");
   input.append(pStart1);
   input.append(" Bark\033];~~\n Bark!");
   input.append(pEnd1);
   input.append(" Meow.");
   std::string expect("Once upon a time. There was a cat. Meow.");

   bool altMode = false;
   std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
   ASSERT_EQ(expect.compare(newStr), 0);
   ASSERT_FALSE(altMode);
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
