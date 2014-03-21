/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * A queryable runtime registry of binding property providers and configuration property
 * values.<br />
 *
 * Supports the execution of runtime rebinding by providing property value lookup to executing
 * runtime rebind rules.
 */
public class RuntimePropertyRegistry {

  /**
   * A base for classes that can return or calculate a value for a property.
   */
  public abstract static class PropertyValueProvider {

    /**
     * Returns the name of the property for which a value is being provided.
     */
    public abstract String getName();

    /**
     * Returns a value for the intended property. The value might or might not be calculated on the
     * fly based on the browser environment.
     */
    public abstract String getValue();
  }

  /**
   * A cache of previously calculated values for requested property names.
   */
  private static Map<String, String> cachedPropertyValuesByName = new HashMap<String, String>();

  /**
   * The registry of property value provider classes, registered by name.
   */
  private static Map<String, PropertyValueProvider> propertyValueProvidersByName =
      new HashMap<String, PropertyValueProvider>();

  /**
   * Returns the value for the given property name. On first access the matching property value
   * provider is found and executed while subsequent access are served from a cache.
   */
  public static String getPropertyValue(String propertyName) {
    if (cachedPropertyValuesByName.containsKey(propertyName)) {
      return cachedPropertyValuesByName.get(propertyName);
    }

    if (propertyValueProvidersByName.containsKey(propertyName)) {
      PropertyValueProvider propertyProvider = propertyValueProvidersByName.get(propertyName);
      String propertyValue = propertyProvider.getValue();
      cachedPropertyValuesByName.put(propertyName, propertyValue);
      return propertyValue;
    }

    throw new RuntimeException("Can't get a value for property '" + propertyName
        + "' since it does not have a registered value generator.");
  }

  /**
   * Registers the given property value provider. Registered providers are indexed by property name
   * for fast retrieval.
   */
  public static void registerPropertyValueProvider(PropertyValueProvider propertyValueProvider) {
    propertyValueProvidersByName.put(propertyValueProvider.getName(), propertyValueProvider);
  }
}
