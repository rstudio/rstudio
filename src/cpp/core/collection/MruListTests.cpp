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
      std::list<std::string> listContents = list.contents();
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
      std::list<std::string> listContents = list.contents();
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
      std::list<std::string> listContents = list.contents();
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
      std::list<std::string> listContents = list.contents();
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
      std::list<std::string> listContents = list.contents();
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

      std::list<std::string> listContents = list.contents();
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

      std::list<std::string> listContents = list.contents();
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
      std::list<std::string> listContents = list.contents();
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
      std::list<std::string> listContents = list.contents();
      std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
      REQUIRE(vectorContents[0] == "item2");
      REQUIRE(vectorContents[1] == "item1");
      REQUIRE(vectorContents[2] == "item3");
      REQUIRE_FALSE(listFilePath.remove());
   }
} // test_context

} // namespace tests
} // namespace collection
} // namespace core
} // namespace rstudio

