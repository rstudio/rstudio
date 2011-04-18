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
package com.google.web.bindery.requestfactory.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Contains a set of checks of how primitive and boxed method declarations
 * interact.
 */
public class BoxesAndPrimitivesTest extends GWTTestCase {

  /**
   * The domain type.
   */
  protected static class Entity {
    static final Entity SINGLETON = new Entity();

    public static Entity findEntity(int id) {
      return SINGLETON;
    }

    public Integer getBoxed() {
      return EXPECTED_BOXED;
    }

    public int getId() {
      return 0;
    }

    public int getPrimitive() {
      return EXPECTED;
    }

    public int getVersion() {
      return 0;
    }

    public boolean hasHas() {
      return EXPECTED_BOOL;
    }

    public Boolean hasHasBoxed() {
      return EXPECTED_BOOL_BOXED;
    }

    public boolean isIs() {
      return EXPECTED_BOOL;
    }

    public Boolean isIsBoxed() {
      return EXPECTED_BOOL_BOXED;
    }

    public void setBoxed(Integer value) {
      assertEquals(EXPECTED_BOXED, value);
    }

    public void setPrimitive(int value) {
      assertEquals(EXPECTED, value);
    }
  }

  /**
   * The RequestFactory.
   */
  protected interface Factory extends RequestFactory {
    Context context();
  }

  /**
   * The service method implementations.
   */
  protected static class ServiceImpl {
    public static void checkBoxed(Integer value) {
      assertEquals(EXPECTED_BOXED, value);
    }

    public static void checkPrimitive(int value) {
      assertEquals(EXPECTED, value);
    }

    public static Integer getBoxed() {
      return EXPECTED_BOXED;
    }

    public static Entity getEntity() {
      return Entity.SINGLETON;
    }

    public static int getPrimitive() {
      return EXPECTED;
    }
  }

  @Service(ServiceImpl.class)
  interface Context extends RequestContext {
    Request<Void> checkBoxed(Integer value);

    Request<Void> checkPrimitive(int value);

    Request<Integer> getBoxed();

    Request<Proxy> getEntity();

    Request<Integer> getPrimitive();
  }

  @ProxyFor(Entity.class)
  interface Proxy extends EntityProxy {
    Integer getBoxed();

    int getPrimitive();

    boolean hasHas();

    Boolean hasHasBoxed();

    boolean isIs();

    Boolean isIsBoxed();

    void setBoxed(Integer value);

    void setPrimitive(int value);
  }

  static abstract class TestReceiver<T> extends Receiver<T> {
    @Override
    public void onFailure(ServerFailure error) {
      fail(error.getMessage());
    }
  }

  private static final int EXPECTED = 42;
  private static final Integer EXPECTED_BOXED = Integer.valueOf(EXPECTED);
  private static final boolean EXPECTED_BOOL = true;
  private static final Boolean EXPECTED_BOOL_BOXED = Boolean.TRUE;
  private static final int TEST_DELAY = 5000;

  private Factory factory;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  /**
   * Tests that domain service methods that return a primitive type are upcast
   * to the boxed type that the generic declaration requires. Also checks that
   * primitive and boxed property types can be retrieved and that boxed and
   * primitive method arguments work.
   */
  public void testReturnAndParamTypes() {
    delayTestFinish(TEST_DELAY);
    Context ctx = context();
    // Boxed service method
    ctx.getBoxed().to(new TestReceiver<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        assertEquals(EXPECTED_BOXED, response);
      }
    });
    // Primitive service method
    ctx.getPrimitive().to(new TestReceiver<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        assertEquals(EXPECTED_BOXED, response);
      }
    });
    // Boxed and primitive properties
    ctx.getEntity().to(new TestReceiver<Proxy>() {
      @Override
      public void onSuccess(Proxy response) {
        assertEquals(EXPECTED_BOXED, response.getBoxed());
        assertEquals(EXPECTED, response.getPrimitive());
        assertEquals(EXPECTED_BOOL, response.isIs());
        assertEquals(EXPECTED_BOOL_BOXED, response.isIsBoxed());
        assertEquals(EXPECTED_BOOL, response.hasHas());
        assertEquals(EXPECTED_BOOL_BOXED, response.hasHasBoxed());
      }
    });
    // Boxed service argument
    ctx.checkBoxed(EXPECTED_BOXED).to(new TestReceiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        // OK
      }
    });
    // Primitive service argument
    ctx.checkPrimitive(EXPECTED).to(new TestReceiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        // OK
      }
    });
    ctx.fire(new TestReceiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        finishTest();
      }
    });
  }

  protected Factory createFactory() {
    Factory toReturn = GWT.create(Factory.class);
    toReturn.initialize(new SimpleEventBus());
    return toReturn;
  }

  @Override
  protected void gwtSetUp() {
    factory = createFactory();
  }

  private Context context() {
    return factory.context();
  }
}
