/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * THIS CODE HAS BEEN EXTENSIVELY HACKED BY GOOGLE TO WORK WITH GWT. 
 */
package org.apache.commons.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Abstract class for testing the ListIterator interface.
 * <p>
 * This class provides a framework for testing an implementation of
 * ListIterator. Concrete subclasses must provide the list iterator to be
 * tested. They must also specify certain details of how the list iterator
 * operates by overriding the supportsXxx() methods if necessary.
 * 
 * @since Commons Collections 3.0
 * @version $Revision$ $Date$
 * 
 * @author Rodney Waldhoff
 * @author Stephen Colebourne
 */
public abstract class AbstractTestListIterator extends TestIterator {

  // -----------------------------------------------------------------------
  /**
   * Implement this method to return a list iterator over an empty collection.
   * 
   * @return an empty iterator
   */
  public abstract ListIterator makeEmptyListIterator();

  /**
   * Implement this method to return a list iterator over a collection with
   * elements.
   * 
   * @return a full iterator
   */
  public abstract ListIterator makeFullListIterator();

  /**
   * Implements the abstract superclass method to return the list iterator.
   * 
   * @return an empty iterator
   */
  public Iterator makeEmptyIterator() {
    return makeEmptyListIterator();
  }

  /**
   * Implements the abstract superclass method to return the list iterator.
   * 
   * @return a full iterator
   */
  public Iterator makeFullIterator() {
    return makeFullListIterator();
  }

  /**
   * Whether or not we are testing an iterator that supports add(). Default is
   * true.
   * 
   * @return true if Iterator supports add
   */
  public boolean supportsAdd() {
    return true;
  }

  /**
   * Whether or not we are testing an iterator that supports set(). Default is
   * true.
   * 
   * @return true if Iterator supports set
   */
  public boolean supportsSet() {
    return true;
  }

  /**
   * The value to be used in the add and set tests. Default is null.
   */
  public Object addSetValue() {
    return null;
  }

  // -----------------------------------------------------------------------
  /**
   * Test that the empty list iterator contract is correct.
   */
  public void testEmptyListIteratorIsIndeedEmpty() {
    if (supportsEmptyIterator() == false) {
      return;
    }

    ListIterator it = makeEmptyListIterator();

    assertEquals(false, it.hasNext());
    assertEquals(0, it.nextIndex());
    assertEquals(false, it.hasPrevious());
    assertEquals(-1, it.previousIndex());

    // next() should throw a NoSuchElementException
    try {
      it.next();
      fail("NoSuchElementException must be thrown from empty ListIterator");
    } catch (NoSuchElementException e) {
    }

    // previous() should throw a NoSuchElementException
    try {
      it.previous();
      fail("NoSuchElementException must be thrown from empty ListIterator");
    } catch (NoSuchElementException e) {
    }
  }

  /**
   * Test navigation through the iterator.
   */
  public void testWalkForwardAndBack() {
    ArrayList list = new ArrayList();
    ListIterator it = makeFullListIterator();
    while (it.hasNext()) {
      list.add(it.next());
    }

    // check state at end
    assertEquals(false, it.hasNext());
    assertEquals(true, it.hasPrevious());
    try {
      it.next();
      fail("NoSuchElementException must be thrown from next at end of ListIterator");
    } catch (NoSuchElementException e) {
    }

    // loop back through comparing
    for (int i = list.size() - 1; i >= 0; i--) {
      assertEquals(i + 1, it.nextIndex());
      assertEquals(i, it.previousIndex());

      Object obj = list.get(i);
      assertEquals(obj, it.previous());
    }

    // check state at start
    assertEquals(true, it.hasNext());
    assertEquals(false, it.hasPrevious());
    try {
      it.previous();
      fail("NoSuchElementException must be thrown from previous at start of ListIterator");
    } catch (NoSuchElementException e) {
    }
  }

  /**
   * Test add behaviour.
   */
  public void testAdd() {
    ListIterator it = makeFullListIterator();

    Object addValue = addSetValue();
    if (supportsAdd() == false) {
      // check for UnsupportedOperationException if not supported
      try {
        it.add(addValue);
      } catch (UnsupportedOperationException ex) {
      }
      return;
    }

    // add at start should be OK, added should be previous
    it = makeFullListIterator();
    it.add(addValue);
    assertEquals(addValue, it.previous());

    // add at start should be OK, added should not be next
    it = makeFullListIterator();
    it.add(addValue);
    assertTrue(addValue != it.next());

    // add in middle and at end should be OK
    it = makeFullListIterator();
    while (it.hasNext()) {
      it.next();
      it.add(addValue);
      // check add OK
      assertEquals(addValue, it.previous());
      it.next();
    }
  }

  /**
   * Test set behaviour.
   */
  public void testSet() {
    ListIterator it = makeFullListIterator();

    if (supportsSet() == false) {
      // check for UnsupportedOperationException if not supported
      try {
        it.set(addSetValue());
      } catch (UnsupportedOperationException ex) {
      }
      return;
    }

    // should throw IllegalStateException before next() called
    try {
      it.set(addSetValue());
      fail();
    } catch (IllegalStateException ex) {
    }

    // set after next should be fine
    it.next();
    it.set(addSetValue());

    // repeated set calls should be fine
    it.set(addSetValue());

  }

  public void testRemoveThenSet() {
    ListIterator it = makeFullListIterator();
    if (supportsRemove() && supportsSet()) {
      it.next();
      it.remove();
      try {
        it.set(addSetValue());
        fail("IllegalStateException must be thrown from set after remove");
      } catch (IllegalStateException e) {
      }
    }
  }

  public void testAddThenSet() {
    ListIterator it = makeFullListIterator();
    // add then set
    if (supportsAdd() && supportsSet()) {
      it.next();
      it.add(addSetValue());
      try {
        it.set(addSetValue());
        fail("IllegalStateException must be thrown from set after add");
      } catch (IllegalStateException e) {
      }
    }
  }

  /**
   * Test remove after add behaviour.
   */
  public void testAddThenRemove() {
    ListIterator it = makeFullListIterator();

    // add then remove
    if (supportsAdd() && supportsRemove()) {
      it.next();
      it.add(addSetValue());
      try {
        it.remove();
        fail("IllegalStateException must be thrown from remove after add");
      } catch (IllegalStateException e) {
      }
    }
  }

}
