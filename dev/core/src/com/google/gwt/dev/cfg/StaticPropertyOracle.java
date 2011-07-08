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
import com.google.gwt.core.ext.DefaultConfigurationProperty;
import com.google.gwt.core.ext.DefaultSelectionProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * An implementation of {@link PropertyOracle} that contains property values,
 * rather than computing them.
 */
public class StaticPropertyOracle implements PropertyOracle, Serializable {

  private final ConfigurationProperty[] configProps;

  private final BindingProperty[] orderedProps;

  private final String[] orderedPropValues;

  /**
   * Create a property oracle that will return the supplied values.
   * 
   * @param orderedProps array of binding properties
   * @param orderedPropValues values of the above binding properties
   * @param configProps array of config properties
   */
  public StaticPropertyOracle(BindingProperty[] orderedProps,
      String[] orderedPropValues, ConfigurationProperty[] configProps) {
    this.orderedProps = orderedProps;
    this.orderedPropValues = orderedPropValues;
    this.configProps = configProps;
    
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

  public com.google.gwt.core.ext.ConfigurationProperty getConfigurationProperty(
      String propertyName) throws BadPropertyValueException {
    for (final ConfigurationProperty prop : configProps) {
      if (prop.getName().equals(propertyName)) {
        return new DefaultConfigurationProperty(prop.getName(),
            prop.getValues());
      }
    }
    throw new BadPropertyValueException(propertyName);
  }

  /**
   * @return an array of binding properties.
   */
  public BindingProperty[] getOrderedProps() {
    return orderedProps;
  }

  /**
   * @return an array of binding property values.
   */
  public String[] getOrderedPropValues() {
    return orderedPropValues;
  }

  @Deprecated
  public String getPropertyValue(TreeLogger logger, String propertyName)
      throws BadPropertyValueException {
    // In practice there will probably be so few properties that a linear
    // search is at least as fast as a map lookup by name would be.
    // If that turns out not to be the case, the ctor could build a
    // name-to-index map.
    //
    for (int i = 0; i < orderedProps.length; i++) {
      BindingProperty prop = orderedProps[i];
      if (prop.getName().equals(propertyName)) {
        String value = orderedPropValues[i];
        if (prop.isAllowedValue(value)) {
          return value;
        } else {
          throw new BadPropertyValueException(propertyName, value);
        }
      }
    }

    for (ConfigurationProperty configProp : configProps) {
      if (configProp.getName().equals(propertyName)) {
        return configProp.getValue();
      }
    }

    // Didn't find it.
    //
    throw new BadPropertyValueException(propertyName);
  }

  @Deprecated
  public String[] getPropertyValueSet(TreeLogger logger, String propertyName)
      throws BadPropertyValueException {
    for (int i = 0; i < orderedProps.length; i++) {
      BindingProperty prop = orderedProps[i];
      if (prop.getName().equals(propertyName)) {
        return prop.getDefinedValues();
      }
    }

    // Configuration properties throw exception per javadoc.

    // Didn't find it.
    //
    throw new BadPropertyValueException(propertyName);
  }

  public com.google.gwt.core.ext.SelectionProperty getSelectionProperty(
      TreeLogger logger, String propertyName)
      throws BadPropertyValueException {
    // In practice there will probably be so few properties that a linear
    // search is at least as fast as a map lookup by name would be.
    // If that turns out not to be the case, the ctor could build a
    // name-to-index map.
    for (int i = 0; i < orderedProps.length; i++) {
      final BindingProperty prop = orderedProps[i];
      final String name = prop.getName();
      if (name.equals(propertyName)) {
        final String value = orderedPropValues[i];
        String[] values = prop.getDefinedValues();
        final TreeSet<String> possibleValues = new TreeSet<String>();
        for (String v : values) {
          possibleValues.add(v);
        }
        return new DefaultSelectionProperty(value, prop.getFallback(), name,
            possibleValues, prop.getFallbackValuesMap());
      }
    }

    throw new BadPropertyValueException(propertyName);
  }
  
  /**
   * Dumps the binding property key/value pairs; For debugging use only.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, j = orderedProps.length; i < j; i++) {
      sb.append(orderedProps[i].getName()).append(" = ").append(
          orderedPropValues[i]).append(" ");
    }
    return sb.toString();
  }
}
