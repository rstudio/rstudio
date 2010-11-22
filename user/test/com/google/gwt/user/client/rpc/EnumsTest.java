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
import com.google.gwt.user.client.rpc.EnumsTestService.Basic;
import com.google.gwt.user.client.rpc.EnumsTestService.Complex;
import com.google.gwt.user.client.rpc.EnumsTestService.FieldEnum;
import com.google.gwt.user.client.rpc.EnumsTestService.FieldEnumWrapper;
import com.google.gwt.user.client.rpc.EnumsTestService.Subclassing;

/**
 * Tests enums over RPC.
 */
public class EnumsTest extends RpcTestBase {

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

  /**
   * Test that basic enums can be used over RPC.
   */
  public void testBasicEnums() {
    delayTestFinishForRpc();
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
    Complex a = Complex.A;
    a.value = "client";

    delayTestFinishForRpc();
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
    delayTestFinishForRpc();

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
    delayTestFinishForRpc();

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

  /**
   * Test that enums as fields in a wrapper class can be passed over RPC.
   */
  public void testFieldEnumWrapperClass() {
    delayTestFinishForRpc();

    FieldEnumWrapper wrapper = new FieldEnumWrapper();
    wrapper.setFieldEnum(FieldEnum.X);
    getService().echo(wrapper, new AsyncCallback<FieldEnumWrapper>() {
      public void onFailure(Throwable caught) {
        rethrowException(caught);
      }

      public void onSuccess(FieldEnumWrapper result) {
        assertNotNull("Was null", result);
        FieldEnum fieldEnum = result.getFieldEnum();
        /*
         * Don't want to do assertEquals(FieldEnum.X, fieldEnum) here,
         * since it will force an implicit upcast on FieldEnum -> Object, 
         * which will bias the test.  We want to assert that the
         * EnumOrdinalizer properly prevents ordinalization of FieldEnum.
         */
        assertTrue(FieldEnum.X == fieldEnum);
        finishTest();
      }
    });
  }
}
