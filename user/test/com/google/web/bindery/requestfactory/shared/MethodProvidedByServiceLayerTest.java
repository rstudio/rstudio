/*
 * Copyright 2014 Google Inc.
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
package com.google.web.bindery.requestfactory.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Tests advanced usage of RequestFactory where a ServiceLayerDecorator
 * provides a service method at runtime, skipping interface validation at
 * compile-time.
 */
public class MethodProvidedByServiceLayerTest extends GWTTestCase {

  /** The factory under test. */
  public interface Factory extends RequestFactory {
    Context context();
  }

  /**
   * RequestContext whose actual server-side methods will be provided
   * dynamically at runtime by a ServiceLayerDecorator.
   * <p>
   * Note: the {@link SkipInterfaceValidation} is put on each method to test
   * that it's actually looked up at that location (it was searched on the
   * RequestContext only at some point).
   */
  @Service(ServiceImpl.class)
  public interface Context extends RequestContext {
    @SkipInterfaceValidation
    Request<String> missingDomainMethod(String string);

    // mapped to SimpleFoo#echo(SimpleFoo)
    @SkipInterfaceValidation
    Request<Proxy> missingDomainType(Proxy proxy);

    // mapped to SimpleFoo#persistAndReturnSelf
    @SkipInterfaceValidation
    InstanceRequest<Proxy, Proxy> missingDomainTypeInstanceMethod();
  }

  /** Proxy for an inexistent domain class; mapped at runtime to SimpleFoo. */
  @SkipInterfaceValidation
  @ProxyForName("does.not.exist")
  public interface Proxy extends EntityProxy {
  }

  /**
   * Dummy service for interface validation.
   * <p>
   * All actual service methods are provided at runtime by
   * MethodProvidedByServiceLayerJreTest.Decorator.
   */
  public static class ServiceImpl {
  }

  private static final int TEST_DELAY = 5000;

  private Factory factory;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  protected Factory createFactory() {
    Factory toReturn = GWT.create(Factory.class);
    toReturn.initialize(new SimpleEventBus());
    return toReturn;
  }

  public void testMissingDomainMethod() {
    delayTestFinish(TEST_DELAY);
    Context ctx = context();
    ctx.missingDomainMethod("foo").fire(new Receiver<String>() {
      @Override
      public void onSuccess(String response) {
        assertEquals("foo", response);
        finishTest();
      }
    });
  }

  public void testMissingDomainType() {
    delayTestFinish(TEST_DELAY);
    Context ctx = context();
    final Proxy proxy = ctx.create(Proxy.class);
    ctx.missingDomainType(proxy).fire(new Receiver<Proxy>() {
      @Override
      public void onSuccess(Proxy response) {
        // we only check that the call succeeds
        finishTest();
      }
    });
  }

  public void testMissingDomainTypeInstanceMethod() {
    delayTestFinish(TEST_DELAY);
    Context ctx = context();
    final Proxy proxy = ctx.create(Proxy.class);
    ctx.missingDomainTypeInstanceMethod().using(proxy).fire(new Receiver<Proxy>() {
      @Override
      public void onSuccess(Proxy response) {
        // we only check that the call succeeds
        finishTest();
      }
    });
  }

  @Override
  protected void gwtSetUp() {
    factory = createFactory();
  }

  private Context context() {
    return factory.context();
  }
}
