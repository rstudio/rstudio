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
package com.google.gwt.safehtml.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * GWT Unit tests for {@link SafeHtmlHostedModeUtils}.
 */
public class GwtSafeHtmlHostedModeUtilsTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.safehtml.SafeHtmlTestsModule";
  }

  public void testMaybeCheckCompleteHtml() {
    if (GWT.isProdMode()) {
      // SafeHtmlHostedModeUtils#isCompleteHtml always returns true in prod mode
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("<foo>blah");
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("baz<em>foo</em> <x");
    } else {
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("");
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("blah");
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("<foo>blah");
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("<>blah");
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("baz");

      assertCheckCompleteHtmlFails("baz<");
      assertCheckCompleteHtmlFails("baz<em>foo</em> <x");
      assertCheckCompleteHtmlFails("baz<em>foo</em> <x a=b");
      assertCheckCompleteHtmlFails("baz<em>foo</em> <x a=\"b");
      assertCheckCompleteHtmlFails("baz<em>foo</em> <x a=\"b\"");
      assertCheckCompleteHtmlFails("baz<em>foo</em> <x a=\"b\" ");

      assertCheckCompleteHtmlFails("<script>");
      assertCheckCompleteHtmlFails("<style>");

      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("baz<em>foo</em> <x a=\"b\"> ");
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("baz<em>foo</em> <x a=\"b\">sadf");
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("baz<em>foo</em> <x a=\"b\">");
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("baz<em>foo</em> <x a=\"b\"/>");
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml("baz<em>foo</em> <x a=\"b\"/>bbb");
    }
  }

  private void assertCheckCompleteHtmlFails(String html) {
    try {
      SafeHtmlHostedModeUtils.maybeCheckCompleteHtml(html);
    } catch (IllegalArgumentException e) {
      // expected
      return;
    } catch (AssertionError e) {
      // expected
      return;
    }
    // This must be outside the try/catch, as it throws an AssertionFailedError which, in some
    // versions of JUnit, extends AssertionError
    fail("maybeCheckCompleteHtml failed to throw exception for: " + html);
  }
}
