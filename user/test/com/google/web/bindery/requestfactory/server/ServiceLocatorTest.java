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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.requestfactory.shared.ServiceLocator;

import junit.framework.TestCase;

/**
 * Tests creating of ServiceLocators with custom ServiceLayerDecorators.
 */
public class ServiceLocatorTest extends TestCase {

  static class CustomLocatorLayer extends ServiceLayerDecorator {
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ServiceLocator> T createServiceLocator(Class<T> clazz) {
      return (T) ServiceLocatorImpl.INSTANCE;
    }
  }

  static class ServiceLocatorImpl implements ServiceLocator {
    static final ServiceLocatorImpl INSTANCE = new ServiceLocatorImpl();

    public Object getInstance(Class<?> clazz) {
      return new Object();
    }
  }

  public void testGetsServiceLocatorFromDecorator() {
    ServiceLayer layer = ServiceLayer.create(new CustomLocatorLayer());
    ServiceLocatorImpl locator = layer.createServiceLocator(ServiceLocatorImpl.class);
    assertSame(ServiceLocatorImpl.INSTANCE, locator);
  }

  public void testInstantiatesServiceLocatorByDefault() {
    ServiceLayer layer = ServiceLayer.create();
    ServiceLocatorImpl locator = layer.createServiceLocator(ServiceLocatorImpl.class);
    assertNotNull(locator);
    assertNotSame(ServiceLocatorImpl.INSTANCE, locator);
  }
}
