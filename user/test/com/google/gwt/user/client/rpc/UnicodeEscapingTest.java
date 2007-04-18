/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test which verifies that we properly escape JSON strings sent back from the 
 * server.
 */
public class UnicodeEscapingTest extends GWTTestCase {

  private static final int DEFAULT_TEST_FINISH_DELAY_MS = 5000;
  private static final int CHARACTER_RANGE_SIZE = 1024;
  private static final int LAST_CHARACTER = 0x10000;

  private int start = 0;

  private static UnicodeEscapingServiceAsync getService() {
    UnicodeEscapingServiceAsync service = (UnicodeEscapingServiceAsync) GWT.create(UnicodeEscapingService.class);
    ServiceDefTarget target = (ServiceDefTarget) service;
    target.setServiceEntryPoint(GWT.getModuleBaseURL() + "unicodeEscape");
    return service;
  }

  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  /**
   * Requests strings of CHARACTER_RANGE_SIZE from the server and validates that
   * the returned string length matches CHARACTER_RANGE_SIZE and that all of the
   * characters remain intact.
   */
  public void testUnicodeEscaping() {
    delayTestFinish(DEFAULT_TEST_FINISH_DELAY_MS);

    getService().getStringContainingCharacterRange(0, CHARACTER_RANGE_SIZE,
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            fail(caught.toString());
          }

          public void onSuccess(Object result) {
            String str = (String) result;

            assertTrue("expected: " + Integer.toString(CHARACTER_RANGE_SIZE)
                + " actual: " + str.length()
                + " for character range [" + Integer.toString(start) + ", "
                + Integer.toString(start + CHARACTER_RANGE_SIZE) + ")",
                CHARACTER_RANGE_SIZE == str.length());

            char[] chars = str.toCharArray();
            for (int i = 0; i < CHARACTER_RANGE_SIZE; ++i) {
              assertEquals( i + start, chars[i]);
            }

            start += CHARACTER_RANGE_SIZE;
            if (start < LAST_CHARACTER) {
              delayTestFinish(DEFAULT_TEST_FINISH_DELAY_MS);

              getService().getStringContainingCharacterRange(start,
                  start + CHARACTER_RANGE_SIZE, this);
            } else {
              finishTest();
            }
          }
        });
  }
}
