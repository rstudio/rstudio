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

import static junit.framework.Assert.assertEquals;

import elemental.util.ArrayOf;
import elemental.util.ArrayOfBoolean;
import elemental.util.ArrayOfInt;
import elemental.util.ArrayOfNumber;
import elemental.util.ArrayOfString;

/**
 * Static test utilities for the tests in this package.
 */
class TestUtils {
  static void assertSamelitude(boolean[] expected, ArrayOfBoolean values) {
    assertEquals(expected.length, values.length());
    for (int i = 0, n = expected.length; i < n; ++i) {
      assertEquals(expected[i], values.get(i));
    }
  }

  static void assertSamelitude(double[] expected, ArrayOfNumber values) {
    assertEquals(expected.length, values.length());
    for (int i = 0, n = expected.length; i < n; ++i) {
      assertEquals(expected[i], values.get(i));
    }
  }

  static void assertSamelitude(int[] expected, ArrayOfInt values) {
    assertEquals(expected.length, values.length());
    for (int i = 0, n = expected.length; i < n; ++i) {
      assertEquals(expected[i], values.get(i));
    }
  }

  static void assertSamelitude(String[] expected, ArrayOfString values) {
    assertEquals(expected.length, values.length());
    for (int i = 0, n = expected.length; i < n; ++i) {
      assertEquals(expected[i], values.get(i));
    }
  }

  static void assertSamelitude(TestItem[] expected, ArrayOf<TestItem> values) {
    assertEquals(expected.length, values.length());
    for (int i = 0, n = expected.length; i < n; ++i) {
      assertEquals(expected[i], values.get(i));
    }
  }

  private TestUtils() {
  }
}
