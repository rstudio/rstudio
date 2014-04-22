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
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.collect.Sets;
import com.google.gwt.thirdparty.guava.common.base.Objects;

import java.util.List;
import java.util.Set;

/**
 * A deferred binding condition to determine whether a named property has a
 * particular value.
 */
public class ConditionWhenPropertyIs extends Condition {

  private final String propName;

  private final String value;

  public ConditionWhenPropertyIs(String propName, String value) {
    this.propName = propName;
    this.value = value;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ConditionWhenPropertyIs) {
      ConditionWhenPropertyIs that = (ConditionWhenPropertyIs) object;
      return Objects.equal(this.propName, that.propName)
          && Objects.equal(this.value, that.value);
    }
    return false;
  }

  @Override
  public Set<String> getRequiredProperties() {
    return Sets.create(propName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(propName, value);
  }

  @Override
  public String toSource() {
    return String.format(
        "@com.google.gwt.lang.RuntimePropertyRegistry::getPropertyValue(*)(\"%s\") == \"%s\"",
        propName, value);
  }

  @Override
  public String toString() {
    return "<when-property-is name='" + propName + "' value='" + value + "'/>";
  }

  @Override
  protected boolean doEval(TreeLogger logger, DeferredBindingQuery query)
      throws UnableToCompleteException {
    PropertyOracle propertyOracle = query.getPropertyOracle();
    String testValue;
    try {
      try {
        SelectionProperty prop = propertyOracle.getSelectionProperty(logger,
            propName);
        testValue = prop.getCurrentValue();
      } catch (BadPropertyValueException e) {
        ConfigurationProperty prop = propertyOracle.getConfigurationProperty(propName);
        testValue = prop.getValues().get(0);
      }
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG, "Property value is '" + testValue + "'",
            null);
      }
      if (testValue.equals(value)) {
        return true;
      } else {
        // no exact match was found, see if any fall back
        // value would satisfy the condition
        try {
          SelectionProperty prop = propertyOracle.getSelectionProperty(logger,
              propName);
          List<? extends Set<String>> fallbackValues = prop.getFallbackValues(value);
          if (fallbackValues != null && fallbackValues.size() > 0) {
            if (logger.isLoggable(TreeLogger.DEBUG)) {
              logger.log(TreeLogger.DEBUG, "Property value '" + value + "'" +
                  " is the fallback of '" + fallbackValues.toString() + "'", null);
            }
            int cost = -1;
            for (Set<String> values : fallbackValues) {
              for (String fallbackValue : values) {
                if (testValue.equals(fallbackValue)) {
                  query.setFallbackEvaluationCost(cost);
                  return false;
                }
              }
              cost--;
            }
          }
        } catch (BadPropertyValueException e) {
          // do nothing - currently, only selection
          // properties support fall back values
        }
        return false;
      }
    } catch (BadPropertyValueException e) {
      String msg = "Unable to get value of property '" + propName + "'";
      logger.log(TreeLogger.ERROR, msg, e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  protected String getEvalAfterMessage(String testType, boolean result) {
    if (result) {
      return "Yes, the property value matched";
    } else {
      return "No, the value did not match";
    }
  }

  @Override
  protected String getEvalBeforeMessage(String testType) {
    return toString();
  }
}
