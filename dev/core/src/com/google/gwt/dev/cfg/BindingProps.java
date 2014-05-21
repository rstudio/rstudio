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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.DefaultSelectionProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;

import java.io.Serializable;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * Contains the values of binding properties for a soft permutation.
 * (Or a hard permutation if soft permutation isn't turned on.)
 *
 * <p>If soft permutations aren't turned on, these are also the properties
 * for the enclosing hard permutation.
 */
public class BindingProps implements Serializable {

  private final ConfigProps configProps;

  private final BindingProperty[] orderedProps;

  private final String[] orderedPropValues;

  /**
   * Create a property oracle that will return the supplied values.
   *
   * @param orderedProps array of binding properties in dependency order
   * @param orderedPropValues a parallel array containing the property values
   */
  public BindingProps(BindingProperty[] orderedProps, String[] orderedPropValues,
      ConfigProps configProps) {
    this.orderedProps = orderedProps;
    this.orderedPropValues = orderedPropValues;
    this.configProps = configProps;

    assert orderedProps.length == orderedPropValues.length;

    // Reject illegal values at construction time
    int len = orderedProps.length;
    for (int i = 0; i < len; i++) {
      BindingProperty prop = orderedProps[i];
      String value = orderedPropValues[i];
      if (!prop.isAllowedValue(value)) {
        throw new IllegalArgumentException("Property " + prop.getName()
            + " cannot have value " + value);
      }
    }
  }

  /**
   * Returns the value of a property as a boolean. If it doesn't exist, returns the default value.
   */
  public boolean getBoolean(String key, boolean defaultValue) {
    String value = getString(key, null);
    if (value == null) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value);
  }

  /**
   * Returns the value of a property as a string. If it doesn't exist, returns the default value.
   */
  public String getString(String key, String defaultValue) {
    for (int i = 0; i < orderedProps.length; i++) {
      if (orderedProps[i].getName().equals(key)) {
        return orderedPropValues[i];
      }
    }
    return defaultValue;
  }

  /**
   * Returns the configuration properties for this compile.
   * (They are the same in every permutation.)
   */
  public ConfigProps getConfigProps() {
    return configProps;
  }

  /**
   * Returns all binding properties in dependency order.
   * (Earlier properties may not depend on later properties.)
   */
  public BindingProperty[] getOrderedProps() {
    return orderedProps;
  }

  /**
   * Returns the value of each binding property, in the same order as {@link #getOrderedProps}.
   */
  public String[] getOrderedPropValues() {
    return orderedPropValues;
  }

  /**
   * Returns a view of the properties as a PropertyOracle.
   */
  public PropertyOracle toPropertyOracle() {
    return new SoftPropsOracle();
  }

  /**
   * Prints the properties as a single string, for logging and soyc.
   */
  public String prettyPrint() {
    StringBuilder out = new StringBuilder();
    for (BindingProperty prop : getOrderedProps()) {
      if (out.length() > 0) {
        out.append(",");
      }
      String name = prop.getName();
      out.append(name);
      out.append("=");
      out.append(getString(name, "(missing)"));
    }
    return out.toString();
  }

  /**
   * Dumps the binding property key/value pairs; For debugging use only.
   */
  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    out.append("SoftProps(");
    for (int i = 0, j = orderedProps.length; i < j; i++) {
      if (out.length() > 0) {
        out.append(" ");
      }
      out.append(orderedProps[i].getName());
      out.append(" = ");
      out.append(orderedPropValues[i]);
    }
    out.append(")");
    return out.toString();
  }

  boolean hasSameBindingProperties(BindingProps other) {
    if (orderedProps.length != other.orderedProps.length) {
      return false;
    }
    for (int i = 0; i < orderedProps.length; i++) {
      if (orderedProps[i] != other.orderedProps[i]) {
        return false;
      }
    }
    return true;
  }

  private class SoftPropsOracle implements PropertyOracle {

    @Override
    public com.google.gwt.core.ext.ConfigurationProperty getConfigurationProperty(
        String propertyName)
        throws BadPropertyValueException {
      return configProps.getConfigurationProperty(propertyName);
    }

    @Override
    public SelectionProperty getSelectionProperty(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {

      // In practice there will probably be so few properties that a linear
      // search is at least as fast as a map lookup by name would be.
      // If that turns out not to be the case, the ctor could build a
      // name-to-index map.
      for (int i = 0; i < orderedProps.length; i++) {
        final BindingProperty prop = orderedProps[i];
        final String name = prop.getName();
        if (name.equals(propertyName)) {
          String value = orderedPropValues[i];
          TreeSet<String> possibleValues =
              new TreeSet<String>(Arrays.asList(prop.getDefinedValues()));
          return new DefaultSelectionProperty(value, prop.getFallback(), name,
              possibleValues, prop.getFallbackValuesMap());
        }
      }

      throw new BadPropertyValueException(propertyName);
    }
  }
}
