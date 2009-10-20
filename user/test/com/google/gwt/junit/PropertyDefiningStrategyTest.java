/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.junit;

import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.junit.client.WithProperties;
import com.google.gwt.junit.client.WithProperties.Property;

import junit.framework.TestCase;

import java.util.SortedSet;

/**
 * Tests the {@link PropertyDefiningStrategy}.
 */
public class PropertyDefiningStrategyTest extends TestCase {
  private static class PropertyValue {
    public String name, value;

    public PropertyValue(String name, String value) {
      this.name = name;
      this.value = value;
    }
  }

  @WithProperties({@Property(name = "name", value = "value")})
  public void methodWithSingleProperty() {
  }

  @WithProperties({
      @Property(name = "name2", value = "value2"),
      @Property(name = "name1", value = "value1")})
  public void methodWithTwoProperties() {
  }

  public void methodWithoutProperties() {
  }

  public void testGetSyntheticModuleExtension() {
    assertEquals("JUnit.name$value",
        getStrategyForSingleProperty().getSyntheticModuleExtension());
    assertEquals("JUnit.name1$value1.name2$value2",
        getStrategyForTwoProperties().getSyntheticModuleExtension());
    assertEquals("JUnit",
        getStrategyForNoProperty().getSyntheticModuleExtension());
  }

  public void testProcessModuleForTestCaseWithSingleProperty() {
    ModuleDef module = new ModuleDef("myModule");
    getStrategyForSingleProperty().processModule(module);
    assertProperties(module, p("name", "value"));
  }

  public void testProcessModuleForTestCaseWithTwoProperties() {
    ModuleDef module = new ModuleDef("myModule");
    getStrategyForTwoProperties().processModule(module);
    assertProperties(module, p("name1", "value1"), p("name2", "value2"));
  }

  public void testProcessModuleForTestCaseWithoutProperties() {
    ModuleDef module = new ModuleDef("myModule");
    getStrategyForNoProperty().processModule(module);
    assertProperties(module);
  }

  private void assertProperties(ModuleDef module, PropertyValue... props) {
    SortedSet<BindingProperty> properties = module.getProperties().getBindingProperties();
    assertEquals(props.length, properties.size());
    int i = 0;
    for (BindingProperty property : properties) {
      assertEquals("property " + i, props[i].name, property.getName());
      assertEquals("property " + i, props[i].value,
          property.getConstrainedValue());
      i++;
    }
  }

  private static PropertyValue p(String name, String value) {
    return new PropertyValue(name, value);
  }

  private PropertyDefiningStrategy getStrategyForSingleProperty() {
    TestCase result = new PropertyDefiningStrategyTest();
    result.setName("methodWithSingleProperty");
    return new PropertyDefiningStrategy(result);
  }

  private PropertyDefiningStrategy getStrategyForTwoProperties() {
    TestCase result = new PropertyDefiningStrategyTest();
    result.setName("methodWithTwoProperties");
    return new PropertyDefiningStrategy(result);
  }

  private PropertyDefiningStrategy getStrategyForNoProperty() {
    TestCase result = new PropertyDefiningStrategyTest();
    result.setName("methodWithoutProperties");
    return new PropertyDefiningStrategy(result);
  }
}
