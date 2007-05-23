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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;

/**
 * Tests for the history system.
 * 
 * TODO: find a way to test unescaping of the initial hash value.
 */
public class HistoryTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /* Tests against issue #572: Double unescaping of history tokens. */
  public void testTokenEscaping() {
    final String escToken = "%24%24%24";
    delayTestFinish(5000);
    History.addHistoryListener(new HistoryListener() {
      public void onHistoryChanged(String token) {
        assertEquals(escToken, token);
        finishTest();
      }
    });
    History.newItem(escToken);
  }

  /*
   * Ensure that non-url-safe strings (such as those containing spaces) are
   * encoded/decoded correctly, and that programmatic 'back' works.
   */
  public void testHistory() {
    delayTestFinish(5000);
    History.addHistoryListener(new HistoryListener() {
      private int state = 0;

      public void onHistoryChanged(String historyToken) {
        switch (state) {
          case 0: {
            if (!historyToken.equals("foo bar")) {
              fail("Expecting token 'foo bar', but got: " + historyToken);
            }

            state = 1;
            History.newItem("baz");
            break;
          }

          case 1: {
            if (!historyToken.equals("baz")) {
              fail("Expecting token 'baz', but got: " + historyToken);
            }

            state = 2;
            History.back();
            break;
          }

          case 2: {
            if (!historyToken.equals("foo bar")) {
              fail("Expecting token 'foo bar', but got: " + historyToken);
            }
            finishTest();
            break;
          }
        }
      }
    });

    History.newItem("foo bar");
  }

  /*
   * Tests against issue #879: Ensure that empty history tokens do not add
   * additional characters after the '#' symbol in the URL.
   */
  public void testEmptyHistoryTokens() {
    delayTestFinish(5000);

    History.addHistoryListener(new HistoryListener() {
      public void onHistoryChanged(String historyToken) {

        if (historyToken == null) {
          fail("historyToken should not be null");
        }

        if (historyToken.equals("foobar")) {
          History.newItem("");
        } else {
          assertEquals(0, historyToken.length());
          finishTest();
        }
      }
    });

    // We must first start out with a non-blank history token. Adding a blank
    // history token in the initial state will not cause an onHistoryChanged
    // event to fire.
    History.newItem("foobar");
  }
}
