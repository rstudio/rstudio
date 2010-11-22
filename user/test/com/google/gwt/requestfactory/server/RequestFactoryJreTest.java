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
package com.google.gwt.requestfactory.server;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.requestfactory.client.RequestFactoryTest;
import com.google.gwt.requestfactory.server.testing.InProcessRequestTransport;
import com.google.gwt.requestfactory.server.testing.RequestFactoryMagic;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;

/**
 * Runs the RequestFactory tests in-process.
 */
public class RequestFactoryJreTest extends RequestFactoryTest {

  public static <T extends RequestFactory> T createInProcess(Class<T> clazz) {
    EventBus eventBus = new SimpleEventBus();
    T req = RequestFactoryMagic.create(clazz);
    ServiceLayer serviceLayer = ServiceLayer.create();
    SimpleRequestProcessor processor = new SimpleRequestProcessor(serviceLayer);
    req.initialize(eventBus, new InProcessRequestTransport(processor));
    return req;
  }

  @Override
  public String getModuleName() {
    return null;
  }

  @Override
  protected SimpleRequestFactory createFactory() {
    return createInProcess(SimpleRequestFactory.class);
  }
}
