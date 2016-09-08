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

import com.google.gwt.testing.TestUtils;

import org.apache.commons.collections.TestMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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

  private static class ConflictingKey implements Comparable<CharSequence> {
    private final String value;

    ConflictingKey(String value) {
      this.value = value;
    }

    @Override
    public int compareTo(CharSequence o) {
      return value.compareTo(o.toString());
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

    _assertEquals(expected.values(), actual.values());
    // TODO: equals is broken for collection returned by values() for submaps of TreeMap.
    // assertEquals(expected.values(), actual.values());
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

  private static <E> Collection<E> reverseCollection(Collection<E> c) {
    List<E> reversedCollection = new ArrayList<E>(c);
    Collections.reverse(reversedCollection);
    return reversedCollection;
  }

  /**
   * Verify entry to be immutable and to have correct values of {@code Map.Entry#toString()}
   * and {@code Map.Entry#hashCode()}.
   */
  @SuppressWarnings("unchecked")
  private static void verifyEntry(Entry entry) {
    try {
      entry.setValue(new Object());
      fail("should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    Object key = entry.getKey();
    Object value = entry.getValue();
    int expectedHashCode = (key == null ? 0 : key.hashCode())
        ^ (value == null ? 0 : value.hashCode());
    assertEquals(expectedHashCode, entry.hashCode());
    assertEquals(key + "=" + value, entry.toString());
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

  public void testCeilingEntry() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertEquals(keys[0], map.ceilingEntry(keys[0]).getKey());
    assertEquals(values[0], map.ceilingEntry(keys[0]).getValue());
    assertEquals(keys[0], map.ceilingEntry(getLessThanMinimumKey()).getKey());
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.ceilingEntry(getLessThanMinimumKey()).getKey());

    // test with two entry map
    map.put(keys[1], values[1]);
    assertEquals(keys[0], map.ceilingEntry(getLessThanMinimumKey()).getKey());
    Entry<K, V> entry = map.ceilingEntry(keys[0]);
    verifyEntry(entry);
    assertEquals(keys[0], entry.getKey());
    assertEquals(keys[1], map.ceilingEntry(keys[1]).getKey());
    assertEquals(values[1], map.ceilingEntry(keys[1]).getValue());
    assertNull(map.ceilingEntry(getGreaterThanMaximumKey()));
  }

  public void testCeilingKey() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertEquals(keys[0], map.ceilingKey(keys[0]));
    assertEquals(keys[0], map.ceilingKey(getLessThanMinimumKey()));
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.ceilingKey(getLessThanMinimumKey()));

    // test with two entry map
    map.put(keys[1], values[1]);
    assertEquals(keys[0], map.ceilingKey(getLessThanMinimumKey()));
    assertEquals(keys[0], map.ceilingKey(keys[0]));
    assertEquals(keys[1], map.ceilingKey(keys[1]));
    assertNull(map.ceilingKey(getGreaterThanMaximumKey()));

    try {
      map.ceilingKey(null);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }
    map.clear();
    assertNull(map.ceilingKey(keys[1]));
    assertNull(map.ceilingKey(null));
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
    SortedMap<K, V> sortedMap = createNavigableMap();
    if (isNaturalOrder()) {
      assertEquals(null, sortedMap.comparator());
    } else {
      assertEquals(getComparator(), sortedMap.comparator());
    }

    TreeMap<K, V> treeMap = new TreeMap<>();
    TreeMap<K, V> secondTreeMap = new TreeMap<>(treeMap);
    assertNull(treeMap.comparator());
    assertNull(secondTreeMap.comparator());

    treeMap = new TreeMap<>((Comparator<? super K>) null);
    secondTreeMap = new TreeMap<>(treeMap);
    assertNull(treeMap.comparator());
    assertNull(secondTreeMap.comparator());

    final Comparator<? super K> customComparator = new Comparator<K>() {
      @Override
      public int compare(K o1, K o2) {
        return o1.compareTo(o2);
      }
    };
    treeMap = new TreeMap<>(customComparator);
    secondTreeMap = new TreeMap<>(treeMap);
    assertSame(customComparator, treeMap.comparator());
    assertSame(customComparator, secondTreeMap.comparator());

    treeMap = new TreeMap<>(new HashMap<K, V>());
    secondTreeMap = new TreeMap<>(treeMap);
    assertNull(treeMap.comparator());
    assertNull(secondTreeMap.comparator());
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
    K[] keys = getKeys();
    V[] values = getValues();
    // The source map should be just a Map. Not a sorted map.
    Map<K, V> sourceMap = new HashMap<K, V>();

    // populate the source map
    sourceMap.put(keys[0], values[0]);
    sourceMap.put(keys[1], values[1]);
    sourceMap.put(keys[2], values[2]);

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
    }
  }

  /**
   * Test method for 'java.util.TreeMap.TreeMap(SortedMap)'.
   *
   * @see java.util.TreeMap#TreeMap(SortedMap)
   */
  public void testConstructor_SortedMap() {
    K[] keys = getKeys();
    V[] values = getValues();
    SortedMap<K, V> sourceMap = new TreeMap<K, V>();
    _assertEmpty(sourceMap);

    // populate the source map
    sourceMap.put(keys[0], values[0]);
    sourceMap.put(keys[1], values[1]);
    sourceMap.put(keys[2], values[2]);

    TreeMap<K, V> copyConstructed = new TreeMap<K, V>(sourceMap);
    _assertEquals(sourceMap, copyConstructed);

    Comparator<K> comp = Collections.reverseOrder(getComparator());
    TreeMap<K, V> reversedTreeMap = new TreeMap<K, V>(comp);
    reversedTreeMap.put(keys[0], values[0]);
    reversedTreeMap.put(keys[1], values[1]);
    TreeMap<K, V> anotherTreeMap = new TreeMap<K, V>(reversedTreeMap);
    assertTrue(anotherTreeMap.comparator() == comp);
    assertEquals(keys[1], anotherTreeMap.firstKey());
    assertEquals(keys[0], anotherTreeMap.lastKey());
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
    }
  }

  /**
   * Test method for 'java.util.Map.containsKey(Object)'. *
   *
   * @see java.util.Map#containsKey(Object)
   */
  public void testContainsKey() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    assertFalse(map.containsKey(keys[0]));
    assertNull(map.put(keys[0], values[0]));
    assertEquals(1, map.keySet().size());
    assertTrue(map.containsKey(keys[0]));
    assertFalse(map.containsKey(keys[1]));
    map.put(keys[1], values[1]);
    assertTrue(map.containsKey(keys[1]));
    assertFalse(map.containsKey(keys[3]));
  }

  public void testContainsKey_ComparableKey() {
    TreeMap<String, Object> map = new TreeMap<String, Object>();
    ConflictingKey conflictingKey = new ConflictingKey("conflictingKey");
    assertFalse(map.containsKey(conflictingKey));
    map.put("something", "value");
    assertFalse(map.containsKey(conflictingKey));
  }

  /**
   * Test method for 'java.util.Map.containsKey(Object)'.
   *
   * @see java.util.Map#containsKey(Object)
   */
  public void testContainsKey_throwsClassCastException() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    map.containsKey(getConflictingKey());

    map.put(keys[0], values[0]);
    try {
      map.containsKey(getConflictingKey());
      fail("ClassCastException expected");
    } catch (ClassCastException expected) {
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
      }
    }
  }

  /**
   * Test method for 'java.util.Map.containsValue(Object)'.
   *
   * @see java.util.Map#containsValue(Object)
   */
  @SuppressWarnings("SuspiciousMethodCalls")
  public void testContainsValue() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    assertFalse(map.containsValue(values[0]));
    map.put(keys[0], values[0]);
    assertEquals(1, map.values().size());
    assertTrue(map.containsValue(values[0]));
    assertFalse(map.containsValue(keys[0]));
    assertFalse(map.containsValue(values[1]));
    assertFalse(map.containsValue(null));
    map.put(keys[0], null);
    assertTrue(map.containsValue(null));
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    assertTrue(map.containsValue(values[1]));
    assertFalse(map.containsValue(values[3]));
  }

  /**
   * Test method for 'java.util.Map.containsValue(Object)'.
   *
   * @see java.util.Map#containsValue(Object)
   */
  public void testContainsValue_throwsClassCastException() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    map.put(keys[0], values[0]);
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

  public void testDescendingKeySet() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);

    NavigableSet<K> keySet = map.descendingKeySet();
    _assertEquals(keySet, map.descendingKeySet());

    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);
    _assertEquals(reverseCollection(keySet), keySet);
    _assertEquals(map.keySet(), keySet.descendingSet());
  }

  @SuppressWarnings("ModifyingCollectionWithItself")
  public void testDescendingKeySet_viewPut() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);

    Set<K> keySet = map.descendingKeySet();
    assertEquals(1, keySet.size());

    map.put(keys[1], values[1]);
    assertEquals(2, keySet.size());

    try {
      keySet.add(keys[2]);
      fail();
    } catch (Exception e) {
      // java.util.NavigableMap.navigableKeySet() does not support add
    }
    try {
      keySet.addAll(keySet);
      fail();
    } catch (Exception e) {
      // java.util.NavigableMap.navigableKeySet() does not support addAll
    }
  }

  public void testDescendingKeySet_viewRemove() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);

    Set<K> keySet = map.descendingKeySet();
    assertEquals(2, keySet.size());

    map.remove(keys[1]);
    assertEquals(1, keySet.size());

    map.put(keys[1], values[1]);
    keySet.remove(keys[0]);
    assertEquals(1, map.size());
    assertEquals(1, keySet.size());
    assertEquals(keys[1], keySet.iterator().next());

    keySet.clear();
    assertEquals(0, map.size());
    assertEquals(0, keySet.size());
  }

  @SuppressWarnings("unchecked")
  public void testDescendingKeySet_iterator() {
    NavigableMap<K, V> map = createNavigableMap();
    map.putAll(makeFullMap());
    resetFull();
    ArrayList<K> keys = new ArrayList<K>();
    for (Object key : getSampleKeys()) {
      keys.add((K) key);
    }

    // JDK < 7 does not handle null keys correctly.
    if (useNullKey() && TestUtils.isJvm() && TestUtils.getJdkVersion() < 7) {
      map.remove(null);
      keys.remove(null);
    }

    Comparator<? super K> cmp = ((TreeMap<K, V>) map).comparator();
    Collections.sort(keys, Collections.reverseOrder(cmp));
    Iterator<K> it = map.descendingKeySet().iterator();
    for (K key : keys) {
      assertTrue(it.hasNext());
      K rem = it.next();
      it.remove();
      assertEquals(key, rem);
    }
    try {
      it.next();
      fail("should throw NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }
    _assertEmpty(map);
  }

  public void testDescendingMap() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);

    NavigableMap<K, V> descendingMap = map.descendingMap();
    _assertEquals(descendingMap, map.descendingMap());

    map.put(keys[1], values[1]);
    _assertEquals(map, descendingMap.descendingMap());
    _assertEquals(reverseCollection(map.entrySet()), descendingMap.entrySet());

    descendingMap.put(keys[2], values[2]);
    _assertEquals(reverseCollection(map.entrySet()), descendingMap.entrySet());
    _assertEquals(map.entrySet(), descendingMap.descendingMap().entrySet());

    descendingMap.remove(keys[1]);
    _assertEquals(reverseCollection(map.entrySet()), descendingMap.entrySet());

    descendingMap.clear();
    assertEquals(0, descendingMap.size());
    assertEquals(0, map.size());

    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);
    assertEquals(3, descendingMap.size());

    NavigableMap<K, V> headMap = descendingMap.headMap(keys[1], false);
    assertEquals(1, headMap.size());
    assertTrue(headMap.containsKey(keys[2]));

    NavigableMap<K, V> subMap = descendingMap.subMap(keys[2], true, keys[1], true);
    assertEquals(2, subMap.size());
    assertTrue(subMap.containsKey(keys[1]));
    assertTrue(subMap.containsKey(keys[2]));

    NavigableMap<K, V> tailMap = descendingMap.tailMap(keys[1], false);
    assertEquals(1, tailMap.size());
    assertTrue(tailMap.containsKey(keys[0]));
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
        @Override
        public K getKey() {
          return null;
        }

        @Override
        public V getValue() {
          return null;
        }

        @Override
        public V setValue(V value) {
          return null;
        }
      });
      fail("expected exception");
    } catch (UnsupportedOperationException e) {
      // expected outcome
    }
  }

  public void testEntrySet() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);

    Set<Map.Entry<K, V>> entries = map.entrySet();
    Iterator<Map.Entry<K, V>> entrySetIterator = entries.iterator();
    assertEquals(3, entries.size());
    assertEquals(keys[0] + "=" + values[0], entrySetIterator.next().toString());
    while (entrySetIterator.hasNext()) {
      Map.Entry<K, V> entry = entrySetIterator.next();
      assertTrue(map.get(entry.getKey()) == entry.getValue());
    }

    assertEquals(map.size(), entries.size());
    _assertEquals(entries, map.entrySet());
    map.clear();
    assertEquals(map.size(), entries.size());
    _assertEquals(entries, map.entrySet());
    map.put(keys[0], values[0]);
    assertEquals(map.size(), entries.size());
    _assertEquals(entries, map.entrySet());
    entries.clear();
    assertEquals(map.size(), entries.size());
    _assertEquals(entries, map.entrySet());

    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);
    Iterator<Entry<K, V>> it = entries.iterator();
    while (it.hasNext()) {
      Map.Entry<K, V> entry = it.next();
      map.containsKey(entry.getKey());
      map.containsValue(entry.getValue());
      it.remove();
    }
    try {
      it.next();
      fail("should throw NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }
    _assertEmpty(map);
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  public void testEntrySet_contains() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> master = createNavigableMap();
    NavigableMap<K, V> testMap = createNavigableMap();

    master.put(keys[0], null);
    Object[] entry = master.entrySet().toArray();
    assertFalse(testMap.entrySet().contains(entry[0]));

    Map<K, V> submap = testMap.subMap(keys[2], keys[3]);
    entry = master.entrySet().toArray();
    assertFalse(submap.entrySet().contains(entry[0]));

    testMap.put(keys[0], null);
    assertTrue(testMap.entrySet().containsAll(master.entrySet()));

    master.clear();
    master.put(keys[0], values[0]);
    entry = master.entrySet().toArray();
    assertFalse(testMap.entrySet().contains(entry[0]));
  }

  /**
   * Test method for 'java.util.Map.entrySet()'.
   *
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_entries() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();

    Set<Entry<K, V>> entrySet = map.entrySet();
    _assertEmpty(entrySet);
    _assertEquals(entrySet, map.entrySet());

    map.put(keys[0], values[0]);

    // Verify the view correctly represents the map
    assertNotNull(entrySet);
    Iterator<Entry<K, V>> iter = entrySet.iterator();
    assertNotNull(iter);
    assertTrue(iter.hasNext());
    Entry<K, V> entry = iter.next();
    assertNotNull(entry);

    assertEquals(entry.getKey(), keys[0]);
    assertEquals(entry.getValue(), values[0]);
    // Don't use assertEquals; we want to be clear about which object's equals()
    // method to test.
    assertEquals(entry, new SimpleEntry<K, V>(keys[0], values[0]));
    _assertEquals(entrySet, map.entrySet());
  }

  /**
   * Test method for 'java.util.Map.entrySet()'.
   *
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_entries_view() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    // Get a view of the entry set before modifying the underlying map.
    Set<Entry<K, V>> entrySet = map.entrySet();
    map.put(keys[0], values[0]);

    // Verify that the entries view reflects updates to the map.
    assertEquals(entrySet.iterator().next().getKey(), keys[0]);
    assertEquals(entrySet.iterator().next().getValue(), values[0]);
    _assertEquals(entrySet, map.entrySet());

    map.put(keys[0], values[1]); // overwrite the value

    // Verify that the entries view reflects updates to the map.
    assertEquals(entrySet.iterator().next().getKey(), keys[0]);
    assertEquals(entrySet.iterator().next().getValue(), values[1]);

    // Verify that the entries view is updated on removes to the map.
    map.remove(keys[0]);
    _assertEmpty(entrySet);
    _assertEquals(entrySet, map.entrySet());
  }

  public void testEntrySet_entry_setValue() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    Set<Entry<K, V>> entrySet = map.entrySet();
    map.put(keys[0], values[0]);
    entrySet.iterator().next().setValue(values[1]);
    assertTrue(map.containsValue(values[1]));
    _assertEquals(entrySet, map.entrySet());
  }

  /**
   * Test method for 'java.util.Map.entrySet().remove(Object)'.
   *
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_remove() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    map.put(keys[0], values[0]);

    Set<Entry<K, V>> entrySet = map.entrySet();
    assertTrue(entrySet.remove(entrySet.iterator().next()));
    assertTrue(entrySet.isEmpty());
    assertEquals(map.size(), entrySet.size());
    _assertEquals(entrySet, map.entrySet());
  }

  /**
   * Test method for 'java.util.Map.entrySet().remove(Object)'.
   *
   * @see java.util.Map#entrySet()
   */
  public void testEntrySet_remove_equivalentEntry() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map0 = createMap();
    map0.put(keys[0], values[0]);

    Map<K, V> map1 = createMap();
    map1.put(keys[0], values[1]);

    // Verify attempting to remove an equivalent entry from a different map has
    // no effect.
    Set<Entry<K, V>> entrySet0 = map0.entrySet();
    assertFalse(entrySet0.remove(map1.entrySet().iterator().next()));
    assertFalse(entrySet0.isEmpty());
    assertEquals(entrySet0.size(), map0.size());
    _assertEquals(entrySet0, map0.entrySet());
  }

  /**
   * Test method for 'java.util.Object.equals(Object)'.
   *
   * @see java.util.Map#equals(Object)
   */
  public void testEquals() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map0 = createMap();
    Map<K, V> map1 = createMap();
    assertTrue(map0.equals(map1));
    map0.put(keys[0], values[0]);
    map1.put(keys[0], values[0]);
    assertTrue(map0.equals(map0));
    assertTrue(map0.equals(map1));
    map0.put(keys[1], values[1]);
    assertFalse(map0.equals(map1));
  }

  public void testFirstEntry() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertEquals(keys[0], map.firstEntry().getKey());
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.firstEntry().getKey());
    assertEquals(keys[0], map.lastEntry().getKey());
    assertEquals(map.lastEntry().getKey(), map.firstEntry().getKey());

    // test with two entry map
    map.put(keys[1], values[1]);
    Entry<K, V> entry = map.firstEntry();
    verifyEntry(entry);
    assertEquals(keys[0], entry.getKey());
    assertFalse(keys[1].equals(map.firstEntry().getKey()));
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.firstEntry().getKey());
    assertFalse(keys[0].equals(map.lastEntry().getKey()));
    assertFalse(map.lastEntry().getKey().equals(map.firstEntry().getKey()));

    map.clear();
    assertNull(map.firstEntry());
  }

  /**
   * Test method for 'java.util.SortedMap.firstKey()'.
   *
   * @see java.util.SortedMap#firstKey()
   */
  public void testFirstKey() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    SortedMap<K, V> map = createNavigableMap();
    // test with a single entry map

    map.put(keys[0], values[0]);
    assertEquals(keys[0], map.firstKey());
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.firstKey());
    assertEquals(keys[0], map.lastKey());
    assertEquals(map.lastKey(), map.firstKey());

    // test with two entry map
    map.put(keys[1], values[1]);
    assertEquals(keys[0], map.firstKey());
    assertFalse(keys[1].equals(map.firstKey()));
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.firstKey());
    assertFalse(keys[0].equals(map.lastKey()));
    assertFalse(map.lastKey().equals(map.firstKey()));

    map.put(keys[2], values[2]);
    map.put(keys[3], values[3]);
    assertEquals(keys[0], map.firstKey());
  }

  /**
   * Test method for 'java.util.SortedMap.firstKey()'.
   *
   * @see java.util.SortedMap#firstKey()
   */
  public void testFirstKey_throwsNoSuchElementException() {
    SortedMap<K, V> sortedMap = createNavigableMap();
    // test with no entries
    try {
      sortedMap.firstKey();
      fail("expected exception");
    } catch (NoSuchElementException e) {
      // expected outcome
    }
  }

  public void testFloorEntry() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertNull(map.floorEntry(getLessThanMinimumKey()));
    assertEquals(keys[0], map.floorEntry(keys[0]).getKey());
    assertEquals(values[0], map.floorEntry(keys[0]).getValue());
    assertEquals(keys[0], map.floorEntry(keys[1]).getKey());
    assertEquals(values[0], map.floorEntry(keys[1]).getValue());
    assertEquals(keys[0], map.floorEntry(getGreaterThanMaximumKey()).getKey());
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.floorEntry(keys[1]).getKey());

    // test with two entry map
    map.put(keys[1], values[1]);
    assertNull(map.floorEntry(getLessThanMinimumKey()));
    assertEquals(keys[0], map.floorEntry(keys[0]).getKey());
    Entry<K, V> entry = map.floorEntry(keys[1]);
    verifyEntry(entry);
    assertEquals(keys[1], entry.getKey());
    assertEquals(values[1], entry.getValue());
    assertEquals(keys[1], map.floorEntry(getGreaterThanMaximumKey()).getKey());

    try {
      map.floorEntry(null);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }
    map.clear();
    assertNull(map.floorEntry(keys[1]));
    assertNull(map.floorEntry(null));
  }

  public void testFloorKey() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertNull(map.floorKey(getLessThanMinimumKey()));
    assertEquals(keys[0], map.floorKey(keys[0]));
    assertEquals(keys[0], map.floorKey(keys[1]));
    assertEquals(keys[0], map.floorKey(getGreaterThanMaximumKey()));
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.floorKey(keys[1]));

    // test with two entry map
    map.put(keys[1], values[1]);
    assertNull(map.floorKey(getLessThanMinimumKey()));
    assertEquals(keys[0], map.floorKey(keys[0]));
    assertEquals(keys[1], map.floorKey(keys[1]));
    assertEquals(keys[1], map.floorKey(getGreaterThanMaximumKey()));

    try {
      map.floorKey(null);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }

    map.clear();
    assertNull(map.floorKey(keys[1]));
    assertNull(map.floorKey(null));
  }

  /**
   * Test method for 'java.util.Map.get(Object)'.
   *
   * @see java.util.Map#get(Object)
   */
  public void testGet() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    if (useNullKey()) {
      assertNull(map.get(null));
    }
    assertNull(map.get(keys[0]));
    assertNull(map.put(keys[0], values[0]));
    assertSame(values[0], map.get(keys[0]));
    map.put(keys[1], values[1]);
    assertEquals(2, map.size());
    assertEquals(2, map.values().size());
    assertEquals(2, map.keySet().size());
    assertSame(values[1], map.get(keys[1]));
    assertSame(values[1], map.put(keys[1], values[2]));
  }

  public void testGet_ComparableKey() {
    TreeMap<String, Object> map = new TreeMap<String, Object>();
    ConflictingKey conflictingKey = new ConflictingKey("conflictingKey");
    assertNull(map.get(conflictingKey));
    map.put("something", "value");
    assertNull(map.get(conflictingKey));
  }

  /**
   * Test method for 'java.util.Map.get(Object)'.
   *
   * @see java.util.Map#get(Object)
   */
  public void testGet_throwsClassCastException() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    map.get(getConflictingKey());

    map.put(keys[0], values[0]);
    try {
      map.get(getConflictingKey());
      fail("ClassCastException expected");
    } catch (ClassCastException expected) {
    }
  }

  /**
   * Test method for 'java.util.Map.get(Object)'.
   *
   * @see java.util.Map#get(Object)
   */
  public void testGet_throwsNullPointerException() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();

    try {
      map.get(null);
      // JDK < 7 does not conform to the specification if the map is empty.
      if (TestUtils.getJdkVersion() > 6) {
        assertTrue(useNullKey());
      }
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }

    map.put(keys[0], values[0]);

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
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map0 = createMap();
    Map<K, V> map1 = createMap();

    int hashCode0 = map0.hashCode();
    int hashCode1 = map1.hashCode();
    assertTrue("empty maps have different hash codes", hashCode0 == hashCode1);

    // Check that hashCode changes
    map0.put(keys[0], values[0]);
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
   * Test method for 'java.util.SortedMap.headMap(Object)' and
   * 'java.util.NavigableMap.headMap(Object, boolean)'.
   *
   * @see java.util.SortedMap#headMap(Object)
   * @see java.util.NavigableMap#headMap(Object, boolean)
   */
  public void testHeadMap() {
    // test with no entries
    K[] keys = getSortedKeys();
    NavigableMap<K, V> map = createNavigableMap();
    assertNotNull(map.headMap(keys[0]));
    assertNotNull(map.headMap(keys[0], false));
    assertNotNull(map.headMap(keys[0], true));
  }

  public void testHeadMapLjava_lang_Object() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], values[i]);
    }

    Map<K, V> head = map.headMap(keys[3]);
    assertEquals(3, head.size());
    assertTrue(head.containsKey(keys[0]));
    assertTrue(head.containsValue(values[1]));
    assertTrue(head.containsKey(keys[2]));

    if (useNullKey() && useNullValue()) {
      map.put(null, null);

      SortedMap<K, V> submap = map.headMap(null);
      assertEquals(0, submap.size());

      Set<K> keySet = submap.keySet();
      assertEquals(0, keySet.size());

      Set<Map.Entry<K, V>> entrySet = submap.entrySet();
      assertEquals(0, entrySet.size());

      Collection<V> valueCollection = submap.values();
      assertEquals(0, valueCollection.size());

      map.remove(null);
    }

    SortedMap<K, V> submap = map.headMap(getLessThanMinimumKey());
    assertEquals(submap.size(), 0);
    assertTrue(submap.isEmpty());
    try {
      submap.firstKey();
      fail("NoSuchElementException should be thrown");
    } catch (NoSuchElementException expected) {
    }

    try {
      submap.lastKey();
      fail("NoSuchElementException should be thrown");
    } catch (NoSuchElementException expected) {
    }

    try {
      submap.headMap(null);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }
  }

  public void testHeadMapLjava_lang_ObjectZL() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], values[i]);
    }

    // normal case
    SortedMap<K, V> subMap = map.headMap(keys[2], true);
    assertEquals(3, subMap.size());
    subMap = map.headMap(keys[3], true);
    assertEquals(4, subMap.size());
    for (int i = 0; i < 4; i++) {
      assertEquals(values[i], subMap.get(keys[i]));
    }
    subMap = map.headMap(keys[2], false);
    assertEquals(2, subMap.size());
    assertNull(subMap.get(keys[3]));

    // Exceptions
    assertEquals(0, map.headMap(keys[0], false).size());

    try {
      map.headMap(null, true);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }

    try {
      map.headMap(null, false);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }

    subMap = map.headMap(keys[2]);
    assertEquals(2, subMap.size());
    try {
      subMap.put(keys[2], values[2]);
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(keys.length, map.size());
    subMap = map.headMap(keys[2], true);
    assertEquals(3, subMap.size());
    subMap.remove(keys[1]);
    assertFalse(subMap.containsKey(keys[1]));
    assertFalse(subMap.containsValue(values[1]));
    assertFalse(map.containsKey(keys[1]));
    assertFalse(map.containsValue(values[1]));
    assertEquals(2, subMap.size());
    assertEquals(keys.length - 1, map.size());

    subMap.put(keys[1], values[1]);

    try {
      subMap.subMap(keys[1], keys[3]);
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    try {
      subMap.subMap(keys[3], keys[1]);
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    if (useNullKey() && useNullValue()) {
      map.put(null, null);

      subMap = map.headMap(null, true);
      assertEquals(1, subMap.size());
      assertTrue(subMap.containsValue(null));
      assertNull(subMap.get(null));

      subMap = map.subMap(null, false, keys[2], true);
      assertEquals(3, subMap.size());

      Set<K> keySet = subMap.keySet();
      assertEquals(3, keySet.size());

      Set<Map.Entry<K, V>> entrySet = subMap.entrySet();
      assertEquals(3, entrySet.size());

      Collection<V> valueCollection = subMap.values();
      assertEquals(3, valueCollection.size());

      map.remove(null);
    }

    // head map of head map
    NavigableMap<K, V> headMap = map.headMap(keys[3], true);
    assertEquals(4, headMap.size());
    headMap = headMap.headMap(keys[3], false);
    assertEquals(3, headMap.size());
    headMap = headMap.headMap(keys[2], false);
    assertEquals(2, headMap.size());
    headMap = headMap.tailMap(keys[0], false);
    assertEquals(1, headMap.size());
    headMap = headMap.tailMap(keys[1], false);
    assertEquals(0, headMap.size());
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object)' and
   * 'java.util.NavigableMap.headMap(Object, boolean)'.
   *
   * @see java.util.SortedMap#headMap(Object)
   * @see java.util.NavigableMap#headMap(Object, boolean)
   */
  public void testHeadMap_entries_size() {
    // test with no entries
    K[] keys = getSortedKeys();
    assertEquals(0, createNavigableMap().headMap(keys[0]).size());

    NavigableMap<K, V> exclusiveHeadMap = createNavigableMap().headMap(keys[0], false);
    assertEquals(0, exclusiveHeadMap.size());
    assertNull(exclusiveHeadMap.firstEntry());
    assertNull(exclusiveHeadMap.lastEntry());
    try {
      assertNull(exclusiveHeadMap.firstKey());
      fail();
    } catch (NoSuchElementException e) {
      // expected outcome
    }
    try {
      assertNull(exclusiveHeadMap.lastKey());
      fail();
    } catch (NoSuchElementException e) {
      // expected outcome
    }

    NavigableMap<K, V> inclusiveHeadMap = createNavigableMap().headMap(keys[0], true);
    assertEquals(0, inclusiveHeadMap.size());
    assertNull(inclusiveHeadMap.firstEntry());
    assertNull(inclusiveHeadMap.lastEntry());
    try {
      assertNull(inclusiveHeadMap.firstKey());
      fail();
    } catch (NoSuchElementException e) {
      // expected outcome
    }
    try {
      assertNull(inclusiveHeadMap.lastKey());
      fail();
    } catch (NoSuchElementException e) {
      // expected outcome
    }
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object)' and
   * 'java.util.NavigableMap.headMap(Object, boolean)'.
   *
   * @see java.util.SortedMap#headMap(Object)
   * @see java.util.NavigableMap#headMap(Object, boolean)
   */
  public void testHeadMap_entries() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);

    assertEquals(0, map.headMap(keys[0]).size());
    assertEquals(0, map.headMap(keys[0], false).size());
    assertEquals(1, map.headMap(keys[0], true).size());

    // test with two entry map
    map.put(keys[1], values[1]);

    assertEquals(0, map.headMap(keys[0]).size());
    assertEquals(1, map.headMap(keys[1]).size());
    assertEquals(keys[0], map.tailMap(keys[0]).keySet().toArray()[0]);

    assertEquals(0, map.headMap(keys[0], false).size());
    assertEquals(1, map.headMap(keys[1], false).size());
    assertEquals(keys[0], map.headMap(keys[0], true).keySet().toArray()[0]);

    assertEquals(1, map.headMap(keys[0], true).size());
    assertEquals(2, map.headMap(keys[1], true).size());
    assertEquals(keys[0], map.headMap(keys[1], false).keySet().toArray()[0]);
    assertEquals(keys[1], map.headMap(keys[1], true).keySet().toArray()[1]);
    assertEquals(0, map.headMap(keys[0], false).keySet().size());
    assertEquals(keys[1], map.headMap(keys[1], true).keySet().toArray()[1]);
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object, Object)'.
   *
   * @see java.util.SortedMap#headMap(Object)
   */
  @SuppressWarnings("unchecked")
  public void testHeadMap_throwsClassCastException() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    SortedMap sortedMap = createNavigableMap();
    if (isNaturalOrder()) {
      // TODO Why does this succeed with natural ordering when subMap doesn't?
      sortedMap.headMap(getConflictingKey());
    } else {
      try {
        sortedMap.headMap(getConflictingKey());
        fail("ClassCastException expected");
      } catch (ClassCastException expected) {
      }
    }

    sortedMap.put(keys[0], values[0]);
    if (isNaturalOrder()) {
      // TODO Why does this succeed with natural ordering when subMap doesn't?
      sortedMap.headMap(getConflictingKey());
    } else {
      try {
        sortedMap.headMap(getConflictingKey());
        fail("ClassCastException expected");
      } catch (ClassCastException expected) {
      }
    }
  }

  /**
   * Test method for 'java.util.SortedMap.headMap(Object, Object)'.
   *
   * @see java.util.SortedMap#headMap(Object)
   */
  public void testHeadMap_throwsNullPointerException() {
    SortedMap<K, V> sortedMap = createNavigableMap();
    try {
      sortedMap.headMap(null);
      assertTrue(useNullKey());
    } catch (NullPointerException e) {
      assertFalse(useNullKey());
    }
  }

  public void testHeadMap_viewPutRemove() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);
    map.put(keys[2], values[2]);
    map.put(keys[3], values[3]);

    NavigableMap<K, V> headMap = map.headMap(keys[2], true);
    try {
      headMap.put(keys[3], values[3]);
      fail();
    } catch (IllegalArgumentException e) {
      // must not insert value outside the range
    }
    headMap.remove(keys[3]);
    assertEquals(2, headMap.size());
    assertEquals(3, map.size());
    assertTrue(map.containsKey(keys[3]));

    headMap.put(keys[1], values[1]);
    assertEquals(3, headMap.size());
    assertEquals(4, map.size());
    assertTrue(map.containsKey(keys[1]));
    assertTrue(headMap.containsKey(keys[1]));

    headMap.remove(keys[1]);
    assertFalse(map.containsKey(keys[1]));
    assertFalse(headMap.containsKey(keys[1]));

    headMap.clear();
    assertEquals(0, headMap.size());
    assertEquals(1, map.size());
    assertTrue(map.containsKey(keys[3]));
  }

  public void testHigherEntry() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertNull(map.higherEntry(keys[0]));
    assertEquals(keys[0], map.higherEntry(getLessThanMinimumKey()).getKey());
    assertEquals(values[0], map.higherEntry(getLessThanMinimumKey()).getValue());
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.higherEntry(getLessThanMinimumKey()).getKey());

    // test with two entry map
    map.put(keys[1], values[1]);
    assertEquals(keys[0], map.higherEntry(getLessThanMinimumKey()).getKey());
    Entry<K, V> entry = map.higherEntry(keys[0]);
    verifyEntry(entry);
    assertEquals(keys[1], entry.getKey());
    assertEquals(values[1], entry.getValue());
    assertNull(map.higherEntry(keys[1]));
    assertNull(map.higherEntry(getGreaterThanMaximumKey()));

    try {
      map.higherEntry(null);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }
    map.clear();
    assertNull(map.higherEntry(keys[1]));
    assertNull(map.higherEntry(null));
  }

  public void testHigherKey() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertEquals(null, map.higherKey(keys[0]));
    assertEquals(keys[0], map.higherKey(getLessThanMinimumKey()));
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.higherKey(getLessThanMinimumKey()));

    // test with two entry map
    map.put(keys[1], values[1]);
    assertEquals(keys[0], map.higherKey(getLessThanMinimumKey()));
    assertEquals(keys[1], map.higherKey(keys[0]));
    assertNull(map.higherKey(keys[1]));
    assertNull(map.higherKey(getGreaterThanMaximumKey()));

    try {
      map.higherKey(null);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }
    map.clear();
    assertNull(map.higherKey(keys[1]));
    assertNull(map.higherKey(null));
  }

  /**
   * Test method for 'java.util.Map.isEmpty()'.
   *
   * @see java.util.Map#isEmpty()
   *
   */
  public void testIsEmpty() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> sourceMap = createMap();
    Map<K, V> destMap = createMap();

    destMap.putAll(sourceMap);
    assertTrue(destMap.isEmpty());

    destMap.put(keys[0], values[0]);
    assertFalse(destMap.isEmpty());

    destMap.remove(keys[0]);
    assertTrue(destMap.isEmpty());
    assertEquals(destMap.size(), 0);
  }

  /**
   * Test method for 'java.util.Map.keySet()'.
   *
   * @see java.util.Map#clear()
   */
  public void testKeySet() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);
    Set<K> keySet = map.keySet();
    _assertEquals(keySet, map.keySet());
    assertEquals(map.size(), keySet.size());
    for (int i = 0; i <= 2; i++) {
      K key = keys[i];
      assertTrue(keySet.contains(key));
    }
  }

  /**
   * Test method for 'java.util.Map.keySet()'.
   *
   * @see java.util.Map#clear()
   */
  public void testKeySet_viewPut() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    map.put(keys[0], values[0]);
    Set<K> keySet = map.keySet();
    assertEquals(1, keySet.size());
    map.put(keys[1], values[1]);
    assertEquals(2, keySet.size());
  }

  /**
   * Test method for 'java.util.Map.keySet()'.
   *
   * @see java.util.Map#clear()
   */
  public void testKeySet_viewRemove() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    Set<K> keySet = map.keySet();
    assertEquals(2, keySet.size());
    map.remove(keys[1]);
    assertEquals(1, keySet.size());
  }

  @SuppressWarnings("unchecked")
  public void testKeySet_iterator() {
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
    try {
      it.next();
      fail("should throw NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }
    _assertEmpty(map);
  }

  public void testLastEntry() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertEquals(keys[0], map.lastEntry().getKey());
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.lastEntry().getKey());
    assertEquals(keys[0], map.firstEntry().getKey());
    assertEquals(values[0], map.firstEntry().getValue());
    assertEquals(map.firstEntry().getKey(), map.lastEntry().getKey());

    // test with two entry map
    map.put(keys[1], values[1]);
    assertEquals(keys[1], map.lastEntry().getKey());
    assertFalse(keys[0].equals(map.lastEntry().getKey()));
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[1], map.lastEntry().getKey());
    Entry<K, V> entry = map.firstEntry();
    verifyEntry(entry);
    assertEquals(keys[0], entry.getKey());
    assertFalse(map.firstEntry().getKey().equals(map.lastEntry().getKey()));

    map.clear();
    assertNull(map.lastEntry());
  }

  /**
   * Test method for 'java.util.SortedMap.lastKey()'.
   *
   * @see java.util.SortedMap#lastKey()
   */
  public void testLastKey() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    SortedMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertEquals(keys[0], map.lastKey());
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.lastKey());
    assertEquals(keys[0], map.firstKey());
    assertEquals(map.firstKey(), map.lastKey());

    // test with two entry map
    map.put(keys[1], values[1]);
    assertEquals(keys[1], map.lastKey());
    assertFalse(keys[0].equals(map.lastKey()));
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[1], map.lastKey());
    assertEquals(keys[0], map.firstKey());
    assertFalse(map.firstKey().equals(map.lastKey()));

    map.put(keys[2], values[2]);
    map.put(keys[3], values[3]);
    assertEquals(keys[0], map.headMap(keys[1]).lastKey());
    assertEquals(keys[keys.length - 1], map.tailMap(keys[2]).lastKey());
    assertEquals(keys[2], map.subMap(keys[1], keys[3]).lastKey());
  }

  public void testLastKey_after_subMap() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);

    SortedMap<K, V> subMap = map;
    K firstKey = subMap.firstKey();
    for (int i = 0; i < map.size(); i++) {
      K lastKey = subMap.lastKey();
      subMap = subMap.subMap(firstKey, lastKey);
    }
  }

  /**
   * Test method for 'java.util.SortedMap.lastKey()'.
   *
   * @see java.util.SortedMap#lastKey()
   */
  public void testLastKey_throwsNoSuchElementException() {
    SortedMap<K, V> sortedMap = createNavigableMap();
    // test with no entries
    try {
      sortedMap.lastKey();
      fail("expected exception");
    } catch (NoSuchElementException e) {
      // expected outcome
    }
  }

  public void testLowerEntry() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertNull(map.lowerEntry(getLessThanMinimumKey()));
    assertNull(map.lowerEntry(keys[0]));
    assertEquals(keys[0], map.lowerEntry(keys[1]).getKey());
    assertEquals(values[0], map.lowerEntry(keys[1]).getValue());
    assertEquals(keys[0], map.lowerEntry(getGreaterThanMaximumKey()).getKey());
    assertEquals(values[0], map.lowerEntry(getGreaterThanMaximumKey()).getValue());
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.lowerEntry(keys[1]).getKey());

    // test with two entry map
    map.put(keys[1], values[1]);
    assertNull(map.lowerEntry(getLessThanMinimumKey()));
    assertNull(map.lowerEntry(keys[0]));
    assertEquals(values[0], map.lowerEntry(keys[1]).getValue());
    Entry<K, V> entry = map.lowerEntry(getGreaterThanMaximumKey());
    verifyEntry(entry);
    assertEquals(keys[1], entry.getKey());

    try {
      map.lowerEntry(null);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }
    map.clear();
    assertNull(map.lowerEntry(keys[1]));
    assertNull(map.lowerEntry(null));
  }

  public void testLowerKey() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);
    assertNull(map.lowerKey(getLessThanMinimumKey()));
    assertNull(map.lowerKey(keys[0]));
    assertEquals(keys[0], map.lowerKey(keys[1]));
    assertEquals(keys[0], map.lowerKey(getGreaterThanMaximumKey()));
    // is it consistent with other methods
    assertEquals(map.keySet().toArray()[0], map.lowerKey(keys[1]));

    // test with two entry map
    map.put(keys[1], values[1]);
    assertNull(map.lowerKey(getLessThanMinimumKey()));
    assertNull(map.lowerKey(keys[0]));
    assertEquals(keys[0], map.lowerKey(keys[1]));
    assertEquals(keys[1], map.lowerKey(getGreaterThanMaximumKey()));

    try {
      map.lowerKey(null);
      assertTrue("expected exception", useNullKey());
    } catch (NullPointerException e) {
      assertFalse("unexpected NPE", useNullKey());
    }
    map.clear();
    assertNull(map.lowerKey(keys[1]));
    assertNull(map.lowerKey(null));
  }

  public void testNavigableKeySet() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);

    Set<K> keySet = map.navigableKeySet();
    _assertEquals(keySet, map.navigableKeySet());

    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);
    _assertEquals(map.navigableKeySet(), keySet);
    _assertEquals(keySet, keySet);

    try {
      keySet.add(keys[3]);
      fail("should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      keySet.add(null);
      fail("should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      keySet.addAll(null);
      fail("should throw NullPointerException");
    } catch (NullPointerException expected) {
    }
    Collection<K> collection = new ArrayList<K>();
    keySet.addAll(collection);
    try {
      collection.add(keys[3]);
      keySet.addAll(collection);
      fail("should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }

    Iterator<K> iter = keySet.iterator();
    iter.next();
    iter.remove();
    assertFalse(map.containsKey(keys[0]));

    collection = new ArrayList<K>();
    collection.add(keys[2]);
    keySet.retainAll(collection);
    assertEquals(1, map.size());
    assertTrue(keySet.contains(keys[2]));

    keySet.removeAll(collection);
    _assertEmpty(map);

    map.put(keys[0], values[0]);
    assertEquals(1, map.size());
    assertTrue(keySet.contains(keys[0]));

    keySet.clear();
    _assertEmpty(map);
  }

  @SuppressWarnings("ModifyingCollectionWithItself")
  public void testNavigableKeySet_viewPut() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);

    Set<K> keySet = map.navigableKeySet();
    assertEquals(1, keySet.size());
    map.put(keys[1], values[1]);
    assertEquals(2, keySet.size());

    try {
      keySet.add(keys[2]);
      fail();
    } catch (Exception e) {
      // java.util.NavigableMap.navigableKeySet() does not support add
    }
    try {
      keySet.addAll(keySet);
      fail();
    } catch (Exception e) {
      // java.util.NavigableMap.navigableKeySet() does not support addAll
    }
  }

  public void testNavigableKeySet_viewRemove() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);

    Set<K> keySet = map.navigableKeySet();
    assertEquals(2, keySet.size());
    map.remove(keys[1]);
    assertEquals(1, keySet.size());

    map.put(keys[1], values[1]);
    keySet.remove(keys[0]);
    assertEquals(1, map.size());
    assertEquals(1, keySet.size());
    assertEquals(keys[1], keySet.iterator().next());

    keySet.clear();
    _assertEmpty(map);
  }

  @SuppressWarnings("unchecked")
  public void testNavigableKeySet_iterator() {
    NavigableMap<K, V> map = createNavigableMap();
    map.putAll(makeFullMap());
    resetFull();
    ArrayList<K> keys = new ArrayList<K>();
    for (Object key : getSampleKeys()) {
      keys.add((K) key);
    }
    Comparator<? super K> cmp = ((TreeMap<K, V>) map).comparator();
    Collections.sort(keys, cmp);
    Iterator<K> it = map.navigableKeySet().iterator();
    for (K key : keys) {
      assertTrue(it.hasNext());
      K rem = it.next();
      it.remove();
      assertEquals(key, rem);
    }
    try {
      it.next();
      fail("should throw NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }
    _assertEmpty(map);
  }

  public void testPollFirstEntry() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    assertNull(map.pollFirstEntry());
    assertEquals(0, map.size());

    map.put(keys[0], values[0]);
    assertEquals(keys[0], map.pollFirstEntry().getKey());
    assertEquals(0, map.size());

    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    assertEquals(keys[0], map.pollFirstEntry().getKey());
    assertEquals(1, map.size());
    Entry<K, V> entry = map.pollFirstEntry();
    verifyEntry(entry);
    assertEquals(keys[1], entry.getKey());
    assertEquals(0, map.size());
    assertNull(map.pollFirstEntry());
  }

  public void testPollLastEntry() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    assertNull(map.pollLastEntry());
    assertEquals(0, map.size());

    map.put(keys[0], values[0]);
    assertEquals(keys[0], map.pollLastEntry().getKey());
    assertEquals(0, map.size());

    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    assertEquals(keys[1], map.pollLastEntry().getKey());
    assertEquals(1, map.size());
    Entry<K, V> entry = map.pollLastEntry();
    verifyEntry(entry);
    assertEquals(keys[0], entry.getKey());
    assertEquals(0, map.size());
    assertNull(map.pollLastEntry());
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> map = createMap();
      assertNull(map.put(keys[0], values[0]));
      assertFalse(map.isEmpty());
      assertEquals(1, map.size());
    }
  }

  public void testPutLjava_lang_ObjectLjava_lang_Object() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    assertNull(map.put(keys[0], values[0]));
    assertTrue(map.get(keys[0]) == values[0]);
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> map = createMap();
      map.put(keys[0], values[0]);
      map.put(keys[1], values[1]);
      map.put(keys[2], values[2]);

      // test contents
      assertFalse(map.isEmpty());
      assertEquals(3, map.size());
      // test contains all values
      Collection<V> mapValues = map.values();
      assertTrue(mapValues.contains(values[0]));
      assertTrue(mapValues.contains(values[1]));
      assertTrue(mapValues.contains(values[2]));
      // test contains all keys
      Collection<K> mapKeys = map.keySet();
      assertTrue(mapKeys.contains(keys[0]));
      assertTrue(mapKeys.contains(keys[1]));
      assertTrue(mapKeys.contains(keys[2]));
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
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    SortedMap<K, V> sortedMap = createNavigableMap();

    if (useNullKey()) {
      assertNull(sortedMap.put(null, values[0]));
      assertTrue(sortedMap.containsValue(values[0]));

      // the map methods the continue to function
      sortedMap.containsValue(null);
      sortedMap.containsValue(values[0]);
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
    } else if (TestUtils.getJdkVersion() > 6) {
      // nulls are rejected immediately and don't poison the map anymore
      try {
        assertNull(sortedMap.put(null, values[0]));
        fail("should have thrown");
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        assertNull(sortedMap.put(null, values[1]));
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
      sortedMap.containsKey(keys[0]);
      try {
        sortedMap.get(null);
        fail("expected exception on get(null)");
      } catch (NullPointerException e) {
        // expected outcome
      }
      sortedMap.get(keys[0]);
      try {
        sortedMap.remove(null);
      } catch (NullPointerException e) {
        // expected
      }
      sortedMap.remove(keys[0]);
    } else {
      // before JDK 7, nulls poisoned the map
      try {
        assertNull(sortedMap.put(null, values[0]));
        // note: first null added is not required to throw NPE since no
        // comparisons are needed
      } catch (NullPointerException e) {
        // expected outcome
      }
      try {
        assertNull(sortedMap.put(null, values[1]));
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
        sortedMap.containsKey(keys[0]);
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
        sortedMap.get(keys[0]);
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
        sortedMap.remove(keys[0]);
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> map = createMap();
      assertNull(map.put(keys[0], values[0]));
      assertFalse(map.isEmpty());
      assertEquals(1, map.size());

      assertEquals(map.put(keys[0], values[1]), values[0]);
      assertEquals(1, map.size());
    }
  }

  public void testPut_ComparableKey() {
    final boolean java6CompatibleSources =
        !TestUtils.isJvm() || TestUtils.getJdkVersion() < 7;
    TreeMap<String, Object> map = new TreeMap<String, Object>();
    ConflictingKey conflictingKey = new ConflictingKey("conflictingKey");
    try {
      TreeMap untypedMap = map;
      untypedMap.put(conflictingKey, "");
      assertTrue("ClassCastException expected", java6CompatibleSources);
    } catch (ClassCastException e) {
      assertFalse(java6CompatibleSources);
    }
    try {
      map.put("something", "value");
      assertFalse("ClassCastException expected", java6CompatibleSources);
    } catch (ClassCastException expected) {
      assertTrue(java6CompatibleSources);
    }
  }

  /**
   * Test method for 'java.util.Map.put(Object, Object)'.
   *
   * @see java.util.Map#put(Object, Object)
   */
  @SuppressWarnings("unchecked")
  public void testPut_throwsClassCastException_key() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutSupported) {
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> map = createMap();
      map.put(keys[0], values[0]);
      try {
        Map untypedMap = map;
        untypedMap.put(getConflictingKey(), values[1]);
        fail("ClassCastException expected");
      } catch (ClassCastException expected) {
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> map = createMap();
      map.put(keys[0], values[0]);

      Map untypedMap = map;
      untypedMap.put(keys[1], getConflictingValue());
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
  public void testPut_throwsNullPointerException() {
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isPutSupported) {
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> map = createMap();

      try {
        map.put(null, values[0]);
        // JDK < 7 does not conform to the specification if the map is empty.
        if (TestUtils.getJdkVersion() > 6) {
          assertTrue(useNullKey());
        }
      } catch (NullPointerException e) {
        assertFalse(useNullKey());
      }

      try {
        map.put(null, values[0]);
        assertTrue(useNullKey());
      } catch (NullPointerException e) {
        assertFalse(useNullKey());
      }

      map = createMap();
      map.put(keys[0], values[0]);
      try {
        map.put(null, values[0]);
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> map = createMap();
      try {
        map.put(keys[0], values[0]);
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> sourceMap = createMap();
      sourceMap.put(keys[0], values[0]);
      sourceMap.put(keys[1], getValues()[1]);
      sourceMap.put(keys[2], getValues()[2]);

      Map<K, V> destMap = createMap();
      destMap.putAll(sourceMap);
      // Make sure that the data is copied correctly
      _assertEquals(sourceMap, destMap);
    }
  }

  public void testPutAllLjava_util_Map() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], values[i]);
    }

    NavigableMap<K, V> newMap = createNavigableMap();
    newMap.putAll(map);
    assertEquals(map.size(), newMap.size());
    for (int i = 0; i < keys.length; i++) {
      V value = values[i];
      assertEquals(value, newMap.get(keys[i]));
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> sourceMap = createMap();
      sourceMap.put(keys[0], values[0]);

      Map<K, V> destMap = createMap();
      destMap.putAll(sourceMap);
      // Verify that entries get added.
      sourceMap.put(keys[1], values[1]);
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> sourceMap = createMap();
      sourceMap.put(keys[0], values[0]);

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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> sourceMap = createMap();
      sourceMap.put(keys[0], values[0]);

      Map<K, V> destMap = createMap();
      destMap.putAll(sourceMap);
      // Verify that entries get replaced.
      sourceMap.put(keys[0], values[1]);
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> sourceMap = createMap();
      sourceMap.put(keys[0], values[0]);
      sourceMap.putAll(sourceMap);
      // verify putAll with self succeeds and has no effect.
      assertEquals(1, sourceMap.size());
      assertEquals(keys[0], sourceMap.keySet().iterator().next());
      assertEquals(values[0], sourceMap.values().iterator().next());
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

      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> destMap = createMap();
      destMap.put(keys[0], values[0]);
      try {
        // This throws in dev mode because we're putting a second
        // entry in the map and the TreeMap calls the compare method.
        destMap.putAll(sourceMap);
        fail("ClassCastException expected");
      } catch (ClassCastException expected) {
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
        map.putAll(null);
        fail("expected exception");
      } catch (NullPointerException e) {
        // expected outcome
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> map = createMap();

      // null keys are special
      if (useNullKey()) {
        assertNull(map.remove(null));
      }

      assertNull(map.remove(keys[0]));
      assertNull(map.put(keys[0], values[0]));
      assertEquals(map.remove(keys[0]), values[0]);
      assertNull(map.remove(keys[0]));
    }
  }

  public void testRemoveLjava_lang_Object() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], values[i]);
    }

    map.remove(keys[2]);
    assertTrue(!map.containsKey(keys[2]));
  }

  public void testRemove_ComparableKey() {
    TreeMap<String, Object> map = new TreeMap<String, Object>();
    ConflictingKey conflictingKey = new ConflictingKey("conflictingKey");
    assertNull(map.remove(conflictingKey));
    map.put("something", "value");
    assertNull(map.remove(conflictingKey));
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
      K[] keys = getKeys();
      V[] values = getValues();
      Map<K, V> map = createMap();
      map.remove(getConflictingKey());

      map.put(keys[0], values[0]);
      try {
        map.remove(getConflictingKey());
        fail("ClassCastException expected");
      } catch (ClassCastException expected) {
      }
    }
  }

  /**
   * Test method for 'java.util.Map.remove(Object)'.
   *
   * @see java.util.Map#remove(Object)
   */
  @SuppressWarnings("unchecked")
  public void testRemove_throwsNullPointerException() {
    K[] keys = getKeys();
    V[] values = getValues();
    // The _throwsUnsupportedOperationException version of this test will
    // verify that the method is not supported.
    if (isRemoveSupported) {
      Map<K, V> map = createMap();

      try {
        map.remove(null);
        // JDK < 7 does not conform to the specification if the map is empty.
        if (TestUtils.getJdkVersion() > 6) {
          assertTrue(useNullKey());
        }
      } catch (NullPointerException e) {
        assertFalse(useNullKey());
      }

      map.put(keys[0], values[0]);

      try {
        map.remove(null);
        assertTrue(useNullKey());
      } catch (NullPointerException e) {
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
    K[] keys = getKeys();
    Map<K, V> map = createMap();
    if (!isRemoveSupported) {
      try {
        map.remove(keys[0]);
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
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // Test size behavior on put
    map.put(keys[0], values[0]);
    assertEquals(1, map.size());
    map.put(keys[1], values[1]);
    assertEquals(2, map.size());
    map.put(keys[2], values[2]);
    assertEquals(3, map.size());

    // Test size behavior on remove
    map.remove(keys[0]);
    assertEquals(2, map.size());
    map.remove(keys[1]);
    assertEquals(1, map.size());
    map.remove(keys[2]);
    assertEquals(0, map.size());

    // Test size behavior on putAll
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);
    assertEquals(3, map.size());

    // Test size behavior on clear
    map.clear();
    _assertEmpty(map);

    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], values[i]);
    }

    assertEquals(keys.length, map.size());
    for (int i = 0; i < keys.length; i++) {
      assertEquals(i, map.headMap(keys[i]).size());
    }
    assertEquals(keys.length, map.headMap(getGreaterThanMaximumKey()).size());
    for (int i = 0; i < keys.length; i++) {
      assertEquals(keys.length - i, map.tailMap(keys[i]).size());
    }
    assertEquals(keys.length, map.tailMap(getLessThanMinimumKey()).size());
    assertEquals(1, map.subMap(keys[1], keys[2]).size());
    assertEquals(2, map.subMap(keys[0], keys[2]).size());
    try {
      map.subMap(keys[2], keys[1]);
      fail("Should throw an IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(keys.length,
        map.subMap(getLessThanMinimumKey(), getGreaterThanMaximumKey()).size());
  }

  /**
   * Test method for 'java.util.SortedMap.subMap(Object, Object)' and
   * 'java.util.NavigableMap.subMap(Object, boolean, Object, boolean)'.
   *
   * @see java.util.SortedMap#subMap(Object, Object)
   * @see java.util.NavigableMap#subMap(Object, boolean, Object, boolean)
   */
  public void testSubMap() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    // test with no entries
    assertEquals(0, map.subMap(keys[0], keys[0]).size());
    assertEquals(0, map.subMap(keys[0], false, keys[0], false).size());
    assertEquals(0, map.subMap(keys[0], true, keys[0], false).size());
    assertEquals(0, map.subMap(keys[0], false, keys[0], true).size());
    assertEquals(0, map.subMap(keys[0], true, keys[0], true).size());

    // test with a single entry map
    map.put(keys[0], values[0]);

    assertEquals(0, map.subMap(keys[0], keys[0]).size());
    // bounded by a "wide" range
    assertEquals(1, map.subMap(getLessThanMinimumKey(), getGreaterThanMaximumKey()).size());
    assertEquals(1, map.subMap(getLessThanMinimumKey(), false,
        getGreaterThanMaximumKey(), false).size());
    assertEquals(1, map.subMap(getLessThanMinimumKey(), true,
        getGreaterThanMaximumKey(), false).size());
    assertEquals(1, map.subMap(getLessThanMinimumKey(), false,
        getGreaterThanMaximumKey(), true).size());
    assertEquals(1, map.subMap(getLessThanMinimumKey(), true,
        getGreaterThanMaximumKey(), true).size());

    // test with two entry map
    map.put(keys[1], values[1]);

    assertEquals(1, map.subMap(keys[0], keys[1]).size());
    assertEquals(keys[0], map.subMap(keys[0], keys[1]).keySet().toArray()[0]);

    assertEquals(0, map.subMap(keys[0], false, keys[1], false).size());

    assertEquals(1, map.subMap(keys[0], false, keys[1], true).size());
    assertEquals(keys[1], map.subMap(keys[0], false,
        keys[1], true).keySet().toArray()[0]);

    assertEquals(1, map.subMap(keys[0], true, keys[1], false).size());
    assertEquals(keys[0], map.subMap(keys[0], true,
        keys[1], false).keySet().toArray()[0]);

    assertEquals(2, map.subMap(keys[0], true, keys[1], true).size());
    assertEquals(keys[0], map.subMap(keys[0], true,
        keys[1], true).keySet().toArray()[0]);
    assertEquals(keys[1], map.subMap(keys[0], true,
        keys[1], true).keySet().toArray()[1]);

    // bounded by a "wide" range
    assertEquals(2, map.subMap(getLessThanMinimumKey(), getGreaterThanMaximumKey()).size());

    assertEquals(2, map.subMap(getLessThanMinimumKey(), false,
        getGreaterThanMaximumKey(), false).size());
    assertEquals(1, map.subMap(keys[0], false,
        getGreaterThanMaximumKey(), false).size());
    assertEquals(0, map.subMap(keys[0], false,
        keys[1], false).size());
    assertEquals(2, map.subMap(keys[0], true,
        getGreaterThanMaximumKey(), false).size());
    assertEquals(1, map.subMap(keys[0], true,
        keys[1], false).size());
    assertEquals(2, map.subMap(keys[0], true,
        getGreaterThanMaximumKey(), true).size());
    assertEquals(2, map.subMap(keys[0], true,
        keys[1], true).size());
  }

  public void testSubMap_empty() {
    NavigableMap<K, V> map = createNavigableMap();
    SortedMap<K, V> subMap = map.tailMap(getLessThanMinimumKey());
    assertTrue(subMap.values().isEmpty());
  }

  public void testSubMap_entrySet() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);
    map.put(keys[3], values[3]);

    SortedMap<K, V> subMap = map.subMap(keys[1], keys[3]);
    Set<Entry<K, V>> entries = subMap.entrySet();
    assertEquals(2, subMap.size());
    assertEquals(subMap.size(), entries.size());
    assertFalse(entries.contains(new SimpleEntry<K, V>(keys[0], values[0])));
    assertTrue(entries.contains(new SimpleEntry<K, V>(keys[1], values[1])));
    assertTrue(entries.contains(new SimpleEntry<K, V>(keys[2], values[2])));
    assertFalse(entries.contains(new SimpleEntry<K, V>(keys[3], values[3])));

    entries.remove(new SimpleEntry<K, V>(keys[1], values[1]));
    assertEquals(3, map.size());
    assertEquals(subMap.size(), entries.size());
    assertFalse(entries.contains(new SimpleEntry<K, V>(keys[1], values[1])));
    assertFalse(subMap.containsKey(keys[1]));
    assertFalse(subMap.containsValue(values[1]));

    entries.clear();
    assertEquals(2, map.size());
    assertEquals(subMap.size(), entries.size());
    assertTrue(entries.isEmpty());
    assertTrue(subMap.isEmpty());

    subMap.put(keys[2], values[2]);
    assertEquals(1, subMap.size());
    assertEquals(subMap.size(), entries.size());

    subMap.put(keys[1], values[1]);
    Iterator<Entry<K, V>> it = entries.iterator();
    while (it.hasNext()) {
      Map.Entry<K, V> entry = it.next();
      subMap.containsKey(entry.getKey());
      subMap.containsValue(entry.getValue());
      it.remove();
    }
    try {
      it.next();
      fail("should throw NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }
    assertEquals(2, map.size());
    assertEquals(0, subMap.size());
    assertEquals(subMap.size(), entries.size());

    map = createNavigableMap();
    Set<Entry<K, V>> entrySet = map.entrySet();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    map.put(keys[2], values[2]);
    assertEquals(map.size(), entrySet.size());
    _assertEquals(entrySet, map.entrySet());
    map.clear();
    assertEquals(map.size(), entrySet.size());
    _assertEquals(entrySet, map.entrySet());
    map.put(keys[0], values[0]);
    assertEquals(map.size(), entrySet.size());
    _assertEquals(entrySet, map.entrySet());
    entrySet.clear();
    assertEquals(map.size(), entrySet.size());
    _assertEquals(entrySet, map.entrySet());
  }

  public void testSubMap_iterator() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], values[i]);
    }

    assertEquals(keys.length, map.size());

    Map<K, V> subMap = map.subMap(getLessThanMinimumKey(), keys[3]);
    assertEquals(3, subMap.size());

    Set<Map.Entry<K, V>> entrySet = subMap.entrySet();
    assertEquals(3, entrySet.size());
    Iterator<Entry<K, V>> it = entrySet.iterator();
    while (it.hasNext()) {
      Entry<K, V> entry = it.next();
      assertTrue(map.containsKey(entry.getKey()));
      assertTrue(map.containsValue(entry.getValue()));
    }
    try {
      it.next();
      fail("should throw NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }

    Set<K> keySet = subMap.keySet();
    assertEquals(3, keySet.size());
    for (K key : keySet) {
      assertTrue(map.containsKey(key));
    }
  }

  public void testSubMap_NullTolerableComparator() {
    if (!useNullKey()) {
      return;
    }

    // JDK < 7 does not handle null keys correctly.
    if (TestUtils.isJvm() && TestUtils.getJdkVersion() < 7) {
      return;
    }

    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[1], values[1]);
    map.put(null, values[2]);

    SortedMap<K, V> subMapWithNull = map.subMap(null, true, keys[1], true);
    assertEquals(2, subMapWithNull.size());
    assertEquals(values[1], subMapWithNull.get(keys[1]));
    assertEquals(values[2], subMapWithNull.get(null));

    map.put(keys[0], values[0]);
    assertEquals(3, subMapWithNull.size());
    subMapWithNull = map.subMap(null, false, keys[0], true);
    assertEquals(1, subMapWithNull.size());
  }

  /**
   * Test method for 'java.util.SortedMap.subMap(Object, Object)'.
   *
   * @see java.util.SortedMap#subMap(Object, Object)
   */
  @SuppressWarnings("unchecked")
  public void testSubMap_throwsClassCastException() {
    K[] keys = getKeys();
    V[] values = getValues();
    SortedMap sortedMap = createNavigableMap();
    try {
      sortedMap.subMap(getConflictingKey(), keys[0]);
      fail("ClassCastException expected");
    } catch (ClassCastException expected) {
    }
    try {
      sortedMap.subMap(keys[0], getConflictingKey());
      fail("ClassCastException expected");
    } catch (ClassCastException expected) {
    }

    sortedMap.put(keys[0], values[0]);
    try {
      sortedMap.subMap(getConflictingKey(), keys[0]);
      fail("ClassCastException expected");
    } catch (ClassCastException expected) {
    }
    try {
      sortedMap.subMap(keys[0], getConflictingKey());
      fail("ClassCastException expected");
    } catch (ClassCastException expected) {
    }
  }

  /**
   * Test method for 'java.util.SortedMap.subMap(Object, Object)'.
   *
   * @see java.util.SortedMap#subMap(Object, Object)
   */
  public void testSubMap_throwsIllegalArgumentException() {
    SortedMap<K, V> sortedMap = createNavigableMap();
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
    SortedMap<K, V> sortedMap = createNavigableMap();
    try {
      sortedMap.subMap(null, getLessThanMinimumKey());
      assertTrue(useNullKey());
    } catch (NullPointerException e) {
      assertFalse(useNullKey());
    }
    try {
      sortedMap.subMap(null, getGreaterThanMaximumKey());
      assertTrue(useNullKey());
    } catch (NullPointerException e) {
      assertFalse(useNullKey());
    }
  }

  public void testSubMap_viewPutRemove() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();

    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    map.put(keys[3], values[3]);

    NavigableMap<K, V> subMap = map.subMap(keys[1], true, keys[3], true);
    try {
      subMap.put(keys[0], values[0]);
      fail();
    } catch (IllegalArgumentException e) {
      // must not insert value outside the range
    }
    assertFalse(subMap.containsKey(keys[0]));
    assertNull(subMap.remove(keys[0]));
    assertTrue(map.containsKey(keys[0]));
    assertEquals(2, subMap.size());
    assertEquals(3, map.size());

    subMap.put(keys[2], values[2]);
    assertEquals(3, subMap.size());
    assertEquals(4, map.size());
    assertTrue(map.containsKey(keys[2]));
    assertTrue(subMap.containsKey(keys[2]));

    subMap.remove(keys[2]);
    assertFalse(map.containsKey(keys[2]));
    assertFalse(subMap.containsKey(keys[2]));

    subMap.clear();
    assertEquals(0, subMap.size());
    assertEquals(1, map.size());
    assertTrue(map.containsKey(keys[0]));
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object)' and
   * 'java.util.NavigableMap.tailMap(Object, boolean)'.
   *
   * @see java.util.SortedMap#tailMap(Object)
   * @see java.util.NavigableMap#tailMap(Object, boolean)
   */
  public void testTailMap_entries() {
    // test with no entries
    K[] keys = getSortedKeys();
    NavigableMap<K, V> map = createNavigableMap();

    assertNotNull(map.tailMap(keys[0]));
    assertNotNull(map.tailMap(keys[0], false));
    assertNotNull(map.tailMap(keys[0], true));
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object)' and
   * 'java.util.NavigableMap.tailMap(Object, boolean)'.
   *
   * @see java.util.SortedMap#tailMap(Object)
   * @see java.util.NavigableMap#tailMap(Object, boolean)
   */
  public void testTailMap_entries_size() {
    // test with no entries
    K[] keys = getSortedKeys();
    NavigableMap<K, V> map = createNavigableMap();

    Map<K, V> tailMap = map.tailMap(keys[0]);
    assertNotNull(tailMap);
    assertEquals(0, tailMap.size());

    Map<K, V> exclusiveTailMap = map.tailMap(keys[0], false);
    assertNotNull(exclusiveTailMap);
    assertEquals(0, exclusiveTailMap.size());

    Map<K, V> inclusiveTailMap = map.tailMap(keys[0], true);
    assertNotNull(inclusiveTailMap);
    assertEquals(0, inclusiveTailMap.size());
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object)' and
   * 'java.util.NavigableMap.tailMap(Object, boolean)'.
   *
   * @see java.util.SortedMap#tailMap(Object)
   * @see java.util.NavigableMap#tailMap(Object, boolean)
   */
  public void testTailMap_entries_size_keyValue() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    // test with a single entry map
    map.put(keys[0], values[0]);

    Map<K, V> tailMap = map.tailMap(keys[0]);
    assertEquals(1, tailMap.size());
    assertEquals(keys[0], tailMap.keySet().toArray()[0]);

    Map<K, V> exclusiveTailMap = map.tailMap(keys[0], false);
    assertEquals(0, exclusiveTailMap.size());
    assertEquals(0, exclusiveTailMap.keySet().size());

    Map<K, V> inclusiveTailMap = map.tailMap(keys[0], true);
    assertEquals(1, inclusiveTailMap.size());
    assertEquals(keys[0], inclusiveTailMap.keySet().toArray()[0]);

    // test with two entry map
    map.put(keys[1], values[1]);

    tailMap = map.tailMap(keys[1]);
    assertEquals(1, tailMap.size());

    exclusiveTailMap = map.tailMap(keys[1], false);
    assertEquals(0, exclusiveTailMap.size());

    inclusiveTailMap = map.tailMap(keys[1], true);
    assertEquals(1, inclusiveTailMap.size());

    tailMap = map.tailMap(keys[0]);
    assertEquals(2, tailMap.size());
    assertEquals(keys[0], tailMap.keySet().toArray()[0]);
    assertEquals(keys[1], tailMap.keySet().toArray()[1]);

    exclusiveTailMap = map.tailMap(keys[0], false);
    assertEquals(1, exclusiveTailMap.size());
    assertEquals(keys[1], exclusiveTailMap.keySet().toArray()[0]);

    inclusiveTailMap = map.tailMap(keys[0], true);
    assertEquals(2, inclusiveTailMap.size());
    assertEquals(keys[0], inclusiveTailMap.keySet().toArray()[0]);
    assertEquals(keys[1], inclusiveTailMap.keySet().toArray()[1]);
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object, Object)'.
   *
   * @see java.util.SortedMap#tailMap(Object)
   */
  @SuppressWarnings("unchecked")
  public void testTailMap_throwsClassCastException() {
    K[] keys = getKeys();
    V[] values = getValues();
    NavigableMap map = createNavigableMap();
    if (isNaturalOrder()) {
      // TODO Why does this succeed with natural ordering when subMap doesn't?
      map.tailMap(getConflictingKey());
    } else {
      try {
        map.tailMap(getConflictingKey());
        fail("ClassCastException expected");
      } catch (ClassCastException expected) {
      }
    }

    map.put(keys[0], values[0]);
    if (isNaturalOrder()) {
      // TODO Why does this succeed with natural ordering when subMap doesn't?
      map.tailMap(getConflictingKey());
    } else {
      try {
        map.tailMap(getConflictingKey());
        fail("ClassCastException expected");
      } catch (ClassCastException expected) {
      }
    }
  }

  /**
   * Test method for 'java.util.SortedMap.tailMap(Object, Object)'.
   *
   * @see java.util.SortedMap#tailMap(Object)
   */
  public void testTailMap_throwsNullPointerException() {
    SortedMap<K, V> sortedMap = createNavigableMap();
    try {
      sortedMap.tailMap(null);
      assertTrue(useNullKey());
    } catch (NullPointerException e) {
      assertFalse(useNullKey());
    }
  }

  public void testTailMap_viewPutRemove() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();

    NavigableMap<K, V> map = createNavigableMap();
    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);
    map.put(keys[3], values[3]);

    NavigableMap<K, V> tailMap = map.tailMap(keys[1], true);
    try {
      tailMap.put(keys[0], values[0]);
      fail();
    } catch (IllegalArgumentException e) {
      // must not insert value outside the range
    }
    tailMap.remove(keys[0]);
    assertEquals(2, tailMap.size());
    assertEquals(3, map.size());
    assertTrue(map.containsKey(keys[0]));

    tailMap.put(keys[2], values[2]);
    assertEquals(3, tailMap.size());
    assertEquals(4, map.size());
    assertTrue(map.containsKey(keys[2]));
    assertTrue(tailMap.containsKey(keys[2]));

    tailMap.remove(keys[2]);
    assertFalse(map.containsKey(keys[2]));
    assertFalse(tailMap.containsKey(keys[2]));

    tailMap.clear();
    assertEquals(0, tailMap.size());
    assertEquals(1, map.size());
    assertTrue(map.containsKey(keys[0]));
  }

  /**
   * Test method for 'java.lang.Object.toString()'.
   */
  public void testToString() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();
    map.put(keys[0], values[0]);
    String entryString = makeEntryString(keys[0], values[0]);
    assertEquals(entryString, map.toString());
  }

  /**
   * Test method for 'java.util.Map.values()'.
   *
   * @see java.util.Map#values()
   */
  public void testValues() {
    K[] keys = getSortedKeys();
    V[] values = getSortedValues();
    NavigableMap<K, V> map = createNavigableMap();

    map.put(keys[0], values[0]);

    Collection<V> mapValues = map.values();
    assertNotNull(mapValues);
    assertEquals(1, mapValues.size());

    Iterator<V> valueIter = mapValues.iterator();
    assertEquals(values[0], valueIter.next());

    _assertEquals(mapValues, map.values());

    mapValues.clear();
    _assertEmpty(map);

    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], values[i]);
    }

    mapValues.iterator();
    assertEquals(map.size(), mapValues.size());
    for (V value : values) {
      assertTrue(mapValues.contains(value));
    }
    assertEquals(values.length, mapValues.size());
    int size = 0;
    for (Iterator iter = mapValues.iterator(); iter.hasNext(); iter.next()) {
      size++;
    }
    assertEquals(values.length, size);

    mapValues = map.descendingMap().values();
    mapValues.iterator();
    assertEquals(map.size(), mapValues.size());
    for (V value : values) {
      assertTrue(mapValues.contains(value));
    }
    assertEquals(values.length, mapValues.size());
    size = 0;
    for (Iterator iter = mapValues.iterator(); iter.hasNext(); iter.next()) {
      size++;
    }
    assertEquals(values.length, size);

    mapValues = map.values();
    mapValues.remove(values[0]);
    assertTrue(!map.containsValue(values[0]));
    assertEquals(values.length - 1, mapValues.size());
    size = 0;
    for (Iterator iter = mapValues.iterator(); iter.hasNext(); iter.next()) {
      size++;
    }
    assertEquals(values.length - 1, size);
  }

  /**
   * Test method for 'java.util.Map.values()'.
   *
   * @see java.util.Map#values()
   */
  public void testValues_nullKey() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();

    map.put(keys[0], values[0]);

    Collection<V> mapValues = map.values();
    assertNotNull(mapValues);
    assertEquals(1, mapValues.size());

    Iterator<V> valueIter = mapValues.iterator();
    V value = valueIter.next();
    assertEquals(value, values[0]);

    _assertEquals(mapValues, map.values());
  }

  /**
   * Test method for 'java.util.Map.values()'.
   *
   * @see java.util.Map#values()
   */
  public void testValues_viewPut() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();

    map.put(keys[0], values[0]);

    Collection<V> mapValues = map.values();
    assertNotNull(mapValues);
    assertEquals(1, mapValues.size());

    map.put(keys[1], values[1]);
    assertEquals(2, mapValues.size());

    _assertEquals(mapValues, map.values());
  }

  /**
   * Test method for 'java.util.Map.values()'.
   *
   * @see java.util.Map#values()
   */
  public void testValues_viewRemove() {
    K[] keys = getKeys();
    V[] values = getValues();
    Map<K, V> map = createMap();

    map.put(keys[0], values[0]);
    map.put(keys[1], values[1]);

    Collection<V> mapValues = map.values();
    assertNotNull(mapValues);
    assertEquals(2, mapValues.size());

    map.remove(keys[1]);
    assertEquals(1, mapValues.size());

    _assertEquals(mapValues, map.values());
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

  @Override
  protected boolean isFailFastExpected() {
    return false;
  }

  @Override
  protected Map makeEmptyMap() {
    return createNavigableMap();
  }

  protected void setComparator(Comparator<K> comparator) {
    this.comparator = comparator;
  }

  @Override
  protected void verifyMap() {
    if (!TestUtils.isJvm()) {
      // Verify red-black correctness in our implementation
      TreeMapViolator.callAssertCorrectness(map);
    }
    super.verifyMap();
  }

  NavigableMap<K, V> createMap() {
    return createNavigableMap();
  }

  NavigableMap<K, V> createNavigableMap() {
    return createTreeMap();
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

  abstract K[] getSortedKeys();

  abstract V[] getSortedValues();

  abstract V[] getValues();

  abstract V[] getValues2();
}
