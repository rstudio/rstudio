/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.junit;

import com.google.gwt.junit.client.ForcePureJavaTest;
import com.google.gwt.junit.client.ModuleOneTest;
import com.google.gwt.junit.client.ModuleOneTest2;
import com.google.gwt.junit.client.ModuleTwoTest;
import com.google.gwt.junit.client.NullModuleNameTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests that a normal test suite will run even if modules are out of order.
 * Also checks that tests are run in pure Java mode (non-GWT).
 */
public class NonGwtTestSuite {

  public static Test suite() {
    // This is intentionally not a GWTTestSuite.
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForcePureJavaTest.class);
    suite.addTestSuite(ModuleOneTest.class);
    suite.addTestSuite(ModuleTwoTest.class);
    suite.addTestSuite(ModuleOneTest2.class);
    suite.addTestSuite(NullModuleNameTest.class);

    return suite;
  }
}
