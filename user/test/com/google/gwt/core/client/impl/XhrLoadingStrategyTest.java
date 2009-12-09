/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.impl.AsyncFragmentLoader.LoadErrorHandler;
import com.google.gwt.core.client.impl.XhrLoadingStrategy.MockableXMLHttpRequest;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Tests the default loading strategy and its retry behavior.
 */
public class XhrLoadingStrategyTest extends TestCase {

  static class MockXhr implements MockableXMLHttpRequest {
    public static final String SUCCESSFUL_RESPONSE_TEXT =
        "successful response text";
    public static final String INSTALL_FAILED_RESPONSE_TEXT =
        "install failed response text";

    private ReadyStateChangeHandler handler;
    private int httpStatus;
    private int state;
    private String statusText; 
    private String text;
    private HashMap<String,String> headers;
    
    public MockXhr(int status, String statusText, boolean loads,
        boolean installs, String... headers) {
      this.httpStatus = status;
      this.statusText = statusText;
      if (installs) {
        text = SUCCESSFUL_RESPONSE_TEXT;
      } else if (loads) {
        text = INSTALL_FAILED_RESPONSE_TEXT;
      } else {
        text = null;
      }
      handler = null;
      state = 0;
      assert headers.length % 2 == 0;
      this.headers = new HashMap<String,String>();
      for (int i = 0; i < headers.length; i += 2) {
        this.headers.put(headers[i], headers[i + 1]);
      }
    }

    public void clearOnReadyStateChange() {
      handler = null;
    }

    public int getReadyState() {
      return state;
    }

    public String getResponseText() {
      return state > 3 ? text : null;
    }

    public int getStatus() { 
      return state > 1 ? httpStatus : 0;
    }

    public String getStatusText() {
      return state > 1 ? statusText : null;
    }

    public void open(String method, String url) {
      state = 1;
    }

    public void send() {
      state = 4;
      if (headers.size() != 0) {
        throw new IllegalStateException("not all expected headers set");
      }
      if (handler != null) {
        /* This is brittle, but I don't have a better idea.  The problem is
         * that onReadyStateChange takes a REAL XMLHttpRequest, which I can't
         * mock because it's all final.  I don't want to open
         * ReadyStateChangeHandler's long-standing API to let it take a
         * non-real XMLHttpRequest, just for my wee test here, so instead I
         * admit that null works 'cause the handler won't *use* its argument.
         */
        handler.onReadyStateChange(null);
      }
    }

    public void setOnReadyStateChange(ReadyStateChangeHandler handler) {
      this.handler = handler;
    }

    public void setRequestHeader(String header, String value) {
      String val = headers.get(header);
      if (val == null) {
        throw new IllegalArgumentException("set of unexpected header "
            + header);
      }
      if (!val.equals(value)) {
        throw new IllegalArgumentException("set of header "
            + header + " to unexpected value " + value + ", not " + val);
      }
      headers.remove(header);
    }
  }

  /**
   * {@link XhrLoadingStrategy}, but without actual live XHRs.
   */
  static class MockXhrLoadingStrategy extends XhrLoadingStrategy {
    private static final String FRAGMENT_URL = "http://nowhere.net/fragment";
    private ArrayList<MockXhr> xhrs;
    
    public MockXhrLoadingStrategy(MockXhr... input) {
      xhrs = new ArrayList<MockXhr>(Arrays.asList(input));
    }

    public void assertDone() {
      if (xhrs.size() != 0) {
        throw new IllegalStateException("leftover createXhr() data" +
        " (too few load retries?)");
      }
    }

    /**
     * Test stub; install succeeds unless text says otherwise.
     */
    @Override
    protected void gwtInstallCode(String text) {
      if (MockXhr.INSTALL_FAILED_RESPONSE_TEXT.equals(text)) {
        throw new RuntimeException(text);
      }
    }

    /**
     * Test stub; bypass the JSNI, but we're returning a (mock) URL.
     */
    @Override
    protected String gwtStartLoadingFragment(int fragment,
        LoadErrorHandler loadErrorHandler) {
      return FRAGMENT_URL;
    }

    @Override
    protected MockableXMLHttpRequest createXhr() {
      if (xhrs.size() == 0) {
        throw new IllegalStateException("createXhr() underflow" +
            " (too many load retries?)");
      }
      return xhrs.remove(0);
    }
  }
  
  /**
   * Basic succeeds-on-first-try case.
   */
  public void testNoRetrySucceeds() {
    MockXhrLoadingStrategy xls = new MockXhrLoadingStrategy(
        new MockXhr(200, "200 Ok", true, true));
    xls.startLoadingFragment(1, new LoadErrorHandler() {
      public void loadFailed(Throwable reason) {
        fail();
      }
    });
    xls.assertDone();
  }

  /**
   * Fails irrevocably on first try; doesn't retry.
   */
  public void testNoRetryFails() {
    final boolean loadFailedCalled[] = new boolean[1];
    loadFailedCalled[0] = false;
    MockXhrLoadingStrategy xls = new MockXhrLoadingStrategy(
        new MockXhr(200, "Ok", true, false));
    xls.startLoadingFragment(1, new LoadErrorHandler() {
      public void loadFailed(Throwable reason) {
        loadFailedCalled[0] = true;
      }
    });
    xls.assertDone();
    if (!loadFailedCalled[0]) {
      fail("should have failed to install, but didn't");
    }
  }

  /**
   * Needs some retries, but succeeds.
   */
  public void testRetrySucceeds() {
    MockXhrLoadingStrategy xls = new MockXhrLoadingStrategy(
        new MockXhr(0, "Could not connect", false, false),
        new MockXhr(200, "Ok", true, true, "Cache-Control", "no-cache"));
    xls.startLoadingFragment(1, new LoadErrorHandler() {
      public void loadFailed(Throwable reason) {
        fail();
      }
    });
    xls.assertDone();    
  }

  /**
   * Needs retries, and never succeeds.
   */
  public void testRetryFails() {
    final boolean loadFailedCalled[] = new boolean[1];
    loadFailedCalled[0] = false;
    MockXhrLoadingStrategy xls = new MockXhrLoadingStrategy(
        new MockXhr(0, "Could not connect", false, false),
        new MockXhr(0, "Could not connect", false, false,
            "Cache-Control", "no-cache"),
        new MockXhr(0, "Could not connect", false, false,
            "Cache-Control", "no-cache"));
    xls.startLoadingFragment(1, new LoadErrorHandler() {
      public void loadFailed(Throwable reason) {
        loadFailedCalled[0] = true;
      }
    });
    xls.assertDone();
    if (!loadFailedCalled[0]) {
      fail("should have failed to install, but didn't");
    }
  }

  /**
   * A bizarre case we've seen in the wild...
   */
  public void testNull200Case() {
    MockXhrLoadingStrategy xls = new MockXhrLoadingStrategy(
        new MockXhr(200, "Ok", false, false),
        new MockXhr(200, "Ok", false, false,
            "Cache-Control", "no-cache"),
        new MockXhr(200, "Ok", true, true,
            "Cache-Control", "no-cache"));
    xls.startLoadingFragment(1, new LoadErrorHandler() {
      public void loadFailed(Throwable reason) {
        fail();
      }
    });
    xls.assertDone();
  }

  /**
   * Check some HTTP status codes....
   */
  public void testRetryCodes() {
    MockXhrLoadingStrategy xls = new MockXhrLoadingStrategy(
        new MockXhr(500, "Server Error", false, false),
        new MockXhr(404, "Not Found", false, false,
            "Cache-Control", "no-cache"),
        new MockXhr(200, "Ok", true, true,
            "Cache-Control", "no-cache"));
    xls.startLoadingFragment(1, new LoadErrorHandler() {
      public void loadFailed(Throwable reason) {
        fail();
      }
    });
    xls.assertDone();
  }
}
