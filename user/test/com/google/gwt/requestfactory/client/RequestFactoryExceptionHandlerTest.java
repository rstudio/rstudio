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
package com.google.gwt.requestfactory.client;

import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.Violation;

import java.util.Set;

/**
 * Tests that {@code RequestFactoryServlet} when using a custom
 * ExceptionHandler.
 */
public class RequestFactoryExceptionHandlerTest extends RequestFactoryTest {

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactoryExceptionHandlerTest";
  }

  @Override
  public void testServerFailure() {
    delayTestFinish(5000);

    SimpleFooProxy rayFoo = req.create(SimpleFooProxy.class);
    final RequestObject<SimpleFooProxy> persistRay = req.simpleFooRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    // 42 is the crash causing magic number
    rayFoo.setPleaseCrash(42);

    persistRay.fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onFailure(ServerFailure error) {
        assertEquals("THIS EXCEPTION IS EXPECTED BY A TEST", error.getMessage());
        assertEquals("java.lang.UnsupportedOperationException",
            error.getExceptionType());
        assertFalse(error.getStackTraceString().isEmpty());
        finishTestAndReset();
      }

      @Override
      public void onViolation(Set<Violation> errors) {
        fail("Failure expected but onViolation() was called");
      }

      public void onSuccess(SimpleFooProxy response) {
        fail("Failure expected but onSuccess() was called");
      }
    });
  }

}
