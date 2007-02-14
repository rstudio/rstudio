// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;

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
		assertEquals(escToken,token);
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
            if (!historyToken.equals("foo bar"))
              fail("Expecting token 'foo bar', but got: " + historyToken);

            state = 1;
            History.newItem("baz");
            break;
          }

          case 1: {
            if (!historyToken.equals("baz"))
              fail("Expecting token 'baz', but got: " + historyToken);

            state = 2;
            History.back();
            break;
          }

          case 2: {
            if (!historyToken.equals("foo bar"))
              fail("Expecting token 'foo bar', but got: " + historyToken);
            finishTest();
            break;
          }
        }
      }
    });

    History.newItem("foo bar");
  }  
}
