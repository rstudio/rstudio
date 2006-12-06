// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.http.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.RequestPermissionException;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * HTTPRequestBuilder tests
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
   * 
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
  public void testRequestBuilderStringString() {
    try {
      RequestBuilder builder = new RequestBuilder((RequestBuilder.Method) null,
          null);
    } catch (NullPointerException ex) {
      // purposely ignored
    }

    try {
      RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, null);
    } catch (NullPointerException ex) {
      // purposely ignored
    }

    try {
      RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, "");
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
    } catch (RequestException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#RequestBuilder(java.lang.String, java.lang.String)}. *
   */
  public void testRequestBuilderStringString_HTTPMethodRestrictionOverride() {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, "FOO");

    try {
      class MyRequestBuilder extends RequestBuilder {
        MyRequestBuilder(String httpMethod, String url) {
          super(httpMethod, url);
        }
      };

      builder = new MyRequestBuilder("HEAD", "FOO");
      // should reach here without any exceptions being thrown
    } catch (IllegalArgumentException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#sendRequest(java.lang.String, com.google.gwt.http.client.RequestCallback)}.
   */
  public void testSendRequest_GET() {
    delayTestFinish(TEST_FINISH_DELAY);

    try {
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
    } catch (RequestException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.http.client.RequestBuilder#sendRequest(java.lang.String, com.google.gwt.http.client.RequestCallback)}.
   */
  public void testSendRequest_POST() {
    delayTestFinish(TEST_FINISH_DELAY);

    try {
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
    } catch (RequestException e) {
      fail(e.getMessage());
    }
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
  public void testSetRequestHeader() {
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

    try {
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
    } catch (RequestException e) {
      fail(e.getMessage());
    }
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
  public void testSetTimeout_noTimeout() {
    delayTestFinish(TEST_FINISH_DELAY);

    try {
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
    } catch (RequestException e) {
      fail(e.getMessage());
    }
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
  public void testSetTimeout_timeout() {
    delayTestFinish(TEST_FINISH_DELAY);

    try {
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
    } catch (RequestException e) {
      fail(e.getMessage());
    }
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
