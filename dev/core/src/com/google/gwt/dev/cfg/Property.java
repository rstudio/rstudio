/*
 * Copyright 2006 Google Inc.
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
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single named deferred binding property that can answer with its
 * value.
 */
public class Property implements Comparable {

  private String activeValue;

  private Set knownValues = new HashSet();

  private String[] knownValuesLazyArray;

  private final String name;

  private PropertyProvider provider;

  public Property(String name) {
    this.name = name;
  }

  public void addKnownValue(String knownValue) {
    knownValues.add(knownValue);
    knownValuesLazyArray = null;
  }

  public int compareTo(Object other) {
    return name.compareTo(((Property) other).name);
  }

  /**
   * Gets the property value or <code>null</code> if the property has no
   * value.
   */
  public String getActiveValue() {
    return activeValue;
  }

  /**
   * Lists all the known values for this property in sorted order.
   */
  public String[] getKnownValues() {
    if (knownValuesLazyArray == null) {
      int n = knownValues.size();
      knownValuesLazyArray = (String[]) knownValues.toArray(new String[n]);
      Arrays.sort(knownValuesLazyArray);
    }
    return knownValuesLazyArray;
  }

  public String getName() {
    return name;
  }

  public PropertyProvider getProvider() {
    return provider;
  }

  public boolean isKnownValue(String value) {
    return knownValues.contains(value);
  }

  /**
   * Sets the property value, or clears it by specifying <code>null</code>.
   */
  public void setActiveValue(String value) {
    this.activeValue = value;
  }

  public void setProvider(PropertyProvider provider) {
    this.provider = provider;
  }

  public String toString() {
    return name;
  }
}
