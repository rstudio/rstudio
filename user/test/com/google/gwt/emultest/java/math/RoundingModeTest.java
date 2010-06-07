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
package com.google.gwt.emultest.java.math;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.math.RoundingMode;

/**
 * Tests for {@link RoundingMode}.
 */
public class RoundingModeTest extends EmulTestBase {

  /**
   * Check the order of the enum values.  This is important for serialization
   * with a real JRE implementation.
   */
  public void testValues() {
    RoundingMode[] values = RoundingMode.values();
    assertEquals(8, values.length);
    int i = 0;
    assertEquals(RoundingMode.UP, values[i++]);
    assertEquals(RoundingMode.DOWN, values[i++]);
    assertEquals(RoundingMode.CEILING, values[i++]);
    assertEquals(RoundingMode.FLOOR, values[i++]);
    assertEquals(RoundingMode.HALF_UP, values[i++]);
    assertEquals(RoundingMode.HALF_DOWN, values[i++]);
    assertEquals(RoundingMode.HALF_EVEN, values[i++]);
    assertEquals(RoundingMode.UNNECESSARY, values[i++]);
  }
}
