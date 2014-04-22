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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests for DynamicPropertyOracle.
 */
public class DynamicPropertyOracleTest extends TestCase {

  public void testGetConfigurationProperty() throws BadPropertyValueException {
    // Sets up.
    List<String> expectedValues = Lists.newArrayList("webkit", "mozilla");
    Properties properties = new Properties();
    ConfigurationProperty userAgentProperty = properties.createConfiguration("user.agent", true);
    for (String expectedValue : expectedValues) {
      userAgentProperty.addValue(expectedValue);
    }
    DynamicPropertyOracle dynamicPropertyOracle = new DynamicPropertyOracle(properties);

    // Pulls out the prepared configuration property.
    com.google.gwt.core.ext.ConfigurationProperty configurationProperty =
        dynamicPropertyOracle.getConfigurationProperty("user.agent");
    assertEquals(expectedValues, configurationProperty.getValues());
  }

  public void testProcess() throws BadPropertyValueException {
    // Sets up.
    Properties properties = new Properties();
    BindingProperty userAgentProperty = properties.createBinding("user.agent");
    BindingProperty localeProperty = properties.createBinding("locale");
    DynamicPropertyOracle dynamicPropertyOracle = new DynamicPropertyOracle(properties);

    // Verifies baseline state.
    assertFalse(dynamicPropertyOracle.haveAccessedPropertiesChanged());
    assertTrue(dynamicPropertyOracle.getAccessedProperties().isEmpty());

    // Finds XML Fallback.
    userAgentProperty.setFallback("mozilla");
    assertEquals("mozilla",
        dynamicPropertyOracle.getSelectionProperty(null, "user.agent").getCurrentValue());
    assertTrue(dynamicPropertyOracle.haveAccessedPropertiesChanged());
    assertEquals(1, dynamicPropertyOracle.getAccessedProperties().size());

    // Finds XML Constrained.
    dynamicPropertyOracle = new DynamicPropertyOracle(properties);
    userAgentProperty.addDefinedValue(new ConditionWhenLinkerAdded("foo"), "webkit");
    userAgentProperty.addDefinedValue(new ConditionWhenLinkerAdded("bar"), "webkit");
    userAgentProperty.addDefinedValue(new ConditionWhenLinkerAdded("baz"), "webkit");
    assertEquals(
        "webkit", dynamicPropertyOracle.getSelectionProperty(null, "user.agent").getCurrentValue());
    assertTrue(dynamicPropertyOracle.haveAccessedPropertiesChanged());
    assertEquals(1, dynamicPropertyOracle.getAccessedProperties().size());

    // Finds first defined.
    dynamicPropertyOracle.reset();
    localeProperty.addDefinedValue(new ConditionWhenLinkerAdded("qwer"), "en");
    localeProperty.addDefinedValue(new ConditionWhenLinkerAdded("asdf"), "fr");
    localeProperty.addDefinedValue(new ConditionWhenLinkerAdded("zxcv"), "ru");
    assertEquals(
        "en", dynamicPropertyOracle.getSelectionProperty(null, "locale").getCurrentValue());
    assertTrue(dynamicPropertyOracle.haveAccessedPropertiesChanged());
    assertEquals(2, dynamicPropertyOracle.getAccessedProperties().size());

    // Finds permutation prescribed.
    dynamicPropertyOracle.reset();
    dynamicPropertyOracle.prescribePropertyValue("user.agent", "redbull");
    assertEquals("redbull",
        dynamicPropertyOracle.getSelectionProperty(null, "user.agent").getCurrentValue());
    assertFalse(dynamicPropertyOracle.haveAccessedPropertiesChanged());
    assertEquals(2, dynamicPropertyOracle.getAccessedProperties().size());

    // Reset clears prescription.
    dynamicPropertyOracle.reset();
    assertEquals(
        "webkit", dynamicPropertyOracle.getSelectionProperty(null, "user.agent").getCurrentValue());
    assertFalse(dynamicPropertyOracle.haveAccessedPropertiesChanged());
    assertEquals(2, dynamicPropertyOracle.getAccessedProperties().size());
  }
}
