/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.collections.iterators;

import org.apache.commons.collections.MapIterator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Abstract class for testing the MapIterator interface.
 * <p>
 * This class provides a framework for testing an implementation of MapIterator.
 * Concrete subclasses must provide the list iterator to be tested.
 * They must also specify certain details of how the list iterator operates by
 * overriding the supportsXxx() methods if necessary.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646780 $ $Date: 2008-04-10 13:48:07 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractTestMapIterator extends AbstractTestIterator {

    /**
     * JUnit constructor.
     *
     * @param testName  the test class name
     */
    public AbstractTestMapIterator(String testName) {
        super(testName);
    }

    //-----------------------------------------------------------------------
    /**
     * Implement this method to return a map iterator over an empty map.
     *
     * @return an empty iterator
     */
    public abstract MapIterator makeEmptyMapIterator();

    /**
     * Implement this method to return a map iterator over a map with elements.
     *
     * @return a full iterator
     */
    public abstract MapIterator makeFullMapIterator();

    /**
     * Implement this method to return the map which contains the same data as the
     * iterator.
     *
     * @return a full map which can be updated
     */
    public abstract Map getMap();

    /**
     * Implement this method to return the confirmed map which contains the same
     * data as the iterator.
     *
     * @return a full map which can be updated
     */
    public abstract Map getConfirmedMap();

    /**
     * Implements the abstract superclass method to return the list iterator.
     *
     * @return an empty iterator
     */
    @Override
    public final Iterator makeEmptyIterator() {
        return makeEmptyMapIterator();
    }

    /**
     * Implements the abstract superclass method to return the list iterator.
     *
     * @return a full iterator
     */
    @Override
    public final Iterator makeFullIterator() {
        return makeFullMapIterator();
    }

    /**
     * Whether or not we are testing an iterator that supports setValue().
     * Default is true.
     *
     * @return true if Iterator supports set
     */
    public boolean supportsSetValue() {
        return true;
    }

    /**
     * Whether the get operation on the map structurally modifies the map,
     * such as with LRUMap. Default is false.
     *
     * @return true if the get method structurally modifies the map
     */
    public boolean isGetStructuralModify() {
        return false;
    }

    /**
     * The values to be used in the add and set tests.
     * Default is two strings.
     */
    public Object[] addSetValues() {
        return new Object[] {"A", "B"};
    }

    //-----------------------------------------------------------------------
    /**
     * Test that the empty list iterator contract is correct.
     */
    public void testEmptyMapIterator() {
        if (supportsEmptyIterator() == false) {
            return;
        }

        MapIterator it = makeEmptyMapIterator();
        Map map = getMap();
        assertEquals(false, it.hasNext());

        // next() should throw a NoSuchElementException
        try {
            it.next();
            fail();
        } catch (NoSuchElementException ex) {}

        // getKey() should throw an IllegalStateException
        try {
            it.getKey();
            fail();
        } catch (IllegalStateException ex) {}

        // getValue() should throw an IllegalStateException
        try {
            it.getValue();
            fail();
        } catch (IllegalStateException ex) {}

        if (supportsSetValue() == false) {
            // setValue() should throw an UnsupportedOperationException/IllegalStateException
            try {
                it.setValue(addSetValues()[0]);
                fail();
            } catch (UnsupportedOperationException ex) {
            } catch (IllegalStateException ex) {}
        } else {
            // setValue() should throw an IllegalStateException
            try {
                it.setValue(addSetValues()[0]);
                fail();
            } catch (IllegalStateException ex) {}
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Test that the full list iterator contract is correct.
     */
    public void testFullMapIterator() {
        if (supportsFullIterator() == false) {
            return;
        }

        MapIterator it = makeFullMapIterator();
        Map map = getMap();
        assertEquals(true, it.hasNext());

        assertEquals(true, it.hasNext());
        Set set = new HashSet();
        while (it.hasNext()) {
            // getKey
            Object key = it.next();
            assertSame("it.next() should equals getKey()", key, it.getKey());
            assertTrue("Key must be in map",  map.containsKey(key));
            assertTrue("Key must be unique", set.add(key));

            // getValue
            Object value = it.getValue();
            if (isGetStructuralModify() == false) {
                assertSame("Value must be mapped to key", map.get(key), value);
            }
            assertTrue("Value must be in map",  map.containsValue(value));

            verify();
        }
    }

    //-----------------------------------------------------------------------
    public void testMapIteratorSet() {
        if (supportsFullIterator() == false) {
            return;
        }

        Object newValue = addSetValues()[0];
        Object newValue2 = (addSetValues().length == 1 ? addSetValues()[0] : addSetValues()[1]);
        MapIterator it = makeFullMapIterator();
        Map map = getMap();
        Map confirmed = getConfirmedMap();
        assertEquals(true, it.hasNext());
        Object key = it.next();
        Object value = it.getValue();

        if (supportsSetValue() == false) {
            try {
                it.setValue(newValue);
                fail();
            } catch (UnsupportedOperationException ex) {}
            return;
        }
        Object old = it.setValue(newValue);
        confirmed.put(key, newValue);
        assertSame("Key must not change after setValue", key, it.getKey());
        assertSame("Value must be changed after setValue", newValue, it.getValue());
        assertSame("setValue must return old value", value, old);
        assertEquals("Map must contain key", true, map.containsKey(key));
        // test against confirmed, as map may contain value twice
        assertEquals("Map must not contain old value",
            confirmed.containsValue(old), map.containsValue(old));
        assertEquals("Map must contain new value", true, map.containsValue(newValue));
        verify();

        it.setValue(newValue);  // same value - should be OK
        confirmed.put(key, newValue);
        assertSame("Key must not change after setValue", key, it.getKey());
        assertSame("Value must be changed after setValue", newValue, it.getValue());
        verify();

        it.setValue(newValue2);  // new value
        confirmed.put(key, newValue2);
        assertSame("Key must not change after setValue", key, it.getKey());
        assertSame("Value must be changed after setValue", newValue2, it.getValue());
        verify();
    }

    //-----------------------------------------------------------------------
    @Override
    public void testRemove() { // override
        MapIterator it = makeFullMapIterator();
        Map map = getMap();
        Map confirmed = getConfirmedMap();
        assertEquals(true, it.hasNext());
        Object key = it.next();

        if (supportsRemove() == false) {
            try {
                it.remove();
                fail();
            } catch (UnsupportedOperationException ex) {
            }
            return;
        }

        it.remove();
        confirmed.remove(key);
        assertEquals(false, map.containsKey(key));
        verify();

        try {
            it.remove();  // second remove fails
        } catch (IllegalStateException ex) {
        }
        verify();
    }

    //-----------------------------------------------------------------------
    public void testMapIteratorSetRemoveSet() {
        if (supportsSetValue() == false || supportsRemove() == false) {
            return;
        }
        Object newValue = addSetValues()[0];
        MapIterator it = makeFullMapIterator();
        Map map = getMap();
        Map confirmed = getConfirmedMap();

        assertEquals(true, it.hasNext());
        Object key = it.next();

        it.setValue(newValue);
        it.remove();
        confirmed.remove(key);
        verify();

        try {
            it.setValue(newValue);
            fail();
        } catch (IllegalStateException ex) {}
        verify();
    }

    //-----------------------------------------------------------------------
    public void testMapIteratorRemoveGetKey() {
        if (supportsRemove() == false) {
            return;
        }
        MapIterator it = makeFullMapIterator();
        Map map = getMap();
        Map confirmed = getConfirmedMap();

        assertEquals(true, it.hasNext());
        Object key = it.next();

        it.remove();
        confirmed.remove(key);
        verify();

        try {
            it.getKey();
            fail();
        } catch (IllegalStateException ex) {}
        verify();
    }

    //-----------------------------------------------------------------------
    public void testMapIteratorRemoveGetValue() {
        if (supportsRemove() == false) {
            return;
        }
        MapIterator it = makeFullMapIterator();
        Map map = getMap();
        Map confirmed = getConfirmedMap();

        assertEquals(true, it.hasNext());
        Object key = it.next();

        it.remove();
        confirmed.remove(key);
        verify();

        try {
            it.getValue();
            fail();
        } catch (IllegalStateException ex) {}
        verify();
    }

}
