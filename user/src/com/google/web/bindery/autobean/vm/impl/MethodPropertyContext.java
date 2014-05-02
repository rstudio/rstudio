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

import com.google.web.bindery.autobean.shared.AutoBeanVisitor.CollectionPropertyContext;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor.MapPropertyContext;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor.ParameterizationVisitor;

import java.lang.reflect.Type;

/**
 * A base type to handle analyzing the return value of a getter method. The
 * accessor methods are implemented in subtypes.
 */
abstract class MethodPropertyContext implements CollectionPropertyContext, MapPropertyContext {
  private final Class<?> elementType;
  private final Type genericType;
  private final Class<?> keyType;
  private final Class<?> valueType;
  private final Class<?> type;

  protected MethodPropertyContext(Type genericType, Class<?> type, Class<?> elementType,
      Class<?> keyType, Class<?> valueType) {
    this.genericType = genericType;
    this.type = type;
    this.elementType = elementType;
    this.keyType = keyType;
    this.valueType = valueType;
  }

  public void accept(ParameterizationVisitor visitor) {
    traverse(visitor, genericType);
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

  private void traverse(ParameterizationVisitor visitor, Type type) {
    Class<?> base = TypeUtils.ensureBaseType(type);
    if (visitor.visitType(base)) {
      Type[] params = TypeUtils.getParameterization(base, type);
      for (Type t : params) {
        if (visitor.visitParameter()) {
          traverse(visitor, t);
        }
        visitor.endVisitParameter();
      }
    }
    visitor.endVisitType(base);
  }
}
