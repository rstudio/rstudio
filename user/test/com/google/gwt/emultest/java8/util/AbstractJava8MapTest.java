/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.emultest.java8.util;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Tests for java.util.Map implementing classes Java 8 API emulation.
 */
abstract class AbstractJava8MapTest extends EmulTestBase {

  private Map<String, String> testSample;

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    testSample = createTestSample();
  }

  public void testCompute() {
    Map<String, String> map = createTestMap();

    String value = map.compute("a", (k, v) -> k + " - " + v);
    assertEquals("a - A", value);
    assertTrue(map.containsKey("a"));
    assertEquals("a - A", map.get("a"));

    value = map.compute("a", (k, v) -> null);
    assertNull(value);
    assertFalse(map.containsKey("a"));

    value = map.compute("a", (k, v) -> {
      assertNull(v);
      return k.toUpperCase();
    });
    assertEquals("A", value);
    assertTrue(map.containsKey("a"));
    assertEquals("A", map.get("a"));
  }

  public void testComputeIfAbsent() {
    Map<String, String> map = createTestMap();

    String value = map.computeIfAbsent("a", k -> {
      fail();
      return null;
    });
    assertEquals("A", value);
    assertTrue(map.containsKey("a"));
    assertEquals("A", map.get("a"));

    map.remove("a");
    value = map.computeIfAbsent("a", String::toUpperCase);
    assertEquals("A", value);
    assertTrue(map.containsKey("a"));
    assertEquals("A", map.get("a"));

    map.remove("a");
    value = map.computeIfAbsent("a", k -> null);
    assertNull(value);
    assertFalse(map.containsKey("a"));
  }

  public void testComputeIfPresent() {
    Map<String, String> map = createTestMap();

    String value = map.computeIfPresent("a", (k, v) -> k + " - " + v);
    assertEquals("a - A", value);
    assertTrue(map.containsKey("a"));
    assertEquals("a - A", map.get("a"));

    value = map.computeIfPresent("a", (k, v) -> null);
    assertNull(value);
    assertFalse(map.containsKey("a"));

    value = map.computeIfPresent("a", (k, v) -> {
      fail();
      return null;
    });
    assertNull(value);
    assertFalse(map.containsKey("a"));
  }

  public void testForeach() {
    Map<String, String> map = createTestMap();
    Map<String, String> expected = new HashMap<>(testSample);

    assertEquals(expected.size(), map.size());
    map.forEach((k, v) -> {
      assertTrue(expected.containsKey(k));
      assertEquals(expected.get(k), v);
      expected.remove(k);
    });
    assertTrue(expected.isEmpty());
  }

  public void testGetOrDefault() {
    Map<String, String> map = createTestMap();

    String value = map.getOrDefault("a", null);
    assertEquals("A", value);
    assertTrue(map.containsKey("a"));
    assertEquals("A", map.get("a"));

    map.remove("a");
    value = map.getOrDefault("a", "A");
    assertEquals("A", value);
    assertFalse(map.containsKey("a"));
    assertNull(map.get("a"));

    map.put("a", null);
    value = map.getOrDefault("a", "A");
    assertNull(value);
    assertTrue(map.containsKey("a"));
    assertNull(map.get("a"));
  }

  public void testMerge() {
    Map<String, String> map = createTestMap();

    String newValue = map.merge("a", "a", (currentValue, value) -> {
      assertEquals("A", currentValue);
      assertEquals("a", value);
      return value;
    });
    assertEquals(newValue, "a");
    assertTrue(map.containsKey("a"));
    assertEquals("a", map.get("a"));

    try {
      map.merge("a", null, (currentValue, value) -> "");
      fail();
    } catch (NullPointerException expected) {
    }

    newValue = map.merge("a", "", (currentValue, value) -> null);
    assertNull(newValue);
    assertFalse(map.containsKey("a"));
    assertNull(map.get("a"));

    newValue = map.merge("a", "A", (currentValue, value) -> value);
    assertEquals("A", newValue);
    assertTrue(map.containsKey("a"));
    assertEquals("A", map.get("a"));
  }

  public void testPutIfAbsent() {
    Map<String, String> map = createTestMap();

    String oldValue = map.putIfAbsent("a", "a");
    assertEquals("A", oldValue);
    assertTrue(map.containsKey("a"));
    assertEquals("A", map.get("a"));

    map.remove("a");
    oldValue = map.putIfAbsent("a", "a");
    assertNull(oldValue);
    assertTrue(map.containsKey("a"));
    assertEquals("a", map.get("a"));
  }

  public void testRemove() {
    Map<String, String> map = createTestMap();

    assertFalse(map.remove("a", "a"));
    assertTrue(map.containsKey("a"));
    assertEquals("A", map.get("a"));

    assertTrue(map.remove("a", "A"));
    assertFalse(map.containsKey("a"));

    assertFalse(map.remove("a", null));

    map.put("a", null);
    assertTrue(map.remove("a", null));
    assertFalse(map.containsKey("a"));
  }

  public void testReplace() {
    Map<String, String> map = createTestMap();

    String oldValue = map.replace("a", "a");
    assertEquals("A", oldValue);
    assertTrue(map.containsKey("a"));
    assertEquals("a", map.get("a"));

    map.remove("a");
    oldValue = map.replace("a", "A");
    assertNull(oldValue);
    assertFalse(map.containsKey("a"));
    assertNull(map.get("a"));
  }

  public void testReplace_Key_OldValue_NewValue() {
    Map<String, String> map = createTestMap();

    assertTrue(map.replace("a", "A", "a"));
    assertTrue(map.containsKey("a"));
    assertEquals("a", map.get("a"));

    assertFalse(map.replace("a", "A", "a"));

    assertTrue(map.replace("a", "a", null));
    assertTrue(map.containsKey("a"));
    assertNull(map.get("a"));

    map.remove("a");
    assertFalse(map.replace("a", "a", "A"));
    assertFalse(map.containsKey("a"));
    assertNull(map.get("a"));
  }

  public void testReplaceAll() {
    Map<String, String> map = createTestMap();
    map.replaceAll((k, v) -> v.toLowerCase());

    assertEquals(testSample.size(), map.size());
    for (Entry<String, String> entry : testSample.entrySet()) {
      assertTrue(map.containsKey(entry.getKey()));
      assertEquals(map.get(entry.getKey()), entry.getValue().toLowerCase());
    }
  }

  private Map<String, String> createTestMap() {
    Map<String, String> map = createMap();
    map.putAll(testSample);
    return map;
  }

  private static Map<String, String> createTestSample() {
    Map<String, String> map = new HashMap<>();
    map.put("a", "A");
    map.put("b", "B");
    map.put("c", "C");
    return Collections.unmodifiableMap(map);
  }

  protected abstract Map<String, String> createMap();

}
