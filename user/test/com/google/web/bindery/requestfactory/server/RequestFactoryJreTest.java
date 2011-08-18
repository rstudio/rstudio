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
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.SimpleRequestFactory;
import com.google.web.bindery.requestfactory.vm.RequestFactorySource;
import com.google.web.bindery.requestfactory.vm.testing.UrlRequestTransport;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Runs the RequestFactory tests in-process.
 */
public class RequestFactoryJreTest extends RequestFactoryTest {
  private static final String TEST_SERVER_ADDRESS = System.getProperty("RequestFactory.testUrl");

  public static <T extends RequestFactory> T createInProcess(Class<T> clazz) {
    EventBus eventBus = new SimpleEventBus();
    T req = RequestFactorySource.create(clazz);
    if (TEST_SERVER_ADDRESS != null) {
      try {
        UrlRequestTransport transport = new UrlRequestTransport(new URL(TEST_SERVER_ADDRESS));
        req.initialize(eventBus, transport);
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    } else {
      ServiceLayer serviceLayer = ServiceLayer.create();
      SimpleRequestProcessor processor = new SimpleRequestProcessor(serviceLayer);
      req.initialize(eventBus, new InProcessRequestTransport(processor));
    }
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
