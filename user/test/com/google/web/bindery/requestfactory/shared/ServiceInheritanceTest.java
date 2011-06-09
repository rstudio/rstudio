/*
 * Copyright 2011 Google Inc.
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
 * Tests the ability of instance services to inherit methods from a base class.
 */
public class ServiceInheritanceTest extends GWTTestCase {

  /**
   * ServiceLocator that returns the base class or subclass implementation
   * specified in the @{@link Service} annotation.
   */
  public static class SumServiceLocator implements ServiceLocator {
    public Object getInstance(Class<?> clazz) {
      if (BaseImpl.class.equals(clazz)) {
        return new BaseImpl();
      } else if (SubclassImpl.class.equals(clazz)) {
        return new SubclassImpl();
      }
      return null;
    }
  }

  /**
   * The factory under test.
   */
  protected interface Factory extends RequestFactory {
    SumServiceBase baseContext();

    SumServiceSub subContext();
  }

  /**
   * Demonstrate that mix-in interfaces work correctly.
   */
  interface HasAdd {
    Request<Integer> add(int n);
  }

  /**
   * Specifies the base class implementation.
   */
  @Service(value = BaseImpl.class, locator = SumServiceLocator.class)
  interface SumServiceBase extends RequestContext, HasAdd {
    Request<Integer> subtract(int n);
  }

  /**
   * Specifies the subclass implementation.
   */
  @Service(value = SubclassImpl.class, locator = SumServiceLocator.class)
  interface SumServiceSub extends SumServiceBase {
  }

  /**
   * Base implementation of {@link SumServiceBase}.
   */
  static class BaseImpl {
    protected int initialValue;

    public BaseImpl() {
      initialValue = 5;
    }

    public Integer add(int n) {
      return initialValue + n;
    }

    public Integer subtract(int n) {
      return initialValue - n;
    }
  }

  /**
   * Subclass implementation of {@link SumServiceSub} inherits the add() method.
   */
  static class SubclassImpl extends BaseImpl {
    public SubclassImpl() {
      /*
       * Init with a different value to distinguish between base & subclass
       * implementations in the tests
       */
      initialValue = 8;
    }

    @Override
    public Integer subtract(int n) {
      return 0;
    }
  }

  private static final int TEST_DELAY = 5000;

  private Factory factory;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  /**
   * Call a method inherited from a base class.
   */
  public void testInvokeInheritedMethod() {
    delayTestFinish(TEST_DELAY);
    factory.subContext().add(13).fire(new Receiver<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        assertEquals((Integer) 21, response);
        finishTest();
      }
    });
  }

  /**
   * Call a method implemented in a base class.
   */
  public void testInvokeMethodOnBaseClass() {
    delayTestFinish(TEST_DELAY);
    factory.baseContext().add(13).fire(new Receiver<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        assertEquals((Integer) 18, response);
        finishTest();
      }
    });
  }

  /**
   * Call a method overridden in a subclass.
   */
  public void testInvokeOverriddenMethod() {
    delayTestFinish(TEST_DELAY);
    factory.subContext().subtract(3).fire(new Receiver<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        assertEquals((Integer) 0, response);
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

}
