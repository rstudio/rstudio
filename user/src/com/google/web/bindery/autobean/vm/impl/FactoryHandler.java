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
package com.google.web.bindery.autobean.vm.impl;

import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.vm.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

/**
 * Handles dispatches on AutoBeanFactory interfaces.
 */
public class FactoryHandler implements InvocationHandler {
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
    String name = method.getName();
    if (name.equals("create")) {
      // Dynamic create. Guaranteed to have at least one argument
      // create(clazz); or create(clazz, toWrap);
      beanType = (Class<?>) args[0];
      if (args.length == 2) {
        toWrap = args[1];
      }
    } else if (name.equals("getEnum")) {
      Class<?> clazz = (Class<?>) args[0];
      String token = (String) args[1];
      return getEnum(clazz, token);
    } else if (name.equals("getToken")) {
      Enum<?> e = (Enum<?>) args[0];
      return getToken(e);
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
        toReturn = new ProxyAutoBean<Object>((AutoBeanFactory) proxy, beanType,
            configuration);
      } else {
        toReturn = new ProxyAutoBean<Object>((AutoBeanFactory) proxy, beanType,
            configuration, toWrap);
      }
    }

    return toReturn;
  }

  /**
   * EnumMap support.
   */
  private Object getEnum(Class<?> clazz, String token)
      throws IllegalAccessException {
    for (Field f : clazz.getFields()) {
      String fieldName;
      PropertyName annotation = f.getAnnotation(PropertyName.class);
      if (annotation != null) {
        fieldName = annotation.value();
      } else {
        fieldName = f.getName();
      }
      if (token.equals(fieldName)) {
        f.setAccessible(true);
        return f.get(null);
      }
    }
    throw new IllegalArgumentException("Cannot find enum " + token
        + " in type " + clazz.getCanonicalName());
  }

  /**
   * EnumMap support.
   */
  private Object getToken(Enum<?> e) throws NoSuchFieldException {
    // Remember enum constants are fields
    PropertyName annotation = e.getDeclaringClass().getField(e.name()).getAnnotation(
        PropertyName.class);
    if (annotation != null) {
      return annotation.value();
    } else {
      return e.name();
    }
  }
}
