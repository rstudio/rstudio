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
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Tests for {@link JsonpRequest}.
 */
public class JsonpRequestTest extends GWTTestCase {

  /**
   * Checks that an error is received.
   */
  private class AssertFailureCallback<T> implements AsyncCallback<T> {
    private String expectedMessage;
    private int id;

    public AssertFailureCallback(String expectedMessage) {
      id = currentTestId;
      this.expectedMessage = expectedMessage;
    }

    public void onFailure(Throwable throwable) {
      if (id == currentTestId) {
        assertEquals(expectedMessage, throwable.getMessage());
        finishTest();
      }
    }

    public void onSuccess(T value) {
      if (id == currentTestId) {
        fail();
      }
    }
  }

  /**
   * Checks that the received value is as expected.
   */
  private class AssertSuccessCallback<T> implements AsyncCallback<T> {
    private final T expectedValue;
    private final int id;
    private final Counter counter;

    private AssertSuccessCallback(T expectedValue) {
      this(expectedValue, null);
    }

    private AssertSuccessCallback(T expectedValue, Counter counter) {
      this.id = currentTestId;
      this.counter = counter;
      this.expectedValue = expectedValue;
    }

    public void onFailure(Throwable throwable) {
      if (id == currentTestId) {
        fail();
      }
    }

    public void onSuccess(T value) {
      if (id == currentTestId) {
        assertEquals(expectedValue, value);
        if (counter != null) {
          counter.count();
        } else {
          finishTest();
        }
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

  /**
   * A class that counts results and calls finishTest when the right number
   * have been received.
   */
  private class Counter {
  
    private int count;

    public Counter(int count) {
      this.count = count;
    }
  
    public void count() {
      if (--count < 0) {
        fail("Too many results");
      }
      if (count == 0) {
        finishTest();
      }
    }
  }

  /**
   * The maximum amount of time to wait for a response in milliseconds.
   */
  private static final int RESPONSE_DELAY = 2500;

  /**
   * The ID of the current test.
   */
  protected static int currentTestId;

  private static String echo(String value) {
    return GWT.getModuleBaseURL() + "echo?action=SUCCESS&value=" + value;
  }

  private static String echoDelayed(String value) {
    return echoDelayed(value, 500);
  }

  private static String echoDelayed(String value, long delayMs) {
    return GWT.getModuleBaseURL() + "echo?action=SUCCESS&delay=" + delayMs
       + "&value=" + value;
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

  /**
   * Fails in devmode with HtmlUnit, JS "null" exception.
   * <p>
   * Call occurs through postponedActions in HtmlUnit that execute
   * synchronously. Should be async. Need to file HtmlUnitBug.
   */
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testCancel() {
    delayTestFinish(2000);
    // setup a server request that will delay for 500ms
    JsonpRequest<String> req = jsonp.requestString(echoDelayed("'A'", 500),
        new AssertFailureCallback<String>("A"));
    // cancel it before it comes back
    req.cancel();
    // wait 1s to make sure we don't get a callback
    new Timer() {
      @Override
      public void run() {
        finishTest();
      }
    }.schedule(1000);
  }

  public void testDelay() {
    delayTestFinish(RESPONSE_DELAY);
    JsonpRequest<String> req = jsonp.requestString(echoDelayed("'A'"),
        new AssertSuccessCallback<String>("A"));
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

  /**
   * Hangs indefinitely in devmode with HtmlUnit.
   * <p>
   * Call occurs through postponedActions in HtmlUnit that execute
   * synchronously. Should be async. Need to file HtmlUnitBug.
   */
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testIds() {
    delayTestFinish(RESPONSE_DELAY);
    JsonpRequest<String> reqA = jsonp.requestString(echo("'A'"),
        new AssertSuccessCallback<String>("A"));
    JsonpRequest<String> reqB = jsonp.requestString(echo("'B'"),
        new AssertSuccessCallback<String>("B"));
    // WARNING: knows the current structure of IDs
    int idA = Integer.parseInt(reqA.getCallbackId().substring(1));
    int idB = Integer.parseInt(reqB.getCallbackId().substring(1));
    assertEquals("Unexpected ID sequence", idA + 1, idB);
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

  public void testOverlapped() {
    delayTestFinish(RESPONSE_DELAY);
    Counter counter = new Counter(3);
    JsonpRequest<String> reqA = jsonp.requestString(echoDelayed("'A'", 750),
        new AssertSuccessCallback<String>("A", counter));
    JsonpRequest<String> reqB = jsonp.requestString(echoDelayed("'B'", 500),
        new AssertSuccessCallback<String>("B", counter));
    JsonpRequest<String> reqC = jsonp.requestString(echo("'C'"),
        new AssertSuccessCallback<String>("C", counter));
  }

  public void testPredeterminedIds() {
    delayTestFinish(RESPONSE_DELAY);
    String PREDETERMINED = "pred";
    jsonp.setPredeterminedId(PREDETERMINED);
    JsonpRequest<String> reqA = jsonp.requestString(echo("'A'"),
        new AssertSuccessCallback<String>("A"));
    String idA = reqA.getCallbackId().substring(1);
    assertEquals("Unexpected ID sequence", PREDETERMINED, idA);
    jsonp.setPredeterminedId(null);
  }

  public void testString() {
    delayTestFinish(RESPONSE_DELAY);
    jsonp.requestString(echo("'Hello'"), new AssertSuccessCallback<String>(
        "Hello"));
  }

  public void testTimeout() {
    delayTestFinish(jsonp.getTimeout() + 1000);
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
  
  @Override
  protected void gwtTearDown() throws Exception {
    ++currentTestId;
  }
}
