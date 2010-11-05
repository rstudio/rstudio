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
package com.google.gwt.autobean.server;

import com.google.gwt.autobean.shared.AutoBeanUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

/**
 * Handles dispatches on AutoBeanFactory interfaces.
 */
class FactoryHandler implements InvocationHandler {
  private final Configuration configuration;

  /**
   * Constructor.
   * 
   * @param categories the classes specified by a Category annotation
   */
  public FactoryHandler(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Handles both declared factory methods as well as the dynamic create
   * methods.
   */
  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {

    Class<?> beanType;
    Object toWrap = null;
    if (method.getName().equals("create")) {
      // Dynamic create. Guaranteed to have at least one argument
      // create(clazz); or create(clazz, toWrap);
      beanType = (Class<?>) args[0];
      if (args.length == 2) {
        toWrap = args[1];
      }
    } else {
      // Declared factory method, use the parameterization
      // AutoBean<Foo> foo(); or Autobean<foo> foo(Foo toWrap);
      ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
      beanType = (Class<?>) returnType.getActualTypeArguments()[0];

      if (args != null && args.length == 1) {
        toWrap = args[0];
      }
    }

    // Return any existing wrapper
    ProxyAutoBean<Object> toReturn = (ProxyAutoBean<Object>) AutoBeanUtils.getAutoBean(toWrap);
    if (toReturn == null) {
      // Create the implementation bean
      if (toWrap == null) {
        toReturn = new ProxyAutoBean<Object>(beanType, configuration);
      } else {
        toReturn = new ProxyAutoBean<Object>(beanType, configuration, toWrap);
      }
    }

    return toReturn;
  }
}