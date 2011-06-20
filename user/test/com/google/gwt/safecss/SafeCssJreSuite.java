/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.safecss;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.safecss.shared.SafeStylesBuilderTest;
import com.google.gwt.safecss.shared.SafeStylesHostedModeUtilsTest;
import com.google.gwt.safecss.shared.SafeStylesStringTest;
import com.google.gwt.safecss.shared.SafeStylesUtilsTest;

import junit.framework.Test;

/**
 * Test suite for SafeCss GWTTestCases.
 */
public class SafeCssJreSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test suite for safe css tests that require the JRE");

    suite.addTestSuite(SafeStylesBuilderTest.class);
    suite.addTestSuite(SafeStylesHostedModeUtilsTest.class);
    suite.addTestSuite(SafeStylesStringTest.class);
    suite.addTestSuite(SafeStylesUtilsTest.class);

    return suite;
  }

  private SafeCssJreSuite() {
  }
}
