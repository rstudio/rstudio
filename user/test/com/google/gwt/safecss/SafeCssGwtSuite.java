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
import com.google.gwt.safecss.shared.GwtSafeStylesBuilderTest;
import com.google.gwt.safecss.shared.GwtSafeStylesHostedModeUtilsTest;
import com.google.gwt.safecss.shared.GwtSafeStylesStringTest;
import com.google.gwt.safecss.shared.GwtSafeStylesUtilsTest;

import junit.framework.Test;

/**
 * Test suite for SafeCss GWTTestCases.
 */
public class SafeCssGwtSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test suite for safe css  GWTTestCases");

    suite.addTestSuite(GwtSafeStylesBuilderTest.class);
    suite.addTestSuite(GwtSafeStylesHostedModeUtilsTest.class);
    suite.addTestSuite(GwtSafeStylesStringTest.class);
    suite.addTestSuite(GwtSafeStylesUtilsTest.class);

    return suite;
  }

  private SafeCssGwtSuite() {
  }
}
