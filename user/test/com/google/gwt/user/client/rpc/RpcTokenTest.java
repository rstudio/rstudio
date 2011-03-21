/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;

/**
 * Tests RpcToken functionality.
 */
public class RpcTokenTest extends RpcTestBase {

  /**
   * First RpcToken implementation.
   */
  public static class TestRpcToken implements RpcToken {
    String tokenValue;
  }

  /**
   * Second RpcToken implementation.
   */
  public static class AnotherTestRpcToken implements RpcToken {
    int token;
  }

  protected static RpcTokenTestServiceAsync getAsyncService() {
    RpcTokenTestServiceAsync service =
      (RpcTokenTestServiceAsync) GWT.create(RpcTokenTestService.class);

    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "rpctokentest");

    return service;
  }

  protected static AnnotatedRpcTokenTestServiceAsync getAnnotatedAsyncService() {
    AnnotatedRpcTokenTestServiceAsync service =
      (AnnotatedRpcTokenTestServiceAsync) GWT.create(AnnotatedRpcTokenTestService.class);

    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "rpctokentest-annotation");

    return service;
  }

  public void testRpcTokenMissing() {
    RpcTokenTestServiceAsync service = getAsyncService();

    delayTestFinishForRpc();

    service.getRpcTokenFromRequest(new AsyncCallback<RpcToken>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(RpcToken token) {
        assertNull(token);
        finishTest();
      }
    });
  }

  public void testRpcTokenPresent() {
    RpcTokenTestServiceAsync service = getAsyncService();

    final TestRpcToken token = new TestRpcToken();
    token.tokenValue = "Drink kumys!";
    ((HasRpcToken) service).setRpcToken(token);

    delayTestFinishForRpc();

    service.getRpcTokenFromRequest(new AsyncCallback<RpcToken>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(RpcToken rpcToken) {
        assertNotNull(rpcToken);
        assertTrue(rpcToken instanceof TestRpcToken);
        assertEquals(token.tokenValue, ((TestRpcToken) rpcToken).tokenValue);
        finishTest();
      }
    });
  }

  public void testRpcTokenExceptionHandler() {
    RpcTokenTestServiceAsync service = getAsyncService();
    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "rpctokentest?throw=true");
    ((HasRpcToken) service).setRpcTokenExceptionHandler(new RpcTokenExceptionHandler() {
      public void onRpcTokenException(RpcTokenException exception) {
        assertNotNull(exception);
        finishTest();
      }
    });

    delayTestFinishForRpc();

    service.getRpcTokenFromRequest(new AsyncCallback<RpcToken>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(RpcToken rpcToken) {
        fail("Should've called RpcTokenExceptionHandler");
      }
    });
  }

  public void testRpcTokenAnnotationDifferentFromActualType() {
    AnnotatedRpcTokenTestServiceAsync service = getAnnotatedAsyncService();

    // service is annotated to use AnotherTestRpcToken and not TestRpcToken,
    // generated proxy should catch this error
    final TestRpcToken token = new TestRpcToken();
    token.tokenValue = "Drink kumys!";
    try {
      ((HasRpcToken) service).setRpcToken(token);
      fail("Should have thrown an RpcTokenException");
    } catch (RpcTokenException e) {
      // Expected
    }
  }

  public void testRpcTokenAnnotation() {
    AnnotatedRpcTokenTestServiceAsync service = getAnnotatedAsyncService();

    final AnotherTestRpcToken token = new AnotherTestRpcToken();
    token.token = 1337;
    ((HasRpcToken) service).setRpcToken(token);

    service.getRpcTokenFromRequest(new AsyncCallback<RpcToken>() {

      public void onSuccess(RpcToken result) {
        assertNotNull(result);
        assertTrue(result instanceof AnotherTestRpcToken);
        assertEquals(token.token, ((AnotherTestRpcToken) result).token);
        finishTest();
      }

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }
    });
  }
}
