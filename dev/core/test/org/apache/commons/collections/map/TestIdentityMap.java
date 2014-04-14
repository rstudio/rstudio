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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.commons.collections.AbstractTestObject;
import org.apache.commons.collections.IterableMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

/**
 * JUnit tests.
 *
 * @version $Revision: 646780 $ $Date: 2008-04-10 13:48:07 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class TestIdentityMap extends AbstractTestObject {

    private static final Integer I1A = new Integer(1);
    private static final Integer I1B = new Integer(1);
    private static final Integer I2A = new Integer(2);
    private static final Integer I2B = new Integer(2);

    public TestIdentityMap(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestIdentityMap.class);
//        return BulkTest.makeSuite(TestIdentityMap.class);  // causes race condition!
    }

    @Override
    public Object makeObject() {
        return new IdentityMap();
    }

    @Override
    public String getCompatibilityVersion() {
        return "3";
    }

    //-----------------------------------------------------------------------
    public void testBasics() {
        IterableMap map = new IdentityMap();
        assertEquals(0, map.size());

        map.put(I1A, I2A);
        assertEquals(1, map.size());
        assertSame(I2A, map.get(I1A));
        assertSame(null, map.get(I1B));
        assertEquals(true, map.containsKey(I1A));
        assertEquals(false, map.containsKey(I1B));
        assertEquals(true, map.containsValue(I2A));
        assertEquals(false, map.containsValue(I2B));

        map.put(I1A, I2B);
        assertEquals(1, map.size());
        assertSame(I2B, map.get(I1A));
        assertSame(null, map.get(I1B));
        assertEquals(true, map.containsKey(I1A));
        assertEquals(false, map.containsKey(I1B));
        assertEquals(false, map.containsValue(I2A));
        assertEquals(true, map.containsValue(I2B));

        map.put(I1B, I2B);
        assertEquals(2, map.size());
        assertSame(I2B, map.get(I1A));
        assertSame(I2B, map.get(I1B));
        assertEquals(true, map.containsKey(I1A));
        assertEquals(true, map.containsKey(I1B));
        assertEquals(false, map.containsValue(I2A));
        assertEquals(true, map.containsValue(I2B));
    }

    //-----------------------------------------------------------------------
    public void testHashEntry() {
        IterableMap map = new IdentityMap();

        map.put(I1A, I2A);
        map.put(I1B, I2A);

        Map.Entry entry1 = (Map.Entry) map.entrySet().iterator().next();
        Iterator it = map.entrySet().iterator();
        Map.Entry entry2 = (Map.Entry) it.next();
        Map.Entry entry3 = (Map.Entry) it.next();

        assertEquals(true, entry1.equals(entry2));
        assertEquals(true, entry2.equals(entry1));
        assertEquals(false, entry1.equals(entry3));
    }

    /**
     * Compare the current serialized form of the Map
     * against the canonical version in CVS.
     */
    public void testEmptyMapCompatibility() throws IOException, ClassNotFoundException {
        // test to make sure the canonical form has been preserved
        Map map = (Map) makeObject();
        if (map instanceof Serializable && !skipSerializedCanonicalTests()) {
            Map map2 = (Map) readExternalFormFromDisk(getCanonicalEmptyCollectionName(map));
            assertEquals("Map is empty", 0, map2.size());
        }
    }

    public void testClone() {
        IdentityMap map = new IdentityMap(10);
        map.put("1", "1");
        Map cloned = (Map) map.clone();
        assertEquals(map.size(), cloned.size());
        assertSame(map.get("1"), cloned.get("1"));
    }

//    public void testCreate() throws Exception {
//        Map map = new IdentityMap();
//        writeExternalFormToDisk((java.io.Serializable) map, "D:/dev/collections/data/test/IdentityMap.emptyCollection.version3.obj");
//        map = new IdentityMap();
//        map.put(I1A, I2A);
//        map.put(I1B, I2A);
//        map.put(I2A, I2B);
//        writeExternalFormToDisk((java.io.Serializable) map, "D:/dev/collections/data/test/IdentityMap.fullCollection.version3.obj");
//    }
}
