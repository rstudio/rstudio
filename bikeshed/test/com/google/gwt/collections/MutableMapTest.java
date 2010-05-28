/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.collections;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Common tests for all classes inheriting from {@link MutableMap}. 
 * Override {@link #gwtSetUp} to initialize {@code keyA} and {@code keyB},
 * {@link #getMap()} with the map constructor.
 * Optionally override {@link #testNullKey()} to test special behavior
 * related to null keys as this test case assumes null keys are a valid case.
 * 
 * @param <K> type of keys to test.
 */
public abstract class MutableMapTest<K> extends GWTTestCase {
  
  protected K keyA;

  protected K keyB;

  @Override
  public String getModuleName() {
    return null;
  }

  public void testClear() {
    MutableMap<K, Integer> msm = getMap();
    
    msm.put(keyA, 1);
    msm.put(keyB, 2);
    
    msm.clear();
    assertFalse(msm.containsKey(keyA));
    assertFalse(msm.containsKey(keyB));
    assertTrue(msm.isEmpty());
  }

  public void testContainsKey() {
    MutableMap<K, Integer> msm = getMap();
    
    assertFalse(msm.containsKey(keyA));
    
    msm.put(keyA, 1);
    assertTrue(msm.containsKey(keyA));
    
    msm.remove(keyA);
    assertFalse(msm.containsKey(keyA));
  }

  public void testGet() {
    MutableMap<K, Integer> msm = getMap();
    
    assertNull(msm.get(keyA));
    
    msm.put(keyA, 1);
    assertEquals(1, (int) msm.get(keyA));
    
    msm.put(keyB, null);
    assertNull(msm.get(keyB));
  }
  
  public void testIsEmpty() {
    MutableMap<K, Integer> msm = getMap();
    assertTrue(msm.isEmpty());
    
    msm.put(keyA, 1);
    assertFalse(msm.isEmpty());
    
    msm.remove(keyA);
    assertTrue(msm.isEmpty());    
  }

  /**
   * Tests expected behavior for <code>null</code> keys. Override this test
   * for maps not supporting null keys.
   */
  public void testNullKey() {
    MutableMap<K, Integer> msm = getMap();

    assertFalse(msm.containsKey(null));
    assertNull(msm.get(null));
    
    msm.put(null, 1);
    assertFalse(msm.isEmpty());
    assertTrue(msm.containsKey(null));
    assertEquals(1, (int) msm.get(null));
    
    msm.remove(null);
    assertTrue(msm.isEmpty());
    assertFalse(msm.containsKey(null));
    assertNull(msm.get(null));    
  }
  
  public void testPut() {
    MutableMap<K, Integer> msm = getMap();
    
    msm.put(keyA, 1);
    assertEquals(1, (int) msm.get(keyA));
    
    msm.put(keyA, 2);
    assertEquals(2, (int) msm.get(keyA));
    
    msm.put(keyB, 3);
    assertEquals(2, (int) msm.get(keyA));
    assertEquals(3, (int) msm.get(keyB));    
  }
  
  public void testRemove() {
    MutableMap<K, Integer> msm = getMap();
    
    msm.put(keyA, 1);    
    msm.remove(keyA);
    assertTrue(msm.isEmpty());
    assertNull(msm.get(keyA));
  }
  
  protected abstract MutableMap<K, Integer> getMap();

}
