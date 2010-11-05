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

import com.google.gwt.autobean.server.impl.TypeUtils;
import com.google.gwt.autobean.shared.AutoBeanVisitor.CollectionPropertyContext;
import com.google.gwt.autobean.shared.AutoBeanVisitor.MapPropertyContext;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * A base type to handle analyzing the return value of a getter method. The
 * accessor methods are implemented in subtypes.
 */
abstract class MethodPropertyContext implements CollectionPropertyContext,
    MapPropertyContext {
  private final Class<?> keyType;
  private final Class<?> valueType;
  private final Class<?> elementType;
  private final Class<?> type;

  public MethodPropertyContext(Method getter) {
    this.type = getter.getReturnType();

    // Compute collection element type
    if (Collection.class.isAssignableFrom(getType())) {
      elementType = TypeUtils.ensureBaseType(TypeUtils.getSingleParameterization(
          Collection.class, getter.getGenericReturnType(),
          getter.getReturnType()));
      keyType = valueType = null;
    } else if (Map.class.isAssignableFrom(getType())) {
      Type[] types = TypeUtils.getParameterization(Map.class,
          getter.getGenericReturnType());
      keyType = TypeUtils.ensureBaseType(types[0]);
      valueType = TypeUtils.ensureBaseType(types[1]);
      elementType = null;
    } else {
      elementType = keyType = valueType = null;
    }
  }

  public abstract boolean canSet();

  public Class<?> getElementType() {
    return elementType;
  }

  public Class<?> getKeyType() {
    return keyType;
  }

  public Class<?> getType() {
    return type;
  }

  public Class<?> getValueType() {
    return valueType;
  }

  public abstract void set(Object value);
}
