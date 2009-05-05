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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a module property which does not impact deferred-binding
 * decisions.
 */
public class ConfigurationProperty extends Property {
  private final boolean allowMultipleValues;
  private List<String> values = new ArrayList<String>();

  public ConfigurationProperty(String name) {
    this(name, false);
  }
  
  public ConfigurationProperty(String name, boolean allowMultipleValues) {
    super(name);
    this.allowMultipleValues = allowMultipleValues;
    if (!allowMultipleValues) {
      values.add(null); // single-valued properties default to null
    }
  }
  
  public void addValue(String value) {
    if (!allowMultipleValues) {
      throw new IllegalStateException(
          "Attempt to add a value to a single-valued ConfigurationProperty");
    }
    values.add(value);
  }

  public boolean allowsMultipleValues() {
    return allowMultipleValues;
  }
  
  public void clear() {
    values.clear();
  }

  public String getValue() {
    if (values.size() != 1) {
      throw new IllegalStateException("size != 1");
    }
    return values.get(0);
  }
  
  public List<String> getValues() {
    ArrayList<String> v = new ArrayList<String>(values.size());
    v.addAll(values);
    return v;
  }
  
  public boolean isMultiValued() {
    return values.size() > 1;
  }

  public void setValue(String value) {
    if (values.size() == 0) {
      values.add(value);
    } else {
      values.set(0, value);
    }
  }
  
  public void setValues(List<String> values) {
    if (!allowMultipleValues && values.size() > 1) {
      throw new IllegalStateException(
      "Attempt to set > 1 value for a single-valued ConfigurationProperty");
    }
    this.values.clear();
    this.values.addAll(values);
  }
}
