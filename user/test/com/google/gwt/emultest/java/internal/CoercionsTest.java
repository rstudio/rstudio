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
package com.google.gwt.emultest.java.internal;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Random;

import javaemul.internal.Coercions;

/**
 * Tests for {@link Coercions}.
 */
public class CoercionsTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @SuppressWarnings("NumericOverflow")
  public void testEnsureInt() {
    Random random = new Random();
    // This variable holds value of 1.
    // We use it to prevent static compiler optimizations.
    int _1 = random.nextInt(1) + 1;
    assertEquals(Integer.MIN_VALUE, Coercions.ensureInt(Integer.MAX_VALUE + _1));
  }
}
