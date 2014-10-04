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
package elemental.js.util;

import static elemental.js.util.TestUtils.assertSamelitude;

import com.google.gwt.junit.client.GWTTestCase;

import elemental.util.ArrayOf;
import elemental.util.ArrayOfBoolean;
import elemental.util.ArrayOfInt;
import elemental.util.ArrayOfNumber;
import elemental.util.ArrayOfString;
import elemental.util.CanCompare;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;
import elemental.util.MapFromStringToBoolean;
import elemental.util.MapFromStringToInt;
import elemental.util.MapFromStringToNumber;
import elemental.util.MapFromStringToString;

import java.util.Arrays;

/**
 * Tests {@link MapFromStringTo}, {@link MapFromStringToBoolean},
 * {@link MapFromStringToInt}, {@link MapFromStringToNumber} and
 * {@link MapFromStringToString}.
 */
public class MapFromStringTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  /**
   * Tests {@link MapFromStringTo}.
   */
  public void testMapsFromString() {
    // This is our test subject.
    final MapFromStringTo<TestItem> map =  Collections.mapFromStringTo();

    // These are his keys.
    final String[] keys = new String[] {"key-1", "key-2", "key-3"};

    // These are the values for those keys.
    final TestItem[] vals = new TestItem[] {new TestItem(0), new TestItem(1), new TestItem(2)};

    // Let's put those values in.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], vals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(vals[i], map.get(keys[i]));
    }

    // These are some new values.
    final TestItem[] newVals = new TestItem[] {new TestItem(3), new TestItem(4), new TestItem(5)};

    // Let's update those keys, ok.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], newVals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(newVals[i], map.get(keys[i]));
    }

    checkMapContents(map, keys, newVals);

    // Let's remove a key, did it go away?
    map.remove(keys[0]);
    assertNull(map.get(keys[0]));
    assertFalse(map.hasKey(keys[0]));
  }

  /**
   * Tests {@link MapFromStringToInt}.
   */
  public void testMapsFromStringsToInts() {
    // This is our test subject.
    final MapFromStringToInt map = Collections.mapFromStringToInt();

    // These are his keys.
    final String[] keys = new String[] {"key-1", "key-2", "key-3"};

    // These are the values for those keys.
    final int[] vals = new int[] {0, 1, 2};

    // Let's put those values in.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], vals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(vals[i], map.get(keys[i]));
    }

    // These are some new values.
    final int[] newVals = new int[] {3, 4, 5};

    // Let's update those keys, ok.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], newVals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(newVals[i], map.get(keys[i]));
    }

    checkMapContents(map, keys, newVals);

    // Let's remove a key, did it go away?
    map.remove(keys[0]);
    assertFalse(map.hasKey(keys[0]));
  }

  /**
   * Tests {@link MapFromStringToNumber}.
   */
  public void testMapsFromStringsToNumbers() {
    // This is our test subject.
    final MapFromStringToNumber map = Collections.mapFromStringToNumber();

    // These are his keys.
    final String[] keys = new String[] {"key-1", "key-2", "key-3"};

    // These are the values for those keys.
    final double[] vals = new double[] {0.0, 1.0, 2.0};

    // Let's put those values in.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], vals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(vals[i], map.get(keys[i]));
    }

    // These are some new values.
    final double[] newVals = new double[] {3.0, 4.0, 5.0};

    // Let's update those keys, ok.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], newVals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(newVals[i], map.get(keys[i]));
    }

    checkMapContents(map, keys, newVals);

    // Let's remove a key, did it go away?
    map.remove(keys[0]);
    assertFalse(map.hasKey(keys[0]));
  }

  /**
   * Tests {@link MapFromStringToString}.
   */
  public void testMapsFromStringsToStrings() {
    // This is our test subject.
    final MapFromStringToString map = Collections.mapFromStringToString();

    // These are his keys.
    final String[] keys = new String[] {"key-1", "key-2", "key-3"};

    // These are the values for those keys.
    final String[] vals = new String[] {"val-0", "val-1", "val-2"};

    // Let's put those values in.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], vals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(vals[i], map.get(keys[i]));
    }

    // These are some new values.
    final String[] newVals = new String[] {"val-3", "val-4", "val-5"};

    // Let's update those keys, ok.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], newVals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(newVals[i], map.get(keys[i]));
    }

    checkMapContents(map, keys, newVals);

    // Let's remove a key, did it go away?
    map.remove(keys[0]);
    assertNull(map.get(keys[0]));
    assertFalse(map.hasKey(keys[0]));
  }

  /**
   * Tests {@link MapFromStringToBoolean}.
   */
  public void testsMapFromStringsToBooleans() {
    // This is our test subject.
    final MapFromStringToBoolean map = Collections.mapFromStringToBoolean();

    // These are his keys.
    final String[] keys = new String[] {"key-1", "key-2", "key-3"};

    // These are the values for those keys.
    final boolean[] vals = new boolean[] {true, false, true};

    // Let's put those values in.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], vals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(vals[i], map.get(keys[i]));
    }

    // These are some new values.
    final boolean[] newVals = new boolean[] {false, true, false};

    // Let's update those keys, ok.
    for (int i = 0, n = keys.length; i < n; ++i) {
      map.put(keys[i], newVals[i]);
    }

    // Are they all in the right place?
    for (int i = 0, n = keys.length; i < n; ++i) {
      assertTrue(map.hasKey(keys[i]));
      assertEquals(newVals[i], map.get(keys[i]));
    }

    checkMapContents(map, keys, newVals);

    // Let's remove a key, did it go away?
    map.remove(keys[0]);
    assertFalse(map.hasKey(keys[0]));
  }

  private void checkMapContents(MapFromStringToBoolean map, String[] keys, boolean[] values) {
    keys = Arrays.copyOf(keys, keys.length);
    Arrays.sort(keys);
    ArrayOfString mapKeys = map.keys();
    mapKeys.sort();
    assertSamelitude(keys, mapKeys);

    // We can't sort boolean array so we just compare true values count.
    ArrayOfBoolean mapValues = map.values();
    assertEquals(values.length, mapValues.length());
    int expectedTrueCount = 0;
    int actualTrueCount = 0;
    for (int i = 0; i < values.length; i++) {
      if (values[i]) {
        expectedTrueCount++;
      }
      if (mapValues.get(i)) {
        actualTrueCount++;
      }
    }
    assertEquals(expectedTrueCount, actualTrueCount);
  }

  private void checkMapContents(MapFromStringToNumber map, String[] keys, double[] values) {
    keys = Arrays.copyOf(keys, keys.length);
    Arrays.sort(keys);
    ArrayOfString mapKeys = map.keys();
    mapKeys.sort();
    assertSamelitude(keys, mapKeys);

    values = Arrays.copyOf(values, values.length);
    Arrays.sort(values);
    ArrayOfNumber mapValues = map.values();
    mapValues.sort();
    assertSamelitude(values, mapValues);
  }

  private void checkMapContents(MapFromStringToInt map, String[] keys, int[] values) {
    keys = Arrays.copyOf(keys, keys.length);
    Arrays.sort(keys);
    ArrayOfString mapKeys = map.keys();
    mapKeys.sort();
    assertSamelitude(keys, mapKeys);

    values = Arrays.copyOf(values, values.length);
    Arrays.sort(values);
    ArrayOfInt mapValues = map.values();
    mapValues.sort();
    assertSamelitude(values, mapValues);
  }

  private void checkMapContents(MapFromStringTo<TestItem> map, String[] keys, TestItem[] values) {
    keys = Arrays.copyOf(keys, keys.length);
    Arrays.sort(keys);
    ArrayOfString mapKeys = map.keys();
    mapKeys.sort();
    assertSamelitude(keys, mapKeys);

    values = Arrays.copyOf(values, values.length);
    Arrays.sort(values);
    ArrayOf<TestItem> mapValues = map.values();
    mapValues.sort(new CanCompare<TestItem>() {
      @Override
      public int compare(TestItem a, TestItem b) {
        return a.compareTo(b);
      }
    });
    assertSamelitude(values, mapValues);
  }

  private void checkMapContents(MapFromStringToString map, String[] keys, String[] values) {
    keys = Arrays.copyOf(keys, keys.length);
    Arrays.sort(keys);
    ArrayOfString mapKeys = map.keys();
    mapKeys.sort();
    assertSamelitude(keys, mapKeys);

    values = Arrays.copyOf(values, values.length);
    Arrays.sort(values);
    ArrayOfString mapValues = map.values();
    mapValues.sort();
    assertSamelitude(values, mapValues);
  }
}
