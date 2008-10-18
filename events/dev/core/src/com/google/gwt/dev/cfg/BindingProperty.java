/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.cfg;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a single named deferred binding or configuration property that can
 * answer with its value. The BindingProperty maintains two sets of values, the
 * "defined" set and the "allowed" set. The allowed set must always be a subset
 * of the defined set.
 */
public class BindingProperty extends Property {

  private SortedSet<String> allowedValues;
  private final SortedSet<String> definedValues = new TreeSet<String>();
  private PropertyProvider provider;

  {
    /*
     * This is initially an alias for definedValues and is only set differently
     * if the user calls setAllowedValues().
     */
    allowedValues = definedValues;
  }

  public BindingProperty(String name) {
    super(name);
  }

  public void addDefinedValue(String newValue) {
    definedValues.add(newValue);
  }

  public String[] getAllowedValues() {
    return allowedValues.toArray(new String[allowedValues.size()]);
  }

  public String[] getDefinedValues() {
    return definedValues.toArray(new String[definedValues.size()]);
  }

  public PropertyProvider getProvider() {
    return provider;
  }

  public boolean isAllowedValue(String value) {
    return allowedValues.contains(value);
  }

  /**
   * Returns <code>true</code> if the value was previously provided to
   * {@link #addDefinedValue(String)} since the last time {@link #clearValues()}
   * was called.
   */
  public boolean isDefinedValue(String value) {
    return definedValues.contains(value);
  }

  /**
   * Set the currently allowed values. The values provided must be a subset of
   * the currently-defined values.
   * 
   * @throws IllegalArgumentException if any of the provided values were not
   *           provided to {@link #addDefinedValue(String)}.
   */
  public void setAllowedValues(String... values) {
    SortedSet<String> temp = new TreeSet<String>(Arrays.asList(values));
    if (!definedValues.containsAll(temp)) {
      throw new IllegalArgumentException(
          "Attempted to set an allowed value that was not previously defined");
    }

    allowedValues = temp;
  }

  public void setProvider(PropertyProvider provider) {
    this.provider = provider;
  }
}
