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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests unused imports removal.
 */
public class UnusedImportsTest extends GWTTestCase {
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  // Just refer to classes in unusedimports and usedimports to test package annotations.
  com.google.gwt.dev.jjs.test.usedimports.Dummy usedImportsDummy;
  com.google.gwt.dev.jjs.test.unusedimports.Dummy unusedImportsDummy;

  /**
   * This is a dummy test for now, The real test is whether the compiler manages to compile the
   * test.
   */
  public void testDummy() {
  }
}
