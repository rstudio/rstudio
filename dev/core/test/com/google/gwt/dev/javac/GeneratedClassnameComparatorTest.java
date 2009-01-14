package com.google.gwt.dev.javac;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * 
 * This class tests to see if the anonymous class names are sorted in the right
 * order. For example, Foo$10 should be after Foo$2.
 */
public class GeneratedClassnameComparatorTest extends TestCase {

  public void testBasicOrder() {
    int max = 15;
    String original[] = new String[max];
    String expected[] = new String[max];
    for (int i = 0; i < max; i++) {
      String name = "Foo$" + (i + 1);
      original[i] = name;
      expected[i] = name;
    }
    Arrays.sort(original, new GeneratedClassnameComparator());
    for (int i = 0; i < max; i++) {
      assertEquals("index = " + i, expected[i], original[i]);
    }
  }

  public void testHierarchicalOrder() {
    String original[] = {
        "Foo$1", "Foo$1$1$1", "Foo$1$2", "Foo$2", "Foo$2$1", "Foo$3",};
    String expected[] = {
        "Foo$1", "Foo$2", "Foo$3", "Foo$1$2", "Foo$2$1", "Foo$1$1$1"};
    Arrays.sort(original, new GeneratedClassnameComparator());
    for (int i = 0; i < original.length; i++) {
      assertEquals("index = " + i, expected[i], original[i]);
    }
  }

  public void testMixedNames() {
    String original[] = {"Foo", "Foo$1", "Foo$1xyz", "Foo$2", "Foo$xyz"};
    String expected[] = {"Foo", "Foo$1", "Foo$2", "Foo$1xyz", "Foo$xyz"};
    Arrays.sort(original, new GeneratedClassnameComparator());
    for (int i = 0; i < original.length; i++) {
      assertEquals("index = " + i, expected[i], original[i]);
    }
  }

}
