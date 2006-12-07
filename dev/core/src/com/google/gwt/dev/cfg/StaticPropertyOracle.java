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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;

/**
 * An implementation of {@link PropertyOracle} that maintains explicit property
 * values, rather than computing them.
 */
public class StaticPropertyOracle implements PropertyOracle {

  private Property[] currentProps;

  private String[] currentValues;

  public StaticPropertyOracle() {
  }

  public String getPropertyValue(TreeLogger logger, String propertyName)
      throws BadPropertyValueException {
    // In practice there will probably be so few properties that a linear
    // search is at least as fast as a map lookup by name would be.
    // If that turns out not to be the case, the ctor could build a
    // name-to-index map.
    //
    for (int i = 0; i < currentProps.length; i++) {
      Property prop = currentProps[i];
      if (prop.getName().equals(propertyName)) {
        String value = currentValues[i];
        if (prop.isKnownValue(value)) {
          return value;
        } else {
          throw new BadPropertyValueException(propertyName, value);
        }
      }
    }

    // Didn't find it.
    //
    return null;
  }

  public void setPropertyValues(Property[] props, String[] values) {
    currentProps = props;
    currentValues = values;
  }
}
