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
package com.google.web.bindery.requestfactory.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Contains a set of checks of using multiple request factories simultaneously.
 */
public class MultipleFactoriesTest extends GWTTestCase {

  /**
   * The domain type.
   */
  protected static class Entity {
    static final Entity SINGLETON = new Entity();

    public String getString1() {
      return EXPECTED_STRING_1;
    }

    public String getString2() {
      return EXPECTED_STRING_2;
    }
  }

  /**
   * The first RequestFactory.
   */
  protected interface Factory1 extends RequestFactory {
    Context1 context();
  }

  /**
   * The second RequestFactory.
   */
  protected interface Factory2 extends RequestFactory {
    Context2 context();
  }

  /**
   * The service method implementations.
   */
  protected static class ServiceImpl {
    public static Entity getEntity() {
      return Entity.SINGLETON;
    }
  }

  @Service(ServiceImpl.class)
  interface Context1 extends RequestContext {
    Request<Proxy1> getEntity();
  }

  @Service(ServiceImpl.class)
  interface Context2 extends RequestContext {
    Request<Proxy2> getEntity();
  }

  @ProxyFor(Entity.class)
  interface Proxy1 extends ValueProxy {
    String getString1();
  }
  @ProxyFor(Entity.class)
  interface Proxy2 extends ValueProxy {
    String getString2();
  }

  static abstract class TestReceiver<T> extends Receiver<T> {
    @Override
    public void onFailure(ServerFailure error) {
      fail(error.getMessage());
    }
  }

  private static final String EXPECTED_STRING_1 = "hello world 1";
  private static final String EXPECTED_STRING_2 = "hello world 2";
  private static final int TEST_DELAY = 5000;

  private Factory1 factory1;
  private Factory2 factory2;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  /**
   * Tests that the 2 calls with 2 RequestFactory with 2 differents Proxy on the same domain class
   * succeed.
   */
  public void test() {
    delayTestFinish(TEST_DELAY);
    context1().getEntity().fire(new TestReceiver<Proxy1>() {
      @Override
      public void onSuccess(Proxy1 response) {
        assertEquals(EXPECTED_STRING_1, response.getString1());

        // test 2
        context2().getEntity().to(new TestReceiver<Proxy2>() {
          @Override
          public void onSuccess(Proxy2 response) {
            assertEquals(EXPECTED_STRING_2, response.getString2());
          }
        }).fire(new TestReceiver<Void>() {
          @Override
          public void onSuccess(Void response) {
            finishTest();
          }
        });
      }
    });
  }

  protected Factory1 createFactory1() {
    Factory1 toReturn = GWT.create(Factory1.class);
    toReturn.initialize(new SimpleEventBus());
    return toReturn;
  }

  protected Factory2 createFactory2() {
    Factory2 toReturn = GWT.create(Factory2.class);
    toReturn.initialize(new SimpleEventBus());
    return toReturn;
  }

  @Override
  protected void gwtSetUp() {
    factory1 = createFactory1();
    factory2 = createFactory2();
  }

  private Context1 context1() {
    return factory1.context();
  }

  private Context2 context2() {
    return factory2.context();
  }
}
