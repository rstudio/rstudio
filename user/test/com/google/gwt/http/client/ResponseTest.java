// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.http.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * 
 */
public class ResponseTest extends GWTTestCase {
  private static final int TEST_FINISH_DELAY = 10000;

  private static RequestBuilder getHTTPRequestBuilder() {
    return getHTTPRequestBuilder(getTestBaseURL());
  }

  private static RequestBuilder getHTTPRequestBuilder(String testURL) {
    return new RequestBuilder(RequestBuilder.GET,
        testURL);
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
  
  public String getModuleName() {
    return "com.google.gwt.http.ResponseTest";
  }
  
  /**
   * Test method for
   * {@link com.google.gwt.http.client.Response#getStatusCode()}.
   */
  public void testGetStatusCode() {
    executeTest(new RequestCallback() {      
      public void onError(Request request, Throwable exception) {
        fail();
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(200, response.getStatusCode());
        finishTest();
      }
    });    
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.Response#getStatusCode()}.
   */
  public void testGetStatusCode_Safari() {
    if (!isSafari()) {
      // Only test this on Safari
      return;
    }
    
    executeTest(getHTTPRequestBuilder(getTestBaseURL() + "noResponseText"),
        new RequestCallback() {      
      public void onError(Request request, Throwable exception) {
        if (exception instanceof RuntimeException) {
          finishTest();
        } else {
          raiseUnexpectedException(exception);
        }
      }

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
   * Test method for
   * {@link com.google.gwt.http.client.Response#getStatusText()}.
   */
  public void testGetStatusText() {
    executeTest(new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        if (exception instanceof RuntimeException) {
          
        } else {
          raiseUnexpectedException(exception);
        }
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals("OK", response.getStatusText());
        finishTest();
      }
    });    
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.Response#getStatusText()}.
   */
  public void testGetStatusText_Safari() {
    if (!isSafari()) {
      // Only test this on Safari
      return;
    }
    
    executeTest(getHTTPRequestBuilder(getTestBaseURL() + "noResponseText"),
        new RequestCallback() {      
      public void onError(Request request, Throwable exception) {
        if (exception instanceof RuntimeException) {
          finishTest();
        } else {
          raiseUnexpectedException(exception);
        }
      }

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

  private void executeTest(RequestBuilder builder, 
      RequestCallback callback) {
    delayTestFinish(TEST_FINISH_DELAY);

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
