/*
 * TermBufferParserTests.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <core/text/TermBufferParser.hpp>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace tests {

const char *pStart1 = "\033[?1049h";
const char *pStart2 = "\033[?1047h";
const char *pStart3 = "\033[?47h";
const char *pEnd1 =   "\033[?1049l";
const char *pEnd2 =   "\033[?1047l";
const char *pEnd3 =   "\033[?47l";

TEST_CASE("Terminal Buffer Mode Parsing")
{
   SECTION("Empty input")
   {
      std::string input;
      std::string newStr = core::text::stripSecondaryBuffer(input, NULL);
      CHECK(newStr.empty());
   }

   SECTION("Empty input doesn't change existing true altmode")
   {
      std::string input;
      bool altMode = true;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(altMode == true);
   }

   SECTION("Empty input doesn't change existing false altmode")
   {
      std::string input;
      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(altMode == false);
   }

   SECTION("No escape codes")
   {
      std::string input = "Hello World!\nHow are you today?";
      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(input.compare(newStr) == 0);
   }

   SECTION("Escape character at start")
   {
      std::string input = "\033";
      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(input.compare(newStr) == 0);
   }

   SECTION("Multiple Escape characters")
   {
      std::string input = "\033\033\033\033\033";
      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(input.compare(newStr) == 0);
   }

   SECTION("Other ESC[ escape code")
   {
      std::string input = "Hello World!\nHow \033[000l are you today?";
      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(input.compare(newStr) == 0);
   }

   SECTION("Other ESC[? escape code")
   {
      std::string input = "Hello. How\033[?000l are you today?";
      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(input.compare(newStr) == 0);
   }

   SECTION("Other ESC[?x escape code")
   {
      std::string input = "Hello. How\033[?x047h are you today?";
      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(input.compare(newStr) == 0);
   }

   SECTION("Other ESC[ escape code at start, middle and end")
   {
      std::string input = "\033[?Hello World!\nHow \033[000l are you today?\033 Great!";
      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(input.compare(newStr) == 0);
   }

   SECTION("Alternate buffer mode returns zero string")
   {
      std::string input = "None of this should be returned!";
      bool altMode = true;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(newStr.empty());
      CHECK(altMode == true);
   }

   SECTION("Alternate buffer mode with additional start sequences")
   {
      std::string input = "None of ";
      input.append(pStart1);
      input.append(" this should \nbe in the output. ");
      input.append(pStart2);
      input.append(pStart3);
      input.append("The end.");
      bool altMode = true;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(newStr.empty());
      CHECK(altMode == true);
   }

   SECTION("Empty alt-mode in middle")
   {
      std::string input = "Once upon ";
      input.append(pStart1);
      input.append(pEnd1);
      input.append("a time.");
      std::string expect("Once upon a time.");

      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(newStr.compare(expect) == 0);
      CHECK(altMode == false);
   }

   SECTION("Empty alt-mode in middle (variation 2)")
   {
      std::string input = "Once upon ";
      input.append(pStart2);
      input.append(pEnd2);
      input.append("a time.");
      std::string expect("Once upon a time.");

      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(newStr.compare(expect) == 0);
      CHECK(altMode == false);
   }

   SECTION("Empty alt-mode in middle (variation 3)")
   {
      std::string input = "Once upon ";
      input.append(pStart3);
      input.append(pEnd3);
      input.append("a time.");
      std::string expect("Once upon a time.");

      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(newStr.compare(expect) == 0);
      CHECK(altMode == false);
   }

   SECTION("Non-empty alt-mode in middle")
   {
      std::string input = "Once upon ";
      input.append(pStart1);
      input.append("a bunch of random stuff \033[?freddy the Wonder Llama");
      input.append(pEnd1);
      input.append("a time.");
      std::string expect("Once upon a time.");

      bool altMode = false;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(newStr.compare(expect) == 0);
      CHECK(altMode == false);
   }

   SECTION("Non-empty alt-mode in middle containing multiple starts")
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
      CHECK(newStr.compare(expect) == 0);
      CHECK(altMode == false);
   }

   SECTION("No-starts and multiple ends")
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
      CHECK(newStr.compare(expect) == 0);
      CHECK(altMode == false);
   }

   SECTION("Starts in alt-mode, ends at the end")
   {
      std::string input = "Once upon a time.";
      input.append(pEnd3);
      bool altMode = true;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(newStr.empty());
      CHECK(altMode == false);
   }

   SECTION("Starts in alt-mode, ends at the start")
   {
      std::string input;
      input.append(pEnd1);
      input.append("Once upon a time.");
      std::string expect("Once upon a time.");
      bool altMode = true;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(expect.compare(newStr) == 0);
      CHECK(altMode == false);
   }

   SECTION("Starts in alt-mode, ends in the middle")
   {
      std::string input;
      input.append("Once upon a time.");
      input.append(pEnd1);
      input.append("There was a cat.");
      std::string expect("There was a cat.");

      bool altMode = true;
      std::string newStr = core::text::stripSecondaryBuffer(input, &altMode);
      CHECK(expect.compare(newStr) == 0);
      CHECK(altMode == false);
   }

   SECTION("Starts in alt-mode, ends and restarts")
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
      CHECK(expect.compare(newStr) == 0);
      CHECK(altMode == false);
   }

   SECTION("Multiple alt-mode runs")
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
      CHECK(expect.compare(newStr) == 0);
      CHECK(altMode == false);
   }
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
