/*
 * Copyright 2014 Google Inc.
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

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Tests Vector.
 */
public class VectorTest extends ListTestBase {

  @Override
  protected List makeEmptyList() {
    return new Vector();
  }

  public void testIndexOf() {
    Vector<String> v = new Vector<String>();
    v.addAll(Arrays.asList("a", "b", "b", "c"));

    assertEquals(-1, v.indexOf("-"));
    assertEquals(0, v.indexOf("a"));
    assertEquals(1, v.indexOf("b"));
    assertEquals(3, v.indexOf("c"));

    assertEquals(1, v.indexOf("b", 0));
    assertEquals(1, v.indexOf("b", 1));
    assertEquals(2, v.indexOf("b", 2));
    assertEquals(-1, v.indexOf("b", 3));

    // Following should not throw per JRE.
    assertEquals(-1, v.indexOf("a", 10));
  }

  public void testLastIndexOf() {
    Vector<String> v = new Vector<String>();
    v.addAll(Arrays.asList("a", "b", "b", "c"));

    assertEquals(-1, v.lastIndexOf("-"));
    assertEquals(0, v.lastIndexOf("a"));
    assertEquals(2, v.lastIndexOf("b"));
    assertEquals(3, v.lastIndexOf("c"));

    assertEquals(-1, v.lastIndexOf("b", 0));
    assertEquals(1, v.lastIndexOf("b", 1));
    assertEquals(2, v.lastIndexOf("b", 2));
    assertEquals(2, v.lastIndexOf("b", 3));

    // Following should not throw per JRE.
    assertEquals(-1, v.lastIndexOf("a", -10));
  }

  public void testExceptions() throws Exception {
    Vector<String> v = new Vector<String>();
    v.add("a");

    try {
      v.indexOf("a", -1);
      fail("should have failed");
    } catch (IndexOutOfBoundsException expected) {
      // Success.
    }

    try {
      v.lastIndexOf("a", 2);
      fail("should have failed");
    } catch (IndexOutOfBoundsException expected) {
      // Success.
    }

    try {
      v.setSize(-1);
      fail("should have failed");
    } catch (ArrayIndexOutOfBoundsException expected) {
      // Success.
    }
  }
}
