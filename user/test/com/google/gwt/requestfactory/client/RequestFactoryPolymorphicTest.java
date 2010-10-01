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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.TestRequestFactory;

/**
 * Just tests the
 * {@link com.google.gwt.requestfactory.rebind.RequestFactoryGenerator} to see
 * if polymorphic signatures are allowed.
 */
public class RequestFactoryPolymorphicTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    /*
     * A single test suite can't have 2 RequestFactory because the compiler runs
     * just once across a suite.
     */
    return "com.google.gwt.requestfactory.RequestFactoryPolymorphicSuite";
  }
  
  public void testGenerator() {
    RequestFactory rf = GWT.create(TestRequestFactory.class);
    EventBus eventBus = new SimpleEventBus();
    rf.initialize(eventBus);
    SimpleFooProxy simpleFoo = rf.create(SimpleFooProxy.class);
    assertNull(simpleFoo.getUserName());
  }
}