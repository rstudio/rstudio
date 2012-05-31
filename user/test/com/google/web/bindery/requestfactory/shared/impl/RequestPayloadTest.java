/*
 * Copyright 2012 Google Inc.
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
package com.google.web.bindery.requestfactory.shared.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.gwt.client.DefaultRequestTransport;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.RequestTransport;
import com.google.web.bindery.requestfactory.shared.SimpleFooProxy;
import com.google.web.bindery.requestfactory.shared.SimpleFooRequest;
import com.google.web.bindery.requestfactory.shared.SimpleRequestFactory;
import com.google.web.bindery.requestfactory.shared.SimpleValueProxy;
import com.google.web.bindery.requestfactory.shared.messages.OperationMessage;
import com.google.web.bindery.requestfactory.shared.messages.RequestMessage;

import java.util.Arrays;
import java.util.Collections;

/**
 * Contains a few tests regarding request payload, to make sure we don't send
 * too many things to the server.
 * <p>
 * Uses a special {@link RequestContext} that records the last request payload,
 * then parses and introspects it to see what's really been sent over the wire
 * in relation to what had been enqueued in the request context.
 */
public class RequestPayloadTest extends GWTTestCase {

  /**
   * Records the request payload so it can be re-parsed and analyzed.
   */
  protected static class RecordingRequestTransport implements RequestTransport {
    public String lastRequestPayload;
    private final RequestTransport realTransport;

    public RecordingRequestTransport(RequestTransport realTransport) {
      this.realTransport = realTransport;
    }

    @Override
    public void send(String payload, TransportReceiver receiver) {
      this.lastRequestPayload = payload;
      realTransport.send(payload, receiver);
    }
  }

  protected SimpleRequestFactory factory;
  protected RecordingRequestTransport transport;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  /**
   * Tests that no more proxies and property values than necessary are being sent.
   */
  public void testOperationPayload() throws Exception {
    delayTestFinish(5000);

    SimpleFooRequest context = factory.simpleFooRequest();
    context.findSimpleFooById(1L).with("barField", "oneToManyField").fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(final SimpleFooProxy response) {
            SimpleFooRequest context = factory.simpleFooRequest();
            final SimpleFooProxy foo = context.edit(response);

            // simply casting 'factory' to AbstractRequestFactory would fail
            // when run as a JRE test.
            final AbstractRequestFactory abstractRequestFactory =
                AutoBeanUtils.getAutoBean(foo).<AbstractRequestContext.State> getTag(
                    Constants.REQUEST_CONTEXT_STATE).requestFactory;
            final String fooTypeToken = abstractRequestFactory.getTypeToken(SimpleFooProxy.class);
            final String valueTypeToken =
                abstractRequestFactory.getTypeToken(SimpleValueProxy.class);

            // Create
            final SimpleValueProxy created = context.create(SimpleValueProxy.class);
            created.setNumber(42);
            created.setString("Hello world!");
            created.setSimpleFoo(foo);
            // Test cycles in value
            created.setSimpleValue(Arrays.asList(created));

            // Set
            foo.setSimpleValue(created);
            foo.setSimpleValues(Collections.singletonList(created));

            context.persistAndReturnSelf().using(foo).with("barField", "oneToManyField",
                "oneToManySetField", "simpleValue", "simpleValues").fire(
                new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy response) {
                    RequestMessage requestMessage =
                        AutoBeanCodex.decode(MessageFactoryHolder.FACTORY, RequestMessage.class,
                            transport.lastRequestPayload).as();

                    int seenFoos = 0;
                    int seenValues = 0;
                    for (OperationMessage operationMessage : requestMessage.getOperations()) {
                      if (fooTypeToken.equals(operationMessage.getTypeToken())) {
                        seenFoos++;
                        SimpleProxyId<?> id = (SimpleProxyId<?>) foo.stableId();
                        assertEquals(id.getServerId(), operationMessage.getServerId());
                        assertEquals(2, operationMessage.getPropertyMap().size());
                        assertTrue(operationMessage.getPropertyMap().containsKey("simpleValue"));
                        assertTrue(operationMessage.getPropertyMap().containsKey("simpleValues"));
                      } else if (valueTypeToken.equals(operationMessage.getTypeToken())) {
                        seenValues++;
                        AutoBean<SimpleValueProxy> bean = AutoBeanUtils.getAutoBean(created);
                        SimpleProxyId<?> id = BaseProxyCategory.stableId(bean);
                        assertEquals(id.getClientId(), operationMessage.getClientId());
                        assertEquals(AutoBeanUtils.getAllProperties(bean).keySet(),
                            operationMessage.getPropertyMap().keySet());
                      }
                    }
                    assertTrue(seenFoos > 0);
                    assertTrue(seenValues > 0);

                    // Persist without any change
                    factory.simpleFooRequest().persistAndReturnSelf().using(response).fire(
                        new Receiver<SimpleFooProxy>() {

                          @Override
                          public void onSuccess(SimpleFooProxy response) {
                            RequestMessage requestMessage =
                                AutoBeanCodex.decode(MessageFactoryHolder.FACTORY,
                                    RequestMessage.class, transport.lastRequestPayload).as();

                            int seenFoos = 0;
                            for (OperationMessage operationMessage : requestMessage.getOperations()) {
                              if (fooTypeToken.equals(operationMessage.getTypeToken())) {
                                seenFoos++;
                                assertNull(operationMessage.getPropertyMap());
                              }
                            }
                            assertTrue(seenFoos > 0);

                            finishTest();
                          }
                        });
                  }
                });
          }
        });
  }

  /**
   * Create (without initializing) a new {@link SimpleRequestFactory}.
   */
  protected SimpleRequestFactory createFactory() {
    return GWT.create(SimpleRequestFactory.class);
  }

  /**
   * Create a new {@link RequestTransport}.
   */
  protected RequestTransport createTransport() {
    return new DefaultRequestTransport();
  }

  @Override
  protected void gwtSetUp() throws Exception {
    factory = createFactory();
    transport = new RecordingRequestTransport(createTransport());
    factory.initialize(new SimpleEventBus(), transport);
  }
}
