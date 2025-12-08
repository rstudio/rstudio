/*
 * DcfParserTests.cpp
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

#include <shared_core/Error.hpp>
#include <core/text/DcfParser.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace tests {

TEST(DcfParserTest, ParseSimpleDcf)
{
   std::string input = "Test: Value\nTest2: Value2\nHello: World";

   std::map<std::string, std::string> fields;
   std::string err;

   Error error = text::parseDcfFile(input, true, &fields, &err);
   ASSERT_FALSE(error);
   ASSERT_TRUE(err.empty());

   ASSERT_EQ(3u, fields.size());
   ASSERT_EQ("Value", fields["Test"]);
   ASSERT_EQ("Value2", fields["Test2"]);
   ASSERT_EQ("World", fields["Hello"]);
}


TEST(DcfParserTest, ParseSingleAsMultiDcf)
{
   std::string input = "A: Apple\nB: Banana\nC: Car";

   Error error = text::parseMultiDcfFile(input, true,
    [&](int lineNumber, const std::map<std::string, std::string>& fields) -> Error
    {
       auto copy = fields;

       if (copy.size() != 3u) 
          return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
       if (copy["A"] != "Apple")
          return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
       if (copy["B"] != "Banana")
          return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
       if (copy["C"] != "Car") 
          return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

       return Success();
    });
   ASSERT_FALSE(error);
}


TEST(DcfParserTest, ParseSimpleMultiDcf)
{
   std::string input = "A: Apple\nB: Banana\nC: Car\n\n"
         "A: Account\nB: Banker\nC: Cash";

   int i = 0;
   Error error = text::parseMultiDcfFile(input, true,
    [&](int lineNumber, const std::map<std::string, std::string>& fields) -> Error
    {
       auto copy = fields;

   EXPECT_EQ(3u, copy.size());
   EXPECT_EQ((i == 0 ? "Apple" : "Account"), copy["A"]);
   EXPECT_EQ((i == 0 ? "Banana" : "Banker"), copy["B"]);
   EXPECT_EQ((i == 0 ? "Car" : "Cash"), copy["C"]);
   EXPECT_EQ((i == 0 ? 1 : 5), lineNumber);

       ++i;
       return Success();
    });
   ASSERT_FALSE(error);
}


TEST(DcfParserTest, ParseMultiDcfWithTrailingWhitespace)
{
   std::string input = "A: Apple\nB: Banana\nC: Car\n      \n"
         "A: Account\nB: Banker\nC: Cash";

   int i = 0;
   Error error = text::parseMultiDcfFile(input, true,
    [&](int lineNumber, const std::map<std::string, std::string>& fields) -> Error
    {
       auto copy = fields;

   EXPECT_EQ(3u, copy.size());
   EXPECT_EQ((i == 0 ? "Apple" : "Account"), copy["A"]);
   EXPECT_EQ((i == 0 ? "Banana" : "Banker"), copy["B"]);
   EXPECT_EQ((i == 0 ? "Car" : "Cash"), copy["C"]);
   EXPECT_EQ((i == 0 ? 1 : 5), lineNumber);

       ++i;
       return Success();
    });
   ASSERT_FALSE(error);
}


TEST(DcfParserTest, ParseMultiDcfWithIndentation)
{
   std::string input = "A: Apple\nB: Banana\nC: Car\n      \n"
         "A: Account\nB: Banker\nC: Cash\n\n"
         "A: This is a long paragraph\n"
         " that has indentation. It is supposed to concat\n"
         " together\n"
         "#B: Ball\n"
         "C: Cat\n\n\n";

   int i = 0;
   Error error = text::parseMultiDcfFile(input, true,
    [&](int lineNumber, const std::map<std::string, std::string>& fields) -> Error
    {
       auto copy = fields;

   EXPECT_EQ((i != 2 ? 3u : 2u), copy.size());
   EXPECT_EQ((i == 0 ? "Apple" : i == 1 ? "Account" : "This is a long paragraph\nthat has indentation. It is supposed to concat\ntogether"), copy["A"]);
   EXPECT_EQ((i == 0 ? "Banana" : i == 1 ? "Banker" : std::string()), copy["B"]);
   EXPECT_EQ((i == 0 ? "Car" : i == 1 ? "Cash" : "Cat"), copy["C"]);
   EXPECT_EQ((i == 0 ? 1 : i == 1 ? 5 : 9), lineNumber);

       ++i;
       return Success();
    });
   ASSERT_FALSE(error);
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
