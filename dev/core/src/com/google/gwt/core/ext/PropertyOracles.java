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
package com.google.gwt.core.ext;

/**
 * Helper functions for operating on multiple property oracles.
 */
public class PropertyOracles {

  /**
   * Look for a configuration property in all property oracles.
   */
  public static int findIntegerConfigurationProperty(
      PropertyOracle[] propertyOracles, String name, int def) {
    int toReturn = def;
    for (PropertyOracle oracle : propertyOracles) {
      try {
        com.google.gwt.core.ext.ConfigurationProperty property =
            oracle.getConfigurationProperty(name);
        toReturn = Integer.parseInt(property.getValues().get(0));
      } catch (Exception e) {
        break;
      }
    }
    return toReturn;
  }

  /**
   * Look for a selection property in all property oracles.
   */
  public static boolean findBooleanProperty(TreeLogger logger, PropertyOracle[] propertyOracles,
      String name, String valueToFind, boolean valueIfFound, boolean valueIfNotFound,
      boolean valueIfError) {
    boolean toReturn = valueIfNotFound;
    for (PropertyOracle oracle : propertyOracles) {
      try {
        SelectionProperty property = oracle.getSelectionProperty(logger, name);
        if (valueToFind.equals(property.getCurrentValue())) {
          toReturn = valueIfFound;
          break;
        }
      } catch (BadPropertyValueException e) {
        // unknown value play it safe
        toReturn = valueIfError;
        break;
      }
    }
    return toReturn;
  }
}
