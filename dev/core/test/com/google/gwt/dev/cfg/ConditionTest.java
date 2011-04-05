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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Test the {@link Condition} class.
 */
public class ConditionTest extends TestCase {
  private PropertyOracle propertyOracle;
  private CompilationState compilationState;
  private static Set<String> activeLinkerNames = new LinkedHashSet<String>(
      Arrays.asList("linker1", "linker2", "xs"));

  @Override
  public void setUp() {
    ModuleDef module = new ModuleDef("fake");

    ConfigurationProperty conf1 = module.getProperties().createConfiguration(
        "conf1", false);
    conf1.setValue("value1");

    BindingProperty binding1 = module.getProperties().createBinding("binding1");
    binding1.addDefinedValue(new ConditionAll(), "true");
    binding1.addDefinedValue(new ConditionAll(), "false");

    propertyOracle = new StaticPropertyOracle(new BindingProperty[] {binding1},
        new String[] {"true"}, new ConfigurationProperty[] {conf1});

    compilationState = TypeOracleTestingUtils.buildStandardCompilationStateWith(TreeLogger.NULL);
  }

  public void testBasics() throws UnableToCompleteException {
    assertTrue(isTrue(new ConditionAll(), null));
    assertFalse(isTrue(new ConditionAny(), null));
    assertTrue(isTrue(new ConditionNone(), null));

    assertTrue(isTrue(new ConditionWhenPropertyIs("conf1", "value1"), null));
    assertFalse(isTrue(new ConditionWhenPropertyIs("conf1", "value2"), null));
    assertTrue(isTrue(new ConditionWhenPropertyIs("binding1", "true"), null));
    assertFalse(isTrue(new ConditionWhenPropertyIs("binding1", "false"), null));

    assertTrue(isTrue(new ConditionWhenLinkerAdded("linker1"), null));
    assertFalse(isTrue(new ConditionWhenLinkerAdded("bogoLinker"), null));

    ConditionWhenTypeAssignableTo assignableToObject = new ConditionWhenTypeAssignableTo(
        "java.lang.Object");
    assertTrue(isTrue(assignableToObject, "java.lang.String"));
    assertFalse(isTrue(new ConditionWhenTypeAssignableTo("java.lang.String"),
        "java.lang.Object"));
    ConditionWhenTypeIs isObject = new ConditionWhenTypeIs("java.lang.Object");
    assertTrue(isTrue(isObject, "java.lang.Object"));
    assertFalse(isTrue(isObject, "java.lang.String"));

    {
      ConditionAll all = new ConditionAll();
      all.getConditions().add(assignableToObject);
      all.getConditions().add(isObject);

      assertTrue(isTrue(all, "java.lang.Object"));
      assertFalse(isTrue(all, "java.lang.String"));
    }

    {
      ConditionAny any = new ConditionAny();
      any.getConditions().add(assignableToObject);
      any.getConditions().add(isObject);

      assertTrue(isTrue(any, "java.lang.Object"));
      assertTrue(isTrue(any, "java.lang.String"));
    }

    {
      ConditionNone none = new ConditionNone();
      none.getConditions().add(assignableToObject);
      none.getConditions().add(isObject);

      assertFalse(isTrue(none, "java.lang.Object"));
      assertFalse(isTrue(none, "java.lang.String"));
    }
  }

  private boolean isTrue(Condition cond, String testType)
      throws UnableToCompleteException {
    return cond.isTrue(TreeLogger.NULL, new DeferredBindingQuery(
        propertyOracle, activeLinkerNames, compilationState, testType));
  }
}