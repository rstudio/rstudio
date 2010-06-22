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
package com.google.gwt.http.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

/**
 * Test cases for the {@link RequestBuilder} class.
 */
public class RequestBuilderTest extends RequestTestBase {

  public static final String SERVLET_DELETE_RESPONSE = "delete";
  public static final String SERVLET_GET_RESPONSE = "get";
  public static final String SERVLET_POST_RESPONSE = "post";
  // W3C's XMLHttpRequest requires it be the empty string
  public static final String SERVLET_HEAD_RESPONSE = "";
  public static final String SERVLET_PUT_RESPONSE = "put";

  private static String getTestBaseURL() {
    return GWT.getModuleBaseURL() + "testRequestBuilder/";
  }

  /**
   * HACK: Part of a work around for IE's failure to throw an exception when an
   * XmlHttpRequest that violates the same origin policy is made.
   */
  private static native boolean isIE() /*-{
    var ua = navigator.userAgent.toLowerCase();
    return ua.indexOf("msie") != -1; 
  }-*/;

  /**
   * HACK: Part of a work around for Safari 2.0.4's failure to throw an
   * exception when an XmlHttpRequest that violates the same origin policy is
   * made.
   */
  private static native boolean isSafari() /*-{
    var ua = navigator.userAgent.toLowerCase();
    return ua.indexOf("webkit") != -1; 
  }-*/;

  /**
   * HACK: Part of a work around for FF 3.5's failure to throw an exception when
   * an XmlHttpRequest that violates the same origin policy is made.
   */
  private static native boolean isFirefox35() /*-{
    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf("gecko") == -1) {
      return false;
    }
    var result = /firefox\/([0-9]+\.[0-9]+)/.exec(ua.toLowerCase());
    if (result && result.length == 2) {
      var version = parseFloat(result[1]);
      if (version >= 3.5) {
        return true;
      }
    }
    return false;
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.http.RequestBuilderTest";
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#RequestBuilder(java.lang.String, java.lang.String)}.
   * <p>
   * NOTE: When running this test against Internet Explorer, the security
   * settings of IE affect this test. The assumption is that the "Access Data
   * Sources Across Domains" setting is set to "Disabled". This is the standard
   * setting for the "Internet" zone, which models the case of a user's browser
   * sending a request to a foreign website. However, if you are running the
   * unit tests against a machine running the GWT app which falls into your
   * "Trusted Sites" or "Local Network" content zone, this setting's value is
   * different. You will have to change the setting to "Disabled" in these zones
   * for this test to pass.
   * <p>
   * Test Cases:
   * <ul>
   * <li>httpMethod == null
   * <li>httpMethod == ""
   * <li>url == null
   * <li>url == ""
   * <li>url == "www.freebsd.org" - violates same source
   * </ul>
   */
  public void testRequestBuilderStringString() throws RequestException {
    try {
      new RequestBuilder((RequestBuilder.Method) null, null);
      fail("NullPointerException should have been thrown for construction with null method.");
    } catch (NullPointerException ex) {
      // purposely ignored
    }

    try {
      new RequestBuilder(RequestBuilder.GET, null);
      fail("NullPointerException should have been thrown for construction with null URL.");
    } catch (NullPointerException ex) {
      // purposely ignored
    }

    try {
      new RequestBuilder(RequestBuilder.GET, "");
      fail("IllegalArgumentException should have been throw for construction with empty URL.");
    } catch (IllegalArgumentException ex) {
      // purposely ignored
    }

    try {
      RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
          "http://www.freebsd.org");
      builder.sendRequest(null, new RequestCallback() {
        public void onError(Request request, Throwable exception) {
          // should never get here
        }

        public void onResponseReceived(Request request, Response response) {
          // should never get here
        }
      });

      if (isIE() || isSafari() || isFirefox35()) {
        /*
         * HACK: Safari 2.0.4 will not throw an exception for XHR's that violate
         * the same-origin policy. It appears to silently ignore them so we do
         * not fail this test if we are on Safari and the
         * RequestPermissionException is not thrown. Even though Safari 3.0.4
         * does throw an exception in this case, we exclude it anyway.
         * 
         * FF3.5 allows XHR's to violate the same-origin policy and offers no
         * way to disable the feature from the client. Only the server can block
         * the same origin policy.
         *
         * IE with certain configuration of WebDriver does not enforce the same
         * origin policy.
         */
      } else {
        /*
         * All other supported browsers throw an exception for XHR's that
         * violate the same-origin policy; fail the test if we get here.
         */
        fail("Expected RequestPermissionException");
      }
    } catch (IllegalArgumentException ex) {
      // purposely ignored
    } catch (RequestPermissionException ex) {
      // this is the type of exception that we expect
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#RequestBuilder(java.lang.String, java.lang.String)}.
   */
  public void testRequestBuilderStringString_HTTPMethodRestrictionOverride() {
    new RequestBuilder(RequestBuilder.GET, "FOO");

    class MyRequestBuilder extends RequestBuilder {
      MyRequestBuilder(String httpMethod, String url) {
        super(httpMethod, url);
      }
    }

    new MyRequestBuilder("HEAD", "FOO");
    // should reach here without any exceptions being thrown
  }

  public void testSend_DELETE() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.DELETE,
        getTestBaseURL());
    testSend(builder, SERVLET_DELETE_RESPONSE);
  }

  public void testSend_GET() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL() + "send_GET");
    testSend(builder, SERVLET_GET_RESPONSE);
  }

  /*
   * Need to file an HtmlUnit bug. TODO(amitmanjhi): contribute an HtmlUnit
   * patch.
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testSend_HEAD() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.HEAD,
        getTestBaseURL());
    testSend(builder, SERVLET_HEAD_RESPONSE);
  }

  public void testSend_POST() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
        getTestBaseURL() + "sendRequest_POST");
    builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
    builder.setRequestData("method=test+request");
    testSend(builder, SERVLET_POST_RESPONSE);
  }

  public void testSend_PUT() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.PUT,
        getTestBaseURL());
    builder.setHeader("Content-Type", "text/html");
    builder.setRequestData("<html><body>Put Me</body></html>");
    testSend(builder, SERVLET_PUT_RESPONSE);
  }

  public void testSendRequest_DELETE() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.DELETE,
        getTestBaseURL());
    testSendRequest(builder, null, SERVLET_DELETE_RESPONSE);
  }

  public void testSendRequest_GET() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL() + "sendRequest_GET");
    testSendRequest(builder, null, SERVLET_GET_RESPONSE);
  }

  /*
   * Need to file an HtmlUnit bug. TODO(amitmanjhi): contribute an HtmlUnit
   * patch.
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testSendRequest_HEAD() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.HEAD,
        getTestBaseURL());
    testSendRequest(builder, null, SERVLET_HEAD_RESPONSE);
  }

  public void testSendRequest_POST() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
        getTestBaseURL() + "sendRequest_POST");
    builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
    testSendRequest(builder, "method=test+request", SERVLET_POST_RESPONSE);
  }

  public void testSendRequest_PUT() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.PUT,
        getTestBaseURL());
    builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
    testSendRequest(builder, "<html><body>Put Me</body></html>",
        SERVLET_PUT_RESPONSE);
  }

  public void testSetCallback() {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL());
    try {
      builder.setCallback(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testSetPassword() {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL());
    try {
      builder.setPassword(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }

    try {
      builder.setPassword("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testSetRequestData() {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL());
    // Legal.
    builder.setRequestData(null);
    builder.setRequestData("");
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#setHeader(java.lang.String, java.lang.String)}.
   * 
   * <p>
   * Test Cases:
   * <ul>
   * <li>name == null
   * <li>name == ""
   * <li>value == null
   * <li>value == ""
   * </ul>
   */
  public void testSetRequestHeader() throws RequestException {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL() + "setRequestHeader");

    try {
      builder.setHeader(null, "bar");
      fail("setRequestHeader(null, \"bar\")");
    } catch (NullPointerException expected) {
    }

    try {
      builder.setHeader("", "bar");
      fail("setRequestHeader(\"\", \"bar\")");
    } catch (IllegalArgumentException expected) {
    }

    try {
      builder.setHeader("foo", null);
      fail("setRequestHeader(\"foo\", null)");
    } catch (NullPointerException expected) {
    }

    try {
      builder.setHeader("foo", "");
      fail("setRequestHeader(\"foo\", \"\")");
    } catch (IllegalArgumentException expected) {
    }

    delayTestFinishForRequest();

    builder = new RequestBuilder(RequestBuilder.GET, getTestBaseURL()
        + "setRequestHeader");
    builder.setHeader("Foo", "Bar");
    builder.setHeader("Foo", "Bar1");

    builder.sendRequest(null, new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        fail(exception.getMessage());
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(SERVLET_GET_RESPONSE, response.getText());
        assertEquals(200, response.getStatusCode());
        finishTest();
      }
    });
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#setTimeoutMillis(int)}.
   * 
   * <p>
   * Test Cases:
   * <ul>
   * <li>Timeout greater than the server's response time
   * <li>Timeout is less than the server's response time
   * </ul>
   */
  public void testSetTimeout_noTimeout() throws RequestException {
    delayTestFinishForRequest();

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL() + "setTimeout/noTimeout");
    builder.setTimeoutMillis(10000);
    builder.sendRequest(null, new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        fail(exception.getMessage());
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(SERVLET_GET_RESPONSE, response.getText());
        assertEquals(200, response.getStatusCode());
        finishTest();
      }
    });
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#setTimeoutMillis(int)}.
   * 
   * <p>
   * Test Cases:
   * <ul>
   * <li>Timeout greater than the server's response time
   * <li>Timeout is less than the server's response time
   * </ul>
   * <p>
   * XHR handling is synchronous in HtmlUnit at present (svn r5607).
   */
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testSetTimeout_timeout() throws RequestException {
    delayTestFinishForRequest();

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL() + "setTimeout/timeout");
    builder.setTimeoutMillis(2000);
    builder.sendRequest(null, new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        finishTest();
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(SERVLET_GET_RESPONSE, response.getText());
        assertEquals(200, response.getStatusCode());
        fail("Test did not timeout");
      }
    });
  }

  public void testSetUser() {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL());
    try {
      builder.setUser(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }

    try {
      builder.setUser("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * Helper method to test {@link RequestBuilder#send()}.
   * 
   * @param builder the {@link RequestBuilder}
   * @param expectedResponse the expected response
   */
  private void testSend(RequestBuilder builder, final String expectedResponse)
      throws RequestException {
    delayTestFinishForRequest();
    builder.setCallback(new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        fail(exception.getMessage());
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(expectedResponse, response.getText());
        assertEquals(200, response.getStatusCode());
        finishTest();
      }
    });
    builder.send();
  }

  /**
   * Helper method to test
   * {@link RequestBuilder#sendRequest(String, RequestCallback)}.
   * 
   * @param builder the {@link RequestBuilder}
   * @param requestData the data to request
   * @param expectedResponse the expected response
   */
  private void testSendRequest(RequestBuilder builder, String requestData,
      final String expectedResponse) throws RequestException {
    delayTestFinishForRequest();
    builder.sendRequest(requestData, new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        fail(exception.getMessage());
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(expectedResponse, response.getText());
        assertEquals(200, response.getStatusCode());
        finishTest();
      }
    });
  }
}
