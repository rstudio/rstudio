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

import com.google.gwt.junit.client.GWTTestCase;
import elemental.util.ArrayOfString;

/**
 * Tests {@link StringUtil}.
 *
 */
public class StringUtilTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  private static void assertSame(String[] expected, ArrayOfString result) {
    assertEquals(expected.length, result.length());
    for (int i = 0, n = expected.length; i < n; ++i) {
      assertEquals(expected[i], result.get(i));
    }
  }

  /**
   * Tests {@link StringUtil#split(String, String)} and
   * {@link StringUtil#split(String, String, int)}.
   */
  public void testSplit() {
    assertSame(
        new String[] {"abc", "", "", "de", "f", "", ""}, StringUtil.split("abcxxxdexfxx", "x"));
    assertSame(new String[] {"a", "b", "c", "x", "x", "d", "e", "x", "f", "x"},
        StringUtil.split("abcxxdexfx", ""));
    final String booAndFoo = "boo:and:foo";
    assertSame(new String[] {"boo", "and"}, StringUtil.split(booAndFoo, ":", 2));
    assertSame(new String[] {"boo", "and", "foo"}, StringUtil.split(booAndFoo, ":", 5));
    assertSame(new String[] {"boo", "and", "foo"}, StringUtil.split(booAndFoo, ":", -2));
    assertSame(new String[] {"", ""}, StringUtil.split("/", "/"));
    assertSame(new String[] {""}, StringUtil.split("", ","));
  }
}
