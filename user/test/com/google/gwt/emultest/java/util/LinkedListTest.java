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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Test LinkedList class.
 */
@SuppressWarnings("unchecked")
public class LinkedListTest extends ListTestBase {

  private static final class LinkedListWithRemoveRange extends LinkedList {
    @Override
    public void removeRange(int fromIndex, int toIndex) {
      super.removeRange(fromIndex, toIndex);
    }
  }

  public void testAddFirst() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    l.addFirst(o1);
    checkListSizeAndContent(l, o1);
    l.addFirst(o2);
    checkListSizeAndContent(l, o2, o1);
    l.addFirst(o3);
    checkListSizeAndContent(l, o3, o2, o1);
  }

  public void testAddLast() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    l.addLast(o1);
    checkListSizeAndContent(l, o1);
    l.addLast(o2);
    checkListSizeAndContent(l, o1, o2);
    l.addLast(o3);
    checkListSizeAndContent(l, o1, o2, o3);
  }

  public void testDescendingIterator() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    Iterator<Object> it = l.descendingIterator();
    assertFalse(it.hasNext());
    try {
      it.next();
      fail();
    } catch (NoSuchElementException e) {
    }

    l.add(o1);
    l.add(o2);
    l.add(o3);
    it = l.descendingIterator();
    assertTrue(it.hasNext());
    assertEquals(o3, it.next());
    assertTrue(it.hasNext());
    assertEquals(o2, it.next());
    assertTrue(it.hasNext());
    assertEquals(o1, it.next());
    assertFalse(it.hasNext());
    try {
      it.next();
      fail();
    } catch (NoSuchElementException e) {
    }
    checkListSizeAndContent(l, o1, o2, o3);

    l = new LinkedList<Object>();
    l.add(o1);
    l.add(o2);
    l.add(o3);
    it = l.descendingIterator();
    assertTrue(it.hasNext());
    assertEquals(o3, it.next());
    it.remove();
    assertEquals(2, l.size());
    assertTrue(it.hasNext());
    assertEquals(o2, it.next());
    assertTrue(it.hasNext());
    assertEquals(o1, it.next());
    it.remove();
    checkListSizeAndContent(l, o2);
  }

  public void testElement() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    try {
      l.element();
      fail();
    } catch (NoSuchElementException e) {
    }

    l.add(o1);
    assertEquals(o1, l.element());
    checkListSizeAndContent(l, o1);

    l.add(o2);
    assertEquals(o1, l.element());
    checkListSizeAndContent(l, o1, o2);
  }

  public void testGetFirst() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    try {
      l.getFirst();
      fail();
    } catch (NoSuchElementException e) {
    }

    l.add(o1);
    assertEquals(o1, l.getFirst());
    checkListSizeAndContent(l, o1);

    l.add(o2);
    assertEquals(o1, l.getFirst());
    checkListSizeAndContent(l, o1, o2);
  }

  public void testGetLast() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    try {
      l.getLast();
      fail();
    } catch (NoSuchElementException e) {
    }

    l.add(o1);
    assertEquals(o1, l.getLast());
    checkListSizeAndContent(l, o1);

    l.add(o2);
    assertEquals(o2, l.getLast());
    checkListSizeAndContent(l, o1, o2);
  }

  public void testOffer() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertTrue(l.offer(o1));
    checkListSizeAndContent(l, o1);
    assertTrue(l.offer(o2));
    checkListSizeAndContent(l, o1, o2);
    assertTrue(l.offer(o3));
    checkListSizeAndContent(l, o1, o2, o3);
  }

  public void testOfferFirst() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertTrue(l.offerFirst(o1));
    checkListSizeAndContent(l, o1);
    assertTrue(l.offerFirst(o2));
    checkListSizeAndContent(l, o2, o1);
    assertTrue(l.offerFirst(o3));
    checkListSizeAndContent(l, o3, o2, o1);
  }

  public void testOfferLast() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertTrue(l.offerLast(o1));
    checkListSizeAndContent(l, o1);
    assertTrue(l.offerLast(o2));
    checkListSizeAndContent(l, o1, o2);
    assertTrue(l.offerLast(o3));
    checkListSizeAndContent(l, o1, o2, o3);
  }

  public void testPeek() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertNull(l.peek());

    l.add(o1);
    assertEquals(o1, l.peek());
    checkListSizeAndContent(l, o1);

    l.add(o2);
    assertEquals(o1, l.peek());
    checkListSizeAndContent(l, o1, o2);
  }

  public void testPeekFirst() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertNull(l.peekFirst());

    l.add(o1);
    assertEquals(o1, l.peekFirst());
    checkListSizeAndContent(l, o1);

    l.add(o2);
    assertEquals(o1, l.peekFirst());
    checkListSizeAndContent(l, o1, o2);
  }

  public void testPeekLast() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertNull(l.peekLast());

    l.add(o1);
    assertEquals(o1, l.peekLast());
    checkListSizeAndContent(l, o1);

    l.add(o2);
    assertEquals(o2, l.peekLast());
    checkListSizeAndContent(l, o1, o2);
  }

  public void testPoll() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertNull(l.poll());

    l.add(o1);
    assertEquals(o1, l.poll());
    assertTrue(l.isEmpty());

    l.add(o1);
    l.add(o2);
    assertEquals(o1, l.poll());
    checkListSizeAndContent(l, o2);
  }

  public void testPollFirst() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertNull(l.pollFirst());

    l.add(o1);
    assertEquals(o1, l.pollFirst());
    assertTrue(l.isEmpty());

    l.add(o1);
    l.add(o2);
    assertEquals(o1, l.pollFirst());
    checkListSizeAndContent(l, o2);
  }

  public void testPollLast() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertNull(l.pollLast());

    l.add(o1);
    assertEquals(o1, l.pollLast());
    assertTrue(l.isEmpty());

    l.add(o1);
    l.add(o2);
    assertEquals(o2, l.pollLast());
    checkListSizeAndContent(l, o1);
  }

  public void testPop() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    try {
      l.pop();
      fail();
    } catch (NoSuchElementException e) {
    }

    l.add(o1);
    assertEquals(o1, l.pop());
    assertTrue(l.isEmpty());

    l.add(o1);
    l.add(o2);
    assertEquals(o1, l.pop());
    checkListSizeAndContent(l, o2);
  }

  public void testPush() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    l.push(o1);
    checkListSizeAndContent(l, o1);
    l.push(o2);
    checkListSizeAndContent(l, o2, o1);
    l.push(o3);
    checkListSizeAndContent(l, o3, o2, o1);
  }

  public void testRemove() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    try {
      l.remove();
      fail();
    } catch (NoSuchElementException e) {
    }

    l.add(o1);
    assertEquals(o1, l.remove());
    assertTrue(l.isEmpty());

    l.add(o1);
    l.add(o2);
    assertEquals(o1, l.remove());
    checkListSizeAndContent(l, o2);
  }

  public void testRemoveFirst() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    try {
      l.removeFirst();
      fail();
    } catch (NoSuchElementException e) {
    }

    l.add(o1);
    assertEquals(o1, l.removeFirst());
    assertTrue(l.isEmpty());

    l.add(o1);
    l.add(o2);
    assertEquals(o1, l.removeFirst());
    checkListSizeAndContent(l, o2);
  }

  public void testRemoveFirstOccurrence() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertFalse(l.removeFirstOccurrence(o1));

    l.add(o1);
    assertTrue(l.removeFirstOccurrence(o1));
    assertTrue(l.isEmpty());

    l = new LinkedList<Object>();
    l.add(o1);
    l.add(o2);
    l.add(o3);
    assertTrue(l.removeFirstOccurrence(o2));
    checkListSizeAndContent(l, o1, o3);

    l = new LinkedList<Object>();
    l.add(o1);
    l.add(o2);
    l.add(o3);
    l.add(o1);
    l.add(o2);
    l.add(o3);
    assertTrue(l.removeFirstOccurrence(o2));
    checkListSizeAndContent(l, o1, o3, o1, o2, o3);
  }

  public void testRemoveLast() {
    Object o1 = new Object();
    Object o2 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    try {
      l.removeLast();
      fail();
    } catch (NoSuchElementException e) {
    }

    l.add(o1);
    assertEquals(o1, l.removeLast());
    assertTrue(l.isEmpty());

    l.add(o1);
    l.add(o2);
    assertEquals(o2, l.removeLast());
    checkListSizeAndContent(l, o1);
  }

  public void testRemoveLastOccurrence() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    LinkedList<Object> l = new LinkedList<Object>();
    assertFalse(l.removeLastOccurrence(o1));

    l.add(o1);
    assertTrue(l.removeLastOccurrence(o1));
    assertTrue(l.isEmpty());

    l = new LinkedList<Object>();
    l.add(o1);
    l.add(o2);
    l.add(o3);
    assertTrue(l.removeLastOccurrence(o2));
    checkListSizeAndContent(l, o1, o3);

    l = new LinkedList<Object>();
    l.add(o1);
    l.add(o2);
    l.add(o3);
    l.add(o1);
    l.add(o2);
    l.add(o3);
    assertTrue(l.removeLastOccurrence(o2));
    checkListSizeAndContent(l, o1, o2, o3, o1, o3);
  }

  public void testRemoveRange() {
    LinkedListWithRemoveRange l = new LinkedListWithRemoveRange();
    for (int i = 0; i < 10; i++) {
      l.add(new Integer(i));
    }
    try {
      l.removeRange(-1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      l.removeRange(2, 11);
      fail();
    } catch (NoSuchElementException expected) {
    }

    assertEquals(2, l.size());
    for (int i = 0; i < 2; i++) {
      Integer elem = (Integer) l.get(i);
      assertEquals(i, elem.intValue());
    }

    for (int i = 2; i < 10; i++) {
      l.add(new Integer(i));
    }

    l.removeRange(3, 5);
    assertEquals(8, l.size());
    for (int i = 0; i < 3; i++) {
      Integer elem = (Integer) l.get(i);
      assertEquals(i, elem.intValue());
    }
    for (int i = 3; i < 8; i++) {
      Integer elem = (Integer) l.get(i);
      assertEquals(i + 2, elem.intValue());
    }
  }

  @Override
  protected List makeEmptyList() {
    return new LinkedList();
  }

  private void checkListSizeAndContent(List<Object> in, Object... expected) {
    assertEquals(expected.length, in.size());
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], in.get(i));
    }
  }
}
