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
 * Represents a property of a type managed by {@link ValueStore}.
 * 
 * @param <T> type of the property holder
 * @param <V> type of the property
 */
public class Property<T, V> {
  // TODO Is this actually useful as a class, or do we need
  // the EntityKey instance? Either way, need a getter for it.
  // Should become clear as ValueStore is implemented
  private final Class<V> valueType;
  private final String name;

  public Property(Class<V> valueType, String name) {
    this.valueType = valueType;
    this.name = name;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the valueClass
   */
  public Class<V> getValueType() {
    return valueType;
  }
}
