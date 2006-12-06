// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.http.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.impl.HTTPRequestImpl;

/**
 * 
 */
public class RequestTest extends GWTTestCase {
  private static final int TEST_FINISH_DELAY = 10000;
  
  private static String getTestBaseURL() {
    return GWT.getModuleBaseURL() + "testRequest/";
  }
  
  public String getModuleName() {
    return "com.google.gwt.http.RequestTest";
  }
  
  /**
   * Test method for {@link com.google.gwt.http.client.Request#cancel()}.
   */
  public void testCancel() {
    delayTestFinish(TEST_FINISH_DELAY);
    
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, 
        getTestBaseURL() + "/cancel");
    try {
      Request request = builder.sendRequest(null, new RequestCallback() {
        public void onResponseReceived(Request request, Response response) {
          fail("Request was canceled - no response should be received");
        }

        public void onError(Request request, Throwable exception) {
          fail("Request was canceled - no timeout should occur");
        }
      });
      
      assertTrue(request.isPending());
      request.cancel();
      assertFalse(request.isPending());
      
      finishTest();
    } catch (RequestException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.Request#Request(com.google.gwt.core.client.JavaScriptObject, int, com.google.gwt.http.client.RequestCallback)}.
   */
  public void testRequest() {
    RequestCallback callback = new RequestCallback() {
      public void onResponseReceived(Request request, Response response) {
      }

      public void onError(Request request, Throwable exception) {
      }
    };

    try {
      Request request = new Request(null, 0, callback);
      fail();
    } catch (NullPointerException ex) {

    }

    HTTPRequestImpl impl = (HTTPRequestImpl) GWT.create(HTTPRequestImpl.class);
    try {
      Request request = new Request(impl.createXmlHTTPRequest(), -1, callback);
      fail();
    } catch (IllegalArgumentException ex) {

    }

    try {
      Request request = new Request(impl.createXmlHTTPRequest(), -1, null);
      fail();
    } catch (NullPointerException ex) {

    }

    try {
      Request request = new Request(impl.createXmlHTTPRequest(), 0, callback);
    } catch (Throwable ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test method for {@link com.google.gwt.http.client.Request#isPending()}.
   */
  public void testIsPending() {
//    delayTestFinish(TEST_FINISH_DELAY);
    
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, 
        getTestBaseURL() + "isPending");
    try {
      Request request = builder.sendRequest(null, new RequestCallback() {
        public void onResponseReceived(Request request, Response response) {
          finishTest();
        }

        public void onError(Request request, Throwable exception) {
          finishTest();
        }
      });
      
      assertTrue(request.isPending());
//      finishTest();
    } catch (RequestException e) {
      fail(e.getMessage());
    }
  }
}
