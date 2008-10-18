/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Tests <code>FastStringMap</code>Right now, no tests are directly run here,
 * because the tests are run in mapTest.FastStringMapTest. This is because
 * otherwise the inclusion of the map testing code causes the system to generate
 * many compiler errors during unit testing, thereby making real errors harder
 * to spot.
 */
public class FastStringMapTest extends GWTTestCase {

  /**
   * These is an example of two correctly formatted java API specification.
   */
  public static Map<String, String> makeEmptyMap() {
    return new FastStringMap<String>();
  }

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void test() {
    // Only FastStringMap specific tests should go here. Look in
    // com.google.gwt.user.maptests.FastStringMapTest for all apache Map tests.
  }
  
  /*
   * Test for collisions between stored strings and JavaScript Object
   * properties.
   */
  public void testJSOCollision() {
    Map<String, String> map = makeEmptyMap();
    assertEquals(0, map.size());
    map.put("k1", "v1");
    assertEquals(1, map.size());
    assertEquals("v1", map.get("k1"));
    map.put("toString", "toStringVal");
    assertEquals(2, map.size());
    assertEquals("toStringVal", map.get("toString"));
    map.put("watch", "watchVal");
    Set<String> keys = map.keySet();
    assertEquals(3, keys.size());
    map.put("__proto__", "__proto__Val");
    assertEquals(4 ,map.size());
    assertEquals("__proto__Val", map.get("__proto__"));
    map.put("k1", "v1b");
    keys = map.keySet();
    assertEquals(4, keys.size());
    Collection<String> values = map.values();
    assertEquals(4, values.size());
    map.put("k2", "v1b");
    values = map.values();
    assertEquals(5, values.size());
    map.put("","empty");
    assertEquals("empty", map.get(""));  
    map.remove("k2");
    assertEquals(5, values.size());
  }

}
