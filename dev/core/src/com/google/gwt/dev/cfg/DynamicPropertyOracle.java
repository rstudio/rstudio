/*
 * Copyright 2013 Google Inc.
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
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.DefaultConfigurationProperty;
import com.google.gwt.core.ext.DefaultSelectionProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * An implementation of {@link PropertyOracle} that helps discover the property values associated
 * with specific rebind results for generators.<br />
 *
 * It does so by recording the properties that are queried, providing a first legal answer for
 * properties not previously queried and allowing an external driver to prescribe values for
 * properties that have been discovered as dependencies.
 */
// TODO(stalcup): get rid of foo.ConfigurationProperty and bar.ConfigurationProperty class name
// collisions.
public class DynamicPropertyOracle implements PropertyOracle {

  private static SelectionProperty createSelectionProperty(
      String value, BindingProperty bindingProperty) {
    SortedSet<String> possibleValues =
        Sets.newTreeSet(Arrays.asList(bindingProperty.getDefinedValues()));
    return new DefaultSelectionProperty(value, bindingProperty.getFallback(),
        bindingProperty.getName(), possibleValues, bindingProperty.getFallbackValuesMap());
  }

  private final Set<BindingProperty> accessedProperties = Sets.newHashSet();
  private boolean accessedPropertiesChanged;
  private final Map<String, String> prescribedPropertyValuesByName = Maps.newHashMap();
  private final Properties properties;

  public DynamicPropertyOracle(Properties properties) {
    this.properties = properties;
  }

  public boolean haveAccessedPropertiesChanged() {
    return accessedPropertiesChanged;
  }

  public Set<BindingProperty> getAccessedProperties() {
    return accessedProperties;
  }

  @Override
  public ConfigurationProperty getConfigurationProperty(String propertyName)
      throws BadPropertyValueException {
    com.google.gwt.dev.cfg.ConfigurationProperty prop = properties.findConfigProp(propertyName);
    if (prop != null) {
      return new DefaultConfigurationProperty(prop.getName(), prop.getValues());
    }
    throw new BadPropertyValueException(propertyName);
  }

  /**
   * Returns the mapping from property names to its currently prescribed value. The internal mapping
   * changes between runs so the returned value is a stable copy.
   */
  public Map<String, String> getPrescribedPropertyValuesByName() {
    return Maps.newHashMap(prescribedPropertyValuesByName);
  }

  @Override
  public SelectionProperty getSelectionProperty(TreeLogger logger, String propertyName)
      throws BadPropertyValueException {
    BindingProperty bindingProperty = getBindingProperty(propertyName);
    accessedPropertiesChanged |= accessedProperties.add(bindingProperty);

    String propertyValue = prescribedPropertyValuesByName.isEmpty()
        ? bindingProperty.getFirstLegalValue() : prescribedPropertyValuesByName.get(propertyName);
    return createSelectionProperty(propertyValue, bindingProperty);
  }

  public void prescribePropertyValue(String propertyName, String propertyValue) {
    prescribedPropertyValuesByName.put(propertyName, propertyValue);
  }

  /**
   * Clears state in preparation for another round of rebind analysis on the same generator. Since
   * some per generator state is not cleared it is necessary to create a new instance per generator.
   */
  public void reset() {
    accessedPropertiesChanged = false;
    prescribedPropertyValuesByName.clear();
  }

  private BindingProperty getBindingProperty(String propertyName) throws BadPropertyValueException {
    BindingProperty property = properties.findBindingProp(propertyName);
    if (property != null) {
      return property;
    }
    throw new BadPropertyValueException(propertyName);
  }
}
