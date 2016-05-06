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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.TreeSet;

/**
 * Test PriorityQueue.
 */
public class PriorityQueueTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testAdd() {
    PriorityQueue<Integer> queue = new PriorityQueue<Integer>();

    try {
      queue.add(null);
    } catch (NullPointerException expected) {
    }

    queue.add(1);
    assertTrue(Arrays.asList(1).containsAll(queue));
    queue.add(2);
    assertTrue(Arrays.asList(1, 2).containsAll(queue));
  }

  public void testAddAll() {
    PriorityQueue<Integer> queue = new PriorityQueue<>();
    try {
      queue.addAll(queue);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    queue = new PriorityQueue<>();
    try {
      queue.addAll(Arrays.asList(1, null));
      fail();
    } catch (NullPointerException expected) {
    }

    queue = new PriorityQueue<>();
    queue.addAll(Arrays.asList(2, 1, 3));
    assertTrue(Arrays.asList(1, 2, 3).containsAll(queue));
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

  public void testComparator() {
    PriorityQueue<Integer> pq = new PriorityQueue<Integer>();
    assertNull(pq.comparator());

    pq = new PriorityQueue<Integer>(11);
    assertNull(pq.comparator());

    Comparator<Integer> comparator = new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return o1 - o2;
      }
    };
    pq = new PriorityQueue<Integer>(11, comparator);
    assertEquals(comparator, pq.comparator());

    PriorityQueue<Integer> anotherQueue = new PriorityQueue<Integer>(pq);
    assertEquals(pq.comparator(), anotherQueue.comparator());

    TreeSet<Integer> sortedSet = new TreeSet<Integer>(comparator);
    pq = new PriorityQueue<Integer>(sortedSet);
    assertEquals(sortedSet.comparator(), pq.comparator());
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

  public void testContains() {
    PriorityQueue<Integer> queue = new PriorityQueue<>();

    assertFalse(queue.contains(null));

    queue.add(3);
    queue.add(1);
    queue.add(2);
    assertTrue(queue.contains(1));
    assertTrue(queue.contains(2));
    assertTrue(queue.contains(3));
    assertFalse(queue.contains(4));
  }
  
  public void testPeekElement() {
    PriorityQueue<Integer> queue = new PriorityQueue<>();
    try {
      queue.element();
      fail();
    } catch (NoSuchElementException expected) {
    }
    assertNull(queue.peek());

    queue.add(3);
    queue.add(1);
    queue.add(2);
    assertEquals(1, (int) queue.element());
    assertEquals(1, (int) queue.peek());
    assertEquals(3, queue.size());
  }

  public void testPollRemove() {
    PriorityQueue<Integer> queue = new PriorityQueue<>();
    try {
      queue.remove();
      fail();
    } catch (NoSuchElementException expected) {
    }
    assertNull(queue.poll());

    queue.add(3);
    queue.add(1);
    queue.add(2);
    assertEquals(1, (int) queue.remove());
    assertEquals(2, queue.size());
    assertEquals(2, (int) queue.remove());
    assertEquals(1, queue.size());
    assertEquals(3, (int) queue.remove());
    assertTrue(queue.isEmpty());

    queue = new PriorityQueue<>();
    queue.add(1);
    queue.add(2);
    queue.add(3);
    assertEquals(1, (int) queue.poll());
    assertEquals(2, queue.size());
    assertEquals(2, (int) queue.poll());
    assertEquals(1, queue.size());
    assertEquals(3, (int) queue.poll());
    assertTrue(queue.isEmpty());
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
