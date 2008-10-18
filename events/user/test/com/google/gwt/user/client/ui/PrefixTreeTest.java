/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TODO(bobv): comment me.
 */
public class PrefixTreeTest extends GWTTestCase {
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Ensure that names of functions declared on the Object prototype are valid
   * data to insert into the PrefixTree (<a
   * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=631">issue
   * #631)</a>.
   */
  public void testBug631Prefixes() {
    // Set the prefix length large enough so that we ensure prefixes are
    // appropriately tested
    final String[] prototypeNames = {
        "__proto__", "constructor", "eval", "prototype", "toString",
        "toSource", "unwatch", "valueOf",};

    for (int i = 0; i < prototypeNames.length; i++) {
      final String name = prototypeNames[i] + "AAAAAAAAAAAAAAAAAAAA";
      final PrefixTree tree = new PrefixTree(prototypeNames[i].length());

      assertFalse("Incorrectly found " + name, tree.contains(name));

      assertTrue("First add() didn't return true: " + name, tree.add(name));
      assertFalse("Second add() of duplicate entry didn't return false: "
          + name, tree.add(name));

      assertTrue("contains() didn't find added word: " + name,
          tree.contains(name));

      testSizeByIterator(tree);
      assertTrue("PrefixTree doesn't contain the desired word",
          1 == tree.size());
    }
  }

  /**
   * Ensure that names of functions declared on the Object prototype are valid
   * data to insert into the PrefixTree(<a
   * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=631">issue
   * #631)</a>.
   */
  public void testBug631Suffixes() {
    // Set the prefix length large enough so that we ensure suffixes are
    // appropriately tested
    final PrefixTree tree = new PrefixTree(100);
    final String[] prototypeNames = {
        "__proto__", "constructor", "eval", "prototype", "toString",
        "toSource", "unwatch", "valueOf",};

    for (int i = 0; i < prototypeNames.length; i++) {
      final String name = prototypeNames[i];

      assertFalse("Incorrectly found " + name, tree.contains(name));

      assertTrue("First add() didn't return true: " + name, tree.add(name));
      assertFalse("Second add() of duplicate entry didn't return false: "
          + name, tree.add(name));

      assertTrue("contains() didn't find added word: " + name,
          tree.contains(name));
    }

    testSizeByIterator(tree);
    assertTrue("PrefixTree doesn't contain all of the desired words",
        prototypeNames.length == tree.size());
  }

  /**
   * Tests adding multiple prefixes and clearing the contents.
   */
  public void testMultipleAddsAndClear() {
    final PrefixTree tree = new PrefixTree();

    assertTrue(tree.add("foo"));
    assertFalse(tree.add("foo"));
    assertTrue(tree.add("bar"));

    assertTrue("Expecting iterator to have next", tree.iterator().hasNext());
    assertTrue("Tree did not have expected size", tree.size() == 2);
    testSizeByIterator(tree);

    tree.clear();
    assertFalse("Expecting cleared tree to not hasNext()",
        tree.iterator().hasNext());
    assertTrue("Clear did not set size to 0", tree.size() == 0);
  }

  public void testNewTree() {
    final PrefixTree tree = new PrefixTree();
    assertTrue("Newly-created tree had non-zero size", tree.size() == 0);
    testSizeByIterator(tree);

    assertFalse("Newly-created tree had iterator with a next element",
        tree.iterator().hasNext());
  }

  /**
   * Tests newly constructed prefix tree assumptions.
   */
  public void testPlaysWellWithOthers() {
    final List l = new ArrayList();
    for (int i = 0; i < 100; i++) {
      l.add(String.valueOf(i));
    }

    final PrefixTree tree = new PrefixTree();
    tree.addAll(l);

    assertTrue("Not all elements copied", tree.size() == l.size());
    testSizeByIterator(tree);

    assertTrue("Original list does not contain all of the tree",
        l.containsAll(tree));

    assertTrue("The tree does not contain the original list",
        tree.containsAll(l));
  }

  /**
   * Test whether the prefix tree works appropriately with collections.
   */
  public void testSuggestions() {
    final PrefixTree tree = new PrefixTree();

    assertTrue(tree.add("onetwothree"));
    assertTrue(tree.add("onetwothree1"));
    assertTrue(tree.add("onetwothree2"));
    assertTrue(tree.add("onetwothree3"));
    assertTrue(tree.add("fourfivesix"));
    assertTrue(tree.add("fourfivesix1"));
    assertTrue(tree.add("fourfivesix2"));
    assertTrue(tree.add("fourfivesix3"));
    assertTrue(tree.add("seveneightnine"));
    assertTrue(tree.add("seveneightnine1"));
    assertTrue(tree.add("seveneightnine2"));
    assertTrue(tree.add("seveneightnine3"));
    assertTrue(tree.add("somethingdifferent"));

    assertTrue(tree.size() == 13);
    testSizeByIterator(tree);
    assertTrue(tree.iterator().hasNext());

    List l;

    l = tree.getSuggestions("", 13);
    assertTrue("Expected size of 13, got " + l.size(), l.size() == 13);
    assertAllStartWith(l, "");

    l = tree.getSuggestions("one", 10);
    assertTrue("Expected size of 4, got " + l.size(), l.size() == 4);
    assertAllStartWith(l, "one");

    l = tree.getSuggestions("onetwothree", 10);
    assertTrue("Expected size of 4, got " + l.size(), l.size() == 4);
    assertAllStartWith(l, "onetwothree");

    l = tree.getSuggestions("onetwothree1", 10);
    assertTrue("Expected size of 1, got " + l.size(), l.size() == 1);
    assertAllStartWith(l, "onetwothree1");

    l = tree.getSuggestions("o", 1);
    assertTrue("Expected size of 1, got " + l.size(), l.size() == 1);
    assertTrue(((String) l.get(0)).endsWith("..."));
    assertAllStartWith(l, "o");

    l = tree.getSuggestions("something", 1);
    assertTrue("Expected size of 1, got " + l.size(), l.size() == 1);
    assertEquals("somethingdifferent", l.get(0));
    assertAllStartWith(l, "somethingdifferent");
  }

  protected void assertAllStartWith(List l, String prefix) {
    for (final Iterator i = l.iterator(); i.hasNext();) {
      final String test = (String) i.next();
      assertTrue(test + " does not start with " + prefix,
          test.startsWith(prefix));
    }
  }

  /**
   * Ensure that size() reports the same number of items that the iterator
   * produces.
   * 
   * @param tree the tree to test
   */
  protected void testSizeByIterator(PrefixTree tree) {
    int count = 0;
    for (final Iterator i = tree.iterator(); i.hasNext();) {
      i.next();
      count++;
    }

    assertTrue("Iteration count " + count + " did not match size "
        + tree.size(), count == tree.size());
  }
}
