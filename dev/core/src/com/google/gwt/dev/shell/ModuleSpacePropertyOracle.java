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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.DefaultConfigurationProperty;
import com.google.gwt.core.ext.DefaultSelectionProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.Condition;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.DeferredBindingQuery;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Implements a {@link PropertyOracle} in terms of a module space, which makes
 * it possible to execute property providers.
 */
public class ModuleSpacePropertyOracle implements PropertyOracle {

  private final Set<String> activeLinkerNames;

  private final Map<String, String> prevAnswers = new HashMap<String, String>();

  private final Properties props;

  private final ModuleSpace space;

  /**
   * Create a property oracle that computes its properties from a module.
   */
  public ModuleSpacePropertyOracle(Properties props,
      Set<String> activeLinkerNames, ModuleSpace space) {
    this.space = space;
    this.activeLinkerNames = activeLinkerNames;
    this.props = props;
  }

  @Override
  public com.google.gwt.core.ext.ConfigurationProperty getConfigurationProperty(
      String propertyName) throws BadPropertyValueException {
    Property prop = getProperty(propertyName);
    if (prop instanceof ConfigurationProperty) {
      final ConfigurationProperty cprop = (ConfigurationProperty) prop;
      final String name = cprop.getName();
      final List<String> values = cprop.getValues();
      return new DefaultConfigurationProperty(name, values);
    } else {
      throw new BadPropertyValueException(propertyName);
    }
  }

  @Override
  public SelectionProperty getSelectionProperty(TreeLogger logger,
      String propertyName) throws BadPropertyValueException {
    Property prop = getProperty(propertyName);
    if (prop instanceof BindingProperty) {
      final BindingProperty cprop = (BindingProperty) prop;
      final String name = cprop.getName();
      final String value;
      if (prevAnswers.containsKey(propertyName)) {
        value = prevAnswers.get(propertyName);
      } else {
        value = computePropertyValue(logger, propertyName, cprop);
        prevAnswers.put(propertyName, value);
      }
      final String fallback = cprop.getFallback();
      final SortedSet<String> possibleValues = new TreeSet<String>();
      for (String v : cprop.getDefinedValues()) {
        possibleValues.add(v);
      }
      return new DefaultSelectionProperty(value, fallback, name, possibleValues,
          cprop.getFallbackValuesMap());
    } else {
      throw new BadPropertyValueException(propertyName);
    }
  }

  private Condition computeActiveCondition(TreeLogger logger,
      BindingProperty prop) throws BadPropertyValueException {
    // Last-one-wins
    Condition winner = null;
    for (Condition cond : prop.getConditionalValues().keySet()) {
      try {
        if (cond.isTrue(logger, new DeferredBindingQuery(this,
            activeLinkerNames))) {
          winner = cond;
        }
      } catch (UnableToCompleteException e) {
        BadPropertyValueException t = new BadPropertyValueException(
            prop.getName());
        t.initCause(e);
        throw t;
      }
    }
    assert winner != null : "No active Condition for " + prop.getName();
    return winner;
  }

  /**
   * Returns the value of the specified property.
   *
   * @throws BadPropertyValueException if the property value could not be
   *           computed, or if the returned result is not a legal value for this
   *           property.
   */
  private String computePropertyValue(TreeLogger logger, String propertyName,
      BindingProperty prop) throws BadPropertyValueException {

    String value = prop.getConstrainedValue();
    if (value != null) {
      // If there is only one legal value, use that.
      return value;
    }

    Condition winner = computeActiveCondition(logger, prop);
    String[] values = prop.getAllowedValues(winner);
    if (values.length == 1) {
      return values[0];
    }

    // Invokes the script function.
    //
    try {
      // Invoke the property provider function in JavaScript.
      //
      value = (String) space.invokeNativeObject("__gwt_getProperty", null,
          new Class[] {String.class}, new Object[] {prop.getName()});
    } catch (Throwable e) {
      // Treat as an unknown value.
      //
      String msg = "Error while executing the JavaScript provider for property '"
          + propertyName + "'";
      logger.log(TreeLogger.ERROR, msg, e);
      throw new BadPropertyValueException(propertyName, "<failed to compute>");
    }

    // value may be null if the provider returned an unknown property value.
    if (Arrays.asList(values).contains(value)) {
      return value;
    } else {
      // Bad value due to the provider returning an unknown value.
      // The fact that the provider returned an invalid value will also
      // have been reported to the JS bad property value handler function.
      throw new BadPropertyValueException(propertyName, value);
    }
  }

  /**
   * Returns a Property given its name, handling error conditions.
   *
   * @throws BadPropertyValueException
   */
  private Property getProperty(String propertyName)
      throws BadPropertyValueException {
    if (propertyName == null) {
      throw new NullPointerException("propertyName");
    }

    Property prop = props.find(propertyName);
    if (prop == null) {
      // Don't know this property; that's not good.
      //
      throw new BadPropertyValueException(propertyName);
    }
    return prop;
  }
}
