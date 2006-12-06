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
