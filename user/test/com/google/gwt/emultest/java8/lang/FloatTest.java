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

package com.google.gwt.emultest.java8.lang;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for the Javascript emulation of the Float/float autoboxed
 * fundamental type.
 */
public class FloatTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testIsFinite() {
    final float[] nonfiniteNumbers = {
        Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN,
    };
    for (float value : nonfiniteNumbers) {
      assertFalse(Float.isFinite(value));
    }

    final float[] finiteNumbers = {
        -Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE,
        -1.0f, -0.5f, -0.1f, -0.0f, 0.0f, 0.1f, 0.5f, 1.0f,
    };
    for (float value : finiteNumbers) {
      assertTrue(Float.isFinite(value));
    }
  }

  public void testIsInfinite() {
    assertTrue(Float.isInfinite(Float.NEGATIVE_INFINITY));
    assertTrue(Float.isInfinite(Float.POSITIVE_INFINITY));

    assertFalse(Float.isInfinite(Float.NaN));

    final float[] finiteNumbers = {
        -Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE,
        -1.0f, -0.5f, -0.1f, -0.0f, 0.0f, 0.1f, 0.5f, 1.0f,
    };
    for (float value : finiteNumbers) {
      assertFalse(Float.isInfinite(value));
    }
  }
}
