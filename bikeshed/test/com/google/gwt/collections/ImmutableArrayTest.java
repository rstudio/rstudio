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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests immutable arrays.
 */
public class ImmutableArrayTest extends GWTTestCase {

  private boolean assertionsEnabled;

  @Override
  public String getModuleName() {
    return null;
  }
  
  public void gwtSetUp() {
    assertionsEnabled = this.getClass().desiredAssertionStatus();
  }

  public void testAccessOutOfBounds() {
    // Do not test undefined behavior with assertions disabled
    if (!assertionsEnabled) {
      return;
    }
    
    MutableArray<Integer> ma;
    ImmutableArray<Integer> ia;
    
    ma = CollectionFactory.createMutableArray();
    ma.add(1);
    ma.add(2);
    
    ia = ma.freeze();
    
    try {
      ia.get(ia.size());
      fail("Expected an assertion failure");
    } catch (AssertionError e) {
      assertEquals(("Index " + ia.size() + " was not in the acceptable range [" + 0 + ", " + ia.size() 
          + ")"), e.getMessage());
    }
    
    assertEquals(2, ma.size());
    
    ma = CollectionFactory.createMutableArray();
    // No elements added
    ia = ma.freeze();
    
    try {
      ia.get(ia.size());
      fail("Expected an assertion failure");
    } catch (AssertionError e) {
      assertEquals("Attempt to get an element from an immutable empty array", e.getMessage());
    }
  }
  
  public void testFreezeEmptyArray() {    
    MutableArray<String> ma = CollectionFactory.createMutableArray();
    // No elements added
    ImmutableArray<String> ia = ma.freeze();
    
    assertEquals(0, ia.size());
  }
  
  public void testFreezeMutable() {
    MutableArray<String> ma = CollectionFactory.createMutableArray();
    ma.add("pear");
    ma.add("apple");
    ImmutableArray<String> ia = ma.freeze();
    
    assertEquals(2, ia.size());
    assertEquals("pear", ia.get(0));
    assertEquals("apple", ia.get(1));
  }
  
  public void testFreezeSingleElement() {    
    MutableArray<String> ma = CollectionFactory.createMutableArray();
    ma.add("pear");
    ImmutableArray<String> ia = ma.freeze();
    
    assertEquals(1, ia.size());
    assertEquals("pear", ia.get(0));
  }
  
  public void testModifyFrozenMutable() {    
    // Do not test undefined behavior with assertions disabled
    if (!assertionsEnabled) {
      return;
    }

    MutableArray<String> ma = CollectionFactory.createMutableArray();
    ma.add("pear");
    ma.add("apple");
    ma.freeze();

    try {
      ma.add("orange");
      fail("Expected an assertion failure");
    } catch (AssertionError e) {
      assertEquals(("This operation is illegal on a frozen collection"), e.getMessage());
    }

    try {
      ma.clear();
      fail("Expected an assertion failure");
    } catch (AssertionError e) {
      assertEquals(("This operation is illegal on a frozen collection"), e.getMessage());
    }

    try {
      ma.insert(0, "orange");
      fail("Expected an assertion failure");
    } catch (AssertionError e) {
      assertEquals(("This operation is illegal on a frozen collection"), e.getMessage());
    }

    try {
      ma.remove(0);
      fail("Expected an assertion failure");
    } catch (AssertionError e) {
      assertEquals(("This operation is illegal on a frozen collection"), e.getMessage());
    }

    try {
      ma.set(0, "orange");
      fail("Expected an assertion failure");
    } catch (AssertionError e) {
      assertEquals(("This operation is illegal on a frozen collection"), e.getMessage());
    }
  }
  
  public void testImmutableNoCopy() {
    MutableArray<String> ma = CollectionFactory.createMutableArray();
    ma.add("pear");
    ma.add("apple");
    ImmutableArrayImpl<String> ia1 = (ImmutableArrayImpl<String>) ma.freeze();
    
    assertTrue(ma.elems == ia1.elems);
  
    ImmutableArrayImpl<String> ia2 = (ImmutableArrayImpl<String>) ma.freeze();
    
    assertTrue(ia1.elems == ia2.elems);
  }
  
}
