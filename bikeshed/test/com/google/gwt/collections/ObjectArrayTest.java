/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.collections;

import static com.google.gwt.collections.CollectionFactory.createMutableArray;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests mutable arrays.
 */
public class ObjectArrayTest extends GWTTestCase {
  
  boolean assertionsEnabled;
  
  @Override
  public String getModuleName() {
    return null;
  }

  public void gwtSetUp() {
    assertionsEnabled = this.getClass().desiredAssertionStatus();
  }

  public void testClear() {
    {
      // Clear an empty array
      MutableArray<Object> b = createMutableArray();
      b.clear();
      assertEquals(0, b.size());
    }

    {
      // Clear a non-empty array
      MutableArray<Object> b = createMutableArray();
      b.add("string");
      b.add(false);
      b.add(3);
      b.clear();
      assertEquals(0, b.size());
    }
  }

  public void testInsertAtBeginning() {
    final int n = 10;
    MutableArray<Integer> b = createMutableArray();

    for (int i = 0; i < n; ++i) {
      b.insert(0, i);
    }

    for (int i = 0; i < n; ++i) {
      assertEquals(n - i - 1, b.get(i).intValue());
    }
  }

  public void testInsertAtEnd() {
    final int n = 10;
    MutableArray<Integer> b = createMutableArray();

    for (int i = 0; i < n; ++i) {
      b.insert(b.size(), i);
    }

    for (int i = 0; i < n; ++i) {
      assertEquals(i, b.get(i).intValue());
    }
  }

  public void testInsertInMiddle() {
    final int n = 10;
    MutableArray<Integer> b = createMutableArray();

    // Fill the array with 0..(N-1)
    for (int i = 0; i < n; ++i) {
      b.insert(b.size(), i);
    }

    // Double each number by inserting.
    for (int i = 0; i < n; ++i) {
      b.insert(i * 2, i);
    }

    for (int i = 0, j = 0; i < 2 * n; i += 2, ++j) {
      assertEquals(j, b.get(i).intValue());
      assertEquals(j, b.get(i + 1).intValue());
    }
  }
  
  public void testMultiElementArrayManipulations() {
    MutableArray<String> b = createMutableArray();
    b.add("apple");
    b.add("banana");
    b.add("coconut");
    b.add(null);
    b.add("donut");

    // On mutable array, get() in order
    assertEquals("apple", b.get(0));
    assertEquals("banana", b.get(1));
    assertEquals("coconut", b.get(2));
    assertEquals(null, b.get(3));
    assertEquals("donut", b.get(4));

    // On mutable array, get() in non-sequential order
    assertEquals("coconut", b.get(2));
    assertEquals("apple", b.get(0));
    assertEquals("donut", b.get(4));
    assertEquals("banana", b.get(1));
    assertEquals(null, b.get(3));

    // Try a set() call, too
    b.set(3, "eclair");
    assertEquals("eclair", b.get(3));
  }

  public void testRemove() {
    MutableArray<Integer> b = createMutableArray();

    b.add(1);
    b.add(2);
    b.add(3);
    b.add(4);
    b.add(5);

    // Remove from the middle to make 1,2,4,5
    b.remove(2);
    assertEquals(4, b.size());
    assertEquals(1, b.get(0).intValue());
    assertEquals(5, b.get(3).intValue());

    // Remove from the beginning to make 2,4,5
    b.remove(0);
    assertEquals(3, b.size());
    assertEquals(2, b.get(0).intValue());
    assertEquals(5, b.get(2).intValue());

    // Remove from the end to make 2,4
    b.remove(b.size() - 1);
    assertEquals(2, b.size());
    assertEquals(2, b.get(0).intValue());
    assertEquals(4, b.get(1).intValue());
  }

  public void testSingleElementAddAndRemove() {
      MutableArray<String> a = createMutableArray();

      a.add("foo");

      assertEquals(1, a.size());
      assertEquals("foo", a.get(0));

      a.remove(0);

      assertEquals(0, a.size());

      a.add("bar");

      assertEquals(1, a.size());
      assertEquals("bar", a.get(0));
  }

  public void testSingleElementNull() {
    MutableArray<String> b = createMutableArray();
    b.add(null);
    
    assertEquals(null, b.get(0));
  }

  public void testSingletonArrayCreationAndRetrieval() throws Exception {
    final int c = 2112;
    MutableArray<Integer> b = createMutableArray();
    b.add(c);
    assertEquals(c, b.get(0).intValue());

    Array<Integer> a = b;
    assertEquals(1, a.size());

    assertEquals((Integer) 2112, a.get(0));
  }

  public void testSingletonArrayInvalidIndex() throws Exception {
    MutableArray<Boolean> b = createMutableArray();
    b.add(false);
    Array<Boolean> a = b;

    assertEquals((Boolean) false, a.get(0));

    // Do not test undefined behavior without assertions
    if (!assertionsEnabled) {
      return;
    }

    try {
      a.get(1);
      fail("That should have failed");        
    } catch (AssertionError e) {
      assertEquals(("Index " + 1 + " was not in the acceptable range [" + 0 + ", " + 1 + ")"), 
          e.getMessage());
    }      
  }

  public void testSingletonArrayManipulations() {
    MutableArray<String> b = createMutableArray();
    b.add("diva");
    b.set(0, "avid");
    assertEquals(1, b.size());
    assertEquals("avid", b.get(0));
  }

  public void testThatEmptyArraysHaveHaveNoValidIndices() throws Exception {
    {
      // Do not test undefined behavior without assertions
      if (!assertionsEnabled) {
        return;
      }
      // Tests get().
      MutableArray<String> b = createMutableArray();
      Array<String> a = b;
      // Iterate i through the various likely errors states.
      for (int i = -1; i < 4; ++i) {
        try {
          a.get(i);
          fail("Should have triggered an assertion");
        } catch (AssertionError e) {
          // Good
          assertEquals(("Index " + i + " was not in the acceptable range [" + 0 + ", "
          + 0 + ")"), e.getMessage());
        }
      }
    }

    {
      // Tests set().
      MutableArray<String> b = createMutableArray();
      // Iterate i through the various likely errors states.
      for (int i = -1; i < 4; ++i) {
        try {
          b.set(i, "random string");
          fail("Should have triggered an assertion");
        } catch (AssertionError e) {
          // Good
          assertEquals(("Index " + i + " was not in the acceptable range [" + 0 + ", "
          + 0 + ")"), e.getMessage());
        }
      }
    }
  }

  public void testThatEmptyArraysHaveSizeZero() {
    MutableArray<String> b = createMutableArray();
    Array<String> a = b;
    assertEquals(0, a.size());
  }
  
  public void testSetSize() {
    MutableArray<String> b = createMutableArray();

    b.setSize(1, "fillValue");
    assertEquals(1, b.size());
    assertEquals("fillValue", b.get(0));
    
    b.setSize(2, "anotherValue");
    assertEquals(2, b.size());
    assertEquals("fillValue", b.get(0));
    assertEquals("anotherValue", b.get(1));

    b.setSize(1, null);
    assertEquals(1, b.size());
    assertEquals("fillValue", b.get(0));

    b.setSize(0, null);
    assertEquals(0, b.size());
    assertEquals(null, b.elems);
  }
  
  public void testCreateNonEmptyArray() {
    MutableArray<String> b = createMutableArray(2, "apples");
    
    assertEquals(2, b.size());
    assertEquals("apples", b.get(0));
    assertEquals("apples", b.get(1));
  }

}
