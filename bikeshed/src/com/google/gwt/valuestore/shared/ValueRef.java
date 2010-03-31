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
package com.google.gwt.valuestore.shared;

/**
 * A pointer to a value.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class ValueRef<K extends ValuesKey<K>, V> {
  private final Values<K> values;
  private final Property<K, V> property;

  public ValueRef(Values<K> values, Property<K, V> property) {
    assert null != values;
    assert null != property;

    this.values = values;
    this.property = property;
  }

  public V get() {
    return values.get(property);
  }

  Property<K, V> getProperty() {
    return property;
  }
}
