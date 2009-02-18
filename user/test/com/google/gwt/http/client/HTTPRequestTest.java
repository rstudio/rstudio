package com.google.gwt.http.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.HTTPRequest;
import com.google.gwt.user.client.ResponseTextHandler;

public class HTTPRequestTest extends GWTTestCase {

  private static final int TEST_TIMEOUT = 10000;

  private static String getTestBaseURL() {
    return GWT.getModuleBaseURL() + "testRequestBuilder/";
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.http.RequestBuilderTest";
  }

  public void testAsyncGet() {
    delayTestFinish(TEST_TIMEOUT);
    HTTPRequest.asyncGet(getTestBaseURL() + "send_GET",
        new ResponseTextHandler() {
          public void onCompletion(String responseText) {
            assertEquals(RequestBuilderTest.SERVLET_GET_RESPONSE, responseText);
            finishTest();
          }
        });
  }

  public void testAsyncPost() {
    delayTestFinish(TEST_TIMEOUT);
    HTTPRequest.asyncPost(getTestBaseURL() + "simplePost",
        "method=test+request", new ResponseTextHandler() {
          public void onCompletion(String responseText) {
            assertEquals(RequestBuilderTest.SERVLET_POST_RESPONSE, responseText);
            finishTest();
          }
        });
  }
}
