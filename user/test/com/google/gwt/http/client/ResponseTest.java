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
package com.google.gwt.http.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * 
 */
public class ResponseTest extends RequestTestBase {

  private static RequestBuilder getHTTPRequestBuilder() {
    return getHTTPRequestBuilder(getTestBaseURL());
  }

  private static RequestBuilder getHTTPRequestBuilder(String testURL) {
    return new RequestBuilder(RequestBuilder.GET, testURL);
  }

  private static String getTestBaseURL() {
    return GWT.getModuleBaseURL() + "testResponse/";
  }

  private static native boolean isSafari() /*-{
    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf('safari') != -1) {
      return true;
    }
    return false;
  }-*/;

  private static void raiseUnexpectedException(Throwable exception) {
    fail("Unexpected exception: " + exception.toString());
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.http.ResponseTest";
  }

  /**
   * Test method for {@link com.google.gwt.http.client.Response#getStatusCode()}.
   */
  public void testGetStatusCode() {
    executeTest(new RequestCallback() {
      @Override
      public void onError(Request request, Throwable exception) {
        fail();
      }

      @Override
      public void onResponseReceived(Request request, Response response) {
        assertEquals(200, response.getStatusCode());
        finishTest();
      }
    });
  }

  /**
   * Test method for {@link com.google.gwt.http.client.Response#getStatusCode()}.
   * DISABLED - we use a newer version of webkit in our hosted browser than
   * Safari's; we can't reliably distinguish between the two so we disabled the
   * test
   */
  public void disabledTestGetStatusCode_Safari() {
    if (!isSafari()) {
      // Only test this on Safari
      return;
    }

    executeTest(getHTTPRequestBuilder(getTestBaseURL() + "noResponseText"),
        new RequestCallback() {
          @Override
          public void onError(Request request, Throwable exception) {
            if (exception instanceof RuntimeException) {
              finishTest();
            } else {
              raiseUnexpectedException(exception);
            }
          }

          @Override
          public void onResponseReceived(Request request, Response response) {
            try {
              int statusCode = response.getStatusCode();
              fail("Unexpected RuntimeException from getStatusCode()");
            } catch (RuntimeException ex) {
            }

            finishTest();
          }
        });
  }

  /**
   * Test method for {@link com.google.gwt.http.client.Response#getStatusText()}.
   */
  public void testGetStatusText() {
    executeTest(new RequestCallback() {
      @Override
      public void onError(Request request, Throwable exception) {
        if (exception instanceof RuntimeException) {

        } else {
          raiseUnexpectedException(exception);
        }
      }

      @Override
      public void onResponseReceived(Request request, Response response) {
        assertEquals("OK", response.getStatusText());
        finishTest();
      }
    });
  }

  public void testGetHeadersOffline() {
    ResponseImpl resp = new ResponseImpl(XMLHttpRequest.create()) {
      @Override
      protected boolean isResponseReady() {
        return true;
      }
    };
    Header[] headers = resp.getHeaders();
    assertNotNull(headers);
    assertEquals(0, headers.length);
  }

  /**
   * Test method for {@link com.google.gwt.http.client.Response#getStatusText()}.
   */
  public void disabledTestGetStatusText_Safari() {
    if (!isSafari()) {
      // Only test this on Safari
      return;
    }

    executeTest(getHTTPRequestBuilder(getTestBaseURL() + "noResponseText"),
        new RequestCallback() {
          @Override
          public void onError(Request request, Throwable exception) {
            if (exception instanceof RuntimeException) {
              finishTest();
            } else {
              raiseUnexpectedException(exception);
            }
          }

          @Override
          public void onResponseReceived(Request request, Response response) {
            try {
              String statusText = response.getStatusText();
              fail("Unexpected RuntimeException from getStatusText()");
            } catch (RuntimeException ex) {
            }

            finishTest();
          }
        });
  }

  private void executeTest(RequestBuilder builder, RequestCallback callback) {
    delayTestFinishForRequest();

    try {
      builder.sendRequest(null, callback);
    } catch (RequestException e) {
      fail(e.getMessage());
    }
  }

  private void executeTest(RequestCallback callback) {
    executeTest(getHTTPRequestBuilder(), callback);
  }
}
