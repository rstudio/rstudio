/*
 * Copyright 2015 Google Inc.
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

import java.util.ArrayList;

/**
 * Tests for ArrayList checks are optimized out when checking is disabled.
 */
@DoNotRunWith(Platform.Devel)
public class ArrayListOptimizationTest extends OptimizationTestBase {

  private static ArrayList<String> arrayListField = new ArrayList<String>();

  public static String getFromArrayList() {
    return arrayListField.get(0);
  }

  private static native String getGeneratedFunctionDefinitionForGet() /*-{
    return function() {
      tmp = @ArrayListOptimizationTest::getFromArrayList()();
    }.toString();
  }-*/;

  public void testArrayListGetChecksAreRemoved() throws Exception {
    String functionDef = getGeneratedFunctionDefinitionForGet();
    assertFunctionMatches(functionDef, "tmp=<obf>.<obf>[0]");
  }

  private static native String getGeneratedFunctionDefinitionForGetIgnoredReturn()/*-{
    return function() {
      @ArrayListOptimizationTest::getFromArrayList()();
    }.toString();
  }-*/;

  public void testArrayListCallRemoved() throws Exception {
    String functionDef = getGeneratedFunctionDefinitionForGetIgnoredReturn();
    assertFunctionMatches(functionDef, "");
  }

  // The compiler will not inline unless arrayListField is a parameter.
  public static void setArrayList(ArrayList<String> arrayListField) {
    arrayListField.set(0, "abc");
  }

  private static native String getGeneratedFunctionDefinitionForAdd() /*-{
    return function() {
      @ArrayListOptimizationTest::setArrayList(*)(@ArrayListOptimizationTest::arrayListField);
    }.toString();
  }-*/;

  public void testArrayListSetChecksAreRemoved() throws Exception {
    String functionDef = getGeneratedFunctionDefinitionForAdd();
    assertFunctionMatches(functionDef, "<obf>.<obf>[0]='abc'");
  }

  private static void iterateArrayList() {
    for (String s : arrayListField) {
      // empty
    }
  }

  private static native String getGeneratedFunctionDefinitionForIterate() /*-{
    return @ArrayListOptimizationTest::iterateArrayList().toString();
  }-*/;
  
  // Disabled as resulting snippet is too complex for assertion
  public void _disabled_testArrayListIterationChecksAreRemoved() throws Exception {
    String functionDef = getGeneratedFunctionDefinitionForIterate();
    assertFunctionMatches(
        functionDef,
        "function <obf>(){"
        + "  var <obf>;"
        + "  for(<obf> = new <obf>(<obf>);<obf>.<obf> < <obf>.<obf>.<obf>.length;){"
        + "    <obf>.<obf>=<obf>.<obf>++;"
        + "  }"
        + "}");
  }
}
