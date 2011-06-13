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
package com.google.gwt.safehtml.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * GWT Unit tests for {@link SafeUriHostedModeUtils}.
 */
public class GwtSafeUriHostedModeUtilsTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.safehtml.SafeHtmlTestsModule";
  }

  public void testIsValidUriCharset() {
    if (GWT.isProdMode()) {
      // isValidUriCharset always returns true in prod mode.
      // Hence we short-circuit this test in prod mode.
      return;
    }
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset(""));
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset("blah"));
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset("blah<>foo"));
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset("blah%foo"));
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset("blah%25foo"));
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset(GwtUriUtilsTest.CONSTANT_URL));
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset(GwtUriUtilsTest.MAILTO_URL));
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset(GwtUriUtilsTest.EMPTY_GIF_DATA_URL));
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset(GwtUriUtilsTest.LONG_DATA_URL));
    assertTrue(SafeUriHostedModeUtils.isValidUriCharset(GwtUriUtilsTest.JAVASCRIPT_URL));

    assertFalse(SafeUriHostedModeUtils
        .isValidUriCharset(GwtUriUtilsTest.INVALID_URL_UNPAIRED_SURROGATE));
  }

  public void testMaybeCheckValidUri() {
    if (GWT.isProdMode()) {
      // SafeUriHostedModeUtils#maybeCheckValidUri is a no-op in prod mode
      SafeUriHostedModeUtils.maybeCheckValidUri(GwtUriUtilsTest.INVALID_URL_UNPAIRED_SURROGATE);
    } else {
      SafeUriHostedModeUtils.maybeCheckValidUri("");
      SafeUriHostedModeUtils.maybeCheckValidUri("blah");
      SafeUriHostedModeUtils.maybeCheckValidUri("blah<>foo");
      SafeUriHostedModeUtils.maybeCheckValidUri("blah%foo");
      SafeUriHostedModeUtils.maybeCheckValidUri("blah%25foo");
      SafeUriHostedModeUtils.maybeCheckValidUri(GwtUriUtilsTest.CONSTANT_URL);
      SafeUriHostedModeUtils.maybeCheckValidUri(GwtUriUtilsTest.MAILTO_URL);
      SafeUriHostedModeUtils.maybeCheckValidUri(GwtUriUtilsTest.EMPTY_GIF_DATA_URL);
      SafeUriHostedModeUtils.maybeCheckValidUri(GwtUriUtilsTest.LONG_DATA_URL);
      SafeUriHostedModeUtils.maybeCheckValidUri(GwtUriUtilsTest.JAVASCRIPT_URL);

      assertCheckValidUriFails(GwtUriUtilsTest.INVALID_URL_UNPAIRED_SURROGATE);
      assertCheckValidUriFails("http://");

      if (GWT.isClient()) {
        SafeUriHostedModeUtils.maybeCheckValidUri(GWT.getModuleBaseURL());
        SafeUriHostedModeUtils.maybeCheckValidUri(GWT.getHostPageBaseURL());
      }
    }
  }

  private void assertCheckValidUriFails(String uri) {
    try {
      SafeUriHostedModeUtils.maybeCheckValidUri(uri);
    } catch (IllegalArgumentException e) {
      // expected
      return;
    } catch (AssertionError e) {
      // expected
      return;
    }
    // This must be outside the try/catch, as it throws an AssertionFailedError which, in some
    // versions of JUnit, extends AssertionError
    fail("maybeCheckValidUri failed to throw exception for: " + uri);
  }
}
