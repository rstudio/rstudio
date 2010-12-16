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

import org.apache.commons.collections.TestMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Tests <code>IdentityHashMap</code>.
 */
public class IdentityHashMapTest extends TestMap {

  /**
   * A class that is equal to all other instances of itself; used to ensure that
   * identity rather than equality is being checked.
   */
  private static class Foo {
    @Override
    public boolean equals(Object obj) {
      return obj instanceof Foo;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  private static final int CAPACITY_16 = 16;
  private static final int CAPACITY_NEG_ONE_HALF = -1;
  private static final int CAPACITY_ZERO = 0;
  private static final Integer INTEGER_1 = new Integer(1);
  private static final Integer INTEGER_11 = new Integer(11);
  private static final Integer INTEGER_2 = new Integer(2);
  private static final Integer INTEGER_22 = new Integer(22);
  private static final Integer INTEGER_3 = new Integer(3);
  private static final Integer INTEGER_33 = new Integer(33);
  private static final Integer INTEGER_ZERO_KEY = new Integer(0);
  private static final String INTEGER_ZERO_VALUE = "integer zero";
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";
  private static final String KEY_3 = "key3";
  private static final String KEY_4 = "key4";
  private static final String KEY_KEY = "key";
  private static final String KEY_TEST_CONTAINS_KEY = "testContainsKey";
  private static final String KEY_TEST_CONTAINS_VALUE = "testContainsValue";
  private static final String KEY_TEST_ENTRY_SET = "testEntrySet";
  private static final String KEY_TEST_GET = "testGet";
  private static final String KEY_TEST_KEY_SET = "testKeySet";
  private static final String KEY_TEST_PUT = "testPut";
  private static final String KEY_TEST_REMOVE = "testRemove";
  private static final float LOAD_FACTOR_NEG_ONE = -1.0F;
  private static final float LOAD_FACTOR_ONE_HALF = 0.5F;
  private static final float LOAD_FACTOR_ONE_TENTH = 0.1F;
  private static final float LOAD_FACTOR_ZERO = 0.0F;
  private static final Object ODD_ZERO_KEY = new Object() {
    public int hashCode() {
      return 0;
    }
  };
  private static final String ODD_ZERO_VALUE = "odd zero";
  private static final int SIZE_ONE = 1;
  private static final int SIZE_THREE = 3;
  private static final int SIZE_TWO = 2;
  private static final int SIZE_ZERO = 0;
  private static final String STRING_ZERO_KEY = "0";
  private static final String STRING_ZERO_VALUE = "string zero";
  private static final String VALUE_1 = "val1";
  private static final String VALUE_2 = "val2";
  private static final String VALUE_3 = "val3";
  private static final String VALUE_4 = "val4";
  private static final String VALUE_TEST_CONTAINS_DOES_NOT_EXIST = "does not exist";
  private static final Integer VALUE_TEST_CONTAINS_KEY = new Integer(5);
  private static final String VALUE_TEST_ENTRY_SET_1 = KEY_TEST_ENTRY_SET
      + " - value1";
  private static final String VALUE_TEST_ENTRY_SET_2 = KEY_TEST_ENTRY_SET
      + " - value2";
  private static final String VALUE_TEST_GET = KEY_TEST_GET + " - Value";
  private static final String VALUE_TEST_KEY_SET = KEY_TEST_KEY_SET
      + " - value";
  private static final String VALUE_TEST_PUT_1 = KEY_TEST_PUT + " - value 1";
  private static final String VALUE_TEST_PUT_2 = KEY_TEST_PUT + " - value 2";
  private static final String VALUE_TEST_REMOVE = KEY_TEST_REMOVE + " - value";
  private static final String VALUE_VAL = "value";

  /**
   * Check the state of a newly constructed, empty IdentityHashMap.
   * 
   * @param hashMap
   */
  private static void checkEmptyHashMapAssumptions(IdentityHashMap hashMap) {
    assertNotNull(hashMap);
    assertTrue(hashMap.isEmpty());

    assertNotNull(hashMap.values());
    assertTrue(hashMap.values().isEmpty());
    assertTrue(hashMap.values().size() == 0);

    assertNotNull(hashMap.keySet());
    assertTrue(hashMap.keySet().isEmpty());
    assertTrue(hashMap.keySet().size() == 0);

    assertNotNull(hashMap.entrySet());
    assertTrue(hashMap.entrySet().isEmpty());
    assertTrue(hashMap.entrySet().size() == 0);

    assertNotNull(hashMap.entrySet().iterator());
    assertFalse(hashMap.entrySet().iterator().hasNext());
  }

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testAddEqualKeys() {
    final IdentityHashMap expected = new IdentityHashMap();
    assertEquals(expected.size(), 0);
    iterateThrough(expected);
    expected.put(new Long(45), new Object());
    assertEquals(expected.size(), 1);
    iterateThrough(expected);
    expected.put(new Integer(45), new Object());
    assertNotSame(new Integer(45), new Long(45));
    assertEquals(expected.size(), 2);
    iterateThrough(expected);
  }

  public void testAddWatch() {
    IdentityHashMap m = new IdentityHashMap();
    m.put("watch", "watch");
    assertEquals(m.get("watch"), "watch");
  }

  /*
   * Test method for 'java.util.IdentityHashMap.clear()'
   */
  public void testClear() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    hashMap.put("Hello", "Bye");
    assertFalse(hashMap.isEmpty());
    assertTrue(hashMap.size() == SIZE_ONE);

    hashMap.clear();
    assertTrue(hashMap.isEmpty());
    assertTrue(hashMap.size() == 0);
  }

  /*
   * Test method for 'java.util.IdentityHashMap.clone()'
   */
  public void testClone() {
    IdentityHashMap srcMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(srcMap);

    // Check empty clone behavior
    IdentityHashMap dstMap = (IdentityHashMap) srcMap.clone();
    assertNotNull(dstMap);
    assertEquals(dstMap.size(), srcMap.size());
    // assertTrue(dstMap.values().toArray().equals(srcMap.values().toArray()));
    assertTrue(dstMap.keySet().equals(srcMap.keySet()));
    assertTrue(dstMap.entrySet().equals(srcMap.entrySet()));

    // Check non-empty clone behavior
    srcMap.put(KEY_1, VALUE_1);
    srcMap.put(KEY_2, VALUE_2);
    srcMap.put(KEY_3, VALUE_3);
    dstMap = (IdentityHashMap) srcMap.clone();
    assertNotNull(dstMap);
    assertEquals(dstMap.size(), srcMap.size());

    assertTrue(dstMap.keySet().equals(srcMap.keySet()));

    assertTrue(dstMap.entrySet().equals(srcMap.entrySet()));
  }

  /*
   * Test method for 'java.util.IdentityHashMap.containsKey(Object)'
   */
  public void testContainsKey() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertFalse(hashMap.containsKey(KEY_TEST_CONTAINS_KEY));
    hashMap.put(KEY_TEST_CONTAINS_KEY, VALUE_TEST_CONTAINS_KEY);
    assertTrue(hashMap.containsKey(KEY_TEST_CONTAINS_KEY));
    assertFalse(hashMap.containsKey(VALUE_TEST_CONTAINS_DOES_NOT_EXIST));

    assertFalse(hashMap.containsKey(null));
    hashMap.put(null, VALUE_TEST_CONTAINS_KEY);
    assertTrue(hashMap.containsKey(null));
  }

  /*
   * Test method for 'java.util.IdentityHashMap.containsValue(Object)'
   */
  public void testContainsValue() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertFalse("check contains of empty map",
        hashMap.containsValue(VALUE_TEST_CONTAINS_KEY));
    hashMap.put(KEY_TEST_CONTAINS_VALUE, VALUE_TEST_CONTAINS_KEY);
    assertTrue("check contains of map with element",
        hashMap.containsValue(VALUE_TEST_CONTAINS_KEY));
    assertFalse("check contains of map other element",
        hashMap.containsValue(VALUE_TEST_CONTAINS_DOES_NOT_EXIST));

    if (useNullValue()) {
      assertFalse(hashMap.containsValue(null));
    }
    hashMap.put(KEY_TEST_CONTAINS_VALUE, null);
    assertTrue(hashMap.containsValue(null));
  }

  /*
   * Test method for 'java.util.IdentityHashMap.entrySet()'
   */
  public void testEntrySet() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    Set entrySet = hashMap.entrySet();
    assertNotNull(entrySet);

    // Check that the entry set looks right
    hashMap.put(KEY_TEST_ENTRY_SET, VALUE_TEST_ENTRY_SET_1);
    entrySet = hashMap.entrySet();
    assertEquals(entrySet.size(), SIZE_ONE);
    Iterator itSet = entrySet.iterator();
    Map.Entry entry = (Map.Entry) itSet.next();
    assertEquals(entry.getKey(), KEY_TEST_ENTRY_SET);
    assertEquals(entry.getValue(), VALUE_TEST_ENTRY_SET_1);

    // Check that entries in the entrySet are update correctly on overwrites
    hashMap.put(KEY_TEST_ENTRY_SET, VALUE_TEST_ENTRY_SET_2);
    entrySet = hashMap.entrySet();
    assertEquals(entrySet.size(), SIZE_ONE);
    itSet = entrySet.iterator();
    entry = (Map.Entry) itSet.next();
    assertEquals(entry.getKey(), KEY_TEST_ENTRY_SET);
    assertEquals(entry.getValue(), VALUE_TEST_ENTRY_SET_2);

    // Check that entries are updated on removes
    hashMap.remove(KEY_TEST_ENTRY_SET);
    checkEmptyHashMapAssumptions(hashMap);
  }

  /*
   * Used to test the entrySet entry's set method.
   */
  public void testEntrySetEntrySetterNonString() {
    HashMap hashMap = new HashMap();
    Integer key = 1;
    hashMap.put(key, 2);
    Set entrySet = hashMap.entrySet();
    Entry entry = (Entry) entrySet.iterator().next();

    entry.setValue(3);
    assertEquals(3, hashMap.get(key));

    hashMap.put(key, 4);
    assertEquals(4, entry.getValue());

    assertEquals(1, hashMap.size());
  }

  /*
   * Used to test the entrySet entry's set method.
   */
  public void testEntrySetEntrySetterNull() {
    HashMap hashMap = new HashMap();
    hashMap.put(null, 2);
    Set entrySet = hashMap.entrySet();
    Entry entry = (Entry) entrySet.iterator().next();

    entry.setValue(3);
    assertEquals(3, hashMap.get(null));

    hashMap.put(null, 4);
    assertEquals(4, entry.getValue());

    assertEquals(1, hashMap.size());
  }

  /*
   * Used to test the entrySet entry's set method.
   */
  public void testEntrySetEntrySetterString() {
    HashMap hashMap = new HashMap();
    String key = "A";
    hashMap.put(key, "B");
    Set entrySet = hashMap.entrySet();
    Entry entry = (Entry) entrySet.iterator().next();

    entry.setValue("C");
    assertEquals("C", hashMap.get(key));

    hashMap.put(key, "D");
    assertEquals("D", entry.getValue());

    assertEquals(1, hashMap.size());
  }

  /*
   * Used to test the entrySet remove method.
   */
  public void testEntrySetRemove() {
    IdentityHashMap hashMap = new IdentityHashMap();
    hashMap.put("A", "B");
    IdentityHashMap dummy = new IdentityHashMap();
    dummy.put("A", "b");
    Entry bogus = (Entry) dummy.entrySet().iterator().next();
    Set entrySet = hashMap.entrySet();
    boolean removed = entrySet.remove(bogus);
    assertEquals(removed, false);
    assertEquals(hashMap.get("A"), "B");
  }

  /*
   * Test method for 'java.util.AbstractMap.equals(Object)'
   */
  public void testEquals() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    hashMap.put(KEY_KEY, VALUE_VAL);

    IdentityHashMap copyMap = (IdentityHashMap) hashMap.clone();

    assertTrue(hashMap.equals(copyMap));
    hashMap.put(VALUE_VAL, KEY_KEY);
    assertFalse(hashMap.equals(copyMap));
  }

  /*
   * Test method for 'java.lang.Object.finalize()'.
   */
  public void testFinalize() {
    // no tests for finalize
  }

  /*
   * Test method for 'java.util.IdentityHashMap.get(Object)'.
   */
  public void testGet() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertNull(hashMap.get(KEY_TEST_GET));
    hashMap.put(KEY_TEST_GET, VALUE_TEST_GET);
    assertNotNull(hashMap.get(KEY_TEST_GET));

    assertNull(hashMap.get(null));
    hashMap.put(null, VALUE_TEST_GET);
    assertNotNull(hashMap.get(null));

    hashMap.put(null, null);
    assertNull(hashMap.get(null));
  }

  /*
   * Test method for 'java.util.AbstractMap.hashCode()'.
   */
  public void testHashCode() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    // Check that hashCode changes
    int hashCode1 = hashMap.hashCode();
    hashMap.put(KEY_KEY, VALUE_VAL);
    int hashCode2 = hashMap.hashCode();

    assertTrue(hashCode1 != hashCode2);
  }

  /*
   * Test method for 'java.util.IdentityHashMap.IdentityHashMap()'.
   */
  public void testHashMap() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);
  }

  /*
   * Test method for 'java.util.IdentityHashMap.IdentityHashMap(int)'
   */
  public void testHashMapInt() {
    IdentityHashMap hashMap = new IdentityHashMap(CAPACITY_16);
    checkEmptyHashMapAssumptions(hashMap);

    // TODO(mmendez): how do we verify capacity?
    boolean failed = true;
    try {
      new IdentityHashMap(-SIZE_ONE);
    } catch (Throwable ex) {
      if (ex instanceof IllegalArgumentException) {
        failed = false;
      }
    }

    if (failed) {
      fail("Failure testing new IdentityHashMap(-1)");
    }

    IdentityHashMap zeroSizedHashMap = new IdentityHashMap(0);
    assertNotNull(zeroSizedHashMap);
  }

  /*
   * Test method for 'java.util.IdentityHashMap.IdentityHashMap(Map)'
   */
  public void testHashMapMap() {
    IdentityHashMap srcMap = new IdentityHashMap();
    assertNotNull(srcMap);
    checkEmptyHashMapAssumptions(srcMap);

    srcMap.put(INTEGER_1, INTEGER_11);
    srcMap.put(INTEGER_2, INTEGER_22);
    srcMap.put(INTEGER_3, INTEGER_33);

    IdentityHashMap hashMap = new IdentityHashMap(srcMap);
    assertFalse(hashMap.isEmpty());
    assertTrue(hashMap.size() == SIZE_THREE);

    Collection valColl = hashMap.values();
    assertTrue(valColl.contains(INTEGER_11));
    assertTrue(valColl.contains(INTEGER_22));
    assertTrue(valColl.contains(INTEGER_33));

    Collection keyColl = hashMap.keySet();
    assertTrue(keyColl.contains(INTEGER_1));
    assertTrue(keyColl.contains(INTEGER_2));
    assertTrue(keyColl.contains(INTEGER_3));
  }

  /**
   * Test that the implementation differs from a standard map in demanding
   * identity.
   */
  public void testIdentity() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    Foo foo1 = new Foo();
    assertNull(hashMap.get(foo1));
    hashMap.put(foo1, VALUE_1);
    assertNotNull(hashMap.get(foo1));
    assertSame(VALUE_1, hashMap.get(foo1));

    Foo foo2 = new Foo();
    assertNull(hashMap.get(foo2));
  }

  /**
   * Test that the implementation differs from a standard map in demanding
   * identity.
   */
  public void testIdentityBasedEquality() {
    IdentityHashMap hashMap1 = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap1);

    IdentityHashMap hashMap2 = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap2);

    hashMap1.put(new Foo(), VALUE_1);
    hashMap2.put(new Foo(), VALUE_1);
    assertFalse(hashMap1.equals(hashMap2));
  }

  /**
   * Test that the implementation differs from a standard map in demanding
   * identity.
   */
  public void testIdentityBasedHashCode() {
    IdentityHashMap hashMap1 = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap1);

    IdentityHashMap hashMap2 = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap2);

    hashMap1.put(new Foo(), VALUE_1);
    hashMap2.put(new Foo(), VALUE_1);
    if (GWT.isScript()) {
      // Only reliable in Production Mode since Development Mode can have
      // identity hash collisions.
      assertFalse(hashMap1.hashCode() == hashMap2.hashCode());
    }
  }

  /*
   * Test method for 'java.util.AbstractMap.isEmpty()'
   */
  public void testIsEmpty() {
    IdentityHashMap srcMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(srcMap);

    IdentityHashMap dstMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(dstMap);

    dstMap.putAll(srcMap);
    assertTrue(dstMap.isEmpty());

    dstMap.put(KEY_KEY, VALUE_VAL);
    assertFalse(dstMap.isEmpty());

    dstMap.remove(KEY_KEY);
    assertTrue(dstMap.isEmpty());
    assertEquals(dstMap.size(), 0);
  }

  public void testKeysConflict() {
    IdentityHashMap hashMap = new IdentityHashMap();

    hashMap.put(STRING_ZERO_KEY, STRING_ZERO_VALUE);
    hashMap.put(INTEGER_ZERO_KEY, INTEGER_ZERO_VALUE);
    hashMap.put(ODD_ZERO_KEY, ODD_ZERO_VALUE);
    assertEquals(hashMap.get(INTEGER_ZERO_KEY), INTEGER_ZERO_VALUE);
    assertEquals(hashMap.get(ODD_ZERO_KEY), ODD_ZERO_VALUE);
    assertEquals(hashMap.get(STRING_ZERO_KEY), STRING_ZERO_VALUE);
    hashMap.remove(INTEGER_ZERO_KEY);
    assertEquals(hashMap.get(ODD_ZERO_KEY), ODD_ZERO_VALUE);
    assertEquals(hashMap.get(STRING_ZERO_KEY), STRING_ZERO_VALUE);
    assertEquals(hashMap.get(INTEGER_ZERO_KEY), null);
    hashMap.remove(ODD_ZERO_KEY);
    assertEquals(hashMap.get(INTEGER_ZERO_KEY), null);
    assertEquals(hashMap.get(ODD_ZERO_KEY), null);
    assertEquals(hashMap.get(STRING_ZERO_KEY), STRING_ZERO_VALUE);
    hashMap.remove(STRING_ZERO_KEY);
    assertEquals(hashMap.get(INTEGER_ZERO_KEY), null);
    assertEquals(hashMap.get(ODD_ZERO_KEY), null);
    assertEquals(hashMap.get(STRING_ZERO_KEY), null);
    assertEquals(hashMap.size(), 0);
  }

  /*
   * Test method for 'java.util.IdentityHashMap.keySet()'
   */
  public void testKeySet() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    Set keySet = hashMap.keySet();
    assertNotNull(keySet);
    assertTrue(keySet.isEmpty());
    assertTrue(keySet.size() == 0);

    hashMap.put(KEY_TEST_KEY_SET, VALUE_TEST_KEY_SET);

    assertTrue(keySet.size() == SIZE_ONE);
    assertTrue(keySet.contains(KEY_TEST_KEY_SET));
    assertFalse(keySet.contains(VALUE_TEST_KEY_SET));
    assertFalse(keySet.contains(KEY_TEST_KEY_SET.toUpperCase()));
  }

  /*
   * Test method for 'java.util.IdentityHashMap.put(Object, Object)'
   */
  public void testPut() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertNull(hashMap.put(KEY_TEST_PUT, VALUE_TEST_PUT_1));
    assertEquals(hashMap.put(KEY_TEST_PUT, VALUE_TEST_PUT_2), VALUE_TEST_PUT_1);
    assertNull(hashMap.put(null, VALUE_TEST_PUT_1));
    assertEquals(hashMap.put(null, VALUE_TEST_PUT_2), VALUE_TEST_PUT_1);
  }

  /**
   * Test method for 'java.util.IdentityHashMap.putAll(Map)'.
   */
  public void testPutAll() {
    IdentityHashMap srcMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(srcMap);

    srcMap.put(KEY_1, VALUE_1);
    srcMap.put(KEY_2, VALUE_2);
    srcMap.put(KEY_3, VALUE_3);

    // Make sure that the data is copied correctly
    IdentityHashMap dstMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(dstMap);

    dstMap.putAll(srcMap);
    assertEquals(srcMap.size(), dstMap.size());
    assertTrue(dstMap.containsKey(KEY_1));
    assertTrue(dstMap.containsValue(VALUE_1));
    assertFalse(dstMap.containsKey(KEY_1.toUpperCase()));
    assertFalse(dstMap.containsValue(VALUE_1.toUpperCase()));

    assertTrue(dstMap.containsKey(KEY_2));
    assertTrue(dstMap.containsValue(VALUE_2));
    assertFalse(dstMap.containsKey(KEY_2.toUpperCase()));
    assertFalse(dstMap.containsValue(VALUE_2.toUpperCase()));

    assertTrue(dstMap.containsKey(KEY_3));
    assertTrue(dstMap.containsValue(VALUE_3));
    assertFalse(dstMap.containsKey(KEY_3.toUpperCase()));
    assertFalse(dstMap.containsValue(VALUE_3.toUpperCase()));

    // Check that an empty map does not blow away the contents of the
    // destination map
    IdentityHashMap emptyMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(emptyMap);
    dstMap.putAll(emptyMap);
    assertTrue(dstMap.size() == srcMap.size());

    // Check that put all overwrite any existing mapping in the destination map
    srcMap.put(KEY_1, VALUE_2);
    srcMap.put(KEY_2, VALUE_3);
    srcMap.put(KEY_3, VALUE_1);

    dstMap.putAll(srcMap);
    assertEquals(dstMap.size(), srcMap.size());
    assertEquals(dstMap.get(KEY_1), VALUE_2);
    assertEquals(dstMap.get(KEY_2), VALUE_3);
    assertEquals(dstMap.get(KEY_3), VALUE_1);

    // Check that a putAll does adds data but does not remove it

    srcMap.put(KEY_4, VALUE_4);
    dstMap.putAll(srcMap);
    assertEquals(dstMap.size(), srcMap.size());
    assertTrue(dstMap.containsKey(KEY_4));
    assertTrue(dstMap.containsValue(VALUE_4));
    assertEquals(dstMap.get(KEY_1), VALUE_2);
    assertEquals(dstMap.get(KEY_2), VALUE_3);
    assertEquals(dstMap.get(KEY_3), VALUE_1);
    assertEquals(dstMap.get(KEY_4), VALUE_4);

    dstMap.putAll(dstMap);
  }

  /**
   * Test method for 'java.util.IdentityHashMap.remove(Object)'.
   */
  public void testRemove() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertNull(hashMap.remove(null));
    hashMap.put(null, VALUE_TEST_REMOVE);
    assertNotNull(hashMap.remove(null));

    hashMap.put(KEY_TEST_REMOVE, VALUE_TEST_REMOVE);
    assertEquals(hashMap.remove(KEY_TEST_REMOVE), VALUE_TEST_REMOVE);
    assertNull(hashMap.remove(KEY_TEST_REMOVE));
  }

  /**
   * Test method for 'java.util.IdentityHashMap.size()'.
   */
  public void testSize() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    // Test size behavior on put
    assertEquals(hashMap.size(), SIZE_ZERO);
    hashMap.put(KEY_1, VALUE_1);
    assertEquals(hashMap.size(), SIZE_ONE);
    hashMap.put(KEY_2, VALUE_2);
    assertEquals(hashMap.size(), SIZE_TWO);
    hashMap.put(KEY_3, VALUE_3);
    assertEquals(hashMap.size(), SIZE_THREE);

    // Test size behavior on remove
    hashMap.remove(KEY_1);
    assertEquals(hashMap.size(), SIZE_TWO);
    hashMap.remove(KEY_2);
    assertEquals(hashMap.size(), SIZE_ONE);
    hashMap.remove(KEY_3);
    assertEquals(hashMap.size(), SIZE_ZERO);

    // Test size behavior on putAll
    hashMap.put(KEY_1, VALUE_1);
    hashMap.put(KEY_2, VALUE_2);
    hashMap.put(KEY_3, VALUE_3);
    IdentityHashMap srcMap = new IdentityHashMap(hashMap);
    hashMap.putAll(srcMap);
    assertEquals(hashMap.size(), SIZE_THREE);

    // Test size behavior on clear
    hashMap.clear();
    assertEquals(hashMap.size(), SIZE_ZERO);
  }

  /**
   * Test method for 'java.util.AbstractMap.toString()'.
   */
  public void testToString() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);
    hashMap.put(KEY_KEY, VALUE_VAL);
    String entryString = makeEntryString(KEY_KEY, VALUE_VAL);
    assertTrue(entryString.equals(hashMap.toString()));
  }

  /**
   * Test method for 'java.util.AbstractMap.values()'.
   */
  public void testValues() {
    IdentityHashMap hashMap = new IdentityHashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertNotNull(hashMap.values());

    hashMap.put(KEY_KEY, VALUE_VAL);

    Collection valColl = hashMap.values();
    assertNotNull(valColl);
    assertEquals(valColl.size(), SIZE_ONE);

    Iterator itVal = valColl.iterator();
    String val = (String) itVal.next();
    assertEquals(val, VALUE_VAL);
  }

  protected Map makeConfirmedMap() {
    return new IdentityHashMap();
  }

  protected Map makeEmptyMap() {
    return new IdentityHashMap();
  }

  @Override
  protected boolean useNullValue() {
    // The JRE IdentityHashMap always thinks it has a null value.
    return false;
  }

  private Iterator iterateThrough(final IdentityHashMap expected) {
    Iterator iter = expected.entrySet().iterator();
    for (int i = 0; i < expected.size(); i++) {
      iter.next();
    }
    return iter;
  }

  private String makeEntryString(final String key, final String value) {
    return "{" + key + "=" + value + "}";
  }
}
