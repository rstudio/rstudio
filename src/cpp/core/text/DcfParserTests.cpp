/*
 * DcfParserTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <shared_core/Error.hpp>
#include <core/text/DcfParser.hpp>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace tests {

TEST_CASE("DcfParser")
{
   SECTION("Can parse simple dcf file")
   {
      std::string input = "Test: Value\nTest2: Value2\nHello: World";

      std::map<std::string, std::string> fields;
      std::string err;

      REQUIRE_FALSE(text::parseDcfFile(input, true, &fields, &err));
      CHECK(err.empty());

      REQUIRE(fields.size() == 3);
      CHECK(fields["Test"] == "Value");
      CHECK(fields["Test2"] == "Value2");
      CHECK(fields["Hello"] == "World");
   }

   SECTION("Can parse single dcf as multi dcf file")
   {
      std::string input = "A: Apple\nB: Banana\nC: Car";

      REQUIRE_FALSE(text::parseMultiDcfFile(input, true,
       [&](int lineNumber, const std::map<std::string, std::string>& fields) -> Error
       {
          auto copy = fields;

          REQUIRE(copy.size() == 3);
          CHECK(copy["A"] == "Apple");
          CHECK(copy["B"] == "Banana");
          CHECK(copy["C"] == "Car");

          return Success();
       }));
   }

   SECTION("Can parse simple multi dcf file")
   {
      std::string input = "A: Apple\nB: Banana\nC: Car\n\n"
            "A: Account\nB: Banker\nC: Cash";

      int i = 0;
      REQUIRE_FALSE(text::parseMultiDcfFile(input, true,
       [&](int lineNumber, const std::map<std::string, std::string>& fields) -> Error
       {
          auto copy = fields;

          REQUIRE(copy.size() == 3);
          CHECK(copy["A"] == (i == 0 ? "Apple" : "Account"));
          CHECK(copy["B"] == (i == 0 ? "Banana" : "Banker"));
          CHECK(copy["C"] == (i == 0 ? "Car" : "Cash"));
          CHECK(lineNumber == (i == 0 ? 1 : 5));

          ++i;
          return Success();
       }));
   }

   SECTION("Can parse multi dcf file with trailing whitespace on blank line")
   {
      std::string input = "A: Apple\nB: Banana\nC: Car\n      \n"
            "A: Account\nB: Banker\nC: Cash";

      int i = 0;
      REQUIRE_FALSE(text::parseMultiDcfFile(input, true,
       [&](int lineNumber, const std::map<std::string, std::string>& fields) -> Error
       {
          auto copy = fields;

          REQUIRE(copy.size() == 3);
          CHECK(copy["A"] == (i == 0 ? "Apple" : "Account"));
          CHECK(copy["B"] == (i == 0 ? "Banana" : "Banker"));
          CHECK(copy["C"] == (i == 0 ? "Car" : "Cash"));
          CHECK(lineNumber == (i == 0 ? 1 : 5));

          ++i;
          return Success();
       }));
   }

   SECTION("Can parse multi dcf file with indentation")
   {
      std::string input = "A: Apple\nB: Banana\nC: Car\n      \n"
            "A: Account\nB: Banker\nC: Cash\n\n"
            "A: This is a long paragraph\n"
            " that has indentation. It is supposed to concat\n"
            " together\n"
            "#B: Ball\n"
            "C: Cat\n\n\n";

      int i = 0;
      REQUIRE_FALSE(text::parseMultiDcfFile(input, true,
       [&](int lineNumber, const std::map<std::string, std::string>& fields) -> Error
       {
          auto copy = fields;

          REQUIRE(copy.size() == (i != 2 ? 3 : 2));
          CHECK(copy["A"] == (i == 0 ? "Apple" : i == 1 ? "Account" : "This is a long paragraph\nthat has indentation. It is supposed to concat\ntogether"));
          CHECK(copy["B"] == (i == 0 ? "Banana" : i == 1 ? "Banker" : std::string()));
          CHECK(copy["C"] == (i == 0 ? "Car" : i == 1 ? "Cash" : "Cat"));
          CHECK(lineNumber == (i == 0 ? 1 : i == 1 ? 5 : 9));

          ++i;
          return Success();
       }));
   }
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
