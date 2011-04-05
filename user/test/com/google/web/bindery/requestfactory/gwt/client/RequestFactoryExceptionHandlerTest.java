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
package com.google.web.bindery.requestfactory.gwt.client;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.SimpleFooProxy;
import com.google.web.bindery.requestfactory.shared.SimpleFooRequest;

/**
 * Tests that {@code RequestFactoryServlet} when using a custom
 * ExceptionHandler.
 */
public class RequestFactoryExceptionHandlerTest extends RequestFactoryTest {

  private static final int DELAY_TEST_FINISH = 5000;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactoryExceptionHandlerTest";
  }

  @Override
  public void testServerFailureCheckedException() {
    delayTestFinish(DELAY_TEST_FINISH);
    SimpleFooRequest context = req.simpleFooRequest();
    SimpleFooProxy rayFoo = context.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> persistRay = context.persistAndReturnSelf().using(
        rayFoo);
    rayFoo = context.edit(rayFoo);
    // 42 is the crash causing magic number
    rayFoo.setPleaseCrash(42);
    persistRay.fire(new FooReciever(rayFoo, persistRay,
        "java.lang.UnsupportedOperationException"));
  }

  @Override
  public void testServerFailureRuntimeException() {
    delayTestFinish(DELAY_TEST_FINISH);
    SimpleFooRequest context = req.simpleFooRequest();
    SimpleFooProxy rayFoo = context.create(SimpleFooProxy.class);
    final Request<SimpleFooProxy> persistRay = context.persistAndReturnSelf().using(
        rayFoo);
    rayFoo = context.edit(rayFoo);
    // 43 is the crash causing magic number
    rayFoo.setPleaseCrash(43);
    persistRay.fire(new FooReciever(rayFoo, persistRay, "java.lang.Exception"));
  }

}
