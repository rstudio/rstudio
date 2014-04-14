/*
 * Copyright 2011 Google Inc.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A container for saving lists of deferred-binding and configuration properties
 * to be compared subsequently with a PropertyOracle.
 */
public class CachedPropertyInformation implements Serializable {

  private final List<SelectionProperty> selectionProperties;
  private final List<ConfigurationProperty> configProperties;

  public CachedPropertyInformation(TreeLogger logger, PropertyOracle oracle,
      Collection<String> selectionPropertyNames,
      Collection<String> configPropertyNames) {

    if (oracle == null) {
      selectionProperties = null;
      configProperties = null;
      return;
    }

    if (selectionPropertyNames == null) {
      selectionProperties = null;
    } else {
      selectionProperties = new ArrayList<SelectionProperty>();
      for (String name : selectionPropertyNames) {
        try {
          SelectionProperty selProp = oracle.getSelectionProperty(logger, name);
          selectionProperties.add(selProp);
        } catch (BadPropertyValueException e) {
        }
      }
    }

    if (configPropertyNames == null) {
      configProperties = null;
    } else {
      configProperties = new ArrayList<ConfigurationProperty>();
      for (String name : configPropertyNames) {
        try {
          ConfigurationProperty configProp = oracle.getConfigurationProperty(name);
          configProperties.add(configProp);
        } catch (BadPropertyValueException e) {
        }
      }
    }
  }

  /**
   * Check a previously cached set of deferred-binding and configuration
   * properties with the provided property oracle.
   */
  public boolean checkPropertiesWithPropertyOracle(TreeLogger logger,
      PropertyOracle oracle) {

    if (selectionProperties != null) {
      try {
        for (SelectionProperty selProp : selectionProperties) {
          SelectionProperty currProp =
            oracle.getSelectionProperty(logger, selProp.getName());
          if (!currProp.getCurrentValue().equals(selProp.getCurrentValue())) {
            logger.log(TreeLogger.TRACE, "Found changed property: " + selProp.getName());
            return false;
          }
        }
      } catch (BadPropertyValueException e) {
        logger.log(TreeLogger.TRACE, "Found problem checking property", e);
        return false;
      }
    }

    if (configProperties != null) {
      try {
        for (ConfigurationProperty configProp : configProperties) {
          ConfigurationProperty currProp =
            oracle.getConfigurationProperty(configProp.getName());
          if (!currProp.equals(configProp)) {
            logger.log(TreeLogger.TRACE,
                "Found changed configuration property: " + configProp.getName());
            return false;
          }
        }
      } catch (BadPropertyValueException e) {
        logger.log(TreeLogger.TRACE,
            "Found problem checking configuration property", e);
        return false;
      }
    }

    return true;
  }
}
