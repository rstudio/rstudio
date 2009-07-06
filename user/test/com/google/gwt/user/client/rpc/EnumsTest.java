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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.EnumsTestService.Basic;
import com.google.gwt.user.client.rpc.EnumsTestService.Complex;
import com.google.gwt.user.client.rpc.EnumsTestService.Subclassing;

/**
 * Tests enums over RPC.
 */
public class EnumsTest extends GWTTestCase {
  private static final int TEST_DELAY = 5000;

  private static EnumsTestServiceAsync getService() {
    EnumsTestServiceAsync service = GWT.create(EnumsTestService.class);
    ServiceDefTarget target = (ServiceDefTarget) service;
    target.setServiceEntryPoint(GWT.getModuleBaseURL() + "enums");
    return service;
  }

  private static void rethrowException(Throwable caught) {
    if (caught instanceof RuntimeException) {
      throw (RuntimeException) caught;
    } else {
      throw new RuntimeException(caught);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  /**
   * Test that basic enums can be used over RPC.
   */
  public void testBasicEnums() {
    delayTestFinish(TEST_DELAY);
    getService().echo(Basic.A, new AsyncCallback<Basic>() {
      public void onFailure(Throwable caught) {
        rethrowException(caught);
      }

      public void onSuccess(Basic result) {
        assertNotNull("Was null", result);
        assertEquals(Basic.A, result);
        finishTest();
      }
    });
  }

  /**
   * Test that complex enums with state and non-default constructors can be used
   * over RPC and that the client state does not change.
   */
  public void testComplexEnums() {
    delayTestFinish(TEST_DELAY);

    Complex a = Complex.A;
    a.value = "client";

    getService().echo(Complex.A, new AsyncCallback<Complex>() {
      public void onFailure(Throwable caught) {
        rethrowException(caught);
      }

      public void onSuccess(Complex result) {
        assertNotNull("Was null", result);
        assertEquals(Complex.A, result);

        // Ensure that the server's changes did not impact us.
        assertEquals("client", result.value);

        finishTest();
      }
    });
  }

  /**
   * Test that null can be used as an enumeration.
   */
  public void testNull() {
    delayTestFinish(TEST_DELAY);

    getService().echo((Basic) null, new AsyncCallback<Basic>() {
      public void onFailure(Throwable caught) {
        rethrowException(caught);
      }

      public void onSuccess(Basic result) {
        assertNull(result);
        finishTest();
      }
    });
  }

  /**
   * Test that enums with subclasses can be passed over RPC.
   */
  public void testSubclassingEnums() {
    delayTestFinish(TEST_DELAY);

    getService().echo(Subclassing.A, new AsyncCallback<Subclassing>() {
      public void onFailure(Throwable caught) {
        rethrowException(caught);
      }

      public void onSuccess(Subclassing result) {
        assertNotNull("Was null", result);
        assertEquals(Subclassing.A, result);
        finishTest();
      }
    });
  }
}
