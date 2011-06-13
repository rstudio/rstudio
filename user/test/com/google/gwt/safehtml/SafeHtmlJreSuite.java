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

import com.google.gwt.safehtml.rebind.HtmlTemplateParserTest;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplateTest;
import com.google.gwt.safehtml.server.SafeHtmlHostedModeUtilsTest;
import com.google.gwt.safehtml.server.SafeUriHostedModeUtilsTest;
import com.google.gwt.safehtml.server.UriUtilsTest;
import com.google.gwt.safehtml.shared.SafeHtmlBuilderTest;
import com.google.gwt.safehtml.shared.SafeHtmlStringTest;
import com.google.gwt.safehtml.shared.SafeHtmlUtilsTest;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizerTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for SafeHtml tests that require the JRE.
 */
public class SafeHtmlJreSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite(
        "Test suite for SafeHtml tests that require the JRE");

    suite.addTestSuite(SafeHtmlUtilsTest.class);
    suite.addTestSuite(SafeHtmlBuilderTest.class);
    suite.addTestSuite(SimpleHtmlSanitizerTest.class);
    suite.addTestSuite(SafeHtmlStringTest.class);
    suite.addTestSuite(UriUtilsTest.class);
    suite.addTestSuite(HtmlTemplateParserTest.class);
    suite.addTestSuite(ParsedHtmlTemplateTest.class);
    suite.addTestSuite(SafeHtmlHostedModeUtilsTest.class);
    suite.addTestSuite(SafeUriHostedModeUtilsTest.class);
    suite.addTestSuite(com.google.gwt.safehtml.shared.UriUtilsTest.class);

    return suite;
  }

  private SafeHtmlJreSuite() {
  }
}
