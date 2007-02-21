/*
 * Copyright 1999-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections;

 import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;


/**
 * Tests base {@link java.util.List} methods and contracts.
 * <p>
 * To use, simply extend this class, and implement
 * the {@link #makeList} method.
 * <p>
 * If your {@link List} fails one of these tests by design,
 * you may still use this base set of cases.  Simply override the
 * test case (method) your {@link List} fails.
 *
 * @author Rodney Waldhoff
 * @author Paul Jack
 * @version $Id: TestList.java,v 1.13.2.1 2004/05/22 12:14:05 scolebourne Exp $
 */
public abstract class TestList extends TestCollection {

 


    /**
     *  Return a new, empty {@link List} to be used for testing.
     *
     *  @return an empty list for testing.
     */
    protected abstract List makeEmptyList();


    /**
     *  Return a new, full {@link List} to be used for testing.
     *
     *  @return a full list for testing
     */
    protected List makeFullList() {
        // only works if list supports optional "addAll(Collection)" 
        List list = makeEmptyList();
        list.addAll(Arrays.asList(getFullElements()));
        return list;
    }


    /**
     *  Returns {@link makeEmptyList()}.
     *
     *  @return an empty list to be used for testing
     */
    final protected Collection makeCollection() {
        return makeEmptyList();
    }


    /**
     *  Returns {@link makeFullList()}.
     *
     *  @return a full list to be used for testing
     */
    final protected Collection makeFullCollection() {
        return makeFullList();
    }



    /**
     * Whether or not we are testing an iterator that can be
     * empty.  Default is true.
     * 
     * @return true if Iterators can be empty
     */
    public boolean supportsEmptyIterator() {
        return true;
    }

    /**
     * Whether or not we are testing an list that allows
     * element set.  Default is true.
     * 
     * @return true if Lists support element set
     */
    public boolean isSetSupported() {
        return true;
    }
    
    
    /**
     *  Returns the {@link collection} field cast to a {@link List}.
     *
     *  @return the collection field as a List
     */
    protected List getList() {
        return (List)collection;
    } 


    /**
     *  Returns the {@link confirmed} field cast to a {@link List}.
     *
     *  @return the confirmed field as a List
     */
    protected List getConfirmedList() {
        return (List)confirmed;
    }


  

      public class TestListIterator extends AbstractTestListIterator {

        public Object addSetValue() {
          return TestList.this.getOtherElements()[0];
        }

        public boolean supportsRemove() {
          return TestList.this.isRemoveSupported();
        }

        public boolean supportsAdd() {
          return TestList.this.isAddSupported();
        }

        public boolean supportsSet() {
          return TestList.this.isSetSupported();
        }

        public ListIterator makeEmptyListIterator() {
          resetEmpty();
          return ((List) TestList.this.collection).listIterator();
        }

        public ListIterator makeFullListIterator() {
          resetFull();
          return ((List) TestList.this.collection).listIterator();
        }

        public Object makeObject() {
          return ((List) TestList.this.collection).listIterator();
        }
      }

      /**
       * Tests the read-only bits of {@link List#listIterator()}.
       */
      public void testListListIterator() {
        resetFull();
        forwardTest(getList().listIterator(), 0);
        backwardTest(getList().listIterator(), 0);
      }

      /**
       * Tests the read-only bits of {@link List#listIterator(int)}.
       */
      public void testListListIteratorByIndex() {
        resetFull();
        try {
          getList().listIterator(-1);
        } catch (IndexOutOfBoundsException ex) {
        }
        resetFull();
        try {
          getList().listIterator(getList().size() + 1);
        } catch (IndexOutOfBoundsException ex) {
        }
        resetFull();
        for (int i = 0; i <= confirmed.size(); i++) {
          forwardTest(getList().listIterator(i), i);
          backwardTest(getList().listIterator(i), i);
        }
        resetFull();
        for (int i = 0; i <= confirmed.size(); i++) {
          backwardTest(getList().listIterator(i), i);
        }
      }

      // -----------------------------------------------------------------------
      /**
       * Tests remove on list iterator is correct.
       */
      public void testListListIteratorPreviousRemoveNext() {
        if (isRemoveSupported() == false)
          return;
        resetFull();
        if (collection.size() < 4)
          return;
        ListIterator it = getList().listIterator();
        Object zero = it.next();
        Object one = it.next();
        Object two = it.next();
        Object two2 = it.previous();
        Object one2 = it.previous();
        assertEquals(one, one2);
        assertEquals(two, two2);
        assertEquals(zero, getList().get(0));
        assertEquals(one, getList().get(1));
        assertEquals(two, getList().get(2));
        
        it.remove(); // removed element at index 1 (one)
        assertEquals(zero, getList().get(0));
        assertEquals(two, getList().get(1));
        Object two3 = it.next(); // do next after remove
        assertEquals(two, two3);
        assertEquals(collection.size() > 2, it.hasNext());
        assertEquals(true, it.hasPrevious());
      }

      /**
       * Tests remove on list iterator is correct.
       */
      public void testListListIteratorPreviousRemovePrevious() {
        if (isRemoveSupported() == false)
          return;
        resetFull();
        if (collection.size() < 4)
          return;
        ListIterator it = getList().listIterator();
        Object zero = it.next();
        Object one = it.next();
        Object two = it.next();
        Object two2 = it.previous();
        Object one2 = it.previous();
        assertEquals(one, one2);
        assertEquals(two, two2);
        assertEquals(zero, getList().get(0));
        assertEquals(one, getList().get(1));
        assertEquals(two, getList().get(2));

        it.remove(); // removed element at index 1 (one)
        assertEquals(zero, getList().get(0));
        assertEquals(two, getList().get(1));
        Object zero3 = it.previous(); // do previous after remove
        assertEquals(zero, zero3);
        assertEquals(false, it.hasPrevious());
        assertEquals(collection.size() > 2, it.hasNext());
      }

      /**
       * Tests remove on list iterator is correct.
       */
      public void testListListIteratorNextRemoveNext() {
        if (isRemoveSupported() == false)
          return;
        resetFull();
        if (collection.size() < 4)
          return;
        ListIterator it = getList().listIterator();
        Object zero = it.next();
        Object one = it.next();
        Object two = it.next();
        assertEquals(zero, getList().get(0));
        assertEquals(one, getList().get(1));
        assertEquals(two, getList().get(2));
        Object three = getList().get(3);

        it.remove(); // removed element at index 2 (two)
        assertEquals(zero, getList().get(0));
        assertEquals(one, getList().get(1));
        Object three2 = it.next(); // do next after remove
        assertEquals(three, three2);
        assertEquals(collection.size() > 3, it.hasNext());
        assertEquals(true, it.hasPrevious());
      }

      /**
       * Tests remove on list iterator is correct.
       */
      public void testListListIteratorNextRemovePrevious() {
        if (isRemoveSupported() == false)
          return;
        resetFull();
        if (collection.size() < 4)
          return;
        ListIterator it = getList().listIterator();
        Object zero = it.next();
        Object one = it.next();
        Object two = it.next();
        assertEquals(zero, getList().get(0));
        assertEquals(one, getList().get(1));
        assertEquals(two, getList().get(2));

        it.remove(); // removed element at index 2 (two)
        assertEquals(zero, getList().get(0));
        assertEquals(one, getList().get(1));
        Object one2 = it.previous(); // do previous after remove
        assertEquals(one, one2);
        assertEquals(true, it.hasNext());
        assertEquals(true, it.hasPrevious());
      }

      // -----------------------------------------------------------------------
      /**
       * Traverses to the end of the given iterator.
       * 
       * @param iter the iterator to traverse
       * @param i the starting index
       */
      private void forwardTest(ListIterator iter, int i) {
        List list = getList();
        int max = getFullElements().length;

        while (i < max) {
          assertTrue("Iterator should have next", iter.hasNext());
          assertEquals("Iterator.nextIndex should work", iter.nextIndex(), i);
          assertEquals("Iterator.previousIndex should work", iter.previousIndex(),
              i - 1);
          Object o = iter.next();
          assertEquals("Iterator returned correct element f", list.get(i), o);
          i++;
        }

        assertTrue("Iterator shouldn't have next", !iter.hasNext());
        assertEquals("nextIndex should be size", iter.nextIndex(), max);
        assertEquals("previousIndex should be size - 1", iter.previousIndex(),
            max - 1);

        try {
          iter.next();
          fail("Exhausted iterator should raise NoSuchElement");
        } catch (NoSuchElementException e) {
          // expected
        }
      }

      /**
       * Traverses to the beginning of the given iterator.
       * 
       * @param iter the iterator to traverse
       * @param i the starting index
       */
      private void backwardTest(ListIterator iter, int i) {
        List list = getList();

        while (i > 0) {
          assertTrue("Iterator should have previous, i:" + i, iter.hasPrevious());
          assertEquals("Iterator.nextIndex should work, i:" + i, iter.nextIndex(),
              i);
          assertEquals("Iterator.previousIndex should work, i:" + i,
              iter.previousIndex(), i - 1);
          Object o = iter.previous();
          assertEquals("Iterator returned correct element b", list.get(i - 1), o);
          i--;
        }

        assertTrue("Iterator shouldn't have previous", !iter.hasPrevious());
        int nextIndex = iter.nextIndex();
        assertEquals("nextIndex should be 0, actual value: " + nextIndex,
            nextIndex, 0);
        int prevIndex = iter.previousIndex();
        assertEquals("previousIndex should be -1, actual value: " + prevIndex,
            prevIndex, -1);

        try {
          iter.previous();
          fail("Exhausted iterator should raise NoSuchElement");
        } catch (NoSuchElementException e) {
          // expected
        }

      }

      /**
       * Tests the {@link ListIterator#add(Object)} method of the list iterator.
       */
      public void testListIteratorAdd() {
        if (!isAddSupported())
          return;

        resetEmpty();
        List list1 = getList();
        List list2 = getConfirmedList();

        Object[] elements = getFullElements();
        ListIterator iter1 = list1.listIterator();
        ListIterator iter2 = list2.listIterator();

        for (int i = 0; i < elements.length; i++) {
          iter1.add(elements[i]);
          iter2.add(elements[i]);
          verify();
        }

        resetFull();
        iter1 = getList().listIterator();
        iter2 = getConfirmedList().listIterator();
        for (int i = 0; i < elements.length; i++) {
          iter1.next();
          iter2.next();
          iter1.add(elements[i]);
          iter2.add(elements[i]);
          verify();
        }
      }

      /**
       * Tests the {@link ListIterator#set(Object)} method of the list iterator.
       */
      public void testListIteratorSet() {
        if (!isSetSupported())
          return;

        Object[] elements = getFullElements();

        resetFull();
        ListIterator iter1 = getList().listIterator();
        ListIterator iter2 = getConfirmedList().listIterator();
        for (int i = 0; i < elements.length; i++) {
          iter1.next();
          iter2.next();
          iter1.set(elements[i]);
          iter2.set(elements[i]);
          verify();
        }
      }

      
      
      
    /**
     *  Tests {@link List#add(int,Object)}.
     */
    public void testListAddByIndex() {
        if (!isAddSupported()) return;

        Object element = getOtherElements()[0];
        int max = getFullElements().length;

        for (int i = 0; i <= max; i++) {
            resetFull();
            ((List)collection).add(i, element);
            ((List)confirmed).add(i, element);
            verify();
        }
    }


    /**
     *  Tests {@link List#equals(Object)}.
     */
    public void testListEquals() {
        resetEmpty();
        List list = getList();
        assertTrue("Empty lists should be equal", list.equals(confirmed));
        verify();
        assertTrue("Empty list should equal self", list.equals(list));
        verify();

        List list2 = Arrays.asList(getFullElements());
        assertTrue("Empty list shouldn't equal full", !list.equals(list2));
        verify();

        list2 = Arrays.asList(getOtherElements());
        assertTrue("Empty list shouldn't equal other", !list.equals(list2));
        verify();

        resetFull();
        list = getList();
        assertTrue("Full lists should be equal", list.equals(confirmed));
        verify();
        assertTrue("Full list should equal self", list.equals(list));
        verify();

        list2 = makeEmptyList();
        assertTrue("Full list shouldn't equal empty", !list.equals(list2));
        verify();

        list2 = Arrays.asList(getOtherElements());
        assertTrue("Full list shouldn't equal other", !list.equals(list2));
        verify();

        list2 = Arrays.asList(getFullElements());
        Collections.reverse(list2);
        assertTrue("Full list shouldn't equal full list with same elements" +
          " but different order", !list.equals(list2));
        verify();

        assertTrue("List shouldn't equal String", !list.equals(""));
        verify();

        final List listForC = Arrays.asList(getFullElements());
        Collection c = new AbstractCollection() {
            public int size() {
                return listForC.size();
            }

            public Iterator iterator() {
                return listForC.iterator();
            }
        };

        assertTrue("List shouldn't equal nonlist with same elements " +
          " in same order", !list.equals(c));
        verify();
    }


    /**
     *  Tests {@link List#hashCode()}.
     */
    public void testListHashCode() {
        resetEmpty();
        int hash1 = collection.hashCode();
        int hash2 = confirmed.hashCode();
        assertEquals("Empty lists should have equal hashCodes", hash1, hash2);
        verify();

        resetFull();
        hash1 = collection.hashCode();
        hash2 = confirmed.hashCode();
        assertEquals("Full lists should have equal hashCodes", hash1, hash2);
        verify();
    }


    /**
     *  Tests {@link List#get(int)}.
     */
    public void testListGetByIndex() {
        resetFull();
        List list = getList();
        Object[] elements = getFullElements();
        for (int i = 0; i < elements.length; i++) {
            assertEquals("List should contain correct elements", 
              elements[i], list.get(i));
            verify();
        }
    }


     
    /**
     *  Tests {@link List#indexOf()}.
     */
    public void testListIndexOf() {
        resetFull();
        List list1 = getList();
        List list2 = getConfirmedList();

        Iterator iterator = list2.iterator();
        while (iterator.hasNext()) {
            Object element = iterator.next();
            assertEquals("indexOf should return correct result", 
              list1.indexOf(element), list2.indexOf(element));            
            verify();
        }

        Object[] other = getOtherElements();
        for (int i = 0; i < other.length; i++) {
            assertEquals("indexOf should return -1 for nonexistent element",
              list1.indexOf(other[i]), -1);
            verify();
        }
    }


    /**
     *  Tests {@link List#lastIndexOf()}.
     */
    public void testListLastIndexOf() {
        resetFull();
        List list1 = getList();
        List list2 = getConfirmedList();

        Iterator iterator = list2.iterator();
        while (iterator.hasNext()) {
            Object element = iterator.next();
            assertEquals("lastIndexOf should return correct result",
              list1.lastIndexOf(element), list2.lastIndexOf(element));
            verify();
        }

        Object[] other = getOtherElements();
        for (int i = 0; i < other.length; i++) {
            assertEquals("lastIndexOf should return -1 for nonexistent " +
              "element", list1.lastIndexOf(other[i]), -1);
            verify();
        }
    }


     

    /**
     *  Tests {@link List#remove(int)}.
     */
    public void testListRemoveByIndex() {
        if (!isRemoveSupported()) return;

        int max = getFullElements().length;
        for (int i = 0; i < max; i++) {
            resetFull();
            Object o1 = ((List)collection).remove(i);
            Object o2 = ((List)confirmed).remove(i);
            assertEquals("remove should return correct element", o1, o2);
            verify();
        }
    }
 
 
  
 
   
   
    


    /**
     *  Returns an empty {@link ArrayList}.
     */
    protected Collection makeConfirmedCollection() {
        ArrayList list = new ArrayList();
        return list;
    }


    /**
     *  Returns a full {@link ArrayList}.
     */
    protected Collection makeConfirmedFullCollection() {
        ArrayList list = new ArrayList();
        list.addAll(Arrays.asList(getFullElements()));
        return list;
    }


    /**
     *  Verifies that the test list implementation matches the confirmed list
     *  implementation.
     */
    protected void verify() {
        super.verify();

        List list1 = getList();
        List list2 = getConfirmedList();

        assertEquals("List should equal confirmed", list1, list2);
        assertEquals("Confirmed should equal list", list2, list1);

        assertEquals("Hash codes should be equal", 
          list1.hashCode(), list2.hashCode());

        int i = 0;
        Iterator iterator1 = list1.iterator();
        Iterator iterator2 = list2.iterator();
        Object[] array = list1.toArray();
        while (iterator2.hasNext()) {
            assertTrue("List iterator should have next", iterator1.hasNext());
            Object o1 = iterator1.next();
            Object o2 = iterator2.next();
            assertEquals("Iterator elements should be equal", o1, o2);
            o2 = list1.get(i);
            assertEquals("get should return correct element", o1, o2);
            o2 = array[i];
            assertEquals("toArray should have correct element", o1, o2);
            i++;
        }
    }

    
 

}
