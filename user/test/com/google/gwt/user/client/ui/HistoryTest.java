/*
 * Copyright 2008 Google Inc.
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

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  private static native String getCurrentLocationHash() /*-{
    var href = $wnd.location.href;
    
    splitted = href.split("#");
    if (splitted.length != 2) {
      return null;
    }
    
    hashPortion = splitted[1];
    
    return hashPortion;
  }-*/;
  
  public void testTokenEscaping() {
    final String shouldBeEncoded = "% ^[]|\"<>{}\\`";
    final String shouldBeEncodedAs = "%25%20%5E%5B%5D%7C%22%3C%3E%7B%7D%5C%60";
    
    delayTestFinish(5000);
    History.addHistoryListener(new HistoryListener() {
      public void onHistoryChanged(String token) {
        assertEquals(shouldBeEncodedAs, getCurrentLocationHash());
        assertEquals(shouldBeEncoded, token);
        finishTest();
        History.removeHistoryListener(this);
      }
    });
    History.newItem(shouldBeEncoded);
  }
  
  public void testTokenNonescaping() {
    final String shouldNotChange = "abc;,/?:@&=+$-_.!~*'()ABC123foo";
    
    delayTestFinish(5000);
    History.addHistoryListener(new HistoryListener() {
      public void onHistoryChanged(String token) {
        assertEquals(shouldNotChange, getCurrentLocationHash());
        assertEquals(shouldNotChange, token);
        finishTest();
        History.removeHistoryListener(this);
      }
    });
    History.newItem(shouldNotChange);
  }

  /* Tests against issue #572: Double unescaping of history tokens. */
  public void testDoubleEscaping() {
    final String escToken = "%24%24%24";

    delayTestFinish(5000);
    History.addHistoryListener(new HistoryListener() {
      public void onHistoryChanged(String token) {
        assertEquals(escToken, token);
        finishTest();
        History.removeHistoryListener(this);
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
            History.removeHistoryListener(this);
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
          History.removeHistoryListener(this);
        }
      }
    });

    // We must first start out with a non-blank history token. Adding a blank
    // history token in the initial state will not cause an onHistoryChanged
    // event to fire.
    History.newItem("foobar");
  }

  /*
   * Test against issue #2500. IE6 has a bug that causes it to not report any
   * part of the current fragment after a '?' when read from location.hash;
   * make sure that on affected browsers, we're not relying on this.
   */
  public void testTokenWithQuestionmark() {
    delayTestFinish(5000);
    final String token = "foo?bar";

    History.addHistoryListener(new HistoryListener() {
      public void onHistoryChanged(String historyToken) {

        if (historyToken == null) {
          fail("historyToken should not be null");
        }

        assertEquals(token, historyToken);
        History.removeHistoryListener(this);
        finishTest();
      }
    });

    History.newItem(token);
  }
}
