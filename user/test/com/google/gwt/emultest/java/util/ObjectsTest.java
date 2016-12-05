/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Comparator;
import java.util.Objects;

/**
 * Tests {@link Objects}.
 */
public class ObjectsTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testCompare() {
    Comparator<Integer> intComparator = new Comparator<Integer>() {
      @SuppressWarnings("NumberEquality")
      @Override
      public int compare(Integer a, Integer b) {
        if (a == b) {
          fail("comparator must not be called if a == b");
        }

        if (a == null) {
          return -1;
        }
        if (b == null) {
          return 1;
        }
        return a.compareTo(b);
      }
    };

    assertEquals(0, Objects.compare(null, null, intComparator));
    assertEquals(-1, Objects.compare(null, new Integer(12345), intComparator));
    assertEquals(1, Objects.compare(new Integer(12345), null, intComparator));
    assertEquals(-1, Objects.compare(new Integer(12345), new Integer(12346), intComparator));
    assertEquals(1, Objects.compare(new Integer("12345"), new Integer(12344), intComparator));
    assertEquals(0, Objects.compare(new Integer("12345"), new Integer(12345), intComparator));
  }

  public void testDeepEquals() {
    assertTrue(Objects.deepEquals(null, null));
    assertFalse(Objects.deepEquals(null, "not null"));
    assertFalse(Objects.deepEquals("not null", null));
    assertTrue(Objects.deepEquals(new Integer("1234"), new Integer(1234)));
    assertFalse(Objects.deepEquals(new Object(), new Object()));

    Object obj = new Object();
    assertTrue(Objects.deepEquals(obj, obj));

    assertFalse(Objects.deepEquals(new int[]{1}, new double[]{1}));
    assertFalse(Objects.deepEquals(new int[0], new double[0]));
    assertTrue(Objects.deepEquals((Object) new Object[]{"a"}, (Object) new String[]{"a"}));
    assertTrue(Objects.deepEquals((Object) new String[]{"a"}, (Object) new Object[]{"a"}));

    int[] intArray1 = new int[] { 2, 3, 5};
    int[] intArray2 = new int[] { 3, 1};
    int[] intArray3 = new int[] { 2, 3, 5};
    assertFalse(Objects.deepEquals(intArray1, intArray2));
    assertFalse(Objects.deepEquals(intArray2, intArray3));
    assertTrue(Objects.deepEquals(intArray1, intArray1));
    assertTrue(Objects.deepEquals(intArray1, intArray3));

    assertTrue(Objects.deepEquals(new int[][]{new int[]{1}}, new int[][]{new int[]{1}}));
    assertFalse(Objects.deepEquals(new int[][]{new int[]{1}}, new double[][]{new double[]{1}}));
  }

  @DoNotRunWith(Platform.Devel) // Objects.equals(String, String)
  public void testEquals() {
    assertTrue(Objects.equals(null, null));
    assertFalse(Objects.equals(null, "not null"));
    assertFalse(Objects.equals("not null", null));
    assertTrue(Objects.equals("a", "a"));
    assertFalse(Objects.equals("a", "b"));

    assertFalse(Objects.equals(new Object(), new Object()));

    Object obj = new Object();
    assertTrue(Objects.equals(obj, obj));
  }

  public void testHashCode() {
    assertEquals(0, Objects.hashCode(null));
    Object obj = new Object();
    assertEquals(obj.hashCode(), Objects.hashCode(obj));
  }
}
