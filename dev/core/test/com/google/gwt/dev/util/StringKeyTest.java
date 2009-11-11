/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.util;

import junit.framework.TestCase;

/**
 * Tests the StringKey utility type.
 */
public class StringKeyTest extends TestCase {

  static final class KeyA extends StringKey {
    public KeyA(String value) {
      super(value);
    }
  };

  static final class KeyB extends StringKey {
    public KeyB(String value) {
      super(value);
    }
  };

  /**
   * Assert that <code>a > b</code>.
   */
  private static <T extends Comparable<T>> void assertGT(T a, T b) {
    assertTrue(a.compareTo(b) > 0);
  }

  /**
   * Assert that <code>a < b</code>.
   */
  private static <T extends Comparable<T>> void assertLT(T a, T b) {
    assertTrue(a.compareTo(b) < 0);
  }

  public void test() {
    StringKey a = new KeyA("hello");
    StringKey b = new KeyB("world");

    assertEquals(a, a);
    assertEquals(a, new KeyA("hello"));
    assertLT(a, new KeyA("helloLater"));
    assertGT(a, new KeyA("h"));
    assertLT(a, b);

    assertEquals(b, new KeyB("world"));
    assertLT(b, new KeyB("worldLater"));
    assertGT(b, new KeyB("w"));
    assertGT(b, a);
  }

  public void testNull() {
    StringKey a = new KeyA(null);
    StringKey b = new KeyB(null);

    assertEquals(a, a);
    assertEquals(a, new KeyA(null));
    assertLT(a, new KeyA(""));
    assertLT(a, b);

    assertEquals(new KeyA(null), a);
    assertGT(new KeyA(""), a);
    assertGT(b, a);
  }
}
