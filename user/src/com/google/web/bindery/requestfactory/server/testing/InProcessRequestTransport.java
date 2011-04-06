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
package com.google.web.bindery.requestfactory.server.testing;

import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import com.google.web.bindery.requestfactory.shared.RequestTransport;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

/**
 * A RequestTransport that calls a {@link SimpleRequestProcessor}. This test can
 * be used for end-to-end tests of RequestFactory service methods in non-GWT
 * test suites.
 * 
 * <pre>
 * ServiceLayer serviceLayer = ServiceLayer.create();
 * SimpleRequestProcessor processor = new SimpleRequestProcessor(serviceLayer);
 * EventBus eventBus = new SimpleEventBus();
 * MyRequestFactory f = RequestFactorySource.create(MyRequestFactory.class);
 * f.initialize(eventBus, new InProcessRequestTransport(processor));
 * </pre>
 * 
 * @see com.google.web.bindery.vm.RequestFactorySource
 * @see com.google.web.bindery.requestfactory.server.ServiceLayer#create(com.google.gwt.requestfactory.server.ServiceLayerDecorator...)
 *      ServiceLayer.create()
 * @see com.google.gwt.event.shared.SimpleEventBus SimpleEventBus
 * @see SimpleRequestProcessor
 */
public class InProcessRequestTransport implements RequestTransport {
  private static final boolean DUMP_PAYLOAD = Boolean.getBoolean("gwt.rpc.dumpPayload");
  private final SimpleRequestProcessor processor;

  public InProcessRequestTransport(SimpleRequestProcessor processor) {
    this.processor = processor;
  }

  public void send(String payload, TransportReceiver receiver) {
    String result;
    try {
      if (DUMP_PAYLOAD) {
        System.out.println(">>> " + payload);
      }
      result = processor.process(payload);
      if (DUMP_PAYLOAD) {
        System.out.println("<<< " + result);
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
      receiver.onTransportFailure(new ServerFailure(e.getMessage()));
      return;
    }
    receiver.onTransportSuccess(result);
  }
}
