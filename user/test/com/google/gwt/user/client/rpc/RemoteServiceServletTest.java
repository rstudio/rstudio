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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

/**
 * This test case is used to check that the RemoteServiceServlet walks the class
 * hierarchy looking for the service interface. Prior to this test the servlet
 * would only look into the concrete class but not in any of its super classes.
 * 
 * See <a href=
 * "http://code.google.com/p/google-web-toolkit/issues/detail?id=50&can=3&q="
 * >Bug 50</a> for more details.
 * <p>
 * This test works in conjunction with
 * {@link com.google.gwt.user.server.rpc.RemoteServiceServletTestServiceImpl}.
 * </p>
 */
public class RemoteServiceServletTest extends RpcTestBase {
  private static class MyRpcRequestBuilder extends RpcRequestBuilder {
    private boolean doCreate;
    private boolean doFinish;
    private boolean doSetCallback;
    private boolean doSetContentType;
    private boolean doSetRequestData;
    private boolean doSetRequestId;

    public void check() {
      assertTrue("doCreate", doCreate);
      assertTrue("doFinish", doFinish);
      assertTrue("doSetCallback", doSetCallback);
      assertTrue("doSetContentType", doSetContentType);
      assertTrue("doSetRequestData", doSetRequestData);
      assertTrue("doSetRequestId", doSetRequestId);
    }

    @Override
    protected RequestBuilder doCreate(String serviceEntryPoint) {
      doCreate = true;
      return super.doCreate(serviceEntryPoint);
    }

    @Override
    protected void doFinish(RequestBuilder rb) {
      doFinish = true;
      super.doFinish(rb);
    }

    @Override
    protected void doSetCallback(RequestBuilder rb, RequestCallback callback) {
      doSetCallback = true;
      super.doSetCallback(rb, callback);
    }

    @Override
    protected void doSetContentType(RequestBuilder rb, String contentType) {
      doSetContentType = true;
      super.doSetContentType(rb, contentType);
    }

    @Override
    protected void doSetRequestData(RequestBuilder rb, String data) {
      doSetRequestData = true;
      super.doSetRequestData(rb, data);
    }

    @Override
    protected void doSetRequestId(RequestBuilder rb, int id) {
      doSetRequestId = true;
      super.doSetRequestId(rb, id);
    }
  }

  protected static RemoteServiceServletTestServiceAsync getAsyncService() {
    RemoteServiceServletTestServiceAsync service = (RemoteServiceServletTestServiceAsync) GWT.create(RemoteServiceServletTestService.class);

    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "servlettest");

    return service;
  }

  private Request req;

  public void testAlternateStatusCode() {
    RemoteServiceServletTestServiceAsync service = getAsyncService();
    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "servlettest/404");

    delayTestFinishForRpc();

    service.test(new AsyncCallback<Void>() {

      public void onFailure(Throwable caught) {
        if (caught instanceof StatusCodeException) {
          assertEquals(Response.SC_NOT_FOUND,
              ((StatusCodeException) caught).getStatusCode());
          finishTest();
        } else {
          TestSetValidator.rethrowException(caught);
        }
      }

      public void onSuccess(Void result) {
        fail("Should not have succeeded");
      }
    });
  }

  /**
   * Verify behavior when the RPC method throws a RuntimeException declared on
   * the RemoteService interface.
   */
  public void testDeclaredRuntimeException() {
    RemoteServiceServletTestServiceAsync service = getAsyncService();

    delayTestFinishForRpc();

    service.throwDeclaredRuntimeException(new AsyncCallback<Void>() {

      public void onFailure(Throwable caught) {
        assertTrue(caught instanceof NullPointerException);
        assertEquals("expected", caught.getMessage());
        finishTest();
      }

      public void onSuccess(Void result) {
        fail();
      }
    });
  }

  public void testManualSend() throws RequestException {
    RemoteServiceServletTestServiceAsync service = getAsyncService();

    delayTestFinishForRpc();

    RequestBuilder builder = service.testExpectCustomHeader(new AsyncCallback<Void>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Void result) {
        assertTrue(!req.isPending());
        finishTest();
      }
    });

    builder.setHeader("X-Custom-Header", "true");
    req = builder.send();
    assertTrue(req.isPending());
  }

  public void testPermutationStrongName() {
    RemoteServiceServletTestServiceAsync service = getAsyncService();

    delayTestFinishForRpc();

    assertNotNull(GWT.getPermutationStrongName());
    service.testExpectPermutationStrongName(GWT.getPermutationStrongName(),
        new AsyncCallback<Void>() {

          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Void result) {
            finishTest();
          }
        });
  }

  /**
   * Test that the policy strong name is available from browser-side Java code.
   */
  public void testPolicyStrongName() {
    String policy = ((ServiceDefTarget) getAsyncService()).getSerializationPolicyName();
    assertNotNull(policy);
    assertTrue(policy.length() != 0);
  }

  /**
   * Send request without the permutation strong name and expect a
   * SecurityException. This tests
   * RemoteServiceServlet#checkPermutationStrongName.
   */
  public void testRequestWithoutStrongNameHeader() {
    RemoteServiceServletTestServiceAsync service = getAsyncService();
    ((ServiceDefTarget) service).setRpcRequestBuilder(new RpcRequestBuilder() {
      /**
       * Copied from base class.
       */
      @Override
      protected void doFinish(RequestBuilder rb) {
        // Don't set permutation strong name
        rb.setHeader(MODULE_BASE_HEADER, GWT.getModuleBaseURL());
      }

    });

    delayTestFinishForRpc();
    service.test(new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {
        assertTrue(caught instanceof StatusCodeException);
        assertEquals(500, ((StatusCodeException) caught).getStatusCode());
        finishTest();
      }

      public void onSuccess(Void result) {
        fail();
      }
    });
  }

  /**
   * Ensure that each doFoo method is called.
   */
  public void testRpcRequestBuilder() {
    final MyRpcRequestBuilder builder = new MyRpcRequestBuilder();
    RemoteServiceServletTestServiceAsync service = getAsyncService();
    ((ServiceDefTarget) service).setRpcRequestBuilder(builder);

    delayTestFinishForRpc();
    service.test(new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Void result) {
        builder.check();
        finishTest();
      }
    });
  }

  public void testServiceInterfaceLocation() {
    RemoteServiceServletTestServiceAsync service = getAsyncService();

    delayTestFinishForRpc();

    req = service.test(new AsyncCallback<Void>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Void result) {
        assertTrue(!req.isPending());
        finishTest();
      }
    });
    assertTrue(req.isPending());
  }

  /**
   * Verify behavior when the RPC method throws an unknown RuntimeException
   * (possibly one unknown to the client).
   */
  public void testUnknownRuntimeException() {
    RemoteServiceServletTestServiceAsync service = getAsyncService();

    delayTestFinishForRpc();

    service.throwUnknownRuntimeException(new AsyncCallback<Void>() {

      public void onFailure(Throwable caught) {
        assertTrue(caught instanceof InvocationException);
        finishTest();
      }

      public void onSuccess(Void result) {
        fail();
      }
    });
  }
}
