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
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.junit.client.GWTTestCase;

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
public class RemoteServiceServletTest extends GWTTestCase {
  private static final int TEST_DELAY = 10000;

  protected static RemoteServiceServletTestServiceAsync getAsyncService() {
    RemoteServiceServletTestServiceAsync service = (RemoteServiceServletTestServiceAsync) GWT.create(RemoteServiceServletTestService.class);

    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "servlettest");

    return service;
  }

  private Request req;

  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  public void testAlternateStatusCode() {
    RemoteServiceServletTestServiceAsync service = getAsyncService();
    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "servlettest/404");

    delayTestFinish(TEST_DELAY);

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

  public void testManualSend() throws RequestException {
    RemoteServiceServletTestServiceAsync service = getAsyncService();

    delayTestFinish(TEST_DELAY);

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

    delayTestFinish(TEST_DELAY);

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

  public void testServiceInterfaceLocation() {
    RemoteServiceServletTestServiceAsync service = getAsyncService();

    delayTestFinish(TEST_DELAY);

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
}
