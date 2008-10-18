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
package test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.TestSetValidator;

/**
 * TODO: document me.
 */
public class ServletMappingTest extends GWTTestCase {

  private static final int RPC_WAIT = 5000;

  public String getModuleName() {
    return "test.ServletMappingTest";
  }

  /**
   * Should call the implementation that returns 1.
   */
  public void testServletMapping1() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test", new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        finishTest();
        assertEquals(new Integer(1), result);
      }
    });
  }

  /**
   * Should call the implementation that returns 2.
   */
  public void testServletMapping2() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/longer", new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        finishTest();
        assertEquals(new Integer(2), result);
      }
    });
  }

  /**
   * Should call the implementation that returns 3.
   */
  public void testServletMapping3() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/long", new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        finishTest();
        assertEquals(new Integer(3), result);
      }
    });
  }

  /**
   * Should fail to find an entry point.
   */
  public void testBadRequestWithExtraPath() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/bogus/extra/path",
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            finishTest();
          }

          public void onSuccess(Object result) {
            finishTest();
            assertEquals(new Integer(1), result);
          }
        });
  }

  /**
   * Should fail to find an entry point.
   */
  public void testBadRequestWithQueryString() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/bogus?a=b&c=d",
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            finishTest();
          }

          public void onSuccess(Object result) {
            finishTest();
            assertEquals(new Integer(1), result);
          }
        });
  }

  /**
   * Should call the implementation that returns 3 with a query string.
   */
  public void testServletMapping3WithQueryString() {
    makeAsyncCall(GWT.getModuleBaseURL() + "test/long?a=b&c=d",
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Object result) {
            finishTest();
            assertEquals(new Integer(3), result);
          }
        });
  }

  /**
   * Should call the implementation that returns 3 with a query string.
   */
  public void testTotallyDifferentServletMapping3() {
    makeAsyncCall(GWT.getModuleBaseURL()
        + "totally/different/but/valid?a=b&c=d", new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        finishTest();
        assertEquals(new Integer(3), result);
      }
    });
  }

  private void makeAsyncCall(String url, AsyncCallback callback) {
    ServletMappingTestServiceAsync async = (ServletMappingTestServiceAsync) GWT.create(ServletMappingTestService.class);
    ServiceDefTarget target = (ServiceDefTarget) async;
    target.setServiceEntryPoint(url);
    delayTestFinish(RPC_WAIT);
    async.which(callback);
  }

}
