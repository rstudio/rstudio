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

import org.apache.commons.collections.TestMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Tests <code>HashMap</code>.
 */
@SuppressWarnings("unchecked")
public class HashMapTest extends TestMap {
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
    @Override
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

  private static void assertEmptyIterator(Iterator it) {
    assertNotNull(it);
    assertFalse(it.hasNext());
    try {
      it.next();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }
  }

  /**
   * Check the state of a newly constructed, empty HashMap.
   * 
   * @param hashMap
   */
  private static void checkEmptyHashMapAssumptions(HashMap hashMap) {
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

    assertEmptyIterator(hashMap.entrySet().iterator());
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testAddEqualKeys() {
    final HashMap expected = new HashMap();
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
    HashMap m = new HashMap();
    m.put("watch", "watch");
    assertEquals(m.get("watch"), "watch");
  }

  /*
   * Test method for 'java.util.HashMap.clear()'
   */
  public void testClear() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    hashMap.put("Hello", "Bye");
    assertFalse(hashMap.isEmpty());
    assertTrue(hashMap.size() == SIZE_ONE);

    hashMap.clear();
    assertTrue(hashMap.isEmpty());
    assertTrue(hashMap.size() == 0);
  }

  /*
   * Test method for 'java.util.HashMap.clone()'
   */
  public void testClone() {
    HashMap srcMap = new HashMap();
    checkEmptyHashMapAssumptions(srcMap);

    // Check empty clone behavior
    HashMap dstMap = (HashMap) srcMap.clone();
    assertNotNull(dstMap);
    assertEquals(dstMap.size(), srcMap.size());
    // assertTrue(dstMap.values().toArray().equals(srcMap.values().toArray()));
    assertTrue(dstMap.keySet().equals(srcMap.keySet()));
    assertTrue(dstMap.entrySet().equals(srcMap.entrySet()));

    // Check non-empty clone behavior
    srcMap.put(KEY_1, VALUE_1);
    srcMap.put(KEY_2, VALUE_2);
    srcMap.put(KEY_3, VALUE_3);
    dstMap = (HashMap) srcMap.clone();
    assertNotNull(dstMap);
    assertEquals(dstMap.size(), srcMap.size());

    assertTrue(dstMap.keySet().equals(srcMap.keySet()));

    assertTrue(dstMap.entrySet().equals(srcMap.entrySet()));
  }

  /*
   * Test method for 'java.util.HashMap.containsKey(Object)'
   */
  public void testContainsKey() {
    HashMap hashMap = new HashMap();
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
   * Test method for 'java.util.HashMap.containsValue(Object)'
   */
  public void testContainsValue() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertFalse("check contains of empty map",
        hashMap.containsValue(VALUE_TEST_CONTAINS_KEY));
    hashMap.put(KEY_TEST_CONTAINS_VALUE, VALUE_TEST_CONTAINS_KEY);
    assertTrue("check contains of map with element",
        hashMap.containsValue(VALUE_TEST_CONTAINS_KEY));
    assertFalse("check contains of map other element",
        hashMap.containsValue(VALUE_TEST_CONTAINS_DOES_NOT_EXIST));

    assertFalse(hashMap.containsValue(null));
    hashMap.put(KEY_TEST_CONTAINS_VALUE, null);
    assertTrue(hashMap.containsValue(null));
  }

  /*
   * Test method for 'java.util.HashMap.entrySet()'
   */
  public void testEntrySet() {
    HashMap hashMap = new HashMap();
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
    assertEmptyIterator(itSet);

    // Check that entries in the entrySet are update correctly on overwrites
    hashMap.put(KEY_TEST_ENTRY_SET, VALUE_TEST_ENTRY_SET_2);
    entrySet = hashMap.entrySet();
    assertEquals(entrySet.size(), SIZE_ONE);
    itSet = entrySet.iterator();
    entry = (Map.Entry) itSet.next();
    assertEquals(entry.getKey(), KEY_TEST_ENTRY_SET);
    assertEquals(entry.getValue(), VALUE_TEST_ENTRY_SET_2);
    assertEmptyIterator(itSet);

    // Check that entries are updated on removes
    hashMap.remove(KEY_TEST_ENTRY_SET);
    checkEmptyHashMapAssumptions(hashMap);
  }

  /*
   * Used to test the entrySet entry's set method.
   */
  public void testEntrySetEntrySetterNonString() {
    HashMap hashMap = new HashMap();
    hashMap.put(1, 2);
    Set entrySet = hashMap.entrySet();
    Entry entry = (Entry) entrySet.iterator().next();

    entry.setValue(3);
    assertEquals(3, hashMap.get(1));

    hashMap.put(1, 4);
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
    hashMap.put("A", "B");
    Set entrySet = hashMap.entrySet();
    Entry entry = (Entry) entrySet.iterator().next();

    entry.setValue("C");
    assertEquals("C", hashMap.get("A"));

    hashMap.put("A", "D");
    assertEquals("D", entry.getValue());

    assertEquals(1, hashMap.size());
  }

  /*
   * Used to test the entrySet remove method.
   */
  public void testEntrySetRemove() {
    HashMap hashMap = new HashMap();
    hashMap.put("A", "B");
    HashMap dummy = new HashMap();
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
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    hashMap.put(KEY_KEY, VALUE_VAL);

    HashMap copyMap = (HashMap) hashMap.clone();

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
   * Test method for 'java.util.HashMap.get(Object)'.
   */
  public void testGet() {
    HashMap hashMap = new HashMap();
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
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    // Check that hashCode changes
    int hashCode1 = hashMap.hashCode();
    hashMap.put(KEY_KEY, VALUE_VAL);
    int hashCode2 = hashMap.hashCode();

    assertTrue(hashCode1 != hashCode2);
  }

  /*
   * Test method for 'java.util.HashMap.HashMap()'.
   */
  public void testHashMap() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);
  }

  /*
   * Test method for 'java.util.HashMap.HashMap(int)'
   */
  public void testHashMapInt() {
    HashMap hashMap = new HashMap(CAPACITY_16);
    checkEmptyHashMapAssumptions(hashMap);

    // TODO(mmendez): how do we verify capacity?
    boolean failed = true;
    try {
      new HashMap(-SIZE_ONE);
    } catch (Throwable ex) {
      if (ex instanceof IllegalArgumentException) {
        failed = false;
      }
    }

    if (failed) {
      fail("Failure testing new HashMap(-1)");
    }

    HashMap zeroSizedHashMap = new HashMap(0);
    assertNotNull(zeroSizedHashMap);
  }

  /*
   * Test method for 'java.util.HashMap.HashMap(int, float)'
   */
  public void testHashMapIntFloat() {

    HashMap hashMap = new HashMap(CAPACITY_16, LOAD_FACTOR_ONE_HALF);
    checkEmptyHashMapAssumptions(hashMap);

    // TODO(mmendez): how do we verify capacity and load factor?

    // Test new HashMap(-1, 0.0F)
    boolean failed = true;
    try {
      new HashMap(CAPACITY_NEG_ONE_HALF, LOAD_FACTOR_ZERO);
    } catch (Throwable ex) {
      if (ex instanceof IllegalArgumentException) {
        failed = false;
      }
    }

    if (failed) {
      fail("Failure testing new HashMap(-1, 0.0F)");
    }

    // Test new HashMap(0, -1.0F)
    failed = true;
    try {
      new HashMap(CAPACITY_ZERO, LOAD_FACTOR_NEG_ONE);
    } catch (Throwable ex) {
      if (ex instanceof IllegalArgumentException) {
        failed = false;
      }
    }

    if (failed) {
      fail("Failure testing new HashMap(0, -1.0F)");
    }

    // Test new HashMap(0,0F);
    hashMap = new HashMap(CAPACITY_ZERO, LOAD_FACTOR_ONE_TENTH);
    assertNotNull(hashMap);
  }

  /*
   * Test method for 'java.util.HashMap.HashMap(Map)'
   */
  public void testHashMapMap() {
    HashMap srcMap = new HashMap();
    assertNotNull(srcMap);
    checkEmptyHashMapAssumptions(srcMap);

    srcMap.put(INTEGER_1, INTEGER_11);
    srcMap.put(INTEGER_2, INTEGER_22);
    srcMap.put(INTEGER_3, INTEGER_33);

    HashMap hashMap = new HashMap(srcMap);
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

  /*
   * Test method for 'java.util.AbstractMap.isEmpty()'
   */
  public void testIsEmpty() {
    HashMap srcMap = new HashMap();
    checkEmptyHashMapAssumptions(srcMap);

    HashMap dstMap = new HashMap();
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
    HashMap hashMap = new HashMap();

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
   * Test method for 'java.util.HashMap.keySet()'
   */
  public void testKeySet() {
    HashMap hashMap = new HashMap();
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
   * Test from issue 2499.
   * The problem was actually in other objects hashCode() function, as
   * the value was not coerced to an int and therefore parseInt in
   * AbstractHashMap.addAllHashEntries was failing.  This was fixed
   * both in our JRE classes to ensure int overflow is handled properly
   * in their hashCode() implementation and in HashMap so that user
   * objects which don't account for int overflow will still be
   * handled properly.  There is a slight performance penalty, as
   * the coercion will be done twice, but that should be fixeable with
   * improved compiler optimization.
   */
  public void testLargeHashCodes() {
    final int LIST_COUNT = 20;
    List<Integer> values = new ArrayList<Integer>(LIST_COUNT);
    for (int n = 0; n < LIST_COUNT; n++) {
      values.add(n);
    }
    Map<List<Integer>, String> testMap = new HashMap<List<Integer>, String>();
    testMap.put(values, "test");
    int count = 0;
    for (Map.Entry<List<Integer>, String> entry : testMap.entrySet()) {
      count++;
    }
    assertEquals(testMap.size(), count);
  }

  /*
   * Test method for 'java.util.HashMap.put(Object, Object)'
   */
  public void testPut() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertNull(hashMap.put(KEY_TEST_PUT, VALUE_TEST_PUT_1));
    assertEquals(hashMap.put(KEY_TEST_PUT, VALUE_TEST_PUT_2), VALUE_TEST_PUT_1);
    assertNull(hashMap.put(null, VALUE_TEST_PUT_1));
    assertEquals(hashMap.put(null, VALUE_TEST_PUT_2), VALUE_TEST_PUT_1);
  }

  /**
   * Test method for 'java.util.HashMap.putAll(Map)'.
   */
  public void testPutAll() {
    HashMap srcMap = new HashMap();
    checkEmptyHashMapAssumptions(srcMap);

    srcMap.put(KEY_1, VALUE_1);
    srcMap.put(KEY_2, VALUE_2);
    srcMap.put(KEY_3, VALUE_3);

    // Make sure that the data is copied correctly
    HashMap dstMap = new HashMap();
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
    HashMap emptyMap = new HashMap();
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
   * Test method for 'java.util.HashMap.remove(Object)'.
   */
  public void testRemove() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertNull(hashMap.remove(null));
    hashMap.put(null, VALUE_TEST_REMOVE);
    assertNotNull(hashMap.remove(null));

    hashMap.put(KEY_TEST_REMOVE, VALUE_TEST_REMOVE);
    assertEquals(hashMap.remove(KEY_TEST_REMOVE), VALUE_TEST_REMOVE);
    assertNull(hashMap.remove(KEY_TEST_REMOVE));
  }

  /**
   * Test method for 'java.util.HashMap.size()'.
   */
  public void testSize() {
    HashMap hashMap = new HashMap();
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
    HashMap srcMap = new HashMap(hashMap);
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
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);
    hashMap.put(KEY_KEY, VALUE_VAL);
    String entryString = makeEntryString(KEY_KEY, VALUE_VAL);
    assertTrue(entryString.equals(hashMap.toString()));
  }

  /**
   * Test method for 'java.util.AbstractMap.values()'.
   */
  public void testValues() {
    HashMap hashMap = new HashMap();
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

  @Override
  protected Map makeEmptyMap() {
    return new HashMap();
  }

  private Iterator iterateThrough(final HashMap expected) {
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
