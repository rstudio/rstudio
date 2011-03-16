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
package com.google.gwt.requestfactory.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the ability of instance services to inherit methods
 * from a base class.
 */
public class ServiceInheritanceTest extends GWTTestCase {

  /**
   * Generic locator returns an instance of the class named in 
   * the @{@link Service} annotation
   */
  public static class AnyServiceLocator implements ServiceLocator {

    @Override
    public Object getInstance(Class<?> clazz) {
      assertTrue(BaseImpl.class.isAssignableFrom(clazz));
      try {
        return clazz.newInstance();
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * The factory under test.
   */
  protected interface Factory extends RequestFactory {
    SumService sumContext();
    SumServiceBase sumBaseContext();
  }

  /**
   * Specifies a service which extends a base class
   */
  @Service(value = SubclassImpl.class, locator = AnyServiceLocator.class)
  interface SumService extends RequestContext {
    Request<Integer> add(int n);
  }

  /**
   * Specifies a service which is a base class
   */
  @Service(value = BaseImpl.class, locator = AnyServiceLocator.class)
  interface SumServiceBase extends RequestContext {
    Request<Integer> add(int n);
  }

  static class BaseImpl {
    protected int base;

    public BaseImpl() {
      base = 5;
    }

    public Integer add(int n) {
      return base + n;
    }
  }

  static class SubclassImpl extends BaseImpl {
    public SubclassImpl() {
      // Distinguish from base service
      base = 8;
    }
  }

  private static final int TEST_DELAY = 5000;

  private Factory factory;

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  /**
   * Verify that the method can be invoked on the base class
   * as well as the subclass
   */
  public void testInvokeMethodOnBaseClass() {
    delayTestFinish(TEST_DELAY);
    factory.sumBaseContext().add(13).fire(new Receiver<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        assertEquals((Integer) 18, response);
        finishTest();
      }
    });
  }

  /**
   * Verify that the method is invoked on the subclass,
   * not the base class
   */
  public void testInvokeMethodOnSubclass() {
    delayTestFinish(TEST_DELAY);
    factory.sumContext().add(13).fire(new Receiver<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        assertEquals((Integer) 21, response);
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
  protected void gwtSetUp() throws Exception {
    factory = createFactory();
  }

}
