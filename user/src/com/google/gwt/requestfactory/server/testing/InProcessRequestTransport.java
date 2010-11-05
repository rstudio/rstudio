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
package com.google.gwt.requestfactory.server.testing;

import com.google.gwt.requestfactory.server.SimpleRequestProcessor;
import com.google.gwt.requestfactory.shared.RequestTransport;

/**
 * A RequesTransports that calls a {@link SimpleRequestProcessor}. This test can
 * be used for end-to-end tests of RequestFactory service methods in non-GWT
 * test suites.
 */
public class InProcessRequestTransport implements RequestTransport {
  private static final boolean DUMP_PAYLOAD = Boolean.getBoolean("gwt.rpc.dumpPayload");
  private final SimpleRequestProcessor processor;

  public InProcessRequestTransport(SimpleRequestProcessor processor) {
    this.processor = processor;
  }

  public void send(String payload, TransportReceiver receiver) {
    try {
      if (DUMP_PAYLOAD) {
        System.out.println(">>> " + payload);
      }
      String result = processor.process(payload);
      if (DUMP_PAYLOAD) {
        System.out.println("<<< " + result);
      }
      receiver.onTransportSuccess(result);
    } catch (RuntimeException e) {
      e.printStackTrace();
      receiver.onTransportFailure(e.getMessage());
    }
  }
}
