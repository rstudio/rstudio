// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HashMapTest extends EmulTestBase {
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

    assertNotNull(hashMap.entrySet().iterator());
    assertFalse(hashMap.entrySet().iterator().hasNext());
}

  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /*
   * Test method for 'java.util.HashMap.clear()'
   */
  public void testClear() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    hashMap.put("Hello", "Bye");
    assertFalse(hashMap.isEmpty());
    assertTrue(hashMap.size() == 1);

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

    final String KEY1 = "1";
    final String VAL1 = "2";
    final String KEY2 = "3";
    final String VAL2 = "4";
    final String KEY3 = "5";
    final String VAL3 = "6";

    // Check non-empty clone behavior
    srcMap.put(KEY1, VAL1);
    srcMap.put(KEY2, VAL2);
    srcMap.put(KEY3, VAL3);
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
    final String KEY = "testContainsKey";
    final Integer VAL = new Integer(5);
    final String NON_EXISTANT_KEY = "does not exist";

    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertFalse(hashMap.containsKey(KEY));
    hashMap.put(KEY, VAL);
    assertTrue(hashMap.containsKey(KEY));
    assertFalse(hashMap.containsKey(NON_EXISTANT_KEY));

    assertFalse(hashMap.containsKey(null));
    hashMap.put(null, VAL);
    assertTrue(hashMap.containsKey(null));
  }

  /*
   * Test method for 'java.util.HashMap.containsValue(Object)'
   */
  public void testContainsValue() {
    final String KEY = "testContainsValue";
    final Integer VAL = new Integer(5);
    final String NON_EXISTANT_KEY = "does not exist";

    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertFalse(hashMap.containsValue(VAL));
    hashMap.put(KEY, VAL);
    assertTrue(hashMap.containsValue(VAL));
    assertFalse(hashMap.containsValue(NON_EXISTANT_KEY));

    assertFalse(hashMap.containsValue(null));
    hashMap.put(KEY, null);
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

    final String KEY = "testEntrySet";
    final String VAL1 = KEY + " - value1";
    final String VAL2 = KEY + " - value2";

    // Check that the entry set looks right
    hashMap.put(KEY, VAL1);
    entrySet = hashMap.entrySet();
    assertEquals(entrySet.size(), 1);
    Iterator itSet = entrySet.iterator();
    Map.Entry entry = (Map.Entry) itSet.next();
    assertEquals(entry.getKey(), KEY);
    assertEquals(entry.getValue(), VAL1);

    // Check that entries in the entryset are update correctly on overwrites
    hashMap.put(KEY, VAL2);
    entrySet = hashMap.entrySet();
    assertEquals(entrySet.size(), 1);
    itSet = entrySet.iterator();
    entry = (Map.Entry) itSet.next();
    assertEquals(entry.getKey(), KEY);
    assertEquals(entry.getValue(), VAL2);

    // Check that entries are updated on removes
    hashMap.remove(KEY);
    checkEmptyHashMapAssumptions(hashMap);
  }

  /*
   * Test method for 'java.util.AbstractMap.equals(Object)'
   */
  public void testEquals() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    final String KEY = "key";
    final String VAL = "val";

    hashMap.put(KEY, VAL);

    HashMap copyMap = (HashMap) hashMap.clone();

    assertTrue(hashMap.equals(copyMap));
    hashMap.put(VAL, KEY);
    assertFalse(hashMap.equals(copyMap));
  }

  /*
   * Test method for 'java.lang.Object.finalize()'
   */
  public void testFinalize() {
  }

  /*
   * Test method for 'java.util.HashMap.get(Object)'
   */
  public void testGet() {
    final String KEY = "testGet";
    final String VAL = KEY + " - Value";

    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertNull(hashMap.get(KEY));
    hashMap.put(KEY, VAL);
    assertNotNull(hashMap.get(KEY));

    assertNull(hashMap.get(null));
    hashMap.put(null, VAL);
    assertNotNull(hashMap.get(null));

    hashMap.put(null, null);
    assertNull(hashMap.get(null));
  }

  /*
   * Test method for 'java.util.AbstractMap.hashCode()'
   */
  public void testHashCode() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    // Check that hashCode changes
    int hashCode1 = hashMap.hashCode();
    hashMap.put("key", "val");
    int hashCode2 = hashMap.hashCode();

    assertTrue(hashCode1 != hashCode2);
  }

  /*
   * Test method for 'java.util.HashMap.HashMap()'
   */
  public void testHashMap() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);
  }

  /*
   * Test method for 'java.util.HashMap.HashMap(int)'
   */
  public void testHashMapInt() {
    final int CAPACITY = 16;
    HashMap hashMap = new HashMap(CAPACITY);
    checkEmptyHashMapAssumptions(hashMap);

    // TODO(mmendez): how do we verify capacity?
    boolean failed = true;
    try {
      new HashMap(-1);
    } catch (Throwable ex) {
      if (ex instanceof IllegalArgumentException)
        failed = false;
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
    final int CAPACITY = 16;
    final float LOAD_FACTOR = 0.5F;

    HashMap hashMap = new HashMap(CAPACITY, LOAD_FACTOR);
    checkEmptyHashMapAssumptions(hashMap);

    // TODO(mmendez): how do we verify capacity and load factor?

    // Test new HashMap(-1, 0.0F)
    boolean failed = true;
    try {
      new HashMap(-1, 0.0F);
    } catch (Throwable ex) {
      if (ex instanceof IllegalArgumentException)
        failed = false;
    }

    if (failed) {
      fail("Failure testing new HashMap(-1, 0.0F)");
    }

    // Test new HashMap(0, -1.0F)
    failed = true;
    try {
      new HashMap(0, -1.0F);
    } catch (Throwable ex) {
      if (ex instanceof IllegalArgumentException)
        failed = false;
    }

    if (failed) {
      fail("Failure testing new HashMap(0, -1.0F)");
    }

    // Test new HashMap(0,0F);
    hashMap = new HashMap(0, 0.1F);
    assertNotNull(hashMap);
  }

  /*
   * Test method for 'java.util.HashMap.HashMap(Map)'
   */
  public void testHashMapMap() {
    HashMap srcMap = new HashMap();
    assertNotNull(srcMap);
    checkEmptyHashMapAssumptions(srcMap);

    final Object KEY1 = new Integer(1);
    final Object VAL1 = new Integer(1);
    final Object KEY2 = new Integer(2);
    final Object VAL2 = new Integer(2);
    final Object KEY3 = new Integer(3);
    final Object VAL3 = new Integer(3);

    srcMap.put(KEY1, VAL1);
    srcMap.put(KEY2, VAL2);
    srcMap.put(KEY3, VAL3);

    HashMap hashMap = new HashMap(srcMap);
    assertFalse(hashMap.isEmpty());
    assertTrue(hashMap.size() == 3);

    Collection valColl = hashMap.values();
    assertTrue(valColl.contains(VAL1));
    assertTrue(valColl.contains(VAL2));
    assertTrue(valColl.contains(VAL3));

    Collection keyColl = hashMap.keySet();
    assertTrue(keyColl.contains(KEY1));
    assertTrue(keyColl.contains(KEY2));
    assertTrue(keyColl.contains(KEY3));
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

    final String KEY = "key";
    final String VAL = "val";
    dstMap.put(KEY, VAL);
    assertFalse(dstMap.isEmpty());

    dstMap.remove(KEY);
    assertTrue(dstMap.isEmpty());
    assertEquals(dstMap.size(), 0);
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

    final String KEY = "testKeySet";
    final String VAL = KEY + " - value";
    hashMap.put(KEY, VAL);

    assertTrue(keySet.size() == 1);
    assertTrue(keySet.contains(KEY));
    assertFalse(keySet.contains(VAL));
    assertFalse(keySet.contains(KEY.toUpperCase()));
  }

  /*
   * Test method for 'java.util.HashMap.put(Object, Object)'
   */
  public void testPut() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    final String KEY = "testPut";
    final String VAL1 = KEY + " - value 1";
    final String VAL2 = KEY + " - value 2";

    assertNull(hashMap.put(KEY, VAL1));
    assertEquals(hashMap.put(KEY, VAL2), VAL1);
    assertNull(hashMap.put(null, VAL1));
    assertEquals(hashMap.put(null, VAL2), VAL1);
  }

  /*
   * Test method for 'java.util.HashMap.putAll(Map)'
   */
  public void testPutAll() {
    final String KEY1 = "key1";
    final String VAL1 = "val1";
    final String KEY2 = "key2";
    final String VAL2 = "val2";
    final String KEY3 = "key3";
    final String VAL3 = "val3";
    final String KEY4 = "key4";
    final String VAL4 = "val4";

    HashMap srcMap = new HashMap();
    checkEmptyHashMapAssumptions(srcMap);

    srcMap.put(KEY1, VAL1);
    srcMap.put(KEY2, VAL2);
    srcMap.put(KEY3, VAL3);

    // Make sure that the data is copied correctly
    HashMap dstMap = new HashMap();
    checkEmptyHashMapAssumptions(dstMap);

    dstMap.putAll(srcMap);
    assertEquals(srcMap.size(), dstMap.size());
    assertTrue(dstMap.containsKey(KEY1));
    assertTrue(dstMap.containsValue(VAL1));
    assertFalse(dstMap.containsKey(KEY1.toUpperCase()));
    assertFalse(dstMap.containsValue(VAL1.toUpperCase()));

    assertTrue(dstMap.containsKey(KEY2));
    assertTrue(dstMap.containsValue(VAL2));
    assertFalse(dstMap.containsKey(KEY2.toUpperCase()));
    assertFalse(dstMap.containsValue(VAL2.toUpperCase()));

    assertTrue(dstMap.containsKey(KEY3));
    assertTrue(dstMap.containsValue(VAL3));
    assertFalse(dstMap.containsKey(KEY3.toUpperCase()));
    assertFalse(dstMap.containsValue(VAL3.toUpperCase()));

    // Check that an empty map does not blow away the contents of the dest map
    HashMap emptyMap = new HashMap();
    checkEmptyHashMapAssumptions(emptyMap);
    dstMap.putAll(emptyMap);
    assertTrue(dstMap.size() == srcMap.size());

    // Check that put all overwrite any existing mapping in the dst map
    srcMap.put(KEY1, VAL2);
    srcMap.put(KEY2, VAL3);
    srcMap.put(KEY3, VAL1);

    dstMap.putAll(srcMap);
    assertEquals(dstMap.size(), srcMap.size());
    assertEquals(dstMap.get(KEY1), VAL2);
    assertEquals(dstMap.get(KEY2), VAL3);
    assertEquals(dstMap.get(KEY3), VAL1);

    // Check that a putall does adds data but does not remove it

    srcMap.put(KEY4, VAL4);
    dstMap.putAll(srcMap);
    assertEquals(dstMap.size(), srcMap.size());
    assertTrue(dstMap.containsKey(KEY4));
    assertTrue(dstMap.containsValue(VAL4));
    assertEquals(dstMap.get(KEY1), VAL2);
    assertEquals(dstMap.get(KEY2), VAL3);
    assertEquals(dstMap.get(KEY3), VAL1);
    assertEquals(dstMap.get(KEY4), VAL4);

    dstMap.putAll(dstMap);
  }

  /*
   * Test method for 'java.util.HashMap.remove(Object)'
   */
  public void testRemove() {
    final String KEY = "testRemove";
    final String VAL = KEY + " - value";

    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertNull(hashMap.remove(null));
    hashMap.put(null, VAL);
    assertNotNull(hashMap.remove(null));

    hashMap.put(KEY, VAL);
    assertEquals(hashMap.remove(KEY), VAL);
    assertNull(hashMap.remove(KEY));
  }

  /*
   * Test method for 'java.util.HashMap.size()'
   */
  public void testSize() {
    final String KEY1 = "key1";
    final String VAL1 = KEY1 + " - value";
    final String KEY2 = "key2";
    final String VAL2 = KEY2 + " - value";
    final String KEY3 = "key3";
    final String VAL3 = KEY3 + " - value";

    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    // Test size behavior on put
    assertEquals(hashMap.size(), 0);
    hashMap.put(KEY1, VAL1);
    assertEquals(hashMap.size(), 1);
    hashMap.put(KEY2, VAL2);
    assertEquals(hashMap.size(), 2);
    hashMap.put(KEY3, VAL3);
    assertEquals(hashMap.size(), 3);

    // Test size behavior on remove
    hashMap.remove(KEY1);
    assertEquals(hashMap.size(), 2);
    hashMap.remove(KEY2);
    assertEquals(hashMap.size(), 1);
    hashMap.remove(KEY3);
    assertEquals(hashMap.size(), 0);

    // Test size behavior on putAll
    hashMap.put(KEY1, VAL1);
    hashMap.put(KEY2, VAL2);
    hashMap.put(KEY3, VAL3);

    HashMap srcMap = new HashMap(hashMap);
    hashMap.putAll(srcMap);
    assertEquals(hashMap.size(), 3);

    // Test size behavior on clear
    hashMap.clear();
    assertEquals(hashMap.size(), 0);
  }

  /*
   * Test method for 'java.util.AbstractMap.toString()'
   */
  public void testToString() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    final String KEY = "key";
    final String VAL = "val";
    final String STR = "{" + KEY + "=" + VAL + "}";
    hashMap.put(KEY, VAL);

    assertTrue(STR.equals(hashMap.toString()));
  }

  /**
   * Test method for 'java.util.AbstractMap.values()'
   */
  public void testValues() {
    HashMap hashMap = new HashMap();
    checkEmptyHashMapAssumptions(hashMap);

    assertNotNull(hashMap.values());

    final String KEY = "key";
    final String VAL = "val";

    hashMap.put(KEY, VAL);

    Collection valColl = hashMap.values();
    assertNotNull(valColl);
    assertEquals(valColl.size(), 1);

    Iterator itVal = valColl.iterator();
    String val = (String) itVal.next();
    assertEquals(val, VAL);
  }
}
