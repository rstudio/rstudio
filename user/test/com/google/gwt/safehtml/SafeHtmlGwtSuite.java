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
package com.google.gwt.safehtml;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.safehtml.client.SafeHtmlTemplatesTest;
import com.google.gwt.safehtml.shared.GwtSafeHtmlBuilderTest;
import com.google.gwt.safehtml.shared.GwtSafeHtmlHostedModeUtilsTest;
import com.google.gwt.safehtml.shared.GwtSafeHtmlStringTest;
import com.google.gwt.safehtml.shared.GwtSafeHtmlUtilsTest;
import com.google.gwt.safehtml.shared.GwtSafeUriHostedModeUtilsTest;
import com.google.gwt.safehtml.shared.GwtUriUtilsTest;

import junit.framework.Test;

/**
 * Test suite for SafeHtml GWTTestCases.
 */
public class SafeHtmlGwtSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Test suite for SafeHtml GWTTestCases");

    suite.addTestSuite(GwtSafeHtmlUtilsTest.class);
    suite.addTestSuite(GwtSafeHtmlStringTest.class);
    suite.addTestSuite(GwtSafeHtmlBuilderTest.class);
    suite.addTestSuite(SafeHtmlTemplatesTest.class);
    suite.addTestSuite(GwtSafeHtmlHostedModeUtilsTest.class);
    suite.addTestSuite(GwtUriUtilsTest.class);
    suite.addTestSuite(GwtSafeUriHostedModeUtilsTest.class);

    return suite;
  }

  private SafeHtmlGwtSuite() {
  }
}
