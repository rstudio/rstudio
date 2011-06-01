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

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Test cases for Java core emulation classes in GWT RPC.
 * 
 * Logging is tested in a separate suite because it requires logging enabled
 * in gwt.xml. Otherwise, only MathContext has non-trivial content for RPC.
 * 
 */
public class CoreJavaTest extends RpcTestBase {

  public static boolean isValid(MathContext value) {
    return createMathContext().equals(value);
  }

  private static MathContext createMathContext() {
    return new MathContext(5, RoundingMode.CEILING);
  }

  private CoreJavaTestServiceAsync coreJavaTestService;

  public void testMathContext() {
    CoreJavaTestServiceAsync service = getServiceAsync();
    final MathContext expected = createMathContext();

    delayTestFinishForRpc();
    service.echoMathContext(expected, new AsyncCallback<MathContext>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(MathContext result) {
        assertNotNull(result);
        assertTrue(isValid(result));
        finishTest();
      }
    });
  }

  private CoreJavaTestServiceAsync getServiceAsync() {
    if (coreJavaTestService == null) {
      coreJavaTestService = (CoreJavaTestServiceAsync) GWT.create(CoreJavaTestService.class);
    }
    return coreJavaTestService;
  }
}
