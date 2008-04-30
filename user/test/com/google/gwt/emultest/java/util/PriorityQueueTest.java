/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.emultest.java.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/**
 * Test PriorityQueue.
 */
public class PriorityQueueTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testBasic() {
    PriorityQueue<Integer> pq = new PriorityQueue<Integer>();
    assertEquals(0, pq.size());
    assertTrue(pq.isEmpty());
    assertNull(pq.peek());
    try {
      pq.remove();
      fail("Expected exception");
    } catch (NoSuchElementException e) {
      // expected
    }
    pq.add(14);
    assertEquals(1, pq.size());
    assertFalse(pq.isEmpty());
    assertEquals(14, pq.peek().intValue());
    pq.add(5);
    assertEquals(2, pq.size());
    assertFalse(pq.isEmpty());
    assertEquals(5, pq.peek().intValue());
    pq.add(7);
    assertEquals(3, pq.size());
    assertFalse(pq.isEmpty());
    assertEquals(5, pq.peek().intValue());
    pq.add(3);
    assertEquals(4, pq.size());
    assertFalse(pq.isEmpty());
    assertEquals(3, pq.peek().intValue());
    assertEquals(3, pq.remove().intValue());
    assertEquals(5, pq.remove().intValue());
    assertEquals(7, pq.remove().intValue());
    assertEquals(14, pq.remove().intValue());
    assertTrue(pq.isEmpty());
  }

  public void testCollectionMethods() {
    PriorityQueue<Integer> pq = buildPQ(3, 4, 21, 5, 23, 31, 22);
    ArrayList<Integer> src = new ArrayList<Integer>();
    addArray(src, 21, 3, 31, 5);
    assertTrue(pq.containsAll(src));
    assertTrue(pq.contains(4));
    assertTrue(pq.contains(21));
    assertEquals(3, pq.peek().intValue());
    pq.remove(21);
    assertEquals(6, pq.size());
    assertTrue(pq.contains(4));
    assertFalse(pq.contains(21));
    pq.remove(5);
    assertFalse(pq.contains(5));
    pq.remove(3);
    assertFalse(pq.contains(3));
    assertEquals(4, pq.remove().intValue());
    assertEquals(22, pq.remove().intValue());
    assertEquals(23, pq.remove().intValue());
    assertEquals(31, pq.remove().intValue());
    assertTrue(pq.isEmpty());
    addArray(pq, 3, 4, 21, 5, 23, 31, 22);
    src.add(99);
    assertTrue(pq.retainAll(src));
    assertFalse(pq.retainAll(src));
    assertEquals(4, pq.size());
    assertEquals(3, pq.remove().intValue());
    assertEquals(5, pq.remove().intValue());
    assertEquals(21, pq.remove().intValue());
    assertEquals(31, pq.remove().intValue());
    assertTrue(pq.isEmpty());
  }

  public void testFromCollection() {
    ArrayList<Integer> src = new ArrayList<Integer>();
    addArray(src, 13, 3, 7, 5);
    PriorityQueue<Integer> pq = new PriorityQueue<Integer>(src);
    assertEquals(4, pq.size());
    assertEquals(3, pq.remove().intValue());
    assertEquals(5, pq.remove().intValue());
    assertEquals(7, pq.remove().intValue());
    assertEquals(13, pq.remove().intValue());
    assertTrue(pq.isEmpty());
  }
  
  public void testPollRemove() {
    PriorityQueue<Integer> pq = new PriorityQueue<Integer>();
    try {
      pq.remove();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException e) {
      // expected
    }
    assertNull(pq.poll());
  }
  
  private void addArray(Collection<Integer> col, int... values) {
    for (int val : values) {
      col.add(val);
    }
  }

  private PriorityQueue<Integer> buildPQ(int... values) {
    PriorityQueue<Integer> pq = new PriorityQueue<Integer>();
    addArray(pq, values);
    return pq;
  } 
}
