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
package org.apache.commons.collections.map;

import org.apache.commons.collections.BulkTest;
import org.apache.commons.collections.IterableMap;
import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.iterators.AbstractTestMapIterator;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

/**
 * Abstract test class for {@link IterableMap} methods and contracts.
 *
 * @version $Revision: 646780 $ $Date: 2008-04-10 13:48:07 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractTestIterableMap extends AbstractTestMap {

    /**
     * JUnit constructor.
     *
     * @param testName  the test name
     */
    public AbstractTestIterableMap(String testName) {
        super(testName);
    }

    //-----------------------------------------------------------------------
    public void testFailFastEntrySet() {
        if (isRemoveSupported() == false) return;
        resetFull();
        Iterator it = map.entrySet().iterator();
        Map.Entry val = (Map.Entry) it.next();
        map.remove(val.getKey());
        try {
            it.next();
            fail();
        } catch (ConcurrentModificationException ex) {}

        resetFull();
        it = map.entrySet().iterator();
        it.next();
        map.clear();
        try {
            it.next();
            fail();
        } catch (ConcurrentModificationException ex) {}
    }

    public void testFailFastKeySet() {
        if (isRemoveSupported() == false) return;
        resetFull();
        Iterator it = map.keySet().iterator();
        Object val = it.next();
        map.remove(val);
        try {
            it.next();
            fail();
        } catch (ConcurrentModificationException ex) {}

        resetFull();
        it = map.keySet().iterator();
        it.next();
        map.clear();
        try {
            it.next();
            fail();
        } catch (ConcurrentModificationException ex) {}
    }

    public void testFailFastValues() {
        if (isRemoveSupported() == false) return;
        resetFull();
        Iterator it = map.values().iterator();
        it.next();
        map.remove(map.keySet().iterator().next());
        try {
            it.next();
            fail();
        } catch (ConcurrentModificationException ex) {}

        resetFull();
        it = map.values().iterator();
        it.next();
        map.clear();
        try {
            it.next();
            fail();
        } catch (ConcurrentModificationException ex) {}
    }

    //-----------------------------------------------------------------------
    public BulkTest bulkTestMapIterator() {
        return new InnerTestMapIterator();
    }

    public class InnerTestMapIterator extends AbstractTestMapIterator {
        public InnerTestMapIterator() {
            super("InnerTestMapIterator");
        }

        @Override
        public Object[] addSetValues() {
            return AbstractTestIterableMap.this.getNewSampleValues();
        }

        @Override
        public boolean supportsRemove() {
            return AbstractTestIterableMap.this.isRemoveSupported();
        }

        @Override
        public boolean isGetStructuralModify() {
            return AbstractTestIterableMap.this.isGetStructuralModify();
        }

        @Override
        public boolean supportsSetValue() {
            return AbstractTestIterableMap.this.isSetValueSupported();
        }

        @Override
        public MapIterator makeEmptyMapIterator() {
            resetEmpty();
            return ((IterableMap) AbstractTestIterableMap.this.map).mapIterator();
        }

        @Override
        public MapIterator makeFullMapIterator() {
            resetFull();
            return ((IterableMap) AbstractTestIterableMap.this.map).mapIterator();
        }

        @Override
        public Map getMap() {
            // assumes makeFullMapIterator() called first
            return AbstractTestIterableMap.this.map;
        }

        @Override
        public Map getConfirmedMap() {
            // assumes makeFullMapIterator() called first
            return AbstractTestIterableMap.this.confirmed;
        }

        @Override
        public void verify() {
            super.verify();
            AbstractTestIterableMap.this.verify();
        }
    }

//  public void testCreate() throws Exception {
//      resetEmpty();
//      writeExternalFormToDisk((Serializable) map, "D:/dev/collections/data/test/HashedMap.emptyCollection.version3.obj");
//      resetFull();
//      writeExternalFormToDisk((Serializable) map, "D:/dev/collections/data/test/HashedMap.fullCollection.version3.obj");
//  }
}
