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


#include <gtest/gtest.h>

#include <core/collection/MruList.hpp>
#include <shared_core/FilePath.hpp>
#include <vector>
#include <list>
#include <string>

using namespace rstudio::core;

namespace rstudio {
namespace core {
namespace collection {
namespace tests {

TEST(MruListTest, CanCreateEmptyFile)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));

   MruList list(listFilePath, 10);

   ASSERT_FALSE(list.initialize());
   EXPECT_TRUE(listFilePath.exists());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAndAccessAnItem)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("item1");

   EXPECT_EQ(1u, list.size());
   EXPECT_EQ(1u, list.contents().size());
   EXPECT_EQ(std::string("item1"), list.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAndAccessAnItemWithLeadingAndTrailingWhitespace)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("  item1    ");

   EXPECT_EQ(1u, list.size());
   EXPECT_EQ(1u, list.contents().size());
   EXPECT_EQ(std::string("  item1    "), list.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, LeadingAndTrailingWhitespaceIsStrippedWhenItemsAreReadFromDisk)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("  item1    ");

   MruList list2(listFilePath, 10);
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(1u, list2.size());
   EXPECT_EQ(1u, list2.contents().size());
   EXPECT_EQ(std::string("item1"), list2.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAndAccessMultipleItems)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.append("item2");
   list.append("item3");

   EXPECT_EQ(3u, list.size());
   EXPECT_EQ(3u, list.contents().size());
   EXPECT_EQ(std::string("item1"), list.contents().front());
   EXPECT_EQ(std::string("item3"), list.contents().back());
   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item1"), vectorContents[0]);
   EXPECT_EQ(std::string("item2"), vectorContents[1]);
   EXPECT_EQ(std::string("item3"), vectorContents[2]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanPrependAndAccessAnItem)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.prepend("item1");

   EXPECT_EQ(1u, list.size());
   EXPECT_EQ(1u, list.contents().size());
   EXPECT_EQ(std::string("item1"), list.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanPrependAndAccessMultipleItems)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.prepend("item1");
   list.prepend("item2");
   list.prepend("item3");
   list.prepend("item4");

   EXPECT_EQ(4u, list.size());
   EXPECT_EQ(4u, list.contents().size());
   EXPECT_EQ(std::string("item4"), list.contents().front());
   EXPECT_EQ(std::string("item1"), list.contents().back());
   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item4"), vectorContents[0]);
   EXPECT_EQ(std::string("item3"), vectorContents[1]);
   EXPECT_EQ(std::string("item2"), vectorContents[2]);
   EXPECT_EQ(std::string("item1"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanUseMixtureOfAppendAndPrepend)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.prepend("item2");
   list.append("item3");
   list.append("item4");

   EXPECT_EQ(4u, list.size());
   EXPECT_EQ(4u, list.contents().size());
   EXPECT_EQ(std::string("item2"), list.contents().front());
   EXPECT_EQ(std::string("item4"), list.contents().back());
   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item2"), vectorContents[0]);
   EXPECT_EQ(std::string("item1"), vectorContents[1]);
   EXPECT_EQ(std::string("item3"), vectorContents[2]);
   EXPECT_EQ(std::string("item4"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanUseAnotherMixtureOfAppendAndPrepend)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.prepend("item2");
   list.prepend("item3");
   list.append("item4");
   list.prepend("item5");
   list.append("item6");

   EXPECT_EQ(6u, list.size());
   EXPECT_EQ(6u, list.contents().size());
   EXPECT_EQ(std::string("item5"), list.contents().front());
   EXPECT_EQ(std::string("item6"), list.contents().back());
   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item5"), vectorContents[0]);
   EXPECT_EQ(std::string("item3"), vectorContents[1]);
   EXPECT_EQ(std::string("item2"), vectorContents[2]);
   EXPECT_EQ(std::string("item1"), vectorContents[3]);
   EXPECT_EQ(std::string("item4"), vectorContents[4]);
   EXPECT_EQ(std::string("item6"), vectorContents[5]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanClearEmptyList)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());
   EXPECT_EQ(0u, list.contents().size());

   list.clear();

   EXPECT_EQ(0u, list.contents().size());
   MruList list2(listFilePath, 10);
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(0u, list2.contents().size());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanClearNonEmptyList)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());
   list.append("item1");
   list.append("item2");
   list.append("item3");
   EXPECT_EQ(3u, list.size());

   list.clear();

   EXPECT_EQ(0u, list.size());
   MruList list2(listFilePath, 10);
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(0u, list2.contents().size());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAddAndStoreTheNumberOfItemsIndicatedInConstructor)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 4);
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.prepend("item2");
   list.append("item3");
   list.prepend("item4");

   EXPECT_EQ(4u, list.size());
   EXPECT_EQ(4u, list.contents().size());
   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(4u, vectorContents.size());
   EXPECT_EQ(std::string("item4"), vectorContents[0]);
   EXPECT_EQ(std::string("item2"), vectorContents[1]);
   EXPECT_EQ(std::string("item1"), vectorContents[2]);
   EXPECT_EQ(std::string("item3"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendMoreItemsThanListSizeAndItemsFromOppositeEndOfListAreRemoved)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 4);
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.append("item2");
   list.append("item3");
   list.append("item4");
   list.append("item5");
   list.append("item6");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(4u, vectorContents.size());
   EXPECT_EQ(std::string("item3"), vectorContents[0]);
   EXPECT_EQ(std::string("item4"), vectorContents[1]);
   EXPECT_EQ(std::string("item5"), vectorContents[2]);
   EXPECT_EQ(std::string("item6"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanPrependMoreItemsThanListSizeAndItemsFromOppositeEndOfListAreRemoved)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 4);
   ASSERT_FALSE(list.initialize());

   list.prepend("item1");
   list.prepend("item2");
   list.prepend("item3");
   list.prepend("item4");
   list.prepend("item5");
   list.prepend("item6");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(4u, vectorContents.size());
   EXPECT_EQ(std::string("item6"), vectorContents[0]);
   EXPECT_EQ(std::string("item5"), vectorContents[1]);
   EXPECT_EQ(std::string("item4"), vectorContents[2]);
   EXPECT_EQ(std::string("item3"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAnExistingItemAndItMovesToTheEndOfTheList)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.append("item2");
   list.append("item3");
   list.append("item1");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item2"), vectorContents[0]);
   EXPECT_EQ(std::string("item3"), vectorContents[1]);
   EXPECT_EQ(std::string("item1"), vectorContents[2]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanPrependAnExistingItemAndItMovesToTheFrontOfTheList)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.append("item2");
   list.append("item3");
   list.prepend("item2");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item2"), vectorContents[0]);
   EXPECT_EQ(std::string("item1"), vectorContents[1]);
   EXPECT_EQ(std::string("item3"), vectorContents[2]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanRemoveAnItemAndItIsNoLongerInTheList)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.append("item2");
   list.append("item3");
   list.remove("item2");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item1"), vectorContents[0]);
   EXPECT_EQ(std::string("item3"), vectorContents[1]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanRemoveAnItemThatDoesntExistAndTheListIsUnchanged)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10);
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.append("item2");
   list.append("item3");
   list.remove("item4");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item1"), vectorContents[0]);
   EXPECT_EQ(std::string("item2"), vectorContents[1]);
   EXPECT_EQ(std::string("item3"), vectorContents[2]);
   ASSERT_FALSE(listFilePath.remove());
}

// ***************************************************************************
//   tests for extra data mode
// ***************************************************************************

TEST(MruListTest, CanCreateEmptyFileWithExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));

   MruList list(listFilePath, 10, '\t');

   ASSERT_FALSE(list.initialize());
   EXPECT_TRUE(listFilePath.exists());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAndAccessAnItemWithNoExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("item1");

   EXPECT_EQ(1u, list.size());
   EXPECT_EQ(1u, list.contents().size());
   EXPECT_EQ(std::string("item1"), list.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAndAccessAnItemWithSeparatorButNoExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("item1\t");

   EXPECT_EQ(1u, list.size());
   EXPECT_EQ(1u, list.contents().size());
   EXPECT_EQ(std::string("item1"), list.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAndAccessAnItemWithExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("item1\tHello World!");

   EXPECT_EQ(1u, list.size());
   EXPECT_EQ(1u, list.contents().size());
   EXPECT_EQ(std::string("item1\tHello World!"), list.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAndAccessAnItemWithLeadingAndTrailingWhitespaceInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("  item1    ");

   EXPECT_EQ(1u, list.size());
   EXPECT_EQ(1u, list.contents().size());
   EXPECT_EQ(std::string("  item1    "), list.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, WhitespaceIsStrippedWhenItemsAreReadFromDiskInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("  item1    ");

   MruList list2(listFilePath, 10, '\t');
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(1u, list2.size());
   EXPECT_EQ(1u, list2.contents().size());
   EXPECT_EQ(std::string("item1"), list2.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, WhitespaceIsStrippedWhenItemsAreReadFromDiskInExtraDataModeWithExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("  item1 \t Hello World!   ");
   list.append("   item2   ");

   MruList list2(listFilePath, 10, '\t');
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(2u, list2.size());
   EXPECT_EQ(2u, list2.contents().size());
   EXPECT_EQ(std::string("item1 \t Hello World!"), list2.contents().front());
   EXPECT_EQ(std::string("item2"), list2.contents().back());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAndAccessMultipleItemsInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.append("item2\tThe Second Item");
   list.append("item3");
   list.append("item4\tThe Fourth Item");

   EXPECT_EQ(4u, list.size());
   auto contents = list.contents();
   EXPECT_EQ(4u, contents.size());
   EXPECT_EQ(std::string("item1"), contents.front());
   EXPECT_EQ(std::string("item4\tThe Fourth Item"), contents.back());
   std::vector<std::string> vectorContents(contents.begin(), contents.end());
   EXPECT_EQ(std::string("item1"), vectorContents[0]);
   EXPECT_EQ(std::string("item2\tThe Second Item"), vectorContents[1]);
   EXPECT_EQ(std::string("item3"), vectorContents[2]);
   EXPECT_EQ(std::string("item4\tThe Fourth Item"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanReadMultipleItemsFromFileInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("item1\tThe First Item");
   list.append("item2\tThe Second Item");
   list.append("item3");
   list.append("item4\tThe Fourth Item");

   MruList list2(listFilePath, 10, '\t');
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(4u, list2.size());
   auto contents = list2.contents();
   EXPECT_EQ(4u, contents.size());
   EXPECT_EQ(std::string("item1\tThe First Item"), contents.front());
   EXPECT_EQ(std::string("item4\tThe Fourth Item"), contents.back());
   std::list<std::string> listContents = list2.contents();
   std::vector<std::string> vectorContents(contents.begin(), contents.end());
   EXPECT_EQ(std::string("item1\tThe First Item"), vectorContents[0]);
   EXPECT_EQ(std::string("item2\tThe Second Item"), vectorContents[1]);
   EXPECT_EQ(std::string("item3"), vectorContents[2]);
   EXPECT_EQ(std::string("item4\tThe Fourth Item"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanPrependAndAccessAnItemInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '|');
   ASSERT_FALSE(list.initialize());

   list.prepend("item1");

   EXPECT_EQ(1u, list.size());
   EXPECT_EQ(1u, list.contents().size());
   EXPECT_EQ(std::string("item1"), list.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanPrependAndAccessAnItemInExtraDataModeWithExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '|');
   ASSERT_FALSE(list.initialize());

   list.prepend("item1|Hello World!");

   EXPECT_EQ(1u, list.size());
   EXPECT_EQ(1u, list.contents().size());
   EXPECT_EQ(std::string("item1|Hello World!"), list.contents().front());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanPrependAndAccessMultipleItemsInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '|');
   ASSERT_FALSE(list.initialize());

   list.prepend("item1");
   list.prepend("item2|Hello");
   list.prepend("item3");
   list.prepend("item4|World");

   EXPECT_EQ(4u, list.size());
   EXPECT_EQ(4u, list.contents().size());
   EXPECT_EQ(std::string("item4|World"), list.contents().front());
   EXPECT_EQ(std::string("item1"), list.contents().back());
   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item4|World"), vectorContents[0]);
   EXPECT_EQ(std::string("item3"), vectorContents[1]);
   EXPECT_EQ(std::string("item2|Hello"), vectorContents[2]);
   EXPECT_EQ(std::string("item1"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanUseMixtureOfAppendAndPrependInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '@');
   ASSERT_FALSE(list.initialize());

   list.append("item1@once");
   list.prepend("item2@upon");
   list.append("item3@a time");
   list.append("item4@there was a");

   EXPECT_EQ(4u, list.size());
   EXPECT_EQ(4u, list.contents().size());
   EXPECT_EQ(std::string("item2@upon"), list.contents().front());
   EXPECT_EQ(std::string("item4@there was a"), list.contents().back());
   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item2@upon"), vectorContents[0]);
   EXPECT_EQ(std::string("item1@once"), vectorContents[1]);
   EXPECT_EQ(std::string("item3@a time"), vectorContents[2]);
   EXPECT_EQ(std::string("item4@there was a"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanUseAnotherMixtureOfAppendAndPrependInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.prepend("item2");
   list.prepend("item3");
   list.append("item4");
   list.prepend("item5\tThe best item ever!!!");
   list.append("item6\t");
   list.prepend("item5\tBack and even better than before");

   EXPECT_EQ(6u, list.size());
   EXPECT_EQ(6u, list.contents().size());
   EXPECT_EQ(std::string("item5\tBack and even better than before"), list.contents().front());
   EXPECT_EQ(std::string("item6"), list.contents().back());
   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item5\tBack and even better than before"), vectorContents[0]);
   EXPECT_EQ(std::string("item3"), vectorContents[1]);
   EXPECT_EQ(std::string("item2"), vectorContents[2]);
   EXPECT_EQ(std::string("item1"), vectorContents[3]);
   EXPECT_EQ(std::string("item4"), vectorContents[4]);
   EXPECT_EQ(std::string("item6"), vectorContents[5]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanClearEmptyListInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());
   EXPECT_EQ(0u, list.contents().size());

   list.clear();

   EXPECT_EQ(0u, list.contents().size());
   MruList list2(listFilePath, 10, '\t');
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(0u, list2.contents().size());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanClearNonEmptyListInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());
   list.append("item1");
   list.append("item2");
   list.append("item3");
   EXPECT_EQ(3u, list.size());

   list.clear();

   EXPECT_EQ(0u, list.size());
   MruList list2(listFilePath, 10);
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(0u, list2.contents().size());
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAddAndStoreTheNumberOfItemsIndicatedInConstructorInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 4, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("item1\tOnce");
   list.prepend("item2\tMore");
   list.append("item3\tInto");
   list.prepend("item4");

   EXPECT_EQ(4u, list.size());
   EXPECT_EQ(4u, list.contents().size());
   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(4u, vectorContents.size());
   EXPECT_EQ(std::string("item4"), vectorContents[0]);
   EXPECT_EQ(std::string("item2\tMore"), vectorContents[1]);
   EXPECT_EQ(std::string("item1\tOnce"), vectorContents[2]);
   EXPECT_EQ(std::string("item3\tInto"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendMoreItemsThanListSizeAndItemsFromOppositeEndAreRemovedInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 4, '\t');
   ASSERT_FALSE(list.initialize());

   list.append("item1");
   list.append("item2\tPlease don't delete me!");
   list.append("item3");
   list.append("item4");
   list.append("item5");
   list.append("item6\tLast but not least.");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(4u, vectorContents.size());
   EXPECT_EQ(std::string("item3"), vectorContents[0]);
   EXPECT_EQ(std::string("item4"), vectorContents[1]);
   EXPECT_EQ(std::string("item5"), vectorContents[2]);
   EXPECT_EQ(std::string("item6\tLast but not least."), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, ExtraDataForADeletedEntryDoesntComeBackIfSameEntryIsAddedAgainWithoutExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 4, '\t');
   ASSERT_FALSE(list.initialize());
   list.append("item1");
   list.append("item2\tPlease don't delete me!");

   list.remove("item2");
   list.append("item2");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(2u, vectorContents.size());
   EXPECT_EQ(std::string("item1"), vectorContents[0]);
   EXPECT_EQ(std::string("item2"), vectorContents[1]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanPrependMoreItemsThanListSizeAndItemsFromOppositeEndOfListAreRemovedInExtraDataMode)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 4, '\t');
   ASSERT_FALSE(list.initialize());

   list.prepend("item1");
   list.prepend("item2");
   list.prepend("item3\tThree is ok");
   list.prepend("item4");
   list.prepend("item5\tFive is great");
   list.prepend("item6\tSix is the best");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(4u, vectorContents.size());
   EXPECT_EQ(std::string("item6\tSix is the best"), vectorContents[0]);
   EXPECT_EQ(std::string("item5\tFive is great"), vectorContents[1]);
   EXPECT_EQ(std::string("item4"), vectorContents[2]);
   EXPECT_EQ(std::string("item3\tThree is ok"), vectorContents[3]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanAppendAnExistingItemAndItMovesToTheEndOfTheListAndUpdatesItsExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());
   list.append("item1\tFirst Try");
   list.append("item2");
   list.append("item3");

   list.append("item1\tNew and improved?");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item2"), vectorContents[0]);
   EXPECT_EQ(std::string("item3"), vectorContents[1]);
   EXPECT_EQ(std::string("item1\tNew and improved?"), vectorContents[2]);
   MruList list2(listFilePath, 10, '\t');
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(3u, list2.size());
   auto list2Contents = list2.contents();
   std::vector<std::string> vectorContents2(list2Contents.begin(), list2Contents.end());
   EXPECT_EQ(std::string("item2"), vectorContents2[0]);
   EXPECT_EQ(std::string("item3"), vectorContents2[1]);
   EXPECT_EQ(std::string("item1\tNew and improved?"), vectorContents2[2]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanPrependAnExistingItemAndItMovesToTheFrontOfTheListWithExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());
   list.append("item1");
   list.append("item2");
   list.append("item3");

   list.prepend("item2");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item2"), vectorContents[0]);
   EXPECT_EQ(std::string("item1"), vectorContents[1]);
   EXPECT_EQ(std::string("item3"), vectorContents[2]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanRemoveAnItemAndItIsNoLongerInTheListWithExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());
   list.append("item1\tItem 1");
   list.append("item2\tItem 2");
   list.append("item3\tItem 3");

   list.remove("item2\tAnything goes here for item 2");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item1\tItem 1"), vectorContents[0]);
   EXPECT_EQ(std::string("item3\tItem 3"), vectorContents[1]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanRemoveAnItemThatDoesntExistAndTheListIsUnchangedWithExtraData)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());
   list.append("item1");
   list.append("item2\tTwo");
   list.append("item3");

   list.remove("item4\tFour");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item1"), vectorContents[0]);
   EXPECT_EQ(std::string("item2\tTwo"), vectorContents[1]);
   EXPECT_EQ(std::string("item3"), vectorContents[2]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, CanUpdateAnItemsExtraDataWithoutChangingItsPositionInTheList)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());
   list.append("item1\tFirst Try");
   list.append("item2\tSo Great");
   list.append("item3\tThree is better");

   list.updateExtraData("item2", "Now two is ultra great!");
   list.updateExtraData("item1\tSecond Try!");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item1\tSecond Try!"), vectorContents[0]);
   EXPECT_EQ(std::string("item2\tNow two is ultra great!"), vectorContents[1]);
   EXPECT_EQ(std::string("item3\tThree is better"), vectorContents[2]);
   MruList list2(listFilePath, 10, '\t');
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(3u, list2.size());
   auto list2Contents = list2.contents();
   std::vector<std::string> vectorContents2(list2Contents.begin(), list2Contents.end());
   EXPECT_EQ(std::string("item1\tSecond Try!"), vectorContents2[0]);
   EXPECT_EQ(std::string("item2\tNow two is ultra great!"), vectorContents2[1]);
   EXPECT_EQ(std::string("item3\tThree is better"), vectorContents2[2]);
   ASSERT_FALSE(listFilePath.remove());
}

TEST(MruListTest, UpdatingAnItemsExtraTextToEmptyStringRemovesTheExtraTextFromTheList)
{
   FilePath listFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".list", listFilePath));
   MruList list(listFilePath, 10, '\t');
   ASSERT_FALSE(list.initialize());
   list.append("item1\tFirst Try");
   list.append("item2\tSo Great");
   list.append("item3\tThree is better");

   list.updateExtraData("item2");
   list.updateExtraData("item3", "");

   auto listContents = list.contents();
   std::vector<std::string> vectorContents(listContents.begin(), listContents.end());
   EXPECT_EQ(std::string("item1\tFirst Try"), vectorContents[0]);
   EXPECT_EQ(std::string("item2"), vectorContents[1]);
   EXPECT_EQ(std::string("item3"), vectorContents[2]);
   MruList list2(listFilePath, 10, '\t');
   ASSERT_FALSE(list2.initialize());
   EXPECT_EQ(3u, list2.size());
   auto list2Contents = list2.contents();
   std::vector<std::string> vectorContents2(list2Contents.begin(), list2Contents.end());
   EXPECT_EQ(std::string("item1\tFirst Try"), vectorContents2[0]);
   EXPECT_EQ(std::string("item2"), vectorContents2[1]);
   EXPECT_EQ(std::string("item3"), vectorContents2[2]);
   ASSERT_FALSE(listFilePath.remove());
}

} // namespace tests
} // namespace collection
} // namespace core
} // namespace rstudio
