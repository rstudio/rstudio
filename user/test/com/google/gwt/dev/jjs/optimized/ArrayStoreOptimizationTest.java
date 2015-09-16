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
package com.google.gwt.dev.jjs.optimized;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

/**
 * Tests for ArrayStore checks are optimized out when cast checking is disabled.
 */
@DoNotRunWith(Platform.Devel)
public class ArrayStoreOptimizationTest extends OptimizationTestBase {

  private static class TestObject { }

  private static Object[] arrayField = new TestObject[3];

  public static void modifyArray() {
    arrayField[0] = "ABC";
  }

  private static native String getGeneratedFunctionDefinition() /*-{
    return function() {
      @ArrayStoreOptimizationTest::modifyArray()();
    }.toString();
  }-*/;

  public void testArrayStoreChecksAreRemoved() throws Exception {
    String functionDef = getGeneratedFunctionDefinition();
    assertFunctionMatches(functionDef, "<obf>[0]='ABC'");
  }
}
