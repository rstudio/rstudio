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
package com.google.gwt.requestfactory.shared;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Defines a property of a {@link com.google.gwt.requestfactory.shared.EntityProxy}.
 *
 * @param <V> the type of the property's value
 */
public class EnumProperty<V> extends Property<V> {

  private V[] values;

  /**
   * @param name the property's name and displayName
   * @param type the class of the property's value
   * @param values the result of Enum.values() method
   */
  public EnumProperty(String name, Class<V> type, V[] values) {
    super(name, type);
    this.values = values;
  }

  /**
   * Returns the values that the enum may take on.
   */
  public V[] getValues() {
    return values;
  }
}
