/*
 * Main.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <iostream>

#include <boost/test/minimal.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/system/FileScanner.hpp>

#include "treehh.hpp"
#include <core/collection/Tree.hpp>

using namespace core ;

template<typename Iterator1, typename Iterator2>
bool areEqual(Iterator1 begin1, Iterator1 end1,
              Iterator2 begin2, Iterator2 end2)
{
   if (std::distance(begin1, end1) != std::distance(begin2, end2))
   {
      return false;
   }

   return std::equal(begin1, end1, begin2);
}

bool areEqual(const tree<FileInfo>& tree1,
              const tcl::unique_tree<FileInfo>& tree2)
{
   return areEqual(tree1.begin(), tree1.end(),
                   tree2.pre_order_begin(), tree2.pre_order_end());
}

void printFileInfo(const FileInfo& fileInfo)
{
   std::cout << ": " << fileInfo.absolutePath() << std::endl;
}

void print(const tree<FileInfo>& tree1)
{
   std::for_each(tree1.begin(), tree1.end(), printFileInfo);
}

void print(const tcl::unique_tree<FileInfo>& tree2)
{
   std::for_each(tree2.pre_order_begin(), tree2.pre_order_end(), printFileInfo);
}

void print(const tree<FileInfo>& tree1,
           const tcl::unique_tree<FileInfo>& tree2)
{
   print(tree1);
   std::cout << std::endl;
   print(tree2);
}


int test_main(int argc, char * argv[])
{
   try
   { 
      // initialize log
      initializeSystemLog("coredev", core::system::kLogLevelWarning);

      // initialize root with constructor
      FileInfo rootFile("/root", true);
      tree<FileInfo> tree1(rootFile);
      tcl::unique_tree<FileInfo> tree2(rootFile);
      BOOST_CHECK(areEqual(tree1, tree2));

      // initialize root with method
      tree1 = tree<FileInfo>();
      tree1.set_head(rootFile);
      tree2 = tcl::unique_tree<FileInfo>(rootFile);
      BOOST_CHECK(areEqual(tree1, tree2));

      // root is the same
      BOOST_CHECK(*(tree1.begin()) == *(tree2.get()));

      // add some children
      FileInfo file1("/root/1", false);
      FileInfo file2("/root/2", true);
      FileInfo file3("/root/3", false);
      tree1.append_child(tree1.begin(), file1);
      tree1.append_child(tree1.begin(), file2);
      tree1.append_child(tree1.begin(), file3);
      tree2.insert(file1);
      tree2.insert(file2);
      tree2.insert(file3);
      BOOST_CHECK(areEqual(tree1, tree2));

      // find child and insert more children
      FileInfo fileA("/root/2/A", false);
      FileInfo fileB("/root/2/B", false);
      FileInfo fileC("/root/2/C", false);
      FileInfo fileD("/root/2/D", false);
      tree<FileInfo>::iterator it1 = std::find(
           tree1.begin(tree1.begin()), tree1.end(tree1.begin()), file2);
      tree1.append_child(it1, fileA);
      tree<FileInfo>::iterator itB1 = tree1.append_child(it1, fileB);
      tree1.append_child(it1, fileC);
      tree1.append_child(it1, fileD);
      tcl::unique_tree<FileInfo>::iterator it2 = tree2.find(file2);
      it2.node()->insert(fileA);
      tcl::unique_tree<FileInfo>::iterator itB2 = it2.node()->insert(fileB);
      it2.node()->insert(fileC);
      it2.node()->insert(fileD);
      BOOST_CHECK(areEqual(tree1, tree2));

      // child iterators
      BOOST_CHECK(areEqual(tree1.begin(tree1.begin()),
                           tree1.end(tree1.begin()),
                           tree2.begin(),
                           tree2.end()));

      // subtree iterators
      BOOST_CHECK(areEqual(tree1.begin(it1), tree1.end(it1),
                           it2.node()->begin(), it2.node()->end()));


      // subtree iterators end
      FileInfo fileZ("/root/2/Z", false);
      tree<FileInfo>::iterator itEnd1 = std::find(tree1.begin(it1), tree1.end(it1), fileZ);
      BOOST_CHECK(itEnd1 == tree1.end(it1));
      tcl::unique_tree<FileInfo>::iterator itEnd2 = it2.node()->find(fileZ);
      BOOST_CHECK(itEnd2 == it2.node()->end());

      // erase
      tree1.erase(itB1);
      it2.node()->erase(itB2);
      BOOST_CHECK(areEqual(tree1, tree2));

      // find deep
      tree<FileInfo>::iterator itDeep1  = std::find(tree1.begin(), tree1.end(), fileD);
      tcl::unique_tree<FileInfo>::iterator itDeep2 = tree2.find_deep(fileD);
      BOOST_CHECK(*it1 == *it2);


      // replace with item
      FileInfo fileF("/root/2/B", false);
      tree1.replace(itDeep1, fileF);
      it2.node()->insert(itDeep2, fileF);
      it2.node()->erase(itDeep2);
      tree1.sort(tree1.begin(it1), tree1.end(it1), core::fileInfoPathLessThan, false);
      BOOST_CHECK(areEqual(tree1, tree2));

      // erase children
      tree1.erase_children(it1);
      it2.node()->clear();
      BOOST_CHECK(areEqual(tree1, tree2));

      // add subtree
      FileInfo subRoot("/root/1/subtree", true);
      tree<FileInfo> subTree1(subRoot);
      tcl::unique_tree<FileInfo> subTree2(subRoot);
      FileInfo subRootX("/root/1/subtree/X", false);
      FileInfo subRootY("/root/1/subtree/Y", false);
      FileInfo subRootZ("/root/1/subtree/Z", false);
      subTree1.append_child(subTree1.begin(), subRootX);
      tree<FileInfo>::iterator itY1 = subTree1.append_child(subTree1.begin(), subRootY);
      subTree1.append_child(subTree1.begin(), subRootZ);
      subTree2.insert(subRootX);
      tcl::unique_tree<FileInfo>::iterator itY2 = subTree2.insert(subRootY);
      subTree2.insert(subRootZ);
      tree<FileInfo>::iterator itroot1 = std::find(
           tree1.begin(tree1.begin()), tree1.end(tree1.begin()), file1);
      tree<FileInfo>::sibling_iterator addedIter =
               tree1.append_child(itroot1, subRoot);
      tree<FileInfo>::sibling_iterator subtreeIter1 = tree1.insert_subtree_after(addedIter, subTree1.begin());
      tree1.erase(addedIter);
      tcl::unique_tree<FileInfo>::iterator itroot2 = tree2.find(file1);
      tcl::unique_tree<FileInfo>::iterator subtreeIter2 = itroot2.node()->insert(subTree2);
      BOOST_CHECK(areEqual(tree1, tree2));

      // replace subtree
      subTree1.erase(itY1);
      subTree2.erase(itY2);
      FileInfo subRootZZZ("/root/1/subtree/ZZZ", false);
      subTree1.append_child(subTree1.begin(), subRootZZZ);
      subTree2.insert(subRootZZZ);
      BOOST_CHECK(areEqual(subTree1, subTree2));

      tree1.insert_subtree_after(subtreeIter1, subTree1.begin());
      tree1.erase(subtreeIter1);

      itroot2.node()->erase(subtreeIter2);
      itroot2.node()->insert(subTree2);
      BOOST_CHECK(areEqual(tree1, tree2));

      // replace entire tree
      tree1.insert_subtree_after(tree1.begin(), subTree1.begin());
      tree1.erase(tree1.begin());
      tree2 = subTree2;
      BOOST_CHECK(areEqual(tree1, tree2));

      print(tree1, tree2);

      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

