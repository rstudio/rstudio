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
import com.google.gwt.junit.client.GWTTestCase;

/**
 * HTTPRequestBuilder tests.
 */
public class RequestBuilderTest extends GWTTestCase {
  private static final int TEST_FINISH_DELAY = 10000;

  private static String getTestBaseURL() {
    return GWT.getModuleBaseURL() + "testRequestBuilder/";
  }

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
      new RequestBuilder((RequestBuilder.Method) null,
          null);
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
          fail("HTTPRequest timed out");
        }

        public void onResponseReceived(Request request, Response response) {
          // should never get here
          fail();
        }
      });
    } catch (IllegalArgumentException ex) {
      // purposely ignored
    } catch (RequestPermissionException ex) {
      // this is the type of exception that we expect
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#RequestBuilder(java.lang.String, java.lang.String)}. *
   */
  public void testRequestBuilderStringString_HTTPMethodRestrictionOverride() {
    new RequestBuilder(RequestBuilder.GET, "FOO");

    class MyRequestBuilder extends RequestBuilder {
      MyRequestBuilder(String httpMethod, String url) {
        super(httpMethod, url);
      }
    };

    new MyRequestBuilder("HEAD", "FOO");
    // should reach here without any exceptions being thrown
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#sendRequest(java.lang.String, com.google.gwt.http.client.RequestCallback)}.
   */
  public void testSendRequest_GET() throws RequestException {
    delayTestFinish(TEST_FINISH_DELAY);

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL() + "sendRequest_GET");
    builder.sendRequest(null, new RequestCallback() {
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
   * {@link com.google.gwt.http.client.RequestBuilder#sendRequest(java.lang.String, com.google.gwt.http.client.RequestCallback)}.
   */
  public void testSendRequest_POST() throws RequestException {
    delayTestFinish(TEST_FINISH_DELAY);

    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
        getTestBaseURL() + "sendRequest_POST");
    builder.sendRequest("method=test+request", new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        fail("HTTPRequest timed out");
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(200, response.getStatusCode());
        finishTest();
      }
    });
  }

  public void testSetPassword() {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL());
    try {
      builder.setPassword(null);
    } catch (NullPointerException ex) {
      // Correct behavior, exception was thrown
    }

    try {
      builder.setPassword("");
    } catch (IllegalArgumentException ex) {
      // Correct behavior, exception was thrown
    }
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
    } catch (NullPointerException ex) {
      // purposely ignored
    }

    try {
      builder.setHeader("", "bar");
      fail("setRequestHeader(\"\", \"bar\")");
    } catch (IllegalArgumentException ex) {
      // purposely ignored
    }

    try {
      builder.setHeader("foo", null);
      fail("setRequestHeader(\"foo\", null)");
    } catch (NullPointerException ex) {
      // purposely ignored
    }

    try {
      builder.setHeader("foo", "");
      fail("setRequestHeader(\"foo\", \"\")");
    } catch (IllegalArgumentException ex) {
      // purposely ignored
    }

    delayTestFinish(TEST_FINISH_DELAY);

    builder = new RequestBuilder(RequestBuilder.GET, getTestBaseURL()
        + "setRequestHeader");
    builder.setHeader("Foo", "Bar");
    builder.setHeader("Foo", "Bar1");

    builder.sendRequest(null, new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        fail("HTTPRequest timed out");
      }

      public void onResponseReceived(Request request, Response response) {
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
    delayTestFinish(TEST_FINISH_DELAY);

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL() + "setTimeout/noTimeout");
    builder.setTimeoutMillis(10000);
    builder.sendRequest(null, new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        fail("Test timed out");
      }

      public void onResponseReceived(Request request, Response response) {
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
  public void testSetTimeout_timeout() throws RequestException {
    delayTestFinish(TEST_FINISH_DELAY);

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        getTestBaseURL() + "setTimeout/timeout");
    builder.setTimeoutMillis(2000);
    builder.sendRequest(null, new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        finishTest();
      }

      public void onResponseReceived(Request request, Response response) {
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
    } catch (NullPointerException ex) {
      // Correct behavior, exception was thrown
    }

    try {
      builder.setUser("");
    } catch (IllegalArgumentException ex) {
      // Correct behavior, exception was thrown
    }
  }
}
