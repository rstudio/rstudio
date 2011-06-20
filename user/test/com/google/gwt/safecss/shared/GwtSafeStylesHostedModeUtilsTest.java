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
package com.google.gwt.safecss.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * GWT Unit tests for {@link SafeStylesHostedModeUtils}.
 */
public class GwtSafeStylesHostedModeUtilsTest extends GWTTestCase {

  private static final String ERROR_MESSAGE_MISMATCH =
      "Expected error message does not match actual error message";

  @Override
  public String getModuleName() {
    return "com.google.gwt.safecss.SafeCss";
  }

  public void testIsValidStyleName() {
    if (GWT.isProdMode()) {
      // isValidStyleName always returns true in prod mode.
      return;
    }

    // Valid names.
    for (String s : GwtSafeStylesUtilsTest.VALID_STYLE_NAMES) {
      String error = SafeStylesHostedModeUtils.isValidStyleValue(s);
      assertNull("'" + s + "' incorrectly reported as an invalid style name: " + error, error);
    }

    // Invalid names.
    for (String s : GwtSafeStylesUtilsTest.INVALID_STYLE_NAMES) {
      assertNotNull("'" + s + "' incorrectly reported as an valid style name",
          SafeStylesHostedModeUtils.isValidStyleName(s));
    }
  }

  public void testIsValidStyleValue() {
    if (GWT.isProdMode()) {
      // isValidStyleValue always returns true in prod mode.
      return;
    }

    // Valid values.
    for (String s : GwtSafeStylesUtilsTest.VALID_STYLE_VALUES) {
      String error = SafeStylesHostedModeUtils.isValidStyleValue(s);
      assertNull("'" + s + "' incorrectly reported as an invalid style value: " + error, error);
    }

    // Invalid values.
    for (String s : GwtSafeStylesUtilsTest.INVALID_STYLE_VALUES) {
      assertNotNull("'" + s + "' incorrectly reported as an valid style value",
          SafeStylesHostedModeUtils.isValidStyleValue(s));
    }
  }

  public void testMaybeCheckValidStyleName() {
    if (GWT.isProdMode()) {
      /*
       * SafeStylesHostedModeUtils.maybeCheckValidStyleName is a no-op in prod
       * mode.
       */
      SafeStylesHostedModeUtils.maybeCheckValidStyleName(GwtSafeStylesUtilsTest.INVALID_STYLE_NAME);
    } else {
      // Check a valid name.
      SafeStylesHostedModeUtils.maybeCheckValidStyleName("name");

      // Check an invalid name.
      String expectedError =
          SafeStylesHostedModeUtils.isValidStyleName(GwtSafeStylesUtilsTest.INVALID_STYLE_NAME);
      assertNotNull(expectedError);
      boolean caught = false;
      try {
        SafeStylesHostedModeUtils
            .maybeCheckValidStyleName(GwtSafeStylesUtilsTest.INVALID_STYLE_NAME);
      } catch (IllegalArgumentException e) {
        /*
         * Expected - maybeCheckValidStyleName() use either
         * Preconditions.checkArgument() (which throws an
         * IllegalArgumentException), or an assert (which throws an
         * AssertionError).
         */
        assertEquals(ERROR_MESSAGE_MISMATCH, expectedError, e.getMessage());
        caught = true;
      } catch (AssertionError e) {
        // Expected - see comment above.
        assertEquals(ERROR_MESSAGE_MISMATCH, expectedError, e.getMessage());
        caught = true;
      }

      if (!caught) {
        fail("Expected an exception for invalid style name.");
      }
    }
  }

  public void testMaybeCheckValidStyleValue() {
    if (GWT.isProdMode()) {
      /*
       * SafeStylesHostedModeUtils.maybeCheckValidStyleValue is a no-op in prod
       * mode.
       */
      SafeStylesHostedModeUtils
          .maybeCheckValidStyleValue(GwtSafeStylesUtilsTest.INVALID_STYLE_VALUE);
    } else {
      // Check a valid value.
      SafeStylesHostedModeUtils.maybeCheckValidStyleValue("value");

      String expectedError =
          SafeStylesHostedModeUtils.isValidStyleValue(GwtSafeStylesUtilsTest.INVALID_STYLE_VALUE);
      assertNotNull(expectedError);
      boolean caught = false;
      try {
        SafeStylesHostedModeUtils
            .maybeCheckValidStyleValue(GwtSafeStylesUtilsTest.INVALID_STYLE_VALUE);
      } catch (IllegalArgumentException e) {
        /*
         * Expected - maybeCheckValidStyleValue() use either
         * Preconditions.checkArgument() (which throws an
         * IllegalArgumentException), or an assert (which throws an
         * AssertionError).
         */
        assertEquals(ERROR_MESSAGE_MISMATCH, expectedError, e.getMessage());
        caught = true;
      } catch (AssertionError e) {
        // Expected - see comment above.
        assertEquals(ERROR_MESSAGE_MISMATCH, expectedError, e.getMessage());
        caught = true;
      }

      if (!caught) {
        fail("Expected an exception for invalid style value.");
      }
    }
  }
}
