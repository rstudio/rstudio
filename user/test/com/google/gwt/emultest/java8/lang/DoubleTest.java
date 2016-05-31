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

package com.google.gwt.emultest.java8.lang;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for the emulated-in-Javascript Double/double autoboxed types.
 */
public class DoubleTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testIsFinite() {
    final double[] nonfiniteNumbers = {
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN,
    };
    for (double value : nonfiniteNumbers) {
      assertFalse(Double.isFinite(value));
    }

    final double[] finiteNumbers = {
        -Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE,
        -1.0, -0.5, -0.1, -0.0, 0.0, 0.1, 0.5, 1.0,
    };
    for (double value : finiteNumbers) {
      assertTrue(Double.isFinite(value));
    }
  }
}

