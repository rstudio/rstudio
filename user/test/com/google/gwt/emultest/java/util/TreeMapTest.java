/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

import org.apache.commons.collections.TestMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * Tests <code>TreeMap</code>.
 * 
 * @param <K> The key type for the underlying TreeMap
 * @param <V> The value type for the underlying TreeMap
 * 
 * TODO(jat): this whole structure needs work. Ideally we would port a new
 * Apache collections test to GWT, but that is not an insignificant amount of
 * work.
 */
public abstract class TreeMapTest<K extends Comparable<K>, V> extends TestMap {
  private static final class SimpleEntry<K, V> implements Entry<K, V> {
    private static boolean equal(Object a, Object b) {
      return (a == null) ? (b == null) : a.equals(b);
    }

    private final K key;
    private final V value;

    private SimpleEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public boolean equals(Object object) {
      if (object instanceof Entry) {
        Entry<?, ?> other = (Entry<?, ?>) object;
        return equal(key, other.getKey()) && equal(value, other.getValue());
      }
      return false;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      return ((key == null) ? 0 : key.hashCode())
          ^ ((value == null) ? 0 : value.hashCode());
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Verify a Collection is explicitly and implicitly empty.
   * 
   * @param collection
   */
  @SuppressWarnings("unchecked")
  private static void _assertEmpty(Collection collection) {
    assertNotNull(collection);
    assertTrue(collection.isEmpty());
    assertEquals(0, collection.size());
    assertNotNull(collection.iterator());
    assertFalse(collection.iterator().hasNext());
  }

  /**
   * Verify a Map is explicitly and implicitly empty.
   * 
   * @param map
   */
  private static <K, V> void _assertEmpty(Map<K, V> map) {
    assertNotNull(map);
    assertTrue(map.isEmpty());
    assertEquals(0, map.size());

    _assertEmpty(map.values());
    _assertEmpty(map.keySet());
    _assertEmpty(map.entrySet());
  }

  /**
   * Verify that two Collections are deeply equivalent. Some of the Sets that
   * need to be verified do not implement a sensible equals method
   * (TreeMap.values for example).
   * 
   * @param expected
   * @param actual
   */
  private static <T> void _assertEquals(Collection<T> expected,
      Collection<T> actual) {
    // verify equivalence using collection interface
    assertEquals(expected.isEmpty(), actual.isEmpty());
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual));
    assertTrue(actual.containsAll(expected));
    for (T expectedValue : expected) {
      assertTrue(actual.contains(expectedValue));
    }
    for (T actualValue : actual) {
      assertTrue(expected.contains(actualValue));
    }
  }

  /**
   * Verify that two Maps are deeply equivalent.
   * 
   * @param expected
   * @param actual
   */
  private static <K, V> void _assertEquals(Map<K, V> expected, Map<K, V> actual) {
    assertEquals(expected.isEmpty(), actual.isEmpty());
    assertEquals(expected.size(), actual.size());

    _assertEquals(expected.keySet(), actual.keySet());
    _assertEquals(expected.entrySet(), actual.entrySet());

    // One might think that the following would true:
    // assertEquals(expected.values(), actual.values());
    // The following verifies what i would perceive as a bug in the jre
    // (rlo). The implementation of the Collection returned by the values method
    // does not implement equals sensibly.
    assertFalse(expected.values().equals(actual.values()));
    _assertEquals(expected.values(), actual.values());
  }

  /**
   * Verify that two SortedMaps are deeply equivalent.
   * 
   * @param expected
   * @param actual
   */
  private static <K, V> void _assertEquals(SortedMap<K, V> expected,
      SortedMap<K, V> actual) {
    _assertEquals((Map<K, V>) expected, (Map<K, V>) actual);

    // verify the order of the associated collections
    assertEquals(expected.keySet().toArray(), actual.keySet().toArray());
    assertEquals(expected.entrySet().toArray(), actual.entrySet().toArray());
    assertEquals(expected.values().toArray(), actual.values().toArray());
  }

  /**
   * Create the expected return of toString for a Map containing only the passed
   * key and value.
   * 
   * @param key
   * @param value
   * @return
   */
  private static <K, V> String makeEntryString(K key, V value) {
    return "{" + key + "=" + value + "}";
  }

  /**
   * comparator used when creating the SortedMap.
   */
  private Comparator<K> comparator = null;
  private boolean isClearSupported = true;
  private boolean isNullKeySupported = true;
  private boolean isNullValueSupported = true;
  private boolean isPutAllSupported = true;
  private boolean isPutSupported = true;
  private boolean isRemoveSupported = true;

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /**
   * Test method for 'java.util.Map.clear()'.
   * 
   * @see java.util.Map#clear()
   */
  public void testClear() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isClearSupported) {
      // Execute this test only if supported.
      Map<K, V> map = createMap();
      map.put(getKeys()[0], getValues()[0]);
      assertFalse(map.isEmpty());
      map.clear();
      _assertEmpty(map);
    }
  }

  /**
   * Test method for 'java.util.Map.clear()'.
   * 
   * @see java.util.Map#clear()
   */
  public void testClear_throwsUnsupportedOperationException() {
    Map<K, V> map = createMap();
    if (!isClearSupported) {
      try {
        map.clear();
        fail("expected exception");
      } catch (UnsupportedOperationException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.lang.Object.clone()'.
   */
  public void testClone() {
    // Map<K, V> map = createMap();
    // Check empty clone behavior
    // TODO (rlo) having .clone() in the code kills the test
    // SortedMap<K, V> clone = (SortedMap<K, V>)
    // map.clone();
    // assertNotNull(clone);
    // testEquivalent(map, clone);
    //
    // // Check non-empty clone behavior
    // map.put(KEY_1, getValues()[0]);
    // map.put(KEY_2, getValues()[1]);
    // map.put(KEY_3, getValues()[2]);
    // clone = (SortedMap<K, V>) map.clone();
    // assertNotNull(clone);
    // testEquivalent(map, clone);
  }

  /**
   * Test method for 'java.util.SortedMap.comparator()'.
   * 
   * @see java.util.SortedMap#comparator()
   */
  public void testComparator() {
    SortedMap<K, V> sortedMap = createSortedMap();
    if (isNaturalOrder()) {
      assertEquals(null, sortedMap.comparator());
    } else {
      assertEquals(getComparator(), sortedMap.comparator());
    }
  }

  /**
   * Test method for default constructor.
   * 
   * @see java.util.TreeMap#TreeMap()
   */
  public void testConstructor() {
    TreeMap<K, V> treeMap = new TreeMap<K, V>();
    _assertEmpty(treeMap);
  }

  /**
   * Test method for 'java.util.TreeMap.TreeMap(Comparator)'.
   * 
   * @see java.util.TreeMap#TreeMap(Comparator)
   */
  public void testConstructor_comparator() {
    TreeMap<K, V> treeMap = new TreeMap<K, V>(getComparator());
    _assertEmpty(treeMap);
    if (isNaturalOrder()) {
      assertNull(treeMap.comparator());
    } else {
      assertSame(getComparator(), treeMap.comparator());
    }
  }

  /**
   * Test method for 'java.util.TreeMap.TreeMap(Map)'.
   * 
   * @see java.util.TreeMap#TreeMap(Map)
   */
  public void testConstructor_Map() {
    // The source map should be just a Map. Not a sorted map.
    Map<K, V> sourceMap = new HashMap<K, V>();

    // populate the source map
    sourceMap.put(getKeys()[0], getValues()[0]);
    sourceMap.put(getKeys()[1], getValues()[1]);
    sourceMap.put(getKeys()[2], getValues()[2]);

    TreeMap<K, V> copyConstructed = new TreeMap<K, V>(sourceMap);
    _assertEquals(sourceMap, copyConstructed);
  }

  /**
   * Test method for 'java.util.TreeMap.TreeMap(Map)'.
   * 
   * @see java.util.TreeMap#TreeMap(Map)
   */
  @SuppressWarnings("unchecked")
  public void testConstructor_Map_rawType() {
    Map sourceMap = new HashMap();
    sourceMap.put(getConflictingKey(), getConflictingValue());
    // In Java, raw types can be used to defeat type checking.
    // For TreeMap, this works if the key is Comparable and there's
    // only one entry in the map. If there's more than one entry,
    // the compare() method will be called and that might throw.
    new TreeMap<K, V>(sourceMap);
  }

  /**
   * Test method for 'java.util.TreeMap.TreeMap(Map)'.
   * 
   * @see java.util.TreeMap#TreeMap(Map)
   */
  public void testConstructor_Map_throwsNullPointerException() {
    try {
      new TreeMap<K, V>((Map<K, V>) null);
      fail("expected exception");
    } catch (NullPointerException e) {
      // expected outcome
    } catch (JavaScriptException e) {
      // in Production Mode we don't actually do null checks, so we get a JS
      // exception
    }
  }

  /**
   * Test method for 'java.util.TreeMap.TreeMap(SortedMap)'.
   * 
   * @see java.util.TreeMap#TreeMap(SortedMap)
   */
  public void testConstructor_SortedMap() {
    SortedMap<K, V> sourceMap = new TreeMap<K, V>();
    _assertEmpty(sourceMap);

    // populate the source map
    sourceMap.put(getKeys()[0], getValues()[0]);
    sourceMap.put(getKeys()[1], getValues()[1]);
    sourceMap.put(getKeys()[2], getValues()[2]);

    TreeMap<K, V> copyConstructed = new TreeMap<K, V>(sourceMap);
    _assertEquals(sourceMap, copyConstructed);
  }

  /**
   * Test method for 'java.util.TreeMap.TreeMap(SortedMap).
   * 
   * @see java.util.TreeMap#TreeMap(SortedMap)
   */
  public void testConstructor_SortedMap_throwsNullPointerException() {
    try {
      new TreeMap<K, V>((SortedMap<K, V>) null);
      fail("expected exception");
    } catch (NullPointerException e) {
      // expected outcome
    } catch (JavaScriptException e) {
      // in Production Mode we don't actually do null checks, so we get a JS
      // exception
    }
  }

  /**
   * Test method for 'java.util.Map.containsKey(Object)'. *
   * 
   * @see java.util.Map#containsKey(Object)
   */
  public void testContainsKey() {
    Map<K, V> map = createMap();
    assertFalse(map.containsKey(getKeys()[0]));
    assertNull(map.put(getKeys()[0], getValues()[0]));
    assertEquals(1, map.keySet().size());
    assertTrue(map.containsKey(getKeys()[0]));
    assertFalse(map.containsKey(getKeys()[1]));
  }

  /**
   * Test method for 'java.util.Map.containsKey(Object)'.
   * 
   * @see java.util.Map#containsKey(Object)
   */
  public void testContainsKey_throwsClassCastException() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);
    try {
      map.containsKey(getConflictingKey());
      assertTrue("CCE expected in Development Mode", GWT.isScript());
    } catch (ClassCastException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.Map.containsKey(Object)'.
   * 
   * @see java.util.Map#containsKey(Object)
   */
  public void testContainsKey_throwsNullPointerException() {
    Map<K, V> map = createMap();
    if (isNaturalOrder() && !isNullKeySupported) {
      try {
        map.containsKey(null);
        fail("expected exception");
      } catch (NullPointerException e) {
        // expected outcome
      } catch (JavaScriptException e) {
        // in Production Mode we don't actually do null checks, so we get a JS
        // exception
      }
    }
  }

  /**
   * Test method for 'java.util.Map.containsValue(Object)'.
   * 
   * @see java.util.Map#containsValue(Object)
   */
  public void testContainsValue() {
    Map<K, V> map = createMap();
    assertFalse(map.containsValue(getValues()[0]));
    map.put(getKeys()[0], getValues()[0]);
    assertEquals(1, map.values().size());
    assertTrue(map.containsValue(getValues()[0]));
    assertFalse(map.containsValue(getKeys()[0]));
    assertFalse(map.containsValue(getValues()[1]));
    assertFalse(map.containsValue(null));
    map.put(getKeys()[0], null);
    assertTrue(map.containsValue(null));
  }

  /**
   * Test method for 'java.util.Map.containsValue(Object)'.
   * 
   * @see java.util.Map#containsValue(Object)
   */
  public void testContainsValue_throwsClassCastExcption() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);
    map.containsValue(getConflictingValue());

    // You might think this should throw an exception here but, no. Makes
    // sense since the class cast is attributed to comparability of the
    // keys... generics really have nothing to do with it .

    // try {
    // map.containsValue(getConflictingValue());
    // fail("expected exception");
    // } catch (ClassCastException e) {
    // // expected outcome
    // }
  }

  /**
   * Test method for 'java.util.Map.containsValue(Object)'.
   * 
   * @see java.util.Map#containsValue(Object)
   */
  public void testContainsValue_throwsNullPointerException() {
    Map<K, V> map = createMap();
    if (!isNullValueSupported) {
      try {
        map.containsValue(null);
        fail("expected exception");
      } catch (NullPointerException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Map.entrySet().remove(Object)'.
   * 
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_add_throwsUnsupportedOperationException() {
    Map<K, V> map = createMap();
    try {
      map.entrySet().add(new Entry<K, V>() {
        public K getKey() {
          return null;
        }

        public V getValue() {
          return null;
        }

        public V setValue(V value) {
          return null;
        }
      });
      fail("expected exception");
    } catch (UnsupportedOperationException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.Map.entrySet()'.
   * 
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_entries0() {
    Map<K, V> map = createMap();
    Set<Entry<K, V>> entrySet = map.entrySet();
    _assertEmpty(entrySet);
  }

  /**
   * Test method for 'java.util.Map.entrySet()'.
   * 
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_entries1() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);

    // Verify the view correctly represents the map
    Set<Entry<K, V>> entrySet = map.entrySet();
    assertNotNull(entrySet);
    Iterator<Entry<K, V>> iter = entrySet.iterator();
    assertNotNull(iter);
    assertTrue(iter.hasNext());
    Entry<K, V> entry = iter.next();
    assertNotNull(entry);

    assertEquals(entry.getKey(), getKeys()[0]);
    assertEquals(entry.getValue(), getValues()[0]);
    // Don't use assertEquals; we want to be clear about which object's equals()
    // method to test.
    assertTrue(
        entry.equals(new SimpleEntry<K, V>(getKeys()[0], getValues()[0])));
  }

  /**
   * Test method for 'java.util.Map.entrySet()'.
   * 
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_entries1_view() {
    Map<K, V> map = createMap();
    // Get a view of the entry set before modifying the underlying map.
    Set<Entry<K, V>> entrySet = map.entrySet();
    map.put(getKeys()[0], getValues()[0]);

    // Verify that the entries view reflects updates to the map.
    assertEquals(entrySet.iterator().next().getKey(), getKeys()[0]);
    assertEquals(entrySet.iterator().next().getValue(), getValues()[0]);
  }

  /**
   * Test method for 'java.util.Map.entrySet()'.
   * 
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_entries1_view_modify() {
    Map<K, V> map = createMap();
    Set<Entry<K, V>> entrySet = map.entrySet(); // get view before modification
    map.put(getKeys()[0], getValues()[0]); // put a value
    map.put(getKeys()[0], getValues()[1]); // overwrite the value

    // Verify that the entries view reflects updates to the map.
    assertEquals(entrySet.iterator().next().getKey(), getKeys()[0]);
    assertEquals(entrySet.iterator().next().getValue(), getValues()[1]);

    // Verify that the entries view is updated on removes to the map.
    map.remove(getKeys()[0]);
    _assertEmpty(entrySet);
  }

  public void testEntrySet_entry_setValue() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);
    map.entrySet().iterator().next().setValue(getValues()[1]);
    assertTrue(map.containsValue(getValues()[1]));
  }

  /**
   * Test method for 'java.util.Map.entrySet().remove(Object)'.
   * 
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_remove() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);

    Set<Entry<K, V>> entrySet = map.entrySet();
    assertTrue(entrySet.remove(entrySet.iterator().next()));
    assertTrue(entrySet.isEmpty());
    assertEquals(entrySet.size(), map.size());
  }

  /**
   * Test method for 'java.util.Map.entrySet().remove(Object)'.
   * 
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_remove_equivalentEntry() {
    Map<K, V> map0 = createMap();
    map0.put(getKeys()[0], getValues()[0]);

    Map<K, V> map1 = createMap();
    map1.put(getKeys()[0], getValues()[1]);

    // Verify attempting to remove an equivalent entry from a different map has
    // no effect.
    Set<Entry<K, V>> entrySet0 = map0.entrySet();
    assertFalse(entrySet0.remove(map1.entrySet().iterator().next()));
    assertFalse(entrySet0.isEmpty());
    assertEquals(entrySet0.size(), map0.size());
  }

  /**
   * Test method for 'java.util.Object.equals(Object)'.
   * 
   * @see java.util.Map#equals(Object)
   */
  public void testEquals() {
    Map<K, V> map0 = createMap();
    Map<K, V> map1 = createMap();
    assertTrue(map0.equals(map1));
    map0.put(getKeys()[0], getValues()[0]);
    map1.put(getKeys()[0], getValues()[0]);
    assertTrue(map0.equals(map0));
    assertTrue(map0.equals(map1));
    map0.put(getKeys()[1], getValues()[1]);
    assertFalse(map0.equals(map1));
  }

  /**
   * Test method for 'java.lang.Object.finalize()'.
   */
  public void testFinalize() {
    // TODO no tests for finalize?
  }

  /**
   * Test method for 'java.util.SortedMap.firstKey()'.
   * 
   * @see java.util.SortedMap#firstKey()
   */
  public void testFirstKey() {
    SortedMap<K, V> sortedMap = createSortedMap();
    // test with a single entry map
    sortedMap.put(getKeys()[0], getValues()[0]);
    assertEquals(getKeys()[0], sortedMap.firstKey());
    // is it consistent with other methods
    assertEquals(sortedMap.keySet().toArray()[0], sortedMap.firstKey());
    assertEquals(getKeys()[0], sortedMap.lastKey());
    assertEquals(sortedMap.lastKey(), sortedMap.firstKey());

    // test with two entry map
    sortedMap.put(getKeys()[1], getValues()[1]);
    assertEquals(getKeys()[0], sortedMap.firstKey());
    assertFalse(getKeys()[1].equals(sortedMap.firstKey()));
    // is it consistent with other methods
    assertEquals(sortedMap.keySet().toArray()[0], sortedMap.firstKey());
    assertFalse(getKeys()[0].equals(sortedMap.lastKey()));
    assertFalse(sortedMap.lastKey().equals(sortedMap.firstKey()));
  }

  /**
   * Test method for 'java.util.SortedMap.firstKey()'.
   * 
   * @see java.util.SortedMap#firstKey()
   */
  public void testFirstKey_throwsNoSuchElementException() {
    SortedMap<K, V> sortedMap = createSortedMap();
    // test with no entries
    try {
      sortedMap.firstKey();
      fail("expected exception");
    } catch (NoSuchElementException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.Map.get(Object)'.
   * 
   * @see java.util.Map#get(Object)
   */
  public void testGet() {
    Map<K, V> map = createMap();
    if (useNullKey()) {
      assertNull(map.get(null));
    }
    assertNull(map.get(getKeys()[0]));
    assertNull(map.put(getKeys()[0], getValues()[0]));
    assertEquals(getValues()[0], map.get(getKeys()[0]));
  }

  /**
   * Test method for 'java.util.Map.get(Object)'.
   * 
   * @see java.util.Map#get(Object)
   */
  public void testGet_throwsClassCastException() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);
    try {
      map.get(getConflictingKey());
      assertTrue("CCE expected in Development Mode", GWT.isScript());
    } catch (ClassCastException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.Map.get(Object)'.
   * 
   * @see java.util.Map#get(Object)
   */
  public void testGet_throwsNullPointerException() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);
    try {
      map.get(null);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }
  }

  /**
   * Test method for 'java.lang.Object.hashCode()'.
   * 
   * @see java.util.Map#hashCode()
   */
  public void testHashCode() {
    Map<K, V> map0 = createMap();
    Map<K, V> map1 = createMap();

    int hashCode0 = map0.hashCode();
    int hashCode1 = map1.hashCode();
    assertTrue("empty maps have different hash codes", hashCode0 == hashCode1);

    // Check that hashCode changes
    map0.put(getKeys()[0], getValues()[0]);
    hashCode0 = map0.hashCode();
    assertTrue("hash code didn't change", hashCode0 != hashCode1);

    // The above is actually not a completely dependable test because hash codes
    // are funky at the edges. The hash code of an abstract map is determined by
    // accumulating the hash code of the contained Entry(s). The TreeMap Entry
    // hash code implementation will always result in 0 if the exclusive or of
    // the key and value for the Entry is 0.

    Map<String, String> map2 = new TreeMap<String, String>();
    Map<Integer, Integer> map3 = new TreeMap<Integer, Integer>();

    map2.put("", "");

    map3.put(0, Integer.MIN_VALUE);
    map3.put(Integer.MIN_VALUE, 0);

    int hashCode2 = map2.hashCode();
    int hashCode3 = map3.hashCode();
    assertEquals("empty string/0 hash codes not the same", hashCode2, hashCode3);
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object)'.
   * 
   * @see java.util.SortedMap#headMap(Object)
   */
  public void testHeadMap() {
    // test with no entries
    assertNotNull(createSortedMap().headMap(getKeys()[0]));
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object)'.
   * 
   * @see java.util.SortedMap#headMap(Object)
   */
  public void testHeadMap_entries0_size() {
    // test with no entries
    assertEquals(0, createSortedMap().headMap(getKeys()[0]).size());
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object)'.
   * 
   * @see java.util.SortedMap#headMap(Object)
   */
  public void testHeadMap_entries1() {
    SortedMap<K, V> sortedMap = createSortedMap();
    // test with a single entry map
    sortedMap.put(getKeys()[0], getValues()[0]);
    assertEquals(0, sortedMap.headMap(getKeys()[0]).size());
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object)'.
   * 
   * @see java.util.SortedMap#headMap(Object)
   */
  public void testHeadMap_entries2() {
    SortedMap<K, V> sortedMap = createSortedMap();
    // test with two entry map
    sortedMap.put(getKeys()[0], getValues()[0]);
    sortedMap.put(getKeys()[1], getValues()[1]);
    assertEquals(0, sortedMap.headMap(getKeys()[0]).size());
    assertEquals(1, sortedMap.headMap(getKeys()[1]).size());
    assertEquals(getKeys()[0],
        sortedMap.tailMap(getKeys()[0]).keySet().toArray()[0]);
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#headMap(Object)
   */
  @SuppressWarnings("unchecked")
  public void testHeadMap_throwsClassCastException() {
    SortedMap sortedMap = createSortedMap();
    sortedMap.put(getKeys()[0], getValues()[0]);
    if (isNaturalOrder()) {
      // TODO Why does this succeed with natural ordering when subMap doesn't?
      sortedMap.headMap(getConflictingKey());
    } else {
      try {
        sortedMap.headMap(getConflictingKey());
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#headMap(Object)
   */
  public void testHeadMap_throwsIllegalArgumentException() {
    // TODO I don't know of any case where this could happen.
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#headMap(Object)
   */
  public void testHeadMap_throwsNullPointerException() {
    SortedMap<K, V> sortedMap = createSortedMap();
    try {
      sortedMap.headMap(null);
      assertTrue(useNullKey() || GWT.isScript());
    } catch (NullPointerException e) {
      assertFalse(useNullKey());
    }
  }

  /**
   * Test method for 'java.util.Map.isEmpty()'. *
   * 
   * @see java.util.Map#isEmpty()
   * 
   */
  public void testIsEmpty() {
    Map<K, V> sourceMap = createMap();
    Map<K, V> destMap = createMap();

    destMap.putAll(sourceMap);
    assertTrue(destMap.isEmpty());

    destMap.put(getKeys()[0], getValues()[0]);
    assertFalse(destMap.isEmpty());

    destMap.remove(getKeys()[0]);
    assertTrue(destMap.isEmpty());
    assertEquals(destMap.size(), 0);
  }

  /**
   * Test method for 'java.util.Map.keySet()'.
   * 
   * @see java.util.Map#clear()
   */
  public void testKeySet() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);
    Set<K> keySet = map.keySet();
    _assertEquals(keySet, keySet);
  }

  /**
   * Test method for 'java.util.Map.keySet()'.
   * 
   * @see java.util.Map#clear()
   */
  public void testKeySet_viewPut() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);
    Set<K> keySet = map.keySet();
    assertEquals(1, keySet.size());
    map.put(getKeys()[1], getValues()[1]);
    assertEquals(2, keySet.size());
  }

  /**
   * Test method for 'java.util.Map.keySet()'.
   * 
   * @see java.util.Map#clear()
   */
  public void testKeySet_viewRemove() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);
    map.put(getKeys()[1], getValues()[1]);
    Set<K> keySet = map.keySet();
    assertEquals(2, keySet.size());
    map.remove(getKeys()[1]);
    assertEquals(1, keySet.size());
  }

  public void testKeySetIteratorRemove() {
    Map<K, V> map = makeFullMap();
    resetFull();
    ArrayList<K> keys = new ArrayList<K>();
    for (Object key : getSampleKeys()) {
      keys.add((K) key);
    }
    Comparator<? super K> cmp = ((TreeMap<K, V>) map).comparator();
    if (cmp != null) {
      Collections.sort(keys, cmp);
    } else {
      Collections.sort(keys);
    }
    Iterator<K> it = map.keySet().iterator();
    for (K key : keys) {
      assertTrue(it.hasNext());
      K rem = it.next();
      it.remove();
      assertEquals(key, rem);
    }
    assertEquals(0, map.size());
  }

  /**
   * Test method for 'java.util.SortedMap.lastKey()'.
   * 
   * @see java.util.SortedMap#lastKey()
   */
  public void testLastKey() {
    SortedMap<K, V> sortedMap = createSortedMap();

    // test with a single entry map
    sortedMap.put(getKeys()[0], getValues()[0]);
    assertEquals(getKeys()[0], sortedMap.lastKey());
    // is it consistent with other methods
    assertEquals(sortedMap.keySet().toArray()[0], sortedMap.lastKey());
    assertEquals(getKeys()[0], sortedMap.firstKey());
    assertEquals(sortedMap.firstKey(), sortedMap.lastKey());

    // test with two entry map
    sortedMap.put(getKeys()[1], getValues()[1]);
    assertEquals(getKeys()[1], sortedMap.lastKey());
    assertFalse(getKeys()[0].equals(sortedMap.lastKey()));
    // is it consistent with other methods
    assertEquals(sortedMap.keySet().toArray()[1], sortedMap.lastKey());
    assertEquals(getKeys()[0], sortedMap.firstKey());
    assertFalse(sortedMap.firstKey().equals(sortedMap.lastKey()));
  }

  /**
   * Test method for 'java.util.SortedMap.lastKey()'.
   * 
   * @see java.util.SortedMap#lastKey()
   */
  public void testLastKey_throwsNoSuchElementException() {
    SortedMap<K, V> sortedMap = createSortedMap();
    // test with no entries
    try {
      sortedMap.lastKey();
      fail("expected exception");
    } catch (NoSuchElementException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'.
   * 
   * @see java.util.Map#put(Object, Object)
   */
  public void testPut() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutSupported) {
      Map<K, V> map = createMap();
      assertNull(map.put(getKeys()[0], getValues()[0]));
      assertFalse(map.isEmpty());
      assertEquals(1, map.size());
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'.
   * 
   * @see java.util.Map#put(Object, Object)
   */
  public void testPut_entries3() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutSupported) {
      // populate the map
      Map<K, V> map = createMap();
      map.put(getKeys()[0], getValues()[0]);
      map.put(getKeys()[1], getValues()[1]);
      map.put(getKeys()[2], getValues()[2]);

      // test contents
      assertFalse(map.isEmpty());
      assertEquals(3, map.size());
      // test contains all values
      Collection<V> values = map.values();
      assertTrue(values.contains(getValues()[0]));
      assertTrue(values.contains(getValues()[1]));
      assertTrue(values.contains(getValues()[2]));
      Collection<K> keys = map.keySet();
      // test contains all keys
      assertTrue(keys.contains(getKeys()[0]));
      assertTrue(keys.contains(getKeys()[1]));
      assertTrue(keys.contains(getKeys()[2]));
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'. This test shows some
   * bad behavior of the TreeMap class before JDK 7. A mapping with null key can
   * be put in but several methods are are unusable afterward.
   * 
   * A SortedMap with natural ordering (no comparator) is supposed to throw a
   * null pointer exception if a null keys are "not supported". For a natural
   * ordered TreeMap before JDK 7, a null pointer exception is not thrown. But,
   * the map is left in a state where any other key based methods result in a
   * null pointer exception.
   * 
   * @see java.util.Map#put(Object, Object)
   */
  public void testPut_nullKey() {
    SortedMap<K, V> sortedMap = createSortedMap();

    if (useNullKey()) {
      assertNull(sortedMap.put(null, getValues()[0]));
      assertTrue(sortedMap.containsValue(getValues()[0]));

      // the map methods the continue to function
      sortedMap.containsValue(null);
      sortedMap.containsValue(getValues()[0]);
      sortedMap.entrySet();
      sortedMap.equals(createMap());
      sortedMap.hashCode();
      sortedMap.isEmpty();
      sortedMap.keySet();
      sortedMap.putAll(createMap());
      sortedMap.size();
      sortedMap.values();

      // all of the sorted map methods still function
      sortedMap.comparator();
      sortedMap.firstKey();
      sortedMap.lastKey();
      sortedMap.subMap(getLessThanMinimumKey(), getGreaterThanMaximumKey());
      sortedMap.headMap(getLessThanMinimumKey());
      sortedMap.tailMap(getLessThanMinimumKey());
    } else if (isJdk7()) {
      // nulls are rejected immediately and don't poison the map anymore
      try {
        assertNull(sortedMap.put(null, getValues()[0]));
        fail("should have thrown");
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        assertNull(sortedMap.put(null, getValues()[1]));
        fail("expected exception adding second null");
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        sortedMap.containsKey(null);
        fail("expected exception on containsKey(null)");
      } catch (NullPointerException e) {
        // expected outcome
      }
      sortedMap.containsKey(getKeys()[0]);
      try {
        sortedMap.get(null);
        fail("expected exception on get(null)");
      } catch (NullPointerException e) {
        // expected outcome
      }
      sortedMap.get(getKeys()[0]);
      try {
        sortedMap.remove(null);
      } catch (NullPointerException e) {
        // expected
      }
      sortedMap.remove(getKeys()[0]);
    } else {
      // before JDK 7, nulls poisoned the map
      try {
        assertNull(sortedMap.put(null, getValues()[0]));
        // note: first null added is not required to throw NPE since no
        // comparisons are needed
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        assertNull(sortedMap.put(null, getValues()[1]));
        fail("expected exception adding second null");
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        sortedMap.containsKey(null);
        fail("expected exception on containsKey(null)");
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        sortedMap.containsKey(getKeys()[0]);
        fail("expected exception on contains(key)");
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        sortedMap.get(null);
        fail("expected exception on get(null)");
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        sortedMap.get(getKeys()[0]);
        fail("expected exception on get(key)");
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        sortedMap.remove(null);
        fail("expected exception on remove(null)");
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        sortedMap.remove(getKeys()[0]);
        fail("expected exception on remove(key)");
      } catch (NullPointerException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'.
   * 
   * @see java.util.Map#put(Object, Object)
   */
  public void testPut_replace() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutSupported) {
      Map<K, V> map = createMap();
      assertNull(map.put(getKeys()[0], getValues()[0]));
      assertFalse(map.isEmpty());
      assertEquals(1, map.size());

      assertEquals(map.put(getKeys()[0], getValues()[1]), getValues()[0]);
      assertEquals(1, map.size());
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'.
   * 
   * @see java.util.Map#put(Object, Object)
   */
  @SuppressWarnings("unchecked")
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testPut_throwsClassCastException_key() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutSupported) {
      Map<K, V> map = createMap();
      map.put(getKeys()[0], getValues()[0]);
      try {
        Map untypedMap = map;
        untypedMap.put(getConflictingKey(), getValues()[1]);
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'.
   * 
   * @see java.util.Map#put(Object, Object)
   */
  @SuppressWarnings("unchecked")
  public void testPut_throwsClassCastException_value() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutSupported) {
      Map<K, V> map = createMap();
      map.put(getKeys()[0], getValues()[0]);

      Map untypedMap = map;
      untypedMap.put(getKeys()[1], getConflictingValue());
      // You might think this should throw an exception here but, no. Makes
      // sense since the class cast is attributed to comparability of the
      // keys... generics really have nothing to do with it .
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'.
   * 
   * @see java.util.Map#put(Object, Object)
   */
  public void testPut_throwsIllegalArgumentException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutSupported) {
      // TODO I don't know of any case where this could happen.
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'.
   * 
   * @see java.util.Map#put(Object, Object)
   */
  public void testPut_throwsNullPointerException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutSupported) {
      Map<K, V> map;
      map = createMap();

      try {
        map.put(null, getValues()[0]);
        // first put of a null key is not required to NPE since no comparisons
        // are needed
      } catch (NullPointerException e) {
        assertFalse(useNullKey());
      }

      try {
        map.put(null, getValues()[0]);
        assertTrue(useNullKey());
      } catch (NullPointerException e) {
        assertFalse(useNullKey());
      }

      map = createMap();
      map.put(getKeys()[0], getValues()[0]);
      try {
        map.put(null, getValues()[0]);
        assertTrue(useNullKey());
      } catch (NullPointerException e) {
        assertFalse(useNullKey());
      }
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'.
   * 
   * @see java.util.Map#put(Object, Object)
   */
  public void testPut_throwsUnsupportedOperationException() {
    if (!isPutSupported) {
      Map<K, V> map = createMap();
      try {
        map.put(getKeys()[0], getValues()[0]);
        fail("expected exception");
      } catch (UnsupportedOperationException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Map.putAll(Map)'.
   * 
   * @see java.util.Map#putAll(Map)
   */
  public void testPutAll() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Map<K, V> sourceMap = createMap();
      sourceMap.put(getKeys()[0], getValues()[0]);
      sourceMap.put(getKeys()[1], getValues()[1]);
      sourceMap.put(getKeys()[2], getValues()[2]);

      Map<K, V> destMap = createMap();
      destMap.putAll(sourceMap);
      // Make sure that the data is copied correctly
      _assertEquals(sourceMap, destMap);
    }
  }

  /**
   * Test method for 'java.util.Map.putAll(Map)'.
   * 
   * @see java.util.Map#putAll(Map)
   */
  public void testPutAll_addEntries() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Map<K, V> sourceMap = createMap();
      sourceMap.put(getKeys()[0], getValues()[0]);

      Map<K, V> destMap = createMap();
      destMap.putAll(sourceMap);
      // Verify that entries get added.
      sourceMap.put(getKeys()[1], getValues()[1]);
      destMap.putAll(sourceMap);
      _assertEquals(sourceMap, destMap);
    }
  }

  /**
   * Test method for 'java.util.Map.putAll(Map)'.
   * 
   * @see java.util.Map#putAll(Map)
   */
  public void testPutAll_emptyMap() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Map<K, V> sourceMap = createMap();
      sourceMap.put(getKeys()[0], getValues()[0]);

      Map<K, V> destMap = createMap();
      destMap.putAll(sourceMap);
      // Verify that putting an empty map does not clear.
      destMap.putAll(createMap());
      _assertEquals(sourceMap, destMap);
    }
  }

  /**
   * Test method for 'java.util.Map.putAll(Map)'.
   * 
   * @see java.util.Map#putAll(Map)
   */
  public void testPutAll_overwrite() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Map<K, V> sourceMap = createMap();
      sourceMap.put(getKeys()[0], getValues()[0]);

      Map<K, V> destMap = createMap();
      destMap.putAll(sourceMap);
      // Verify that entries get replaced.
      sourceMap.put(getKeys()[0], getValues()[1]);
      destMap.putAll(sourceMap);
      _assertEquals(sourceMap, destMap);
    }
  }

  /**
   * Test method for 'java.util.Map.putAll(Map)'.
   * 
   * @see java.util.Map#putAll(Map)
   */
  public void testPutAll_self() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Map<K, V> sourceMap = createMap();
      sourceMap.put(getKeys()[0], getValues()[0]);
      sourceMap.putAll(sourceMap);
      // verify putAll with self succeeds and has no effect.
      assertEquals(1, sourceMap.size());
      assertEquals(getKeys()[0], sourceMap.keySet().iterator().next());
      assertEquals(getValues()[0], sourceMap.values().iterator().next());
    }
  }

  /**
   * Test method for 'java.util.Map.putAll(Map)'.
   * 
   * @see java.util.Map#putAll(Map)
   */
  @SuppressWarnings("unchecked")
  public void testPutAll_throwsClassCastException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Map sourceMap = new HashMap();
      sourceMap.put(getConflictingKey(), getConflictingValue());

      Map<K, V> destMap = createMap();
      destMap.put(getKeys()[0], getValues()[0]);
      try {
        // This throws in dev mode because we're putting a second
        // entry in the map and the TreeMap calls the compare method.
        destMap.putAll(sourceMap);
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Map.putAll(Map)'.
   * 
   * @see java.util.Map#putAll(Map)
   */
  public void testPutAll_throwsIllegalOperationException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      // TODO I don't know of any case where this could happen.
    }
  }

  /**
   * Test method for 'java.util.Map.putAll(Map)'.
   * 
   * @see java.util.Map#putAll(Map)
   */
  public void testPutAll_throwsNullPointerException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutAllSupported) {
      Map<K, V> map = createMap();
      try {
        map.putAll((Map<K, V>) null);
        fail("expected exception");
      } catch (NullPointerException e) {
        // expected outcome
      } catch (JavaScriptException e) {
        // in Production Mode we don't actually do null checks, so we get a JS
        // exception
      }
    }
  }

  /**
   * Test method for 'java.util.Map.putAll(Map)'.
   * 
   * @see java.util.Map#putAll(Map)
   */
  public void testPutAll_throwsUnsupportedOperationException() {
    Map<K, V> map = createMap();
    if (!isPutAllSupported) {
      try {
        map.putAll(createMap());
        fail("expected exception");
      } catch (UnsupportedOperationException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Map.remove(Object)'.
   * 
   * @see java.util.Map#remove(Object)
   */
  public void testRemove() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isRemoveSupported) {
      Map<K, V> map = createMap();

      // null keys are special
      if (useNullKey()) {
        assertNull(map.remove(null));
      }

      assertNull(map.remove(getKeys()[0]));
      assertNull(map.put(getKeys()[0], getValues()[0]));
      assertEquals(map.remove(getKeys()[0]), getValues()[0]);
      assertNull(map.remove(getKeys()[0]));
    }
  }

  /**
   * Test method for 'java.util.Map.remove(Object)'.
   * 
   * @see java.util.Map#remove(Object)
   */
  public void testRemove_throwsClassCastException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isRemoveSupported) {
      Map<K, V> map = createMap();
      map.put(getKeys()[0], getValues()[0]);
      try {
        map.remove(getConflictingKey());
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Map.remove(Object)'.
   * 
   * @see java.util.Map#remove(Object)
   */
  public void testRemove_throwsNullPointerException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isRemoveSupported) {
      
      // TODO(acleung): Post JDK7, map.put(null) will actually throw a NPE.
      // Lets disable this for now. Once we no longer test on JDK6, we can
      // add this back and always assert an NPE.
      /*
      Map<K, V> map;
      map = createMap();
      // test remove null key with map containing a single null key
      map.put(null, getValues()[0]);
      try {
        map.remove(null);
        assertTrue(useNullKey());
      } catch (NullPointerException e) {
        assertFalse(useNullKey());
      }
      */

      map = createMap();
      // test remove null key with map containing a single non-null key
      map.put(getKeys()[0], getValues()[0]);
      try {
        map.remove(null);
        assertTrue(useNullKey());
      } catch (NullPointerException e) {
        assertFalse(useNullKey());
      } catch (JavaScriptException e) {
        assertFalse(useNullKey());
      }
    }
  }

  /**
   * Test method for 'java.util.Map.remove(Object)'.
   * 
   * @see java.util.Map#remove(Object)
   */
  public void testRemove_throwsUnsupportedOperationException() {
    Map<K, V> map = createMap();
    if (!isRemoveSupported) {
      try {
        map.remove(getKeys()[0]);
        fail("expected exception");
      } catch (UnsupportedOperationException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.Map.size()'.
   * 
   * @see java.util.Map#size()
   */
  public void testSize() {
    Map<K, V> map = createMap();

    // Test size behavior on put
    map.put(getKeys()[0], getValues()[0]);
    assertEquals(1, map.size());
    map.put(getKeys()[1], getValues()[1]);
    assertEquals(2, map.size());
    map.put(getKeys()[2], getValues()[2]);
    assertEquals(3, map.size());

    // Test size behavior on remove
    map.remove(getKeys()[0]);
    assertEquals(2, map.size());
    map.remove(getKeys()[1]);
    assertEquals(1, map.size());
    map.remove(getKeys()[2]);
    assertEquals(0, map.size());

    // Test size behavior on putAll
    map.put(getKeys()[0], getValues()[0]);
    map.put(getKeys()[1], getValues()[1]);
    map.put(getKeys()[2], getValues()[2]);
    assertEquals(3, map.size());

    // Test size behavior on clear
    map.clear();
    _assertEmpty(map);
  }

  /**
   * Test method for 'java.util.SortedMap.subMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#subMap(Object, Object)
   */
  public void testSubMap() {
    SortedMap<K, V> sortedMap = createSortedMap();
    // test with no entries
    assertEquals(0, sortedMap.subMap(getKeys()[0], getKeys()[0]).size());

    // test with a single entry map
    sortedMap.put(getKeys()[0], getValues()[0]);
    assertEquals(0, sortedMap.subMap(getKeys()[0], getKeys()[0]).size());
    // bounded by a "wide" range
    assertEquals(1, sortedMap.subMap(getLessThanMinimumKey(),
        getGreaterThanMaximumKey()).size());

    // test with two entry map
    sortedMap.put(getKeys()[1], getValues()[1]);
    assertEquals(1, sortedMap.subMap(getKeys()[0], getKeys()[1]).size());
    assertEquals(getKeys()[0],
        sortedMap.subMap(getKeys()[0], getKeys()[1]).keySet().toArray()[0]);
    // bounded by a "wide" range
    SortedMap<K, V> subMap = sortedMap.subMap(getLessThanMinimumKey(),
        getGreaterThanMaximumKey());
    assertEquals(2, subMap.size());
  }

  /**
   * Test method for 'java.util.SortedMap.subMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#subMap(Object, Object)
   */
  @SuppressWarnings("unchecked")
  public void testSubMap_throwsClassCastException() {
    SortedMap sortedMap = createSortedMap();
    sortedMap.put(getKeys()[0], getValues()[0]);
    try {
      sortedMap.subMap(getConflictingKey(), getKeys()[0]);
      assertTrue("CCE expected in Development Mode", GWT.isScript());
    } catch (IllegalArgumentException e) {
      // since we can't ensure CCEs in Production Mode, we may get IAE
      assertTrue("IllegalArgumentException in Development Mode", GWT.isScript());
    } catch (ClassCastException e) {
      // expected outcome
    }
    try {
      sortedMap.subMap(getKeys()[0], getConflictingKey());
      assertTrue("CCE expected in Development Mode", GWT.isScript());
    } catch (IllegalArgumentException e) {
      // since we can't ensure CCEs in Production Mode, we may get IAE
      assertTrue("IllegalArgumentException in Development Mode", GWT.isScript());
    } catch (ClassCastException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.SortedMap.subMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#subMap(Object, Object)
   */
  public void testSubMap_throwsIllegalArgumentException() {
    SortedMap<K, V> sortedMap = createSortedMap();
    try {
      sortedMap.subMap(getGreaterThanMaximumKey(), getLessThanMinimumKey());
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // from key is greater than the to key
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.SortedMap.subMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#subMap(Object, Object)
   */
  public void testSubMap_throwsNullPointerException() {
    SortedMap<K, V> sortedMap = createSortedMap();
    try {
      sortedMap.subMap(null, getLessThanMinimumKey());
      assertTrue(useNullKey());
    } catch (NullPointerException e) {
      assertFalse(useNullKey());
    } catch (JavaScriptException e) {
      assertFalse(useNullKey());
    }
    try {
      sortedMap.subMap(null, getGreaterThanMaximumKey());
      assertTrue(useNullKey());
    } catch (NullPointerException e) {
      assertFalse(useNullKey());
    } catch (JavaScriptException e) {
      assertFalse(useNullKey());
    }
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object)'.
   * 
   * @see java.util.SortedMap#tailMap(Object)
   */
  public void testTailMap_entries0() {
    // test with no entries
    Map<K, V> tailMap = createSortedMap().tailMap(getKeys()[0]);
    assertNotNull(tailMap);
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object)'.
   * 
   * @see java.util.SortedMap#tailMap(Object)
   */
  public void testTailMap_entries0_size() {
    // test with no entries
    Map<K, V> tailMap = createSortedMap().tailMap(getKeys()[0]);
    assertNotNull(tailMap);
    assertEquals(0, tailMap.size());
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object)'.
   * 
   * @see java.util.SortedMap#tailMap(Object)
   */
  public void testTailMap_entries1_size_keyValue() {
    SortedMap<K, V> sortedMap = createSortedMap();
    // test with a single entry map
    sortedMap.put(getKeys()[0], getValues()[0]);
    Map<K, V> tailMap = sortedMap.tailMap(getKeys()[0]);
    assertEquals(1, tailMap.size());
    assertEquals(getKeys()[0], tailMap.keySet().toArray()[0]);
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object)'.
   * 
   * @see java.util.SortedMap#tailMap(Object)
   */
  public void testTailMap_entries2_size_keyValue() {
    SortedMap<K, V> sortedMap = createSortedMap();
    // test with two entry map
    sortedMap.put(getKeys()[0], getValues()[0]);
    Map<K, V> tailMap = sortedMap.tailMap(getKeys()[0]);
    assertEquals(1, tailMap.size());
    sortedMap.put(getKeys()[1], getValues()[1]);
    tailMap = sortedMap.tailMap(getKeys()[1]);
    assertEquals(1, tailMap.size());
    tailMap = sortedMap.tailMap(getKeys()[0]);
    assertEquals(2, tailMap.size());
    assertEquals(getKeys()[0], tailMap.keySet().toArray()[0]);
    assertEquals(getKeys()[1], tailMap.keySet().toArray()[1]);
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#tailMap(Object)
   */
  @SuppressWarnings("unchecked")
  public void testTailMap_throwsClassCastException() {
    SortedMap sortedMap = createSortedMap();
    sortedMap.put(getKeys()[0], getValues()[0]);
    if (isNaturalOrder()) {
      // TODO Why does this succeed with natural ordering when subMap doesn't?
      sortedMap.tailMap(getConflictingKey());
    } else {
      try {
        sortedMap.tailMap(getConflictingKey());
        assertTrue("CCE expected in Development Mode", GWT.isScript());
      } catch (ClassCastException e) {
        // expected outcome
      }
    }
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#tailMap(Object)
   */
  public void testTailMap_throwsIllegalArgumentException() {
    // TODO I don't know of any case where this could happen.
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object, Object)'.
   * 
   * @see java.util.SortedMap#tailMap(Object)
   */
  public void testTailMap_throwsNullPointerException() {
    SortedMap<K, V> sortedMap = createSortedMap();
    try {
      sortedMap.tailMap(null);
      assertTrue(useNullKey());
    } catch (NullPointerException e) {
      assertFalse(useNullKey());
    }
  }

  /**
   * Test method for 'java.lang.Object.toString()'.
   */
  public void testToString() {
    Map<K, V> map = createMap();
    map.put(getKeys()[0], getValues()[0]);
    String entryString = makeEntryString(getKeys()[0], getValues()[0]);
    assertEquals(entryString, map.toString());
  }

  /**
   * Test method for 'java.util.Map.values()'.
   * 
   * @see java.util.Map#values()
   */
  public void testValues() {
    Map<K, V> map = createMap();

    map.put(getKeys()[0], getValues()[0]);

    Collection<V> values = map.values();
    assertNotNull(values);
    assertEquals(1, values.size());

    Iterator<V> valueIter = values.iterator();
    V value = valueIter.next();
    assertEquals(value, getValues()[0]);
  }

  /**
   * Test method for 'java.util.Map.values()'.
   * 
   * @see java.util.Map#values()
   */
  public void testValues_nullKey() {
    Map<K, V> map = createMap();

    map.put(getKeys()[0], getValues()[0]);

    Collection<V> values = map.values();
    assertNotNull(values);
    assertEquals(1, values.size());

    Iterator<V> valueIter = values.iterator();
    V value = valueIter.next();
    assertEquals(value, getValues()[0]);
  }

  /**
   * Test method for 'java.util.Map.values()'.
   * 
   * @see java.util.Map#values()
   */
  public void testValues_viewPut() {
    Map<K, V> map = createMap();

    map.put(getKeys()[0], getValues()[0]);

    Collection<V> values = map.values();
    assertNotNull(values);
    assertEquals(1, values.size());

    map.put(getKeys()[1], getValues()[1]);
    assertEquals(2, values.size());
  }

  /**
   * Test method for 'java.util.Map.values()'.
   * 
   * @see java.util.Map#values()
   */
  public void testValues_viewRemove() {
    Map<K, V> map = createMap();

    map.put(getKeys()[0], getValues()[0]);
    map.put(getKeys()[1], getValues()[1]);

    Collection<V> values = map.values();
    assertNotNull(values);
    assertEquals(2, values.size());

    map.remove(getKeys()[1]);
    assertEquals(1, values.size());
  }

  @Override
  public boolean useNullKey() {
    return false;
  }

  protected Comparator<K> getComparator() {
    return comparator;
  }

  protected abstract Object getConflictingKey();

  protected abstract Object getConflictingValue();

  @Override
  protected void gwtSetUp() throws Exception {
    setComparator(null);
  }

  protected boolean isNaturalOrder() {
    return comparator == null;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Map makeEmptyMap() {
    return createTreeMap();
  }

  protected void setComparator(Comparator<K> comparator) {
    this.comparator = comparator;
  }

  @Override
  protected void verifyMap() {
    if (GWT.isScript()) {
      // Verify red-black correctness in our implementation
      TreeMapViolator.callAssertCorrectness(map);
    }
    super.verifyMap();
  }

  Map<K, V> createMap() {
    return createSortedMap();
  }

  SortedMap<K, V> createSortedMap() {
    SortedMap<K, V> map = createTreeMap();
    return map;
  }

  TreeMap<K, V> createTreeMap() {
    if (isNaturalOrder()) {
      return new TreeMap<K, V>();
    } else {
      return new TreeMap<K, V>(getComparator());
    }
  }

  abstract K getGreaterThanMaximumKey();

  abstract K[] getKeys();

  abstract K[] getKeys2();

  abstract K getLessThanMinimumKey();

  abstract V[] getValues();

  abstract V[] getValues2();
}
