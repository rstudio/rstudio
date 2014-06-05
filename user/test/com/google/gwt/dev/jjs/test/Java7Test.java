/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.junit.JUnitShell;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Dummy test case. Java7Test is super sourced so that GWT can be compiled by Java 6.
 *
 * NOTE: Make sure this class has the same test methods of its supersourced variant.
 */
public class Java7Test extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.Java7Test";
  }

  @Override
  public void runTest() throws Throwable {
    // Only run these tests if -sourceLevel 7 (or greated) is enabled.
    if (JUnitShell.getCompilerOptions().getSourceLevel()
        .compareTo(SourceLevel.JAVA7) >= 0) {
      super.runTest();
    }
  }

  public void testNewStyleLiterals() {
    // Make sure we are using the right Java7Test if the source compatibility level is set to Java 7
    // or above.
    assertFalse((JUnitShell.getCompilerOptions().getSourceLevel()
        .compareTo(SourceLevel.JAVA7) >= 0));
  }

  public void testSwitchOnString() {
  }

  public void testResource() throws Exception {
  }

  public void test3Resources() throws Exception {
  }

  public void testResourcesWithExceptions() throws Exception {
  }

  public void testResourcesWithSuppressedExceptions() throws Exception {
  }

  public void testMultiExceptions() {
  }

  public void testAddSuppressedExceptions() {
  }

  public void testPrimitiveCastsFromObject() {
  }
}
