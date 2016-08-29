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
package com.google.gwt.dev.jjs.optimized;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

/**
 * Tests that the String class gets optimized properly.
 */
@DoNotRunWith(Platform.Devel)
public class StringOptimizationTest extends OptimizationTestBase {

  private static String createString() {
    return new String();
  }

  private static native String getGeneratedFunctionDefintionThatTriggersStringClinit() /*-{
    return function() {
      tmp = @StringOptimizationTest::createString()();
    }.toString();
  }-*/;

  public void testStringClinitIsRemoved() throws Exception {
    String functionDef = getGeneratedFunctionDefintionThatTriggersStringClinit();
    assertFunctionMatches(functionDef, "tmp=''");
  }

  private static native String getGeneratedFunctionDefintionThatCallsStringFromCharCode() /*-{
    return function() {
      tmp = @java.lang.String::valueOf(C)('c');
    }.toString();
  }-*/;

  /*
   * Makes sure that static String functions are emitted without the $wnd qualifier because doing
   * so causes runtime performance degradation.
   */
  public void testNativeStringDoesNotUse$wnd() throws Exception {
    String functionDef = getGeneratedFunctionDefintionThatCallsStringFromCharCode();
    assertFunctionMatches(functionDef, "tmp=String.fromCharCode('c')");
  }
}
