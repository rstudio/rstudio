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
package com.google.gwt.autobean.server.impl;

import com.google.gwt.autobean.shared.AutoBeanVisitor.CollectionPropertyContext;
import com.google.gwt.autobean.shared.AutoBeanVisitor.MapPropertyContext;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A base type to handle analyzing the return value of a getter method. The
 * accessor methods are implemented in subtypes.
 */
abstract class MethodPropertyContext implements CollectionPropertyContext,
    MapPropertyContext {
  private static class Data {
    Class<?> keyType;
    Class<?> valueType;
    Class<?> elementType;
    Class<?> type;
  }

  /**
   * Save prior instances in order to decrease the amount of data computed.
   */
  private static final Map<Method, Data> cache = new WeakHashMap<Method, Data>();

  private final Data data;

  public MethodPropertyContext(Method getter) {
    synchronized (cache) {
      Data previous = cache.get(getter);
      if (previous != null) {
        this.data = previous;
        return;
      }

      this.data = new Data();
      data.type = getter.getReturnType();
      // Compute collection element type
      if (Collection.class.isAssignableFrom(getType())) {
        data.elementType = TypeUtils.ensureBaseType(TypeUtils.getSingleParameterization(
            Collection.class, getter.getGenericReturnType(),
            getter.getReturnType()));
      } else if (Map.class.isAssignableFrom(getType())) {
        Type[] types = TypeUtils.getParameterization(Map.class,
            getter.getGenericReturnType());
        data.keyType = TypeUtils.ensureBaseType(types[0]);
        data.valueType = TypeUtils.ensureBaseType(types[1]);
      }
      cache.put(getter, data);
    }
  }

  public abstract boolean canSet();

  public Class<?> getElementType() {
    return data.elementType;
  }

  public Class<?> getKeyType() {
    return data.keyType;
  }

  public Class<?> getType() {
    return data.type;
  }

  public Class<?> getValueType() {
    return data.valueType;
  }

  public abstract void set(Object value);
}
