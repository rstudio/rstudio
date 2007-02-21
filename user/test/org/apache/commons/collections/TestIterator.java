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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Base class for tetsing Iterator interface
 * 
 * @author Morgan Delagrange
 */
public abstract class TestIterator extends TestObject {

   

    public abstract Iterator makeEmptyIterator();

    public abstract Iterator makeFullIterator();

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
     * Whether or not we are testing an iterator that can contain
     * elements.  Default is true.
     * 
     * @return true if Iterators can be empty
     */
    public boolean supportsFullIterator() {
        return true;
    }

    /**
     * Whether or not we are testing an iterator that supports remove.  
     * Default is true.
     * 
     * @return true if Iterators can remove elements
     */
    public boolean supportsRemove() {
        return true;
    }
    
    /**
     * Should throw a NoSuchElementException.
     */
    public void testEmptyIterator() {
        if (supportsEmptyIterator() == false) {
            return;
        }

        Iterator iter = makeEmptyIterator();
        assertTrue("hasNext() should return false for empty iterators",iter.hasNext() == false);
        try {
	    iter.next();
            fail("NoSuchElementException must be thrown when Iterator is exhausted");
	} catch (NoSuchElementException e) {
	}
    }

    /**
     * NoSuchElementException (or any other exception)
     * should not be thrown for the first element.  
     * NoSuchElementException must be thrown when
     * hasNext() returns false
     */
    public void testFullIterator() {
        if (supportsFullIterator() == false) {
            return;
        }

        Iterator iter = makeFullIterator();

        assertTrue("hasNext() should return true for at least one element",iter.hasNext());

        try {
	    iter.next();
	} catch (NoSuchElementException e) {
            fail("Full iterators must have at least one element");
	}

        while (iter.hasNext()) {
            iter.next();
        }

        try {
	    iter.next();
            fail("NoSuchElementException must be thrown when Iterator is exhausted");
	} catch (NoSuchElementException e) {
	}
    }

}
