/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.user.client.Cookies;

/**
 * Tests XSRF protection provided by {@link XsrfProtectedServiceServlet} and
 * {@link XsrfTokenService}.
 */
public class XsrfProtectionTest extends RpcTestBase {

  public static final String SESSION_COOKIE_NAME = "MYSESSIONCOOKIE";

  @Override
  protected void gwtSetUp() {
    // MD5 test vector
    Cookies.setCookie(SESSION_COOKIE_NAME, "abc");
  }

  @Override
  protected void gwtTearDown() {
    Cookies.removeCookie(SESSION_COOKIE_NAME);
  }

  protected static XsrfTestServiceAsync getAsyncService() {
    XsrfTestServiceAsync service =
      (XsrfTestServiceAsync) GWT.create(XsrfTestService.class);

    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "xsrftestservice");

    return service;
  }

  protected static XsrfTokenServiceAsync getAsyncXsrfService() {
    XsrfTokenServiceAsync service =
      (XsrfTokenServiceAsync) GWT.create(XsrfTokenService.class);

    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "xsrfmock");

    return service;
  }

  public void testRpcWithoutXsrfTokenFails() throws Exception {
    XsrfTestServiceAsync service = getAsyncService();

    delayTestFinishForRpc();

    service.drink("kumys", new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {
        RpcTokenException e = (RpcTokenException) caught;
        assertTrue(e.getMessage().contains("XSRF token missing"));
        checkServerState("kumys", false);
      }

      public void onSuccess(Void result) {
        fail("Should've failed without XSRF token");
      }
    });
  }

  public void testRpcWithBadXsrfTokenFails() throws Exception {
    XsrfToken badToken = new XsrfToken("Invalid Token");
    XsrfTestServiceAsync service = getAsyncService();
    ((HasRpcToken) service).setRpcToken(badToken);
    delayTestFinishForRpc();

    service.drink("maksym", new AsyncCallback<Void>() {

      public void onSuccess(Void result) {
        fail("Should've failed with bad XSRF token");
      }

      public void onFailure(Throwable caught) {
        checkServerState("maksym", false);
      }
    });
  }

  public void testXsrfTokenService() throws Exception {
    XsrfTokenServiceAsync xsrfService = getAsyncXsrfService();

    delayTestFinishForRpc();

    xsrfService.getNewXsrfToken(new AsyncCallback<XsrfToken>() {
      public void onSuccess(XsrfToken result) {
        assertNotNull(result);
        assertNotNull(result.getToken());
        // MD5("abc")
        assertEquals("900150983CD24FB0D6963F7D28E17F72", result.getToken());
        finishTest();
      }

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }
    });
  }

  public void testRpcWithXsrfToken() throws Exception {
    XsrfTokenServiceAsync xsrfService = getAsyncXsrfService();

    delayTestFinishForRpc();

    xsrfService.getNewXsrfToken(new AsyncCallback<XsrfToken>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(XsrfToken result) {
        XsrfTestServiceAsync service = getAsyncService();

        ((HasRpcToken) service).setRpcToken(result);
        service.drink("airan", new AsyncCallback<Void>() {

          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Void result) {
            checkServerState("airan", true);
          }
        });
      }
    });
  }

  public void testXsrfTokenWithDifferentSessionCookieFails() throws Exception {
    XsrfTokenServiceAsync xsrfService = getAsyncXsrfService();

    final XsrfTestServiceAsync service = getAsyncService();

    delayTestFinishForRpc();

    xsrfService.getNewXsrfToken(new AsyncCallback<XsrfToken>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(XsrfToken result) {
        // Ensure it's MD5
        assertEquals(32, result.getToken().length());

        ((HasRpcToken) service).setRpcToken(result);

        // change cookie to ensure verification fails since
        // XSRF token was derived from previous cookie value
        Cookies.setCookie(SESSION_COOKIE_NAME, "sometingrandom");

        service.drink("bozo", new AsyncCallback<Void>() {

          public void onFailure(Throwable caught) {
            RpcTokenException e = (RpcTokenException) caught;
            assertTrue(e.getMessage().contains(
                "Invalid XSRF token"));
            checkServerState("bozo", false);
          }

          public void onSuccess(Void result) {
            fail("Should've failed since session cookie has changed");
          }
        });
      }
    });
  }

  private void checkServerState(String drink, final boolean stateShouldChange) {
    XsrfTestServiceAsync service = getAsyncService();

    service.checkIfDrankDrink(drink, new AsyncCallback<Boolean>() {

      public void onSuccess(Boolean result) {
        assertTrue(stateShouldChange == result);
        finishTest();
      }

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }
    });
  }
}
