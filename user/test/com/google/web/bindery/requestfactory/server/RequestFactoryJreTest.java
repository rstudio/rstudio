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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryTest;
import com.google.web.bindery.requestfactory.server.testing.InProcessRequestTransport;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.SimpleBarProxy;
import com.google.web.bindery.requestfactory.shared.SimpleFooProxy;
import com.google.web.bindery.requestfactory.shared.SimpleRequestFactory;
import com.google.web.bindery.requestfactory.vm.RequestFactorySource;
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;
import com.google.web.bindery.requestfactory.vm.impl.TypeTokenResolver;

import java.io.IOException;

/**
 * Runs the RequestFactory tests in-process.
 */
public class RequestFactoryJreTest extends RequestFactoryTest {

  public static <T extends RequestFactory> T createInProcess(Class<T> clazz) {
    EventBus eventBus = new SimpleEventBus();
    T req = RequestFactorySource.create(clazz);
    ServiceLayer serviceLayer = ServiceLayer.create();
    SimpleRequestProcessor processor = new SimpleRequestProcessor(serviceLayer);
    req.initialize(eventBus, new InProcessRequestTransport(processor));
    return req;
  }

  @Override
  public String getModuleName() {
    return null;
  }

  public void testTypeTokenResolver() throws IOException {
    TypeTokenResolver resolver = TypeTokenResolver.loadFromClasspath();
    testResolver(resolver, SimpleBarProxy.class);
    testResolver(resolver, SimpleFooProxy.class);
  }

  @Override
  protected SimpleRequestFactory createFactory() {
    return createInProcess(SimpleRequestFactory.class);
  }

  private void testResolver(TypeTokenResolver resolver, Class<? extends BaseProxy> type) {
    String token = OperationKey.hash(type.getName());
    String typeName = resolver.getTypeFromToken(token);
    assertEquals(type.getName(), typeName);
  }
}
