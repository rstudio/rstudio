// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * This test case is used to check that the RemoteServiceServlet walks the class
 * hierarchy looking for the service interface. Prior to this test the servlet
 * would only look into the concrete class but not in any of its super classes.
 * 
 * See <a
 * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=50&can=3&q=">Bug
 * 50</a> for more details.
 * <p>
 * This test works in conjunction with
 * {@link com.google.gwt.user.server.rpc.RemoteServiceServletTestServiceImpl}.
 * </p>
 */
public class RemoteServiceServletTest extends GWTTestCase {
  private static final int TEST_DELAY = Integer.MAX_VALUE;

  private static RemoteServiceServletTestServiceAsync getAsyncService() {
    RemoteServiceServletTestServiceAsync service = (RemoteServiceServletTestServiceAsync) GWT
        .create(RemoteServiceServletTestService.class);

    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "servlettest");

    return service;
  }

  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  public void testServiceInterfaceLocation() {
    RemoteServiceServletTestServiceAsync service = getAsyncService();

    delayTestFinish(TEST_DELAY);

    service.test(new AsyncCallback() {

      public void onFailure(Throwable caught) {
        fail(caught.toString());
      }

      public void onSuccess(Object result) {
        finishTest();
      }
    });
  }
}
