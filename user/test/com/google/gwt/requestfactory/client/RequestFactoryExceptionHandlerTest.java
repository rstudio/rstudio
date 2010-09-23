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

import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;

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
  public void testServerFailureCheckedException() {
    delayTestFinish(5000);
    SimpleFooProxy rayFoo = req.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> persistRay = req.simpleFooRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    // 42 is the crash causing magic number
    rayFoo.setPleaseCrash(42);
    persistRay.fire(new FooReciever(rayFoo, persistRay,
        "java.lang.UnsupportedOperationException"));
  }
  
  @Override
  public void testServerFailureRuntimeException() {
    delayTestFinish(5000);
    SimpleFooProxy rayFoo = req.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> persistRay = req.simpleFooRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    // 43 is the crash causing magic number
    rayFoo.setPleaseCrash(43);
    persistRay.fire(new FooReciever(rayFoo, persistRay,
        "java.lang.Exception"));
  }

}
