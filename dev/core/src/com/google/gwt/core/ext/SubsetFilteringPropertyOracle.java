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
package com.google.gwt.core.ext;

import com.google.gwt.dev.cfg.RuleGenerateWith;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

import java.util.Set;

/**
 * A property oracle that prevents access to any properties not named in its predefined set.<br />
 *
 * Used by the generator driver framework to limit property access for the purpose of
 * forcing generators to accurately declare their property dependencies.
 */
public class SubsetFilteringPropertyOracle implements PropertyOracle {

  private final Set<String> accessiblePropertyNames;
  private final String accessViolationMessage;
  private final PropertyOracle wrappedPropertyOracle;

  public SubsetFilteringPropertyOracle(Set<String> accessiblePropertyNames,
      PropertyOracle wrappedPropertyOracle, String accessViolationMessage) {
    this.accessiblePropertyNames = accessiblePropertyNames;
    this.wrappedPropertyOracle = wrappedPropertyOracle;
    this.accessViolationMessage = accessViolationMessage;
  }

  @Override
  public ConfigurationProperty getConfigurationProperty(String propertyName)
      throws BadPropertyValueException {
    Preconditions.checkState(accessiblePropertyNames.equals(RuleGenerateWith.ALL_PROPERTIES)
        || accessiblePropertyNames.contains(propertyName), "Access to configuration property '"
        + propertyName + "' is not allowed. " + accessViolationMessage);
    return wrappedPropertyOracle.getConfigurationProperty(propertyName);
  }

  @Override
  public SelectionProperty getSelectionProperty(TreeLogger logger, String propertyName)
      throws BadPropertyValueException {
    Preconditions.checkState(accessiblePropertyNames.equals(RuleGenerateWith.ALL_PROPERTIES)
        || accessiblePropertyNames.contains(propertyName), "Access to binding property '"
        + propertyName + "' is not allowed. " + accessViolationMessage);
    return wrappedPropertyOracle.getSelectionProperty(logger, propertyName);
  }
}
