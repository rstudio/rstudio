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

import org.apache.commons.collections.AbstractTestObject;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract class for testing the Iterator interface.
 * <p>
 * This class provides a framework for testing an implementation of Iterator.
 * Concrete subclasses must provide the iterator to be tested.
 * They must also specify certain details of how the iterator operates by
 * overriding the supportsXxx() methods if necessary.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646780 $ $Date: 2008-04-10 13:48:07 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Morgan Delagrange
 * @author Stephen Colebourne
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractTestIterator extends AbstractTestObject {

    /**
     * JUnit constructor.
     *
     * @param testName  the test class name
     */
    public AbstractTestIterator(String testName) {
        super(testName);
    }

    //-----------------------------------------------------------------------
    /**
     * Implement this method to return an iterator over an empty collection.
     *
     * @return an empty iterator
     */
    public abstract Iterator makeEmptyIterator();

    /**
     * Implement this method to return an iterator over a collection with elements.
     *
     * @return a full iterator
     */
    public abstract Iterator makeFullIterator();

    /**
     * Implements the abstract superclass method to return the full iterator.
     *
     * @return a full iterator
     */
    @Override
    public Object makeObject() {
        return makeFullIterator();
    }

    /**
     * Whether or not we are testing an iterator that can be empty.
     * Default is true.
     *
     * @return true if Iterator can be empty
     */
    public boolean supportsEmptyIterator() {
        return true;
    }

    /**
     * Whether or not we are testing an iterator that can contain elements.
     * Default is true.
     *
     * @return true if Iterator can be full
     */
    public boolean supportsFullIterator() {
        return true;
    }

    /**
     * Whether or not we are testing an iterator that supports remove().
     * Default is true.
     *
     * @return true if Iterator supports remove
     */
    public boolean supportsRemove() {
        return true;
    }

    /**
     * Allows subclasses to add complex cross verification
     */
    public void verify() {
        // do nothing
    }

    //-----------------------------------------------------------------------
    /**
     * Test the empty iterator.
     */
    public void testEmptyIterator() {
        if (supportsEmptyIterator() == false) {
            return;
        }

        Iterator it = makeEmptyIterator();

        // hasNext() should return false
        assertEquals("hasNext() should return false for empty iterators", false, it.hasNext());

        // next() should throw a NoSuchElementException
        try {
            it.next();
            fail("NoSuchElementException must be thrown when Iterator is exhausted");
        } catch (NoSuchElementException e) {
        }
        verify();

        assertNotNull(it.toString());
    }

    /**
     * Test normal iteration behaviour.
     */
    public void testFullIterator() {
        if (supportsFullIterator() == false) {
            return;
        }

        Iterator it = makeFullIterator();

        // hasNext() must be true (ensure makeFullIterator is correct!)
        assertEquals("hasNext() should return true for at least one element", true, it.hasNext());

        // next() must not throw exception (ensure makeFullIterator is correct!)
        try {
            it.next();
        } catch (NoSuchElementException e) {
            fail("Full iterators must have at least one element");
        }

        // iterate through
        while (it.hasNext()) {
            it.next();
            verify();
        }

        // next() must throw NoSuchElementException now
        try {
            it.next();
            fail("NoSuchElementException must be thrown when Iterator is exhausted");
        } catch (NoSuchElementException e) {
        }

        assertNotNull(it.toString());
    }

    /**
     * Test remove behaviour.
     */
    public void testRemove() {
        Iterator it = makeFullIterator();

        if (supportsRemove() == false) {
            // check for UnsupportedOperationException if not supported
            try {
                it.remove();
            } catch (UnsupportedOperationException ex) {}
            return;
        }

        // should throw IllegalStateException before next() called
        try {
            it.remove();
            fail();
        } catch (IllegalStateException ex) {}
        verify();

        // remove after next should be fine
        it.next();
        it.remove();

        // should throw IllegalStateException for second remove()
        try {
            it.remove();
            fail();
        } catch (IllegalStateException ex) {}
    }

}
