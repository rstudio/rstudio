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
package com.google.gwt.autobean.shared.impl;

import com.google.gwt.autobean.shared.AutoBeanVisitor.ParameterizationVisitor;
import com.google.gwt.autobean.shared.AutoBeanVisitor.PropertyContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides base methods for generated implementations of PropertyContext.
 */
public abstract class AbstractPropertyContext implements PropertyContext {

  private final Class<?>[] types;
  private final int[] paramCounts;

  protected AbstractPropertyContext(Class<?>[] types, int[] paramCounts) {
    this.types = types;
    this.paramCounts = paramCounts;
  }

  public void accept(ParameterizationVisitor visitor) {
    traverse(visitor, 0);
  }

  public boolean canSet() {
    return true;
  }

  /**
   * @see com.google.gwt.autobean.shared.AutoBeanVisitor.CollectionPropertyContext#getElementType()
   */
  public Class<?> getElementType() {
    assert types.length >= 2;
    assert List.class.equals(types[0]) || Set.class.equals(types[0]);
    return types[1];
  }

  /**
   * @see com.google.gwt.autobean.shared.AutoBeanVisitor.MapPropertyContext#getKeyType()
   */
  public Class<?> getKeyType() {
    assert types.length >= 2;
    assert Map.class.equals(types[0]);
    return types[1];
  }

  public Class<?> getType() {
    return types[0];
  }

  /**
   * @see com.google.gwt.autobean.shared.AutoBeanVisitor.MapPropertyContext#getValueType()
   */
  public Class<?> getValueType() {
    assert types.length >= 2;
    assert Map.class.equals(types[0]);
    return types[2];
  }

  private int traverse(ParameterizationVisitor visitor, int count) {
    Class<?> type = types[count];
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
