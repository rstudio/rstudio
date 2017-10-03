// CHECKSTYLE_OFF: Copyrighted to members of JCP JSR-166 Expert Group.
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */
// CHECKSTYLE_ON
package com.google.gwt.emultest.java.util.concurrent;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests for {@link java.util.concurrent.ConcurrentHashMap}.
 * It's adopted from tests from {@code
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/ConcurrentHashMapTest.java?view=markup}
 */
// TODO(hhchan): Fix the type parameters (missing from the original test).
@SuppressWarnings({"unchecked", "MismatchedQueryAndUpdateOfCollection"})
public class ConcurrentHashMapTest extends EmulTestBase {

  private static final int ZERO = 0;
  private static final int ONE = 1;
  private static final int TWO = 2;
  private static final int THREE = 3;
  private static final int FOUR = 4;
  private static final int FIVE = 5;
  private static final int SIX = 6;

  /** Create a map from Integers 1-5 to Strings "A"-"E". */
  private static ConcurrentHashMap map5() {
    ConcurrentHashMap map = new ConcurrentHashMap(5);
    assertTrue(map.isEmpty());
    map.put(ONE, "A");
    map.put(TWO, "B");
    map.put(THREE, "C");
    map.put(FOUR, "D");
    map.put(FIVE, "E");
    assertFalse(map.isEmpty());
    assertEquals(5, map.size());
    return map;
  }

  /** clear removes all pairs */
  public void testClear() {
    ConcurrentHashMap map = map5();
    map.clear();
    assertEquals(0, map.size());
  }

  /** Maps with same contents are equal */
  public void testEquals() {
    ConcurrentHashMap map1 = map5();
    ConcurrentHashMap map2 = map5();
    assertEquals(map1, map2);
    assertEquals(map2, map1);
    map1.clear();
    assertFalse(map1.equals(map2));
    assertFalse(map2.equals(map1));
  }

  /** contains returns true for contained value */
  public void testContains() {
    ConcurrentHashMap map = map5();
    assertTrue(map.containsValue("A"));
    assertFalse(map.containsValue("Z"));
  }

  /** containsKey returns true for contained key */
  public void testContainsKey() {
    ConcurrentHashMap map = map5();
    assertTrue(map.containsKey(ONE));
    assertFalse(map.containsKey(ZERO));
  }

  /** containsValue returns true for held values */
  public void testContainsValue() {
    ConcurrentHashMap map = map5();
    assertTrue(map.containsValue("A"));
    assertFalse(map.containsValue("Z"));
  }

  /** enumeration returns an enumeration containing the correct elements */
  public void testEnumeration() {
    ConcurrentHashMap map = map5();
    Enumeration e = map.elements();
    int count = 0;
    while (e.hasMoreElements()) {
      count++;
      e.nextElement();
    }
    assertEquals(5, count);
  }

  /** get returns the correct element at the given key, or null if not present */
  public void testGet() {
    ConcurrentHashMap map = map5();
    assertEquals("A", (String) map.get(ONE));
    assertNull(map.get("anything"));
  }

  /** isEmpty is true of empty map and false for non-empty */
  public void testIsEmpty() {
    ConcurrentHashMap empty = new ConcurrentHashMap();
    ConcurrentHashMap map = map5();
    assertTrue(empty.isEmpty());
    assertFalse(map.isEmpty());
  }

  /** keys returns an enumeration containing all the keys from the map */
  public void testKeys() {
    ConcurrentHashMap map = map5();
    Enumeration e = map.keys();
    int count = 0;
    while (e.hasMoreElements()) {
      count++;
      e.nextElement();
    }
    assertEquals(5, count);
  }

  /** keySet returns a Set containing all the keys */
  public void testKeySet() {
    ConcurrentHashMap map = map5();
    Set s = map.keySet();
    assertEquals(5, s.size());
    assertTrue(s.contains(ONE));
    assertTrue(s.contains(TWO));
    assertTrue(s.contains(THREE));
    assertTrue(s.contains(FOUR));
    assertTrue(s.contains(FIVE));
  }

  /** keySet.toArray returns contains all keys */
  public void testKeySetToArray() {
    ConcurrentHashMap map = map5();
    Set s = map.keySet();
    Object[] ar = s.toArray();
    assertTrue(s.containsAll(Arrays.asList(ar)));
    assertEquals(5, ar.length);
    ar[0] = 10;
    assertFalse(s.containsAll(Arrays.asList(ar)));
  }

  /** Values.toArray contains all values */
  public void testValuesToArray() {
    ConcurrentHashMap map = map5();
    Collection v = map.values();
    Object[] ar = v.toArray();
    ArrayList s = new ArrayList(Arrays.asList(ar));
    assertEquals(5, ar.length);
    assertTrue(s.contains("A"));
    assertTrue(s.contains("B"));
    assertTrue(s.contains("C"));
    assertTrue(s.contains("D"));
    assertTrue(s.contains("E"));
  }

  /** entrySet.toArray contains all entries */
  public void testEntrySetToArray() {
    ConcurrentHashMap map = map5();
    Set s = map.entrySet();
    Object[] ar = s.toArray();
    assertEquals(5, ar.length);
    for (int i = 0; i < 5; ++i) {
      assertTrue(map.containsKey(((Map.Entry) (ar[i])).getKey()));
      assertTrue(map.containsValue(((Map.Entry) (ar[i])).getValue()));
    }
  }

  /** values collection contains all values */
  public void testValues() {
    ConcurrentHashMap map = map5();
    Collection s = map.values();
    assertEquals(5, s.size());
    assertTrue(s.contains("A"));
    assertTrue(s.contains("B"));
    assertTrue(s.contains("C"));
    assertTrue(s.contains("D"));
    assertTrue(s.contains("E"));
  }

  /** entrySet contains all pairs */
  public void testEntrySet() {
    ConcurrentHashMap map = map5();
    Set s = map.entrySet();
    assertEquals(5, s.size());
    for (Object value : s) {
      Entry e = (Entry) value;
      assertTrue(
          (e.getKey().equals(ONE) && e.getValue().equals("A"))
              || (e.getKey().equals(TWO) && e.getValue().equals("B"))
              || (e.getKey().equals(THREE) && e.getValue().equals("C"))
              || (e.getKey().equals(FOUR) && e.getValue().equals("D"))
              || (e.getKey().equals(FIVE) && e.getValue().equals("E")));
    }
  }

  /** putAll adds all key-value pairs from the given map */
  public void testPutAll() {
    ConcurrentHashMap empty = new ConcurrentHashMap();
    ConcurrentHashMap map = map5();
    empty.putAll(map);
    assertEquals(5, empty.size());
    assertTrue(empty.containsKey(ONE));
    assertTrue(empty.containsKey(TWO));
    assertTrue(empty.containsKey(THREE));
    assertTrue(empty.containsKey(FOUR));
    assertTrue(empty.containsKey(FIVE));
  }

  /** putIfAbsent works when the given key is not present */
  public void testPutIfAbsent() {
    ConcurrentHashMap map = map5();
    map.putIfAbsent(SIX, "Z");
    assertTrue(map.containsKey(SIX));
  }

  /** putIfAbsent does not add the pair if the key is already present */
  public void testPutIfAbsent2() {
    ConcurrentHashMap map = map5();
    assertEquals("A", map.putIfAbsent(ONE, "Z"));
  }

  /** replace fails when the given key is not present */
  public void testReplace() {
    ConcurrentHashMap map = map5();
    assertNull(map.replace(SIX, "Z"));
    assertFalse(map.containsKey(SIX));
  }

  /** replace succeeds if the key is already present */
  public void testReplace2() {
    ConcurrentHashMap map = map5();
    assertNotNull(map.replace(ONE, "Z"));
    assertEquals("Z", map.get(ONE));
  }

  /** replace value fails when the given key not mapped to expected value */
  public void testReplaceValue() {
    ConcurrentHashMap map = map5();
    assertEquals("A", map.get(ONE));
    assertFalse(map.replace(ONE, "Z", "Z"));
    assertEquals("A", map.get(ONE));
  }

  /** replace value succeeds when the given key mapped to expected value */
  public void testReplaceValue2() {
    ConcurrentHashMap map = map5();
    assertEquals("A", map.get(ONE));
    assertTrue(map.replace(ONE, "A", "Z"));
    assertEquals("Z", map.get(ONE));
  }

  /** remove removes the correct key-value pair from the map */
  public void testRemove() {
    ConcurrentHashMap map = map5();
    map.remove(FIVE);
    assertEquals(4, map.size());
    assertFalse(map.containsKey(FIVE));
  }

  /** remove(key,value) removes only if pair present */
  public void testRemove2() {
    ConcurrentHashMap map = map5();
    map.remove(FIVE, "E");
    assertEquals(4, map.size());
    assertFalse(map.containsKey(FIVE));
    map.remove(FOUR, "A");
    assertEquals(4, map.size());
    assertTrue(map.containsKey(FOUR));
  }

  /** size returns the correct values */
  public void testSize() {
    ConcurrentHashMap map = map5();
    ConcurrentHashMap empty = new ConcurrentHashMap();
    assertEquals(0, empty.size());
    assertEquals(5, map.size());
  }

  /** toString contains toString of elements */
  public void testToString() {
    ConcurrentHashMap map = map5();
    String s = map.toString();
    for (int i = 1; i <= 5; ++i) {
      assertTrue(s.indexOf(String.valueOf(i)) >= 0);
    }
  }

  // Exception tests

  /** Cannot create with only negative capacity */
  public void testConstructor() {
    try {
      new ConcurrentHashMap(-1);
      fail("Exception expected");
    } catch (IllegalArgumentException expected) {
    }
  }

  /** get(null) throws NPE */
  public void testGet_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.get(null);
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** containsKey(null) throws NPE */
  public void testContainsKey_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.containsKey(null);
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** containsValue(null) throws NPE */
  public void testContainsValue_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.containsValue(null);
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** contains(null) throws NPE */
  public void testContains_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.containsValue(null);
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** put(null,x) throws NPE */
  public void testPut1_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.put(null, "whatever");
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** put(x, null) throws NPE */
  public void testPut2_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.put("whatever", null);
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** putIfAbsent(null, x) throws NPE */
  public void testPutIfAbsent1_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.putIfAbsent(null, "whatever");
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** replace(null, x) throws NPE */
  public void testReplace_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.replace(null, "whatever");
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** replace(null, x, y) throws NPE */
  public void testReplaceValue_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.replace(null, ONE, "whatever");
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** putIfAbsent(x, null) throws NPE */
  public void testPutIfAbsent2_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.putIfAbsent("whatever", null);
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** replace(x, null) throws NPE */
  public void testReplace2_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.replace("whatever", null);
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** replace(x, null, y) throws NPE */
  public void testReplaceValue2_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.replace("whatever", null, "A");
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** replace(x, y, null) throws NPE */
  public void testReplaceValue3_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.replace("whatever", ONE, null);
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** remove(null) throws NPE */
  public void testRemove1_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.put("sadsdf", "asdads");
      c.remove(null);
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** remove(null, x) throws NPE */
  public void testRemove2_NullPointerException() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.put("sadsdf", "asdads");
      c.remove(null, "whatever");
      fail("Exception expected");
    } catch (NullPointerException expected) {
    }
  }

  /** remove(x, null) returns false */
  public void testRemove3() {
    try {
      ConcurrentHashMap c = new ConcurrentHashMap(5);
      c.put("sadsdf", "asdads");
      assertFalse(c.remove("sadsdf", null));
    } catch (NullPointerException e) {
      fail();
    }
  }

  /** SetValue of an EntrySet entry sets value in the map. */
  public void testSetValueWriteThrough() {
    // Adapted from a bug report by Eric Zoerner
    ConcurrentHashMap map = new ConcurrentHashMap(2);
    assertTrue(map.isEmpty());
    for (int i = 0; i < 20; i++) {
      map.put(new Integer(i), new Integer(i));
    }
    assertFalse(map.isEmpty());
    Map.Entry entry1 = (Map.Entry) map.entrySet().iterator().next();

    if (entry1.getKey().equals(new Integer(16))) {
      // can't perform the test this time
      // TODO(cpovirk): Could we just pick a different key?
      return;
    }

    // remove 16 (a different key) from map
    // which just happens to cause entry1 to be cloned in map
    map.remove(new Integer(16));
    entry1.setValue("XYZ");
    assertTrue(map.containsValue("XYZ")); // fails
  }
}
