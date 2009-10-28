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

import java.util.Arrays;

/**
 * Tests {@link DiskCache}.
 */
public class DiskCacheTest extends TestCase {
  private final DiskCache diskCache = new DiskCache();

  public void testBytes() {
    byte[] a = new byte[0];
    byte[] b = new byte[] {1, 5, 9, 7, 3, 4, 2};
    byte[] c = new byte[3524];
    for (int i = 1; i < c.length; ++i) {
      c[i] = (byte) (i * 31 + c[i - 1]);
    }
    byte[][] insertOrder = new byte[][] {a, b, c, b, c, a, a, b, b, c, c, a};
    long[] tokens = new long[insertOrder.length];
    for (int i = 0; i < insertOrder.length; ++i) {
      tokens[i] = diskCache.writeByteArray(insertOrder[i]);
    }

    int testIndex = 0;
    for (int i = 0; i < 20; ++i) {
      testIndex += tokens[i % tokens.length];
      testIndex %= insertOrder.length;
      byte[] expected = insertOrder[testIndex];
      byte[] actual = diskCache.readByteArray(tokens[testIndex]);
      assertTrue("Values were not equals at index '" + testIndex + "'",
          Arrays.equals(expected, actual));
    }
  }

  public void testStrings() {
    String a = "";
    String b = "abjdsfkl;jasdf";
    char[] c = new char[2759];
    for (int i = 1; i < c.length; ++i) {
      c[i] = (char) (i * 31 + c[i - 1]);
      // Avoid problematic characters.
      c[i] &= 0x7FFF;
      --c[i];
    }
    String s = String.valueOf(c);
    String[] insertOrder = new String[] {s, a, b, s, b, s, a, a, s, b, b, a, s};
    long[] tokens = new long[insertOrder.length];
    for (int i = 0; i < insertOrder.length; ++i) {
      tokens[i] = diskCache.writeString(insertOrder[i]);
    }

    int testIndex = 0;
    for (int i = 0; i < 20; ++i) {
      testIndex += tokens[i % tokens.length];
      testIndex %= insertOrder.length;
      String expected = insertOrder[testIndex];
      String actual = diskCache.readString(tokens[testIndex]);
      assertEquals("Values were not equals at index '" + testIndex + "'",
          expected, actual);
    }
  }
}
