/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.web.bindery.requestfactory.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.SkipInterfaceValidation;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

/**
 * Tests RequestFactory when using proxies for interfaces.
 * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=5762
 */
@SkipInterfaceValidation
public class ProxyForInterfacesTest extends GWTTestCase {

  /**
   * The Factory.
   */
  protected interface Factory extends RequestFactory {
    Context ctx();
  }

  static class C0 implements I1 {
    public String getName() {
      return "C0";
    }
  }

  static class C1 implements I2 {
    public String getName() {
      return "C1";
    }
  }

  static class C2 extends C1 {
    public String getName() {
      return "C2";
    }
  }

  @Service(ContextImpl.class)
  interface Context extends RequestContext {
    Request<I1> getC0();

    Request<I1> getC1_RetI1();

    Request<I2> getC1_RetI2();

    Request<I1> getC2_RetI1();

    Request<I2> getC2_RetI2();
  }

  static class ContextImpl {
    public static I1 getC0() {
      return new C0();
    }

    public static I1 getC1_RetI1() {
      return new C1();
    }

    public static I2 getC1_RetI2() {
      return new C1();
    }

    public static I1 getC2_RetI1() {
      return new C2();
    }

    public static I2 getC2_RetI2() {
      return new C2();
    }
  }

  @ProxyFor(I1.class)
  interface I1 extends ValueProxy {
    String getName();
  }

  @ProxyFor(I2.class)
  interface I2 extends I1, ValueProxy {
  }

  class ReceiverAssert<T extends I1> extends Receiver<T> {
    String name;

    public ReceiverAssert(String name) {
      this.name = name;
    }

    @Override
    public void onSuccess(T response) {
      assertEquals(name, response.getName());
    }
  }

  private static final int TEST_DELAY = 5000;

  protected Factory factory;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  public void testProxyForInterfaces() {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    ctx.getC0().to(new ReceiverAssert<I1>("C0"));
    ctx.getC1_RetI1().to(new ReceiverAssert<I1>("C1"));
    ctx.getC2_RetI1().to(new ReceiverAssert<I1>("C2"));
    ctx.getC1_RetI2().to(new ReceiverAssert<I2>("C1"));
    ctx.getC2_RetI2().to(new ReceiverAssert<I2>("C2"));
    ctx.fire(new Receiver<Void>() {
      public void onSuccess(Void response) {
        finishTest();
      }
    });
  }

  protected Factory createFactory() {
    Factory f = GWT.create(Factory.class);
    f.initialize(new SimpleEventBus());
    return f;
  }

  @Override
  protected void gwtSetUp() throws Exception {
    factory = createFactory();
  }
}
