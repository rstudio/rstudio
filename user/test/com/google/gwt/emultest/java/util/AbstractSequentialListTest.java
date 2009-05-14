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
package com.google.gwt.emultest.java.util;

import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Test custom iterators for AbstractSequentialList. Checks issue 3057.
 */
@SuppressWarnings("unchecked")
public class AbstractSequentialListTest extends EmulTestBase {

  // a simple wrapper over LinkedList.
  private class AbstractSequentialListImpl<E> extends AbstractSequentialList<E> {

    private LinkedList<E> internalList = new LinkedList<E>();

    @Override
    public ListIterator<E> listIterator(final int index) {
      // a custom iterator that skips the 3rd element
      return new ListIterator<E>() {

        int position = index;

        public void add(E e) {
          internalList.add(position, e);
          position++;
        }

        public boolean hasNext() {
          return position < internalList.size();
        }

        public boolean hasPrevious() {
          return position > 0;
        }

        public E next() {
          E el = internalList.get(position);
          position++;
          // skip the 3rd element
          if (position == 3) {
            return next();
          }
          return el;
        }

        public int nextIndex() {
          throw new UnsupportedOperationException(
              "nextIndex operation not supported");
        }

        public E previous() {
          throw new UnsupportedOperationException(
              "previous operation not supported");
        }

        public int previousIndex() {
          throw new UnsupportedOperationException(
              "previousIndex operation not supported");
        }

        public void remove() {
          throw new UnsupportedOperationException(
              "remove operation not supported");
        }

        public void set(E e) {
          throw new UnsupportedOperationException("set operation not supported");
        }

      };
    }

    @Override
    public int size() {
      return internalList.size();
    }
  }

  public void testIteratorFunction() {
    List l = new AbstractSequentialListImpl<Integer>();
    int firstFivePrimes[] = {2, 3, 5, 7, 11};
    for (int prime : firstFivePrimes) {
      l.add(new Integer(prime));
    }
    assertEquals(5, l.size());

    /*
     * check that the iteration order is correct irrespective of whether
     * iterator(), listIterator(), or listIterator(0) is called.
     */
    checkIterator(l.iterator());
    checkIterator(l.listIterator());
    checkIterator(l.listIterator(0));
  }

  private void checkIterator(Iterator iterator) {
    assertEquals(new Integer(2), iterator.next());
    assertEquals(new Integer(3), iterator.next());
    // assertEquals(new Integer(5), iterator.next()); // skip 3rd element
    assertEquals(new Integer(7), iterator.next());
    assertEquals(new Integer(11), iterator.next());
  }

}
