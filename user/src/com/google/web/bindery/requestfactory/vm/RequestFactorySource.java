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
package com.google.web.bindery.requestfactory.vm;

import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.vm.InProcessRequestFactory.RequestFactoryHandler;

import java.lang.reflect.Proxy;

/**
 * Create JRE-compatible instances of a RequestFactory interface.
 * 
 * <span style='color: red'>This is experimental, unsupported code.</span>
 */
public class RequestFactorySource {
  /**
   * Create an instance of a RequestFactory. The returned RequestFactory must be
   * initialized with an explicit
   * {@link com.google.web.bindery.requestfactory.shared.RequestTransport
   * RequestTransport} via the
   * {@link RequestFactory#initialize(com.google.web.bindery.event.shared.EventBus, com.google.web.bindery.requestfactory.shared.RequestTransport)
   * initialize(EventBus, RequestTransport} method.
   * 
   * @param <T> the RequestFactory type
   * @param requestFactory the RequestFactory type
   * @return an instance of the RequestFactory type
   * @see InProcessRequestTransport
   */
  public static <T extends RequestFactory> T create(Class<T> requestFactory) {
    RequestFactoryHandler handler =
        new InProcessRequestFactory(requestFactory).new RequestFactoryHandler();
    return requestFactory.cast(Proxy.newProxyInstance(Thread.currentThread()
        .getContextClassLoader(), new Class<?>[] {requestFactory}, handler));
  }

  private RequestFactorySource() {
  }
}
