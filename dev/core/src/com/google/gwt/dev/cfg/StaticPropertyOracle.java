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
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;

/**
 * An implementation of {@link PropertyOracle} that contains property values,
 * rather than computing them.
 */
public class StaticPropertyOracle implements PropertyOracle {

  private final ConfigurationProperty[] configProps;

  private final BindingProperty[] orderedProps;

  private final String[] orderedPropValues;

  public StaticPropertyOracle(BindingProperty[] orderedProps,
      String[] orderedPropValues, ConfigurationProperty[] configProps) {
    this.orderedProps = orderedProps;
    this.orderedPropValues = orderedPropValues;
    this.configProps = configProps;
  }

  public BindingProperty[] getOrderedProps() {
    return orderedProps;
  }

  public String[] getOrderedPropValues() {
    return orderedPropValues;
  }

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
}
