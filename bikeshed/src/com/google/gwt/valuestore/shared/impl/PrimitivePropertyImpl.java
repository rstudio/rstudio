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
package com.google.gwt.valuestore.shared.impl;

import com.google.gwt.valuestore.shared.PrimitiveProperty;
import com.google.gwt.valuestore.shared.ValuesKey;

/**
 * Represents a "primitive" property of a record managed by {@link ValueStore}.
 * Primitives include {@link java.lang.Number} and its subclasses,
 * {@link java.lang.String}, {@link java.lang.Date} and enums (tbd).
 * 
 * @param <K> type of the property holder
 * @param <V> type of the property
 */
public class PrimitivePropertyImpl<K extends ValuesKey<K>, V> extends
    PropertyImpl<K, V> implements PrimitiveProperty<K, V> {
  private final Class<V> valueType;

  public PrimitivePropertyImpl(Class<V> valueType, String name) {
    super(name);
    this.valueType = valueType;
  }

  /**
   * @return the valueClass
   */
  public Class<V> getValueType() {
    return valueType;
  }
}
