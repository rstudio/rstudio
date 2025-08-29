/*
 * TextCursorTests.cpp
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

#include <core/text/TextCursor.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace text {
namespace tests {

TEST(TextCursorTest, Navigation)
{
   std::string text("a;b:c.d");
   TextCursor cursor(text);
   EXPECT_EQ(cursor.offset(), 0u);
   EXPECT_EQ(*cursor, 'a');
   
   // consume one character
   EXPECT_TRUE(cursor.consume('a'));
   EXPECT_EQ(*cursor, ';');
   
   // check that we fail to consume the wrong character
   EXPECT_FALSE(cursor.consume('z'));
   
   // check that we can consume a string
   EXPECT_TRUE(cursor.consume(";b"));
   EXPECT_EQ(*cursor, ':');
   
   // check that we can consume until a sequence
   EXPECT_TRUE(cursor.consumeUntil(".d"));
   EXPECT_EQ(*cursor, '.');
   
   // check that we can advance to the end
   EXPECT_TRUE(cursor.advance());
   EXPECT_EQ(*cursor, 'd');
   
   // check that we can't advance once we've reached
   // the end of our string
   EXPECT_FALSE(cursor.advance());
}

} // end namespace tests
} // end namespace text
} // end namespace core
} // end namespace rstudio
