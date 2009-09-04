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
package com.google.gwt.jsonp.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Tests for {@link JsonpRequest}.
 */
public class JsonpRequestTest extends GWTTestCase {

  /**
   * The maximum amount of time to wait for a response in milliseconds.
   */
  private static final int RESPONSE_DELAY = 2500;

  /**
   * The current Id of the async callback.
   */
  protected static int currentId;

  /**
   * Checks that an error is received.
   */
  private class AssertFailureCallback<T> implements AsyncCallback<T> {
    private String expectedMessage;
    private int id;

    public AssertFailureCallback(String expectedMessage) {
      id = ++currentId;
      this.expectedMessage = expectedMessage;
    }

    public void onFailure(Throwable throwable) {
      if (id == currentId) {
        assertEquals(expectedMessage, throwable.getMessage());
        finishTest();
      }
    }

    public void onSuccess(T value) {
      if (id == currentId) {
        fail();
      }
    }
  }

  /**
   * Checks that the received value is as expected.
   */
  private class AssertSuccessCallback<T> implements AsyncCallback<T> {
    private T expectedValue;
    private int id;

    private AssertSuccessCallback(T expectedValue) {
      this.id = ++currentId;
      this.expectedValue = expectedValue;
    }

    public void onFailure(Throwable throwable) {
      if (id == currentId) {
        fail();
      }
    }

    public void onSuccess(T value) {
      if (id == currentId) {
        assertEquals(expectedValue, value);
        finishTest();
      }
    }
  }

  /**
   * Checks that a timeout happens.
   */
  private class AssertTimeoutExceptionCallback<T> implements AsyncCallback<T> {
    public void onFailure(Throwable throwable) {
      assertTrue(throwable instanceof TimeoutException);
      finishTest();
    }

    public void onSuccess(T value) {
      fail();
    }
  }

  private static String echo(String value) {
    return GWT.getModuleBaseURL() + "echo?action=SUCCESS&value=" + value;
  }

  private static String echoFailure(String error) {
    return GWT.getModuleBaseURL() + "echo?action=FAILURE&error=" + error;
  }

  private static String echoTimeout() {
    return GWT.getModuleBaseURL() + "echo?action=TIMEOUT";
  }

  private JsonpRequestBuilder jsonp;

  @Override
  public String getModuleName() {
    return "com.google.gwt.jsonp.JsonpTest";
  }

  public void testBooleanFalse() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestBoolean(echo("false"), new AssertSuccessCallback<Boolean>(
        Boolean.FALSE));
  }

  public void testBooleanTrue() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestBoolean(echo("true"), new AssertSuccessCallback<Boolean>(
        Boolean.TRUE));
  }

  public void testDouble() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestDouble(echo("123.456"), new AssertSuccessCallback<Double>(
        123.456));
  }

  public void testFailureCallback() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.setFailureCallbackParam("failureCallback");
    jsonp.requestString(echoFailure("ERROR"),
        new AssertFailureCallback<String>("ERROR"));
  }

  public void testInteger() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestInteger(echo("123"), new AssertSuccessCallback<Integer>(123));
  }

  /**
   * Tests that if no failure callback is defined, the servlet receives well
   * only a 'callback' parameter, and sends back the error to it.
   */
  public void testNoFailureCallback() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.setFailureCallbackParam(null);
    jsonp.requestString(echoFailure("ERROR"),
        new AssertSuccessCallback<String>("ERROR"));
  }

  public void testNullBoolean() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestBoolean(echo("null"), new AssertSuccessCallback<Boolean>(null));
  }

  public void testNullDouble() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestDouble(echo("null"), new AssertSuccessCallback<Double>(null));
  }

  public void testNullInteger() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestInteger(echo("null"), new AssertSuccessCallback<Integer>(null));
  }

  public void testNullString() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestString(echo("null"), new AssertSuccessCallback<String>(null));
  }

  public void testString() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestString(echo("'Hello'"), new AssertSuccessCallback<String>(
        "Hello"));
  }

  public void testTimeout() {
    delayTestFinish(jsonp.getTimeout() + 500);
    jsonp.requestString(echoTimeout(),
        new AssertTimeoutExceptionCallback<String>());
  }

  public void testVoid() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.send(echo(null), new AssertSuccessCallback<Void>(null));
  }

  @Override
  protected void gwtSetUp() throws Exception {
    jsonp = new JsonpRequestBuilder();
    jsonp.setTimeout(RESPONSE_DELAY + 500);
  }
}
