/*
 * MruListTests.cpp
 *
 * Copyright (C) 2023 by Posit Software, PBC
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


#include <tests/TestThat.hpp>

#include <core/collection/MruList.hpp>
#include <shared_core/FilePath.hpp>
#include <vector>

using namespace rstudio::core;

namespace rstudio {
namespace core {
namespace collection {
namespace tests {

test_context("MruList Tests")
{
   test_that("Can create empty file")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));

      MruList list(listFilePath, 10);

      REQUIRE_FALSE(list.initialize());
      REQUIRE(listFilePath.exists());
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append and access an item")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");

      REQUIRE(list.size() == 1);
      REQUIRE(list.contents().size() == 1);
      REQUIRE(list.contents().front() == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append and access an item with leading and trailing whitespace")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("  item1    ");

      REQUIRE(list.size() == 1);
      REQUIRE(list.contents().size() == 1);
      REQUIRE(list.contents().front() == "  item1    ");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Leading and trailing whitespace is stripped when items are read from disk")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("  item1    ");

      MruList list2(listFilePath, 10);
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.size() == 1);
      REQUIRE(list2.contents().size() == 1);
      REQUIRE(list2.contents().front() == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append and access multiple items")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.append("item2");
      list.append("item3");

      REQUIRE(list.size() == 3);
      REQUIRE(list.contents().size() == 3);
      REQUIRE(list.contents().front() == "item1");
      REQUIRE(list.contents().back() == "item3");
      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item1");
      REQUIRE(vectorContents[1] == "item2");
      REQUIRE(vectorContents[2] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can prepend and access an item")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.prepend("item1");

      REQUIRE(list.size() == 1);
      REQUIRE(list.contents().size() == 1);
      REQUIRE(list.contents().front() == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can prepend and access multiple items")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.prepend("item1");
      list.prepend("item2");
      list.prepend("item3");
      list.prepend("item4");

      REQUIRE(list.size() == 4);
      REQUIRE(list.contents().size() == 4);
      REQUIRE(list.contents().front() == "item4");
      REQUIRE(list.contents().back() == "item1");
      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item4");
      REQUIRE(vectorContents[1] == "item3");
      REQUIRE(vectorContents[2] == "item2");
      REQUIRE(vectorContents[3] == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can use mixture of append and prepend")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.prepend("item2");
      list.append("item3");
      list.append("item4");

      REQUIRE(list.size() == 4);
      REQUIRE(list.contents().size() == 4);
      REQUIRE(list.contents().front() == "item2");
      REQUIRE(list.contents().back() == "item4");
      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item2");
      REQUIRE(vectorContents[1] == "item1");
      REQUIRE(vectorContents[2] == "item3");
      REQUIRE(vectorContents[3] == "item4");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can use another mixture of append and prepend")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.prepend("item2");
      list.prepend("item3");
      list.append("item4");
      list.prepend("item5");
      list.append("item6");

      REQUIRE(list.size() == 6);
      REQUIRE(list.contents().size() == 6);
      REQUIRE(list.contents().front() == "item5");
      REQUIRE(list.contents().back() == "item6");
      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item5");
      REQUIRE(vectorContents[1] == "item3");
      REQUIRE(vectorContents[2] == "item2");
      REQUIRE(vectorContents[3] == "item1");
      REQUIRE(vectorContents[4] == "item4");
      REQUIRE(vectorContents[5] == "item6");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can clear empty list")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());
      REQUIRE(list.contents().size() == 0);

      list.clear();

      REQUIRE(list.contents().size() == 0);
      MruList list2(listFilePath, 10);
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.contents().size() == 0);
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can clear non-empty list")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());
      list.append("item1");
      list.append("item2");
      list.append("item3");
      REQUIRE(list.size() == 3);

      list.clear();

      REQUIRE(list.size() == 0);
      MruList list2(listFilePath, 10);
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.contents().size() == 0);
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can add and store the number of items indicated in constructor")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 4);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.prepend("item2");
      list.append("item3");
      list.prepend("item4");

      REQUIRE(list.size() == 4);
      REQUIRE(list.contents().size() == 4);
      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents.size() == 4);
      REQUIRE(vectorContents[0] == "item4");
      REQUIRE(vectorContents[1] == "item2");
      REQUIRE(vectorContents[2] == "item1");
      REQUIRE(vectorContents[3] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append more items than list size and items from opposite end of list are removed")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 4);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.append("item2");
      list.append("item3");
      list.append("item4");
      list.append("item5");
      list.append("item6");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents.size() == 4);
      REQUIRE(vectorContents[0] == "item3");
      REQUIRE(vectorContents[1] == "item4");
      REQUIRE(vectorContents[2] == "item5");
      REQUIRE(vectorContents[3] == "item6");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can prepend more items than list size and items from opposite end of list are removed")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 4);
      REQUIRE_FALSE(list.initialize());

      list.prepend("item1");
      list.prepend("item2");
      list.prepend("item3");
      list.prepend("item4");
      list.prepend("item5");
      list.prepend("item6");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents.size() == 4);
      REQUIRE(vectorContents[0] == "item6");
      REQUIRE(vectorContents[1] == "item5");
      REQUIRE(vectorContents[2] == "item4");
      REQUIRE(vectorContents[3] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append an existing item and it moves to the end of the list")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.append("item2");
      list.append("item3");
      list.append("item1");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item2");
      REQUIRE(vectorContents[1] == "item3");
      REQUIRE(vectorContents[2] == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can prepend an existing item and it moves to the front of the list")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.append("item2");
      list.append("item3");
      list.prepend("item2");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item2");
      REQUIRE(vectorContents[1] == "item1");
      REQUIRE(vectorContents[2] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can remove an item and it is no longer in the list")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.append("item2");
      list.append("item3");
      list.remove("item2");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item1");
      REQUIRE(vectorContents[1] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can remove an item that doesn't exist and the list is unchanged")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10);
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.append("item2");
      list.append("item3");
      list.remove("item4");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item1");
      REQUIRE(vectorContents[1] == "item2");
      REQUIRE(vectorContents[2] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }
   
   // ***************************************************************************
   //   tests for extra data mode
   // ***************************************************************************

   test_that("Can create empty file with extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));

      MruList list(listFilePath, 10, '\t');

      REQUIRE_FALSE(list.initialize());
      REQUIRE(listFilePath.exists());
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append and access an item with no extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("item1");

      REQUIRE(list.size() == 1);
      REQUIRE(list.contents().size() == 1);
      REQUIRE(list.contents().front() == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append and access an item with separator but no extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("item1\t");

      REQUIRE(list.size() == 1);
      REQUIRE(list.contents().size() == 1);
      REQUIRE(list.contents().front() == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append and access an item with extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("item1\tHello World!");

      REQUIRE(list.size() == 1);
      REQUIRE(list.contents().size() == 1);
      REQUIRE(list.contents().front() == "item1\tHello World!");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append and access an item with leading and trailing whitespace in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("  item1    ");

      REQUIRE(list.size() == 1);
      REQUIRE(list.contents().size() == 1);
      REQUIRE(list.contents().front() == "  item1    ");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Whitespace is stripped when items are read from disk in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("  item1    ");

      MruList list2(listFilePath, 10, '\t');
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.size() == 1);
      REQUIRE(list2.contents().size() == 1);
      REQUIRE(list2.contents().front() == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Whitespace is stripped when items are read from disk in extra data mode with extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("  item1 \t Hello World!   ");
      list.append("   item2   ");

      MruList list2(listFilePath, 10, '\t');
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.size() == 2);
      REQUIRE(list2.contents().size() == 2);
      REQUIRE(list2.contents().front() == "item1 \t Hello World!");
      REQUIRE(list2.contents().back() == "item2");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append and access multiple items in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.append("item2\tThe Second Item");
      list.append("item3");
      list.append("item4\tThe Fourth Item");

      REQUIRE(list.size() == 4);
      auto contents = list.contents();
      REQUIRE(contents.size() == 4);
      REQUIRE(contents.front() == "item1");
      REQUIRE(contents.back() == "item4\tThe Fourth Item");
      std::vector<std::string> vectorContents(contents.begin(), contents.end());
      REQUIRE(vectorContents[0] == "item1");
      REQUIRE(vectorContents[1] == "item2\tThe Second Item");
      REQUIRE(vectorContents[2] == "item3");
      REQUIRE(vectorContents[3] == "item4\tThe Fourth Item");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can read multiple items from file in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("item1\tThe First Item");
      list.append("item2\tThe Second Item");
      list.append("item3");
      list.append("item4\tThe Fourth Item");

      MruList list2(listFilePath, 10, '\t');
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.size() == 4);
      auto contents = list2.contents();
      REQUIRE(contents.size() == 4);
      REQUIRE(contents.front() == "item1\tThe First Item");
      REQUIRE(contents.back() == "item4\tThe Fourth Item");
      std::list<std::string> listContents = list2.contents();
      std::vector<std::string> vectorContents(contents.begin(), contents.end());
      REQUIRE(vectorContents[0] == "item1\tThe First Item");
      REQUIRE(vectorContents[1] == "item2\tThe Second Item");
      REQUIRE(vectorContents[2] == "item3");
      REQUIRE(vectorContents[3] == "item4\tThe Fourth Item");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can prepend and access an item in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '|');
      REQUIRE_FALSE(list.initialize());

      list.prepend("item1");

      REQUIRE(list.size() == 1);
      REQUIRE(list.contents().size() == 1);
      REQUIRE(list.contents().front() == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can prepend and access an item in extra data mode with extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '|');
      REQUIRE_FALSE(list.initialize());

      list.prepend("item1|Hello World!");

      REQUIRE(list.size() == 1);
      REQUIRE(list.contents().size() == 1);
      REQUIRE(list.contents().front() == "item1|Hello World!");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can prepend and access multiple items in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '|');
      REQUIRE_FALSE(list.initialize());

      list.prepend("item1");
      list.prepend("item2|Hello");
      list.prepend("item3");
      list.prepend("item4|World");

      REQUIRE(list.size() == 4);
      REQUIRE(list.contents().size() == 4);
      REQUIRE(list.contents().front() == "item4|World");
      REQUIRE(list.contents().back() == "item1");
      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item4|World");
      REQUIRE(vectorContents[1] == "item3");
      REQUIRE(vectorContents[2] == "item2|Hello");
      REQUIRE(vectorContents[3] == "item1");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can use mixture of append and prepend in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '@');
      REQUIRE_FALSE(list.initialize());

      list.append("item1@once");
      list.prepend("item2@upon");
      list.append("item3@a time");
      list.append("item4@there was a");

      REQUIRE(list.size() == 4);
      REQUIRE(list.contents().size() == 4);
      REQUIRE(list.contents().front() == "item2@upon");
      REQUIRE(list.contents().back() == "item4@there was a");
      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item2@upon");
      REQUIRE(vectorContents[1] == "item1@once");
      REQUIRE(vectorContents[2] == "item3@a time");
      REQUIRE(vectorContents[3] == "item4@there was a");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can use another mixture of append and prepend in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.prepend("item2");
      list.prepend("item3");
      list.append("item4");
      list.prepend("item5\tThe best item ever!!!");
      list.append("item6\t");
      list.prepend("item5\tBack and even better than before");

      REQUIRE(list.size() == 6);
      REQUIRE(list.contents().size() == 6);
      REQUIRE(list.contents().front() == "item5\tBack and even better than before");
      REQUIRE(list.contents().back() == "item6");
      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item5\tBack and even better than before");
      REQUIRE(vectorContents[1] == "item3");
      REQUIRE(vectorContents[2] == "item2");
      REQUIRE(vectorContents[3] == "item1");
      REQUIRE(vectorContents[4] == "item4");
      REQUIRE(vectorContents[5] == "item6");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can clear empty list in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());
      REQUIRE(list.contents().size() == 0);

      list.clear();

      REQUIRE(list.contents().size() == 0);
      MruList list2(listFilePath, 10, '\t');
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.contents().size() == 0);
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can clear non-empty list in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());
      list.append("item1");
      list.append("item2");
      list.append("item3");
      REQUIRE(list.size() == 3);

      list.clear();

      REQUIRE(list.size() == 0);
      MruList list2(listFilePath, 10);
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.contents().size() == 0);
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can add and store the number of items indicated in constructor in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 4, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("item1\tOnce");
      list.prepend("item2\tMore");
      list.append("item3\tInto");
      list.prepend("item4");

      REQUIRE(list.size() == 4);
      REQUIRE(list.contents().size() == 4);
      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents.size() == 4);
      REQUIRE(vectorContents[0] == "item4");
      REQUIRE(vectorContents[1] == "item2\tMore");
      REQUIRE(vectorContents[2] == "item1\tOnce");
      REQUIRE(vectorContents[3] == "item3\tInto");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append more items than list size and items from opposite end are removed in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 4, '\t');
      REQUIRE_FALSE(list.initialize());

      list.append("item1");
      list.append("item2\tPlease don't delete me!");
      list.append("item3");
      list.append("item4");
      list.append("item5");
      list.append("item6\tLast but not least.");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents.size() == 4);
      REQUIRE(vectorContents[0] == "item3");
      REQUIRE(vectorContents[1] == "item4");
      REQUIRE(vectorContents[2] == "item5");
      REQUIRE(vectorContents[3] == "item6\tLast but not least.");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Extra data for a deleted entry doesn't come back if same entry is added again without extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 4, '\t');
      REQUIRE_FALSE(list.initialize());
      list.append("item1");
      list.append("item2\tPlease don't delete me!");

      list.remove("item2");
      list.append("item2");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents.size() == 2);
      REQUIRE(vectorContents[0] == "item1");
      REQUIRE(vectorContents[1] == "item2");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can prepend more items than list size and items from opposite end of list are removed in extra data mode")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 4, '\t');
      REQUIRE_FALSE(list.initialize());

      list.prepend("item1");
      list.prepend("item2");
      list.prepend("item3\tThree is ok");
      list.prepend("item4");
      list.prepend("item5\tFive is great");
      list.prepend("item6\tSix is the best");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents.size() == 4);
      REQUIRE(vectorContents[0] == "item6\tSix is the best");
      REQUIRE(vectorContents[1] == "item5\tFive is great");
      REQUIRE(vectorContents[2] == "item4");
      REQUIRE(vectorContents[3] == "item3\tThree is ok");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can append an existing item and it moves to the end of the list and updates its extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());
      list.append("item1\tFirst Try");
      list.append("item2");
      list.append("item3");

      list.append("item1\tNew and improved?");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item2");
      REQUIRE(vectorContents[1] == "item3");
      REQUIRE(vectorContents[2] == "item1\tNew and improved?");
      MruList list2(listFilePath, 10, '\t');
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.size() == 3);
      auto list2Contents = list2.contents();
      std::vector<std::string> vectorContents2(list2Contents.begin(), list2Contents.end());
      REQUIRE(vectorContents2[0] == "item2");
      REQUIRE(vectorContents2[1] == "item3");
      REQUIRE(vectorContents2[2] == "item1\tNew and improved?");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can prepend an existing item and it moves to the front of the list with extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());
      list.append("item1");
      list.append("item2");
      list.append("item3");

      list.prepend("item2");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item2");
      REQUIRE(vectorContents[1] == "item1");
      REQUIRE(vectorContents[2] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can remove an item and it is no longer in the list with extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());
      list.append("item1\tItem 1");
      list.append("item2\tItem 2");
      list.append("item3\tItem 3");

      list.remove("item2\tAnything goes here for item 2");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item1\tItem 1");
      REQUIRE(vectorContents[1] == "item3\tItem 3");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can remove an item that doesn't exist and the list is unchanged with extra data")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());
      list.append("item1");
      list.append("item2\tTwo");
      list.append("item3");

      list.remove("item4\tFour");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item1");
      REQUIRE(vectorContents[1] == "item2\tTwo");
      REQUIRE(vectorContents[2] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }

   test_that("Can update an item's extra data without changing its position in the list")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());
      list.append("item1\tFirst Try");
      list.append("item2\tSo Great");
      list.append("item3\tThree is better");

      list.updateExtraData("item2", "Now two is ultra great!");
      list.updateExtraData("item1\tSecond Try!");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item1\tSecond Try!");
      REQUIRE(vectorContents[1] == "item2\tNow two is ultra great!");
      REQUIRE(vectorContents[2] == "item3\tThree is better");
      MruList list2(listFilePath, 10, '\t');
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.size() == 3);
      auto list2Contents = list2.contents();
      std::vector<std::string> vectorContents2(list2Contents.begin(), list2Contents.end());
      REQUIRE(vectorContents2[0] == "item1\tSecond Try!");
      REQUIRE(vectorContents2[1] == "item2\tNow two is ultra great!");
      REQUIRE(vectorContents2[2] == "item3\tThree is better");
      REQUIRE_FALSE(listFilePath.remove());
   }

  test_that("Updating an item's extra text to empty string removes the extra text from the list")
   {
      FilePath listFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".list", listFilePath));
      MruList list(listFilePath, 10, '\t');
      REQUIRE_FALSE(list.initialize());
      list.append("item1\tFirst Try");
      list.append("item2\tSo Great");
      list.append("item3\tThree is better");

      list.updateExtraData("item2");
      list.updateExtraData("item3", "");

      auto listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item1\tFirst Try");
      REQUIRE(vectorContents[1] == "item2");
      REQUIRE(vectorContents[2] == "item3");
      MruList list2(listFilePath, 10, '\t');
      REQUIRE_FALSE(list2.initialize());
      REQUIRE(list2.size() == 3);
      auto list2Contents = list2.contents();
      std::vector<std::string> vectorContents2(list2Contents.begin(), list2Contents.end());
      REQUIRE(vectorContents2[0] == "item1\tFirst Try");
      REQUIRE(vectorContents2[1] == "item2");
      REQUIRE(vectorContents2[2] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }

} // test_context
} // namespace tests
} // namespace collection
} // namespace core
} // namespace rstudio
