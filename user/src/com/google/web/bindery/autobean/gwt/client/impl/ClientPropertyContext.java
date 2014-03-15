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
package com.google.web.bindery.autobean.gwt.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor.CollectionPropertyContext;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor.MapPropertyContext;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor.ParameterizationVisitor;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor.PropertyContext;
import com.google.web.bindery.autobean.shared.impl.AbstractAutoBean;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides base methods for generated implementations of PropertyContext.
 */
public final class ClientPropertyContext implements PropertyContext, CollectionPropertyContext,
    MapPropertyContext {

  /**
   * A reference to an instance setter method.
   */
  public static final class Setter extends JavaScriptObject {
    /**
     * Create a trivial Setter that calls {@link AbstractAutoBean#setProperty()}
     * .
     */
    public static native Setter beanSetter(AbstractAutoBean<?> bean, String key) /*-{
      return function(value) {
        bean.@com.google.web.bindery.autobean.shared.impl.AbstractAutoBean::setProperty(*)(key, value);
      };
    }-*/;

    protected Setter() {
    }

    public native void call(Object instance, Object value) /*-{
      this.call(instance, value);
    }-*/;
  }

  private final Object instance;
  private final int[] paramCounts;
  private final Class<?>[] paramTypes;
  private final Setter setter;
  private final Class<?> simpleType;

  public ClientPropertyContext(Object instance, Setter setter, Class<?> type) {
    this.instance = instance;
    this.setter = setter;
    this.simpleType = type;
    this.paramTypes = null;
    this.paramCounts = null;
  }

  public ClientPropertyContext(Object instance, Setter setter, Class<?>[] types, int[] paramCounts) {
    this.instance = instance;
    this.setter = setter;
    this.simpleType = null;
    this.paramTypes = types;
    this.paramCounts = paramCounts;

    /*
     * Verify input arrays of same length and that the total parameter count,
     * plus one for the root type, equals the total number of types passed in.
     */
    if (ClientPropertyContext.class.desiredAssertionStatus()) {
      assert types.length == paramCounts.length : "Length mismatch " + types.length + " != "
          + paramCounts.length;
      int count = 1;
      for (int i = 0, j = paramCounts.length; i < j; i++) {
        count += paramCounts[i];
      }
      assert count == types.length : "Mismatch in total parameter count " + count + " != "
          + types.length;
    }
  }

  public void accept(ParameterizationVisitor visitor) {
    traverse(visitor, 0);
  }

  public boolean canSet() {
    return setter != null;
  }

  public Class<?> getElementType() {
    if (paramTypes == null || paramTypes.length < 2) {
      return null;
    }
    if (List.class.equals(paramTypes[0]) || Set.class.equals(paramTypes[0])) {
      return paramTypes[1];
    }
    return null;
  }

  public Class<?> getKeyType() {
    if (paramTypes == null || paramTypes.length < 3) {
      return null;
    }
    if (Map.class.equals(paramTypes[0])) {
      return paramTypes[1];
    }
    return null;
  }

  public Class<?> getType() {
    return simpleType == null ? paramTypes[0] : simpleType;
  }

  public Class<?> getValueType() {
    if (paramTypes == null || paramTypes.length < 3) {
      return null;
    }
    if (Map.class.equals(paramTypes[0])) {
      return paramTypes[2];
    }
    return null;
  }

  public void set(Object value) {
    setter.call(instance, value);
  }

  private int traverse(ParameterizationVisitor visitor, int count) {
    if (simpleType != null) {
      visitor.visitType(simpleType);
      visitor.endVisitType(simpleType);
      return 0;
    }

    Class<?> type = paramTypes[count];
    int paramCount = paramCounts[count];
    ++count;
    if (visitor.visitType(type)) {
      for (int i = 0; i < paramCount; i++) {
        if (visitor.visitParameter()) {
          count = traverse(visitor, count);
        }
        visitor.endVisitParameter();
      }
    }
    visitor.endVisitType(type);
    return count;
  }
}
