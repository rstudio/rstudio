/*
 * TextCursorTests.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <core/text/TextCursor.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace text {
namespace tests {

TEST_CASE("TextCursor")
{
   SECTION("Navigation")
   {
      std::string text("a;b:c.d");
      TextCursor cursor(text);
      CHECK(cursor.offset() == 0);
      CHECK(*cursor == 'a');
      
      // consume one character
      CHECK(cursor.consume('a'));
      CHECK(*cursor == ';');
      
      // check that we fail to consume the wrong character
      CHECK(!cursor.consume('z'));
      
      // check that we can consume a string
      CHECK(cursor.consume(";b"));
      CHECK(*cursor == ':');
      
      // check that we can consume until a sequence
      CHECK(cursor.consumeUntil(".d"));
      CHECK(*cursor == '.');
      
      // check that we can advance to the end
      CHECK(cursor.advance());
      CHECK(*cursor == 'd');
      
      // check that we can't advance once we've reached
      // the end of our string
      CHECK(!cursor.advance());
   }
}

} // end namespace tests
} // end namespace text
} // end namespace core
} // end namespace rstudio
