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

import org.apache.commons.collections.AbstractTestObject;
import org.apache.commons.collections.BulkTest;
import org.apache.commons.collections.collection.AbstractTestCollection;
import org.apache.commons.collections.set.AbstractTestSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract test class for {@link java.util.Map} methods and contracts.
 * <p>
 * The forces at work here are similar to those in {@link AbstractTestCollection}.
 * If your class implements the full Map interface, including optional
 * operations, simply extend this class, and implement the
 * {@link #makeEmptyMap()} method.
 * <p>
 * On the other hand, if your map implementation is weird, you may have to
 * override one or more of the other protected methods.  They're described
 * below.
 * <p>
 * <b>Entry Population Methods</b>
 * <p>
 * Override these methods if your map requires special entries:
 *
 * <ul>
 * <li>{@link #getSampleKeys()}
 * <li>{@link #getSampleValues()}
 * <li>{@link #getNewSampleValues()}
 * <li>{@link #getOtherKeys()}
 * <li>{@link #getOtherValues()}
 * </ul>
 *
 * <b>Supported Operation Methods</b>
 * <p>
 * Override these methods if your map doesn't support certain operations:
 *
 * <ul>
 * <li> {@link #isPutAddSupported()}
 * <li> {@link #isPutChangeSupported()}
 * <li> {@link #isSetValueSupported()}
 * <li> {@link #isRemoveSupported()}
 * <li> {@link #isGetStructuralModify()}
 * <li> {@link #isAllowDuplicateValues()}
 * <li> {@link #isAllowNullKey()}
 * <li> {@link #isAllowNullValue()}
 * </ul>
 *
 * <b>Fixture Methods</b>
 * <p>
 * For tests on modification operations (puts and removes), fixtures are used
 * to verify that that operation results in correct state for the map and its
 * collection views.  Basically, the modification is performed against your
 * map implementation, and an identical modification is performed against
 * a <I>confirmed</I> map implementation.  A confirmed map implementation is
 * something like <Code>java.util.HashMap</Code>, which is known to conform
 * exactly to the {@link Map} contract.  After the modification takes place
 * on both your map implementation and the confirmed map implementation, the
 * two maps are compared to see if their state is identical.  The comparison
 * also compares the collection views to make sure they're still the same.<P>
 *
 * The upshot of all that is that <I>any</I> test that modifies the map in
 * <I>any</I> way will verify that <I>all</I> of the map's state is still
 * correct, including the state of its collection views.  So for instance
 * if a key is removed by the map's key set's iterator, then the entry set
 * is checked to make sure the key/value pair no longer appears.<P>
 *
 * The {@link #map} field holds an instance of your collection implementation.
 * The {@link #entrySet}, {@link #keySet} and {@link #values} fields hold
 * that map's collection views.  And the {@link #confirmed} field holds
 * an instance of the confirmed collection implementation.  The
 * {@link #resetEmpty()} and {@link #resetFull()} methods set these fields to
 * empty or full maps, so that tests can proceed from a known state.<P>
 *
 * After a modification operation to both {@link #map} and {@link #confirmed},
 * the {@link #verify()} method is invoked to compare the results.  The
 * {@link #verify} method calls separate methods to verify the map and its three
 * collection views ({@link #verifyMap}, {@link #verifyEntrySet},
 * {@link #verifyKeySet}, and {@link #verifyValues}).  You may want to override
 * one of the verification methodsto perform additional verifications.  For
 * instance, TestDoubleOrderedMap would want override its
 * {@link #verifyValues()} method to verify that the values are unique and in
 * ascending order.<P>
 *
 * <b>Other Notes</b>
 * <p>
 * If your {@link Map} fails one of these tests by design, you may still use
 * this base set of cases.  Simply override the test case (method) your map
 * fails and/or the methods that define the assumptions used by the test
 * cases.  For example, if your map does not allow duplicate values, override
 * {@link #isAllowDuplicateValues()} and have it return <code>false</code>
 *
 * @author Michael Smith
 * @author Rodney Waldhoff
 * @author Paul Jack
 * @author Stephen Colebourne
 * @version $Revision: 646780 $ $Date: 2008-04-10 13:48:07 +0100 (Thu, 10 Apr 2008) $
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractTestMap extends AbstractTestObject {

    /**
     * JDK1.2 has bugs in null handling of Maps, especially HashMap.Entry.toString
     * This avoids nulls for JDK1.2
     */
    private static final boolean JDK12;
    static {
        String str = System.getProperty("java.version");
        JDK12 = str.startsWith("1.2");
    }

    // These instance variables are initialized with the reset method.
    // Tests for map methods that alter the map (put, putAll, remove)
    // first call reset() to create the map and its views; then perform
    // the modification on the map; perform the same modification on the
    // confirmed; and then call verify() to ensure that the map is equal
    // to the confirmed, that the already-constructed collection views
    // are still equal to the confirmed's collection views.


    /** Map created by reset(). */
    protected Map map;

    /** Entry set of map created by reset(). */
    protected Set entrySet;

    /** Key set of map created by reset(). */
    protected Set keySet;

    /** Values collection of map created by reset(). */
    protected Collection values;

    /** HashMap created by reset(). */
    protected Map confirmed;

    /**
     * JUnit constructor.
     *
     * @param testName  the test name
     */
    public AbstractTestMap(String testName) {
        super(testName);
    }

    /**
     * Returns true if the maps produced by
     * {@link #makeEmptyMap()} and {@link #makeFullMap()}
     * support the <code>put</code> and <code>putAll</code> operations
     * adding new mappings.
     * <p>
     * Default implementation returns true.
     * Override if your collection class does not support put adding.
     */
    public boolean isPutAddSupported() {
        return true;
    }

    /**
     * Returns true if the maps produced by
     * {@link #makeEmptyMap()} and {@link #makeFullMap()}
     * support the <code>put</code> and <code>putAll</code> operations
     * changing existing mappings.
     * <p>
     * Default implementation returns true.
     * Override if your collection class does not support put changing.
     */
    public boolean isPutChangeSupported() {
        return true;
    }

    /**
     * Returns true if the maps produced by
     * {@link #makeEmptyMap()} and {@link #makeFullMap()}
     * support the <code>setValue</code> operation on entrySet entries.
     * <p>
     * Default implementation returns isPutChangeSupported().
     * Override if your collection class does not support setValue but does
     * support put changing.
     */
    public boolean isSetValueSupported() {
        return isPutChangeSupported();
    }

    /**
     * Returns true if the maps produced by
     * {@link #makeEmptyMap()} and {@link #makeFullMap()}
     * support the <code>remove</code> and <code>clear</code> operations.
     * <p>
     * Default implementation returns true.
     * Override if your collection class does not support removal operations.
     */
    public boolean isRemoveSupported() {
        return true;
    }

    /**
     * Returns true if the maps produced by
     * {@link #makeEmptyMap()} and {@link #makeFullMap()}
     * can cause structural modification on a get(). The example is LRUMap.
     * <p>
     * Default implementation returns false.
     * Override if your map class structurally modifies on get.
     */
    public boolean isGetStructuralModify() {
        return false;
    }

    /**
     * Returns whether the sub map views of SortedMap are serializable.
     * If the class being tested is based around a TreeMap then you should
     * override and return false as TreeMap has a bug in deserialization.
     *
     * @return false
     */
    public boolean isSubMapViewsSerializable() {
        return true;
    }

    /**
     * Returns true if the maps produced by
     * {@link #makeEmptyMap()} and {@link #makeFullMap()}
     * supports null keys.
     * <p>
     * Default implementation returns true.
     * Override if your collection class does not support null keys.
     */
    public boolean isAllowNullKey() {
        return true;
    }

    /**
     * Returns true if the maps produced by
     * {@link #makeEmptyMap()} and {@link #makeFullMap()}
     * supports null values.
     * <p>
     * Default implementation returns true.
     * Override if your collection class does not support null values.
     */
    public boolean isAllowNullValue() {
        return true;
    }

    /**
     * Returns true if the maps produced by
     * {@link #makeEmptyMap()} and {@link #makeFullMap()}
     * supports duplicate values.
     * <p>
     * Default implementation returns true.
     * Override if your collection class does not support duplicate values.
     */
    public boolean isAllowDuplicateValues() {
        return true;
    }

    /**
     *  Returns the set of keys in the mappings used to test the map.  This
     *  method must return an array with the same length as {@link
     *  #getSampleValues()} and all array elements must be different. The
     *  default implementation constructs a set of String keys, and includes a
     *  single null key if {@link #isAllowNullKey()} returns <code>true</code>.
     */
    public Object[] getSampleKeys() {
        Object[] result = new Object[] {
            "blah", "foo", "bar", "baz", "tmp", "gosh", "golly", "gee",
            "hello", "goodbye", "we'll", "see", "you", "all", "again",
            "key",
            "key2",
            (isAllowNullKey() && !JDK12) ? null : "nonnullkey"
        };
        return result;
    }


    public Object[] getOtherKeys() {
        return getOtherNonNullStringElements();
    }

    public Object[] getOtherValues() {
        return getOtherNonNullStringElements();
    }

    /**
     * Returns a list of string elements suitable for return by
     * {@link #getOtherKeys()} or {@link #getOtherValues}.
     *
     * <p>Override getOtherElements to returnthe results of this method if your
     * collection does not support heterogenous elements or the null element.
     * </p>
     */
    public Object[] getOtherNonNullStringElements() {
        return new Object[] {
            "For","then","despite",/* of */"space","I","would","be","brought",
            "From","limits","far","remote","where","thou","dost","stay"
        };
    }

    /**
     * Returns the set of values in the mappings used to test the map.  This
     * method must return an array with the same length as
     * {@link #getSampleKeys()}.  The default implementation constructs a set of
     * String values and includes a single null value if
     * {@link #isAllowNullValue()} returns <code>true</code>, and includes
     * two values that are the same if {@link #isAllowDuplicateValues()} returns
     * <code>true</code>.
     */
    public Object[] getSampleValues() {
        Object[] result = new Object[] {
            "blahv", "foov", "barv", "bazv", "tmpv", "goshv", "gollyv", "geev",
            "hellov", "goodbyev", "we'llv", "seev", "youv", "allv", "againv",
            (isAllowNullValue() && !JDK12) ? null : "nonnullvalue",
            "value",
            (isAllowDuplicateValues()) ? "value" : "value2",
        };
        return result;
    }

    /**
     * Returns a the set of values that can be used to replace the values
     * returned from {@link #getSampleValues()}.  This method must return an
     * array with the same length as {@link #getSampleValues()}.  The values
     * returned from this method should not be the same as those returned from
     * {@link #getSampleValues()}.  The default implementation constructs a
     * set of String values and includes a single null value if
     * {@link #isAllowNullValue()} returns <code>true</code>, and includes two values
     * that are the same if {@link #isAllowDuplicateValues()} returns
     * <code>true</code>.
     */
    public Object[] getNewSampleValues() {
        Object[] result = new Object[] {
            (isAllowNullValue() && !JDK12 && isAllowDuplicateValues()) ? null : "newnonnullvalue",
            "newvalue",
            (isAllowDuplicateValues()) ? "newvalue" : "newvalue2",
            "newblahv", "newfoov", "newbarv", "newbazv", "newtmpv", "newgoshv",
            "newgollyv", "newgeev", "newhellov", "newgoodbyev", "newwe'llv",
            "newseev", "newyouv", "newallv", "newagainv",
        };
        return result;
    }

    /**
     *  Helper method to add all the mappings described by
     * {@link #getSampleKeys()} and {@link #getSampleValues()}.
     */
    public void addSampleMappings(Map m) {

        Object[] keys = getSampleKeys();
        Object[] values = getSampleValues();

        for(int i = 0; i < keys.length; i++) {
            try {
                m.put(keys[i], values[i]);
            } catch (NullPointerException exception) {
                assertTrue("NullPointerException only allowed to be thrown " +
                           "if either the key or value is null.",
                           keys[i] == null || values[i] == null);

                assertTrue("NullPointerException on null key, but " +
                           "isAllowNullKey is not overridden to return false.",
                           keys[i] == null || !isAllowNullKey());

                assertTrue("NullPointerException on null value, but " +
                           "isAllowNullValue is not overridden to return false.",
                           values[i] == null || !isAllowNullValue());

                assertTrue("Unknown reason for NullPointer.", false);
            }
        }
        assertEquals("size must reflect number of mappings added.",
                     keys.length, m.size());
    }

    //-----------------------------------------------------------------------
    /**
     * Return a new, empty {@link Map} to be used for testing.
     *
     * @return the map to be tested
     */
    public abstract Map makeEmptyMap();

    /**
     * Return a new, populated map.  The mappings in the map should match the
     * keys and values returned from {@link #getSampleKeys()} and
     * {@link #getSampleValues()}.  The default implementation uses makeEmptyMap()
     * and calls {@link #addSampleMappings} to add all the mappings to the
     * map.
     *
     * @return the map to be tested
     */
    public Map makeFullMap() {
        Map m = makeEmptyMap();
        addSampleMappings(m);
        return m;
    }

    /**
     * Implements the superclass method to return the map to be tested.
     *
     * @return the map to be tested
     */
    @Override
    public Object makeObject() {
        return makeEmptyMap();
    }

    /**
     * Override to return a map other than HashMap as the confirmed map.
     *
     * @return a map that is known to be valid
     */
    public Map makeConfirmedMap() {
        return new HashMap();
    }

    /**
     * Creates a new Map Entry that is independent of the first and the map.
     */
    public Map.Entry cloneMapEntry(Map.Entry entry) {
        HashMap map = new HashMap();
        map.put(entry.getKey(), entry.getValue());
        return (Map.Entry) map.entrySet().iterator().next();
    }

    /**
     * Gets the compatability version, needed for package access.
     */
    @Override
    public String getCompatibilityVersion() {
        return super.getCompatibilityVersion();
    }
    //-----------------------------------------------------------------------
    /**
     * Test to ensure the test setup is working properly.  This method checks
     * to ensure that the getSampleKeys and getSampleValues methods are
     * returning results that look appropriate.  That is, they both return a
     * non-null array of equal length.  The keys array must not have any
     * duplicate values, and may only contain a (single) null key if
     * isNullKeySupported() returns true.  The values array must only have a null
     * value if useNullValue() is true and may only have duplicate values if
     * isAllowDuplicateValues() returns true.
     */
    public void testSampleMappings() {
      Object[] keys = getSampleKeys();
      Object[] values = getSampleValues();
      Object[] newValues = getNewSampleValues();

      assertTrue("failure in test: Must have keys returned from " +
                 "getSampleKeys.", keys != null);

      assertTrue("failure in test: Must have values returned from " +
                 "getSampleValues.", values != null);

      // verify keys and values have equivalent lengths (in case getSampleX are
      // overridden)
      assertEquals("failure in test: not the same number of sample " +
                   "keys and values.",  keys.length, values.length);

      assertEquals("failure in test: not the same number of values and new values.",
                   values.length, newValues.length);

      // verify there aren't duplicate keys, and check values
      for(int i = 0; i < keys.length - 1; i++) {
          for(int j = i + 1; j < keys.length; j++) {
              assertTrue("failure in test: duplicate null keys.",
                         (keys[i] != null || keys[j] != null));
              assertTrue("failure in test: duplicate non-null key.",
                         (keys[i] == null || keys[j] == null ||
                          (!keys[i].equals(keys[j]) &&
                           !keys[j].equals(keys[i]))));
          }
          assertTrue("failure in test: found null key, but isNullKeySupported " +
                     "is false.", keys[i] != null || isAllowNullKey());
          assertTrue("failure in test: found null value, but isNullValueSupported " +
                     "is false.", values[i] != null || isAllowNullValue());
          assertTrue("failure in test: found null new value, but isNullValueSupported " +
                     "is false.", newValues[i] != null || isAllowNullValue());
          assertTrue("failure in test: values should not be the same as new value",
                     values[i] != newValues[i] &&
                     (values[i] == null || !values[i].equals(newValues[i])));
      }
    }

    // tests begin here.  Each test adds a little bit of tested functionality.
    // Many methods assume previous methods passed.  That is, they do not
    // exhaustively recheck things that have already been checked in a previous
    // test methods.

    /**
     * Test to ensure that makeEmptyMap and makeFull returns a new non-null
     * map with each invocation.
     */
    public void testMakeMap() {
        Map em = makeEmptyMap();
        assertTrue("failure in test: makeEmptyMap must return a non-null map.",
                   em != null);

        Map em2 = makeEmptyMap();
        assertTrue("failure in test: makeEmptyMap must return a non-null map.",
                   em != null);

        assertTrue("failure in test: makeEmptyMap must return a new map " +
                   "with each invocation.", em != em2);

        Map fm = makeFullMap();
        assertTrue("failure in test: makeFullMap must return a non-null map.",
                   fm != null);

        Map fm2 = makeFullMap();
        assertTrue("failure in test: makeFullMap must return a non-null map.",
                   fm != null);

        assertTrue("failure in test: makeFullMap must return a new map " +
                   "with each invocation.", fm != fm2);
    }

    /**
     * Tests Map.isEmpty()
     */
    public void testMapIsEmpty() {
        resetEmpty();
        assertEquals("Map.isEmpty() should return true with an empty map",
                     true, map.isEmpty());
        verify();

        resetFull();
        assertEquals("Map.isEmpty() should return false with a non-empty map",
                     false, map.isEmpty());
        verify();
    }

    /**
     * Tests Map.size()
     */
    public void testMapSize() {
        resetEmpty();
        assertEquals("Map.size() should be 0 with an empty map",
                     0, map.size());
        verify();

        resetFull();
        assertEquals("Map.size() should equal the number of entries " +
                     "in the map", getSampleKeys().length, map.size());
        verify();
    }

    /**
     * Tests {@link Map#clear()}.  If the map {@link #isRemoveSupported()}
     * can add and remove elements}, then {@link Map#size()} and
     * {@link Map#isEmpty()} are used to ensure that map has no elements after
     * a call to clear.  If the map does not support adding and removing
     * elements, this method checks to ensure clear throws an
     * UnsupportedOperationException.
     */
    public void testMapClear() {
        if (!isRemoveSupported()) {
            try {
                resetFull();
                map.clear();
                fail("Expected UnsupportedOperationException on clear");
            } catch (UnsupportedOperationException ex) {}
            return;
        }

        resetEmpty();
        map.clear();
        confirmed.clear();
        verify();

        resetFull();
        map.clear();
        confirmed.clear();
        verify();
    }


    /**
     * Tests Map.containsKey(Object) by verifying it returns false for all
     * sample keys on a map created using an empty map and returns true for
     * all sample keys returned on a full map.
     */
    public void testMapContainsKey() {
        Object[] keys = getSampleKeys();

        resetEmpty();
        for(int i = 0; i < keys.length; i++) {
            assertTrue("Map must not contain key when map is empty",
                       !map.containsKey(keys[i]));
        }
        verify();

        resetFull();
        for(int i = 0; i < keys.length; i++) {
            assertTrue("Map must contain key for a mapping in the map. " +
                       "Missing: " + keys[i], map.containsKey(keys[i]));
        }
        verify();
    }

    /**
     * Tests Map.containsValue(Object) by verifying it returns false for all
     * sample values on an empty map and returns true for all sample values on
     * a full map.
     */
    public void testMapContainsValue() {
        Object[] values = getSampleValues();

        resetEmpty();
        for(int i = 0; i < values.length; i++) {
            assertTrue("Empty map must not contain value",
                       !map.containsValue(values[i]));
        }
        verify();

        resetFull();
        for(int i = 0; i < values.length; i++) {
            assertTrue("Map must contain value for a mapping in the map.",
                       map.containsValue(values[i]));
        }
        verify();
    }


    /**
     * Tests Map.equals(Object)
     */
    public void testMapEquals() {
        resetEmpty();
        assertTrue("Empty maps unequal.", map.equals(confirmed));
        verify();

        resetFull();
        assertTrue("Full maps unequal.", map.equals(confirmed));
        verify();

        resetFull();
        // modify the HashMap created from the full map and make sure this
        // change results in map.equals() to return false.
        Iterator iter = confirmed.keySet().iterator();
        iter.next();
        iter.remove();
        assertTrue("Different maps equal.", !map.equals(confirmed));

        resetFull();
        assertTrue("equals(null) returned true.", !map.equals(null));
        assertTrue("equals(new Object()) returned true.",
                   !map.equals(new Object()));
        verify();
    }


    /**
     * Tests Map.get(Object)
     */
    public void testMapGet() {
        resetEmpty();

        Object[] keys = getSampleKeys();
        Object[] values = getSampleValues();

        for (int i = 0; i < keys.length; i++) {
            assertTrue("Empty map.get() should return null.",
                       map.get(keys[i]) == null);
        }
        verify();

        resetFull();
        for (int i = 0; i < keys.length; i++) {
            assertEquals("Full map.get() should return value from mapping.",
                         values[i], map.get(keys[i]));
        }
    }

    /**
     * Tests Map.hashCode()
     */
    public void testMapHashCode() {
        resetEmpty();
        assertTrue("Empty maps have different hashCodes.",
                   map.hashCode() == confirmed.hashCode());

        resetFull();
        assertTrue("Equal maps have different hashCodes.",
                   map.hashCode() == confirmed.hashCode());
    }

    /**
     * Tests Map.toString().  Since the format of the string returned by the
     * toString() method is not defined in the Map interface, there is no
     * common way to test the results of the toString() method.  Thereforce,
     * it is encouraged that Map implementations override this test with one
     * that checks the format matches any format defined in its API.  This
     * default implementation just verifies that the toString() method does
     * not return null.
     */
    public void testMapToString() {
        resetEmpty();
        assertTrue("Empty map toString() should not return null",
                   map.toString() != null);
        verify();

        resetFull();
        assertTrue("Empty map toString() should not return null",
                   map.toString() != null);
        verify();
    }


    /**
     * Compare the current serialized form of the Map
     * against the canonical version in CVS.
     */
    public void testEmptyMapCompatibility() throws Exception {
        /**
         * Create canonical objects with this code
        Map map = makeEmptyMap();
        if (!(map instanceof Serializable)) return;

        writeExternalFormToDisk((Serializable) map, getCanonicalEmptyCollectionName(map));
        */

        // test to make sure the canonical form has been preserved
        Map map = makeEmptyMap();
        if (map instanceof Serializable && !skipSerializedCanonicalTests() && isTestSerialization()) {
            Map map2 = (Map) readExternalFormFromDisk(getCanonicalEmptyCollectionName(map));
            assertEquals("Map is empty", 0, map2.size());
        }
    }

    /**
     * Compare the current serialized form of the Map
     * against the canonical version in CVS.
     */
    public void testFullMapCompatibility() throws Exception {
        /**
         * Create canonical objects with this code
        Map map = makeFullMap();
        if (!(map instanceof Serializable)) return;

        writeExternalFormToDisk((Serializable) map, getCanonicalFullCollectionName(map));
        */

        // test to make sure the canonical form has been preserved
        Map map = makeFullMap();
        if (map instanceof Serializable && !skipSerializedCanonicalTests() && isTestSerialization()) {
            Map map2 = (Map) readExternalFormFromDisk(getCanonicalFullCollectionName(map));
            assertEquals("Map is the right size", getSampleKeys().length, map2.size());
        }
    }

    /**
     * Tests Map.put(Object, Object)
     */
    public void testMapPut() {
        resetEmpty();
        Object[] keys = getSampleKeys();
        Object[] values = getSampleValues();
        Object[] newValues = getNewSampleValues();

        if (isPutAddSupported()) {
            for (int i = 0; i < keys.length; i++) {
                Object o = map.put(keys[i], values[i]);
                confirmed.put(keys[i], values[i]);
                verify();
                assertTrue("First map.put should return null", o == null);
                assertTrue("Map should contain key after put",
                           map.containsKey(keys[i]));
                assertTrue("Map should contain value after put",
                           map.containsValue(values[i]));
            }
            if (isPutChangeSupported()) {
                for (int i = 0; i < keys.length; i++) {
                    Object o = map.put(keys[i], newValues[i]);
                    confirmed.put(keys[i], newValues[i]);
                    verify();
                    assertEquals("Map.put should return previous value when changed",
                                 values[i], o);
                    assertTrue("Map should still contain key after put when changed",
                               map.containsKey(keys[i]));
                    assertTrue("Map should contain new value after put when changed",
                               map.containsValue(newValues[i]));

                    // if duplicates are allowed, we're not guaranteed that the value
                    // no longer exists, so don't try checking that.
                    if (!isAllowDuplicateValues()) {
                        assertTrue("Map should not contain old value after put when changed",
                                   !map.containsValue(values[i]));
                    }
                }
            } else {
                try {
                    // two possible exception here, either valid
                    map.put(keys[0], newValues[0]);
                    fail("Expected IllegalArgumentException or UnsupportedOperationException on put (change)");
                } catch (IllegalArgumentException ex) {
                } catch (UnsupportedOperationException ex) {}
            }

        } else if (isPutChangeSupported()) {
            resetEmpty();
            try {
                map.put(keys[0], values[0]);
                fail("Expected UnsupportedOperationException or IllegalArgumentException on put (add) when fixed size");
            } catch (IllegalArgumentException ex) {
            } catch (UnsupportedOperationException ex) {
            }

            resetFull();
            int i = 0;
            for (Iterator it = map.keySet().iterator(); it.hasNext() && i < newValues.length; i++) {
                Object key = it.next();
                Object o = map.put(key, newValues[i]);
                Object value = confirmed.put(key, newValues[i]);
                verify();
                assertEquals("Map.put should return previous value when changed",
                    value, o);
                assertTrue("Map should still contain key after put when changed",
                    map.containsKey(key));
                assertTrue("Map should contain new value after put when changed",
                    map.containsValue(newValues[i]));

                // if duplicates are allowed, we're not guaranteed that the value
                // no longer exists, so don't try checking that.
                if (!isAllowDuplicateValues()) {
                    assertTrue("Map should not contain old value after put when changed",
                        !map.containsValue(values[i]));
                }
            }
        } else {
            try {
                map.put(keys[0], values[0]);
                fail("Expected UnsupportedOperationException on put (add)");
            } catch (UnsupportedOperationException ex) {}
        }
    }

    /**
     * Tests Map.put(null, value)
     */
    public void testMapPutNullKey() {
        resetFull();
        Object[] values = getSampleValues();

        if (isPutAddSupported()) {
            if (isAllowNullKey()) {
                map.put(null, values[0]);
            } else {
                try {
                    map.put(null, values[0]);
                    fail("put(null, value) should throw NPE/IAE");
                } catch (NullPointerException ex) {
                } catch (IllegalArgumentException ex) {}
            }
        }
    }

    /**
     * Tests Map.put(null, value)
     */
    public void testMapPutNullValue() {
        resetFull();
        Object[] keys = getSampleKeys();

        if (isPutAddSupported()) {
            if (isAllowNullValue()) {
                map.put(keys[0], null);
            } else {
                try {
                    map.put(keys[0], null);
                    fail("put(key, null) should throw NPE/IAE");
                } catch (NullPointerException ex) {
                } catch (IllegalArgumentException ex) {}
            }
        }
    }

    /**
     * Tests Map.putAll(map)
     */
    public void testMapPutAll() {
        if (!isPutAddSupported()) {
            if (!isPutChangeSupported()) {
                Map temp = makeFullMap();
                resetEmpty();
                try {
                    map.putAll(temp);
                    fail("Expected UnsupportedOperationException on putAll");
                } catch (UnsupportedOperationException ex) {}
            }
            return;
        }

        // check putAll OK adding empty map to empty map
        resetEmpty();
        assertEquals(0, map.size());
        map.putAll(new HashMap());
        assertEquals(0, map.size());

        // check putAll OK adding empty map to non-empty map
        resetFull();
        int size = map.size();
        map.putAll(new HashMap());
        assertEquals(size, map.size());

        // check putAll OK adding non-empty map to empty map
        resetEmpty();
        Map m2 = makeFullMap();
        map.putAll(m2);
        confirmed.putAll(m2);
        verify();

        // check putAll OK adding non-empty JDK map to empty map
        resetEmpty();
        m2 = makeConfirmedMap();
        Object[] keys = getSampleKeys();
        Object[] values = getSampleValues();
        for(int i = 0; i < keys.length; i++) {
            m2.put(keys[i], values[i]);
        }
        map.putAll(m2);
        confirmed.putAll(m2);
        verify();

        // check putAll OK adding non-empty JDK map to non-empty map
        resetEmpty();
        m2 = makeConfirmedMap();
        map.put(keys[0], values[0]);
        confirmed.put(keys[0], values[0]);
        verify();
        for(int i = 1; i < keys.length; i++) {
            m2.put(keys[i], values[i]);
        }
        map.putAll(m2);
        confirmed.putAll(m2);
        verify();
    }

    /**
     * Tests Map.remove(Object)
     */
    public void testMapRemove() {
        if (!isRemoveSupported()) {
            try {
                resetFull();
                map.remove(map.keySet().iterator().next());
                fail("Expected UnsupportedOperationException on remove");
            } catch (UnsupportedOperationException ex) {}
            return;
        }

        resetEmpty();

        Object[] keys = getSampleKeys();
        Object[] values = getSampleValues();
        for(int i = 0; i < keys.length; i++) {
            Object o = map.remove(keys[i]);
            assertTrue("First map.remove should return null", o == null);
        }
        verify();

        resetFull();

        for(int i = 0; i < keys.length; i++) {
            Object o = map.remove(keys[i]);
            confirmed.remove(keys[i]);
            verify();

            assertEquals("map.remove with valid key should return value",
                         values[i], o);
        }

        Object[] other = getOtherKeys();

        resetFull();
        int size = map.size();
        for (int i = 0; i < other.length; i++) {
            Object o = map.remove(other[i]);
            assertEquals("map.remove for nonexistent key should return null",
                         o, null);
            assertEquals("map.remove for nonexistent key should not " +
                         "shrink map", size, map.size());
        }
        verify();
    }

    //-----------------------------------------------------------------------
    /**
     * Tests that the {@link Map#values} collection is backed by
     * the underlying map for clear().
     */
    public void testValuesClearChangesMap() {
        if (!isRemoveSupported()) return;

        // clear values, reflected in map
        resetFull();
        Collection values = map.values();
        assertTrue(map.size() > 0);
        assertTrue(values.size() > 0);
        values.clear();
        assertTrue(map.size() == 0);
        assertTrue(values.size() == 0);

        // clear map, reflected in values
        resetFull();
        values = map.values();
        assertTrue(map.size() > 0);
        assertTrue(values.size() > 0);
        map.clear();
        assertTrue(map.size() == 0);
        assertTrue(values.size() == 0);
    }

    /**
     * Tests that the {@link Map#keySet} collection is backed by
     * the underlying map for clear().
     */
    public void testKeySetClearChangesMap() {
        if (!isRemoveSupported()) return;

        // clear values, reflected in map
        resetFull();
        Set keySet = map.keySet();
        assertTrue(map.size() > 0);
        assertTrue(keySet.size() > 0);
        keySet.clear();
        assertTrue(map.size() == 0);
        assertTrue(keySet.size() == 0);

        // clear map, reflected in values
        resetFull();
        keySet = map.keySet();
        assertTrue(map.size() > 0);
        assertTrue(keySet.size() > 0);
        map.clear();
        assertTrue(map.size() == 0);
        assertTrue(keySet.size() == 0);
    }

    /**
     * Tests that the {@link Map#entrySet()} collection is backed by
     * the underlying map for clear().
     */
    public void testEntrySetClearChangesMap() {
        if (!isRemoveSupported()) return;

        // clear values, reflected in map
        resetFull();
        Set entrySet = map.entrySet();
        assertTrue(map.size() > 0);
        assertTrue(entrySet.size() > 0);
        entrySet.clear();
        assertTrue(map.size() == 0);
        assertTrue(entrySet.size() == 0);

        // clear map, reflected in values
        resetFull();
        entrySet = map.entrySet();
        assertTrue(map.size() > 0);
        assertTrue(entrySet.size() > 0);
        map.clear();
        assertTrue(map.size() == 0);
        assertTrue(entrySet.size() == 0);
    }

    //-----------------------------------------------------------------------
    public void testEntrySetContains1() {
        resetFull();
        Set entrySet = map.entrySet();
        Map.Entry entry = (Map.Entry) entrySet.iterator().next();
        assertEquals(true, entrySet.contains(entry));
    }
    public void testEntrySetContains2() {
        resetFull();
        Set entrySet = map.entrySet();
        Map.Entry entry = (Map.Entry) entrySet.iterator().next();
        Map.Entry test = cloneMapEntry(entry);
        assertEquals(true, entrySet.contains(test));
    }
    public void testEntrySetContains3() {
        resetFull();
        Set entrySet = map.entrySet();
        Map.Entry entry = (Map.Entry) entrySet.iterator().next();
        HashMap temp = new HashMap();
        temp.put(entry.getKey(), "A VERY DIFFERENT VALUE");
        Map.Entry test = (Map.Entry) temp.entrySet().iterator().next();
        assertEquals(false, entrySet.contains(test));
    }

    public void testEntrySetRemove1() {
        if (!isRemoveSupported()) return;
        resetFull();
        int size = map.size();
        Set entrySet = map.entrySet();
        Map.Entry entry = (Map.Entry) entrySet.iterator().next();
        Object key = entry.getKey();

        assertEquals(true, entrySet.remove(entry));
        assertEquals(false, map.containsKey(key));
        assertEquals(size - 1, map.size());
    }
    public void testEntrySetRemove2() {
        if (!isRemoveSupported()) return;
        resetFull();
        int size = map.size();
        Set entrySet = map.entrySet();
        Map.Entry entry = (Map.Entry) entrySet.iterator().next();
        Object key = entry.getKey();
        Map.Entry test = cloneMapEntry(entry);

        assertEquals(true, entrySet.remove(test));
        assertEquals(false, map.containsKey(key));
        assertEquals(size - 1, map.size());
    }
    public void testEntrySetRemove3() {
        if (!isRemoveSupported()) return;
        resetFull();
        int size = map.size();
        Set entrySet = map.entrySet();
        Map.Entry entry = (Map.Entry) entrySet.iterator().next();
        Object key = entry.getKey();
        HashMap temp = new HashMap();
        temp.put(entry.getKey(), "A VERY DIFFERENT VALUE");
        Map.Entry test = (Map.Entry) temp.entrySet().iterator().next();

        assertEquals(false, entrySet.remove(test));
        assertEquals(true, map.containsKey(key));
        assertEquals(size, map.size());
    }

    //-----------------------------------------------------------------------
    /**
     * Tests that the {@link Map#values} collection is backed by
     * the underlying map by removing from the values collection
     * and testing if the value was removed from the map.
     * <p>
     * We should really test the "vice versa" case--that values removed
     * from the map are removed from the values collection--also,
     * but that's a more difficult test to construct (lacking a
     * "removeValue" method.)
     * </p>
     * <p>
     * See bug <a href="http://issues.apache.org/bugzilla/show_bug.cgi?id=9573">
     * 9573</a>.
     * </p>
     */
    public void testValuesRemoveChangesMap() {
        resetFull();
        Object[] sampleValues = getSampleValues();
        Collection values = map.values();
        for (int i = 0; i < sampleValues.length; i++) {
            if (map.containsValue(sampleValues[i])) {
                int j = 0;  // loop counter prevents infinite loops when remove is broken
                while (values.contains(sampleValues[i]) && j < 10000) {
                    try {
                        values.remove(sampleValues[i]);
                    } catch (UnsupportedOperationException e) {
                        // if values.remove is unsupported, just skip this test
                        return;
                    }
                    j++;
                }
                assertTrue("values().remove(obj) is broken", j < 10000);
                assertTrue(
                    "Value should have been removed from the underlying map.",
                    !map.containsValue(sampleValues[i]));
            }
        }
    }

    /**
     * Tests that the {@link Map#keySet} set is backed by
     * the underlying map by removing from the keySet set
     * and testing if the key was removed from the map.
     */
    public void testKeySetRemoveChangesMap() {
        resetFull();
        Object[] sampleKeys = getSampleKeys();
        Set keys = map.keySet();
        for (int i = 0; i < sampleKeys.length; i++) {
            try {
                keys.remove(sampleKeys[i]);
            } catch (UnsupportedOperationException e) {
                // if key.remove is unsupported, just skip this test
                return;
            }
            assertTrue(
                "Key should have been removed from the underlying map.",
                !map.containsKey(sampleKeys[i]));
        }
    }

    // TODO: Need:
    //    testValuesRemovedFromEntrySetAreRemovedFromMap
    //    same for EntrySet/KeySet/values's
    //      Iterator.remove, removeAll, retainAll


    /**
     * Utility methods to create an array of Map.Entry objects
     * out of the given key and value arrays.<P>
     *
     * @param keys    the array of keys
     * @param values  the array of values
     * @return an array of Map.Entry of those keys to those values
     */
    private Map.Entry[] makeEntryArray(Object[] keys, Object[] values) {
        Map.Entry[] result = new Map.Entry[keys.length];
        for (int i = 0; i < keys.length; i++) {
            Map map = makeConfirmedMap();
            map.put(keys[i], values[i]);
            result[i] = (Map.Entry) map.entrySet().iterator().next();
        }
        return result;
    }


    /**
     * Bulk test {@link Map#entrySet()}.  This method runs through all of
     * the tests in {@link AbstractTestSet}.
     * After modification operations, {@link #verify()} is invoked to ensure
     * that the map and the other collection views are still valid.
     *
     * @return a {@link AbstractTestSet} instance for testing the map's entry set
     */
    public BulkTest bulkTestMapEntrySet() {
        return new TestMapEntrySet();
    }

    public class TestMapEntrySet extends AbstractTestSet {
        public TestMapEntrySet() {
            super("MapEntrySet");
        }

        // Have to implement manually; entrySet doesn't support addAll
        @Override
        public Object[] getFullElements() {
            Object[] k = getSampleKeys();
            Object[] v = getSampleValues();
            return makeEntryArray(k, v);
        }

        // Have to implement manually; entrySet doesn't support addAll
        @Override
        public Object[] getOtherElements() {
            Object[] k = getOtherKeys();
            Object[] v = getOtherValues();
            return makeEntryArray(k, v);
        }

        @Override
        public Set makeEmptySet() {
            return makeEmptyMap().entrySet();
        }

        @Override
        public Set makeFullSet() {
            return makeFullMap().entrySet();
        }

        @Override
        public boolean isAddSupported() {
            // Collection views don't support add operations.
            return false;
        }
        @Override
        public boolean isRemoveSupported() {
            // Entry set should only support remove if map does
            return AbstractTestMap.this.isRemoveSupported();
        }
        public boolean isGetStructuralModify() {
            return AbstractTestMap.this.isGetStructuralModify();
        }
        @Override
        public boolean isTestSerialization() {
            return false;
        }

        @Override
        public void resetFull() {
            AbstractTestMap.this.resetFull();
            collection = map.entrySet();
            TestMapEntrySet.this.confirmed = AbstractTestMap.this.confirmed.entrySet();
        }

        @Override
        public void resetEmpty() {
            AbstractTestMap.this.resetEmpty();
            collection = map.entrySet();
            TestMapEntrySet.this.confirmed = AbstractTestMap.this.confirmed.entrySet();
        }

        public void testMapEntrySetIteratorEntry() {
            resetFull();
            Iterator it = collection.iterator();
            int count = 0;
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                assertEquals(true, AbstractTestMap.this.map.containsKey(entry.getKey()));
                assertEquals(true, AbstractTestMap.this.map.containsValue(entry.getValue()));
                if (isGetStructuralModify() == false) {
                    assertEquals(AbstractTestMap.this.map.get(entry.getKey()), entry.getValue());
                }
                count++;
            }
            assertEquals(collection.size(), count);
        }

        public void testMapEntrySetIteratorEntrySetValue() {
            Object key1 = getSampleKeys()[0];
            Object key2 = (getSampleKeys().length ==1 ? getSampleKeys()[0] : getSampleKeys()[1]);
            Object newValue1 = getNewSampleValues()[0];
            Object newValue2 = (getNewSampleValues().length ==1 ? getNewSampleValues()[0] : getNewSampleValues()[1]);

            resetFull();
            // explicitly get entries as sample values/keys are connected for some maps
            // such as BeanMap
            Iterator it = TestMapEntrySet.this.collection.iterator();
            Map.Entry entry1 = getEntry(it, key1);
            it = TestMapEntrySet.this.collection.iterator();
            Map.Entry entry2 = getEntry(it, key2);
            Iterator itConfirmed = TestMapEntrySet.this.confirmed.iterator();
            Map.Entry entryConfirmed1 = getEntry(itConfirmed, key1);
            itConfirmed = TestMapEntrySet.this.confirmed.iterator();
            Map.Entry entryConfirmed2 = getEntry(itConfirmed, key2);
            verify();

            if (isSetValueSupported() == false) {
                try {
                    entry1.setValue(newValue1);
                } catch (UnsupportedOperationException ex) {
                }
                return;
            }

            entry1.setValue(newValue1);
            entryConfirmed1.setValue(newValue1);
            assertEquals(newValue1, entry1.getValue());
            assertEquals(true, AbstractTestMap.this.map.containsKey(entry1.getKey()));
            assertEquals(true, AbstractTestMap.this.map.containsValue(newValue1));
            assertEquals(newValue1, AbstractTestMap.this.map.get(entry1.getKey()));
            verify();

            entry1.setValue(newValue1);
            entryConfirmed1.setValue(newValue1);
            assertEquals(newValue1, entry1.getValue());
            assertEquals(true, AbstractTestMap.this.map.containsKey(entry1.getKey()));
            assertEquals(true, AbstractTestMap.this.map.containsValue(newValue1));
            assertEquals(newValue1, AbstractTestMap.this.map.get(entry1.getKey()));
            verify();

            entry2.setValue(newValue2);
            entryConfirmed2.setValue(newValue2);
            assertEquals(newValue2, entry2.getValue());
            assertEquals(true, AbstractTestMap.this.map.containsKey(entry2.getKey()));
            assertEquals(true, AbstractTestMap.this.map.containsValue(newValue2));
            assertEquals(newValue2, AbstractTestMap.this.map.get(entry2.getKey()));
            verify();
        }

        public Map.Entry getEntry(Iterator itConfirmed, Object key) {
            Map.Entry entry = null;
            while (itConfirmed.hasNext()) {
                Map.Entry temp = (Map.Entry) itConfirmed.next();
                if (temp.getKey() == null) {
                    if (key == null) {
                        entry = temp;
                        break;
                    }
                } else if (temp.getKey().equals(key)) {
                    entry = temp;
                    break;
                }
            }
            assertNotNull("No matching entry in map for key '" + key + "'", entry);
            return entry;
        }

        public void testMapEntrySetRemoveNonMapEntry() {
            if (isRemoveSupported() == false) return;
            resetFull();
            assertEquals(false, getSet().remove(null));
            assertEquals(false, getSet().remove(new Object()));
        }

        @Override
        public void verify() {
            super.verify();
            AbstractTestMap.this.verify();
        }
    }


    /**
     * Bulk test {@link Map#keySet()}.  This method runs through all of
     * the tests in {@link AbstractTestSet}.
     * After modification operations, {@link #verify()} is invoked to ensure
     * that the map and the other collection views are still valid.
     *
     * @return a {@link AbstractTestSet} instance for testing the map's key set
     */
    public BulkTest bulkTestMapKeySet() {
        return new TestMapKeySet();
    }

    public class TestMapKeySet extends AbstractTestSet {
        public TestMapKeySet() {
            super("");
        }
        @Override
        public Object[] getFullElements() {
            return getSampleKeys();
        }

        @Override
        public Object[] getOtherElements() {
            return getOtherKeys();
        }

        @Override
        public Set makeEmptySet() {
            return makeEmptyMap().keySet();
        }

        @Override
        public Set makeFullSet() {
            return makeFullMap().keySet();
        }

        @Override
        public boolean isNullSupported() {
            return AbstractTestMap.this.isAllowNullKey();
        }
        @Override
        public boolean isAddSupported() {
            return false;
        }
        @Override
        public boolean isRemoveSupported() {
            return AbstractTestMap.this.isRemoveSupported();
        }
        @Override
        public boolean isTestSerialization() {
            return false;
        }

        @Override
        public void resetEmpty() {
            AbstractTestMap.this.resetEmpty();
            collection = map.keySet();
            TestMapKeySet.this.confirmed = AbstractTestMap.this.confirmed.keySet();
        }

        @Override
        public void resetFull() {
            AbstractTestMap.this.resetFull();
            collection = map.keySet();
            TestMapKeySet.this.confirmed = AbstractTestMap.this.confirmed.keySet();
        }

        @Override
        public void verify() {
            super.verify();
            AbstractTestMap.this.verify();
        }
    }


    /**
     * Bulk test {@link Map#values()}.  This method runs through all of
     * the tests in {@link AbstractTestCollection}.
     * After modification operations, {@link #verify()} is invoked to ensure
     * that the map and the other collection views are still valid.
     *
     * @return a {@link AbstractTestCollection} instance for testing the map's
     *    values collection
     */
    public BulkTest bulkTestMapValues() {
        return new TestMapValues();
    }

    public class TestMapValues extends AbstractTestCollection {
        public TestMapValues() {
            super("");
        }

        @Override
        public Object[] getFullElements() {
            return getSampleValues();
        }

        @Override
        public Object[] getOtherElements() {
            return getOtherValues();
        }

        @Override
        public Collection makeCollection() {
            return makeEmptyMap().values();
        }

        @Override
        public Collection makeFullCollection() {
            return makeFullMap().values();
        }

        @Override
        public boolean isNullSupported() {
            return AbstractTestMap.this.isAllowNullKey();
        }
        @Override
        public boolean isAddSupported() {
            return false;
        }
        @Override
        public boolean isRemoveSupported() {
            return AbstractTestMap.this.isRemoveSupported();
        }
        @Override
        public boolean isTestSerialization() {
            return false;
        }

        @Override
        public boolean areEqualElementsDistinguishable() {
            // equal values are associated with different keys, so they are
            // distinguishable.
            return true;
        }

        @Override
        public Collection makeConfirmedCollection() {
            // never gets called, reset methods are overridden
            return null;
        }

        @Override
        public Collection makeConfirmedFullCollection() {
            // never gets called, reset methods are overridden
            return null;
        }

        @Override
        public void resetFull() {
            AbstractTestMap.this.resetFull();
            collection = map.values();
            TestMapValues.this.confirmed = AbstractTestMap.this.confirmed.values();
        }

        @Override
        public void resetEmpty() {
            AbstractTestMap.this.resetEmpty();
            collection = map.values();
            TestMapValues.this.confirmed = AbstractTestMap.this.confirmed.values();
        }

        @Override
        public void verify() {
            super.verify();
            AbstractTestMap.this.verify();
        }

        // TODO: should test that a remove on the values collection view
        // removes the proper mapping and not just any mapping that may have
        // the value equal to the value returned from the values iterator.
    }


    /**
     * Resets the {@link #map}, {@link #entrySet}, {@link #keySet},
     * {@link #values} and {@link #confirmed} fields to empty.
     */
    public void resetEmpty() {
        this.map = makeEmptyMap();
        views();
        this.confirmed = makeConfirmedMap();
    }

    /**
     * Resets the {@link #map}, {@link #entrySet}, {@link #keySet},
     * {@link #values} and {@link #confirmed} fields to full.
     */
    public void resetFull() {
        this.map = makeFullMap();
        views();
        this.confirmed = makeConfirmedMap();
        Object[] k = getSampleKeys();
        Object[] v = getSampleValues();
        for (int i = 0; i < k.length; i++) {
            confirmed.put(k[i], v[i]);
        }
    }


    /**
     * Resets the collection view fields.
     */
    private void views() {
        this.keySet = map.keySet();
        this.values = map.values();
        this.entrySet = map.entrySet();
    }


    /**
     * Verifies that {@link #map} is still equal to {@link #confirmed}.
     * This method checks that the map is equal to the HashMap,
     * <I>and</I> that the map's collection views are still equal to
     * the HashMap's collection views.  An <Code>equals</Code> test
     * is done on the maps and their collection views; their size and
     * <Code>isEmpty</Code> results are compared; their hashCodes are
     * compared; and <Code>containsAll</Code> tests are run on the
     * collection views.
     */
    public void verify() {
        verifyMap();
        verifyEntrySet();
        verifyKeySet();
        verifyValues();
    }

    public void verifyMap() {
        int size = confirmed.size();
        boolean empty = confirmed.isEmpty();
        assertEquals("Map should be same size as HashMap",
                     size, map.size());
        assertEquals("Map should be empty if HashMap is",
                     empty, map.isEmpty());
        assertEquals("hashCodes should be the same",
                     confirmed.hashCode(), map.hashCode());
        // this fails for LRUMap because confirmed.equals() somehow modifies
        // map, causing concurrent modification exceptions.
        //assertEquals("Map should still equal HashMap", confirmed, map);
        // this works though and performs the same verification:
        assertTrue("Map should still equal HashMap", map.equals(confirmed));
        // TODO: this should really be reexamined to figure out why LRU map
        // behaves like it does (the equals shouldn't modify since all accesses
        // by the confirmed collection should be through an iterator, thus not
        // causing LRUMap to change).
    }

    public void verifyEntrySet() {
        int size = confirmed.size();
        boolean empty = confirmed.isEmpty();
        assertEquals("entrySet should be same size as HashMap's" +
                     "\nTest: " + entrySet + "\nReal: " + confirmed.entrySet(),
                     size, entrySet.size());
        assertEquals("entrySet should be empty if HashMap is" +
                     "\nTest: " + entrySet + "\nReal: " + confirmed.entrySet(),
                     empty, entrySet.isEmpty());
        assertTrue("entrySet should contain all HashMap's elements" +
                   "\nTest: " + entrySet + "\nReal: " + confirmed.entrySet(),
                   entrySet.containsAll(confirmed.entrySet()));
        assertEquals("entrySet hashCodes should be the same" +
                     "\nTest: " + entrySet + "\nReal: " + confirmed.entrySet(),
                     confirmed.entrySet().hashCode(), entrySet.hashCode());
        assertEquals("Map's entry set should still equal HashMap's",
                     confirmed.entrySet(), entrySet);
    }

    public void verifyKeySet() {
        int size = confirmed.size();
        boolean empty = confirmed.isEmpty();
        assertEquals("keySet should be same size as HashMap's" +
                     "\nTest: " + keySet + "\nReal: " + confirmed.keySet(),
                     size, keySet.size());
        assertEquals("keySet should be empty if HashMap is" +
                     "\nTest: " + keySet + "\nReal: " + confirmed.keySet(),
                     empty, keySet.isEmpty());
        assertTrue("keySet should contain all HashMap's elements" +
                   "\nTest: " + keySet + "\nReal: " + confirmed.keySet(),
                   keySet.containsAll(confirmed.keySet()));
        assertEquals("keySet hashCodes should be the same" +
                     "\nTest: " + keySet + "\nReal: " + confirmed.keySet(),
                     confirmed.keySet().hashCode(), keySet.hashCode());
        assertEquals("Map's key set should still equal HashMap's",
                     confirmed.keySet(), keySet);
    }

    public void verifyValues() {
        List known = new ArrayList(confirmed.values());
        List test = new ArrayList(values);

        int size = confirmed.size();
        boolean empty = confirmed.isEmpty();
        assertEquals("values should be same size as HashMap's" +
                     "\nTest: " + test + "\nReal: " + known,
                     size, values.size());
        assertEquals("values should be empty if HashMap is" +
                     "\nTest: " + test + "\nReal: " + known,
                     empty, values.isEmpty());
        assertTrue("values should contain all HashMap's elements" +
                   "\nTest: " + test + "\nReal: " + known,
                    test.containsAll(known));
        assertTrue("values should contain all HashMap's elements" +
                   "\nTest: " + test + "\nReal: " + known,
                   known.containsAll(test));
        // originally coded to use a HashBag, but now separate jar so...
        for (Iterator it = known.iterator(); it.hasNext();) {
            boolean removed = test.remove(it.next());
            assertTrue("Map's values should still equal HashMap's", removed);
        }
        assertTrue("Map's values should still equal HashMap's", test.isEmpty());
    }


    /**
     * Erases any leftover instance variables by setting them to null.
     */
    @Override
    public void tearDown() throws Exception {
        map = null;
        keySet = null;
        entrySet = null;
        values = null;
        confirmed = null;
    }

}
