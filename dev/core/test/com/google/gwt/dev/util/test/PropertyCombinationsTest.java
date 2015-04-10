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
package com.google.gwt.dev.util.test;

import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConditionAny;
import com.google.gwt.dev.cfg.ConditionWhenPropertyIs;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.PropertyCombinations;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Tests the PropertyPermutations code.
 */
public class PropertyCombinationsTest extends TestCase {

  /**
   * Make sure that a cycle doesn't cause an infinite loop.
   */
  public void testCycle() {
    // This is what you'd get with a conditional <set-property value="false">
    ModuleDef md = new ModuleDef("testCycle");
    Properties properties = md.getProperties();

    {
      BindingProperty bindingProperty = properties.createBinding("A");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "a1");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "a2");

      bindingProperty.addDefinedValue(new ConditionWhenPropertyIs("B", "b3"), "a3");
    }

    {
      BindingProperty bindingProperty = properties.createBinding("B");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "b1");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "b2");

      bindingProperty.addDefinedValue(new ConditionWhenPropertyIs("A", "a3"), "b3");
    }

    try {
      new PropertyCombinations(properties, md.getActiveLinkerNames());
      fail();
    } catch (IllegalStateException e) {
      // OK
    }
  }

  public void testOneDimensionPerm() {
    ModuleDef md = new ModuleDef("testOneDimensionPerm");
    Properties properties = md.getProperties();

    {
      BindingProperty bindingProperty = properties.createBinding("debug");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "false");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "true");
    }

    // Permutations and their values are in stable alphabetical order.
    //
    PropertyCombinations permutations = new PropertyCombinations(md.getProperties(),
        md.getActiveLinkerNames());
    String[] permutation;
    Iterator<String[]> it = permutations.iterator();

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("false", permutation[0]);

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("true", permutation[0]);
  }

  public void testOneDimensionPermWithCollapse() {
    ModuleDef md = new ModuleDef("testOneDimensionPerm");
    Properties properties = md.getProperties();

    {
      BindingProperty bindingProperty = properties.createBinding("debug");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "false");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "true");
      bindingProperty.addCollapsedValues("true", "false");
    }

    // Permutations and their values are in stable alphabetical order.
    //
    PropertyCombinations permutations = new PropertyCombinations(md.getProperties(),
        md.getActiveLinkerNames());

    List<PropertyCombinations> collapsed = permutations.collapseProperties();
    assertEquals("size", 1, collapsed.size());
    permutations = collapsed.get(0);

    String[] permutation;
    Iterator<String[]> it = permutations.iterator();

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("false", permutation[0]);

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("true", permutation[0]);
  }

  public void testTwoDimensionPerm() {
    ModuleDef md = new ModuleDef("testTwoDimensionPerm");
    Properties properties = md.getProperties();

    {
      BindingProperty bindingProperty = properties.createBinding("user.agent");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "moz");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "ie6");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "opera");
    }

    {
      BindingProperty bindingProperty = properties.createBinding("debug");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "false");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "true");
    }

    // String[]s and their values are in stable alphabetical order.
    //
    PropertyCombinations permutations = new PropertyCombinations(md.getProperties(),
        md.getActiveLinkerNames());
    String[] permutation;
    Iterator<String[]> it = permutations.iterator();

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("false", permutation[0]);
    assertEquals("ie6", permutation[1]);

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("false", permutation[0]);
    assertEquals("moz", permutation[1]);

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("false", permutation[0]);
    assertEquals("opera", permutation[1]);

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("true", permutation[0]);
    assertEquals("ie6", permutation[1]);

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("true", permutation[0]);
    assertEquals("moz", permutation[1]);

    assertTrue(it.hasNext());
    permutation = it.next();
    assertEquals("true", permutation[0]);
    assertEquals("opera", permutation[1]);
  }

  public void testTwoDimensionPermWithCollapse() {
    ModuleDef md = new ModuleDef("testTwoDimensionPerm");
    Properties properties = md.getProperties();

    {
      BindingProperty bindingProperty = properties.createBinding("user.agent");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "moz");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "ie6");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "opera");
      bindingProperty.addCollapsedValues("moz", "ie6", "opera");
    }

    {
      BindingProperty bindingProperty = properties.createBinding("debug");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "false");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "true");
    }

    // String[]s and their values are in stable alphabetical order.
    //
    PropertyCombinations permutations = new PropertyCombinations(md.getProperties(),
        md.getActiveLinkerNames());

    List<PropertyCombinations> collapsed = permutations.collapseProperties();
    assertEquals("size", 2, collapsed.size());

    Iterator<String[]> it = collapsed.get(0).iterator();
    assertEquals(Arrays.asList("false", "ie6"), Arrays.asList(it.next()));
    assertEquals(Arrays.asList("false", "moz"), Arrays.asList(it.next()));
    assertEquals(Arrays.asList("false", "opera"), Arrays.asList(it.next()));
    assertFalse(it.hasNext());

    it = collapsed.get(1).iterator();
    assertEquals(Arrays.asList("true", "ie6"), Arrays.asList(it.next()));
    assertEquals(Arrays.asList("true", "moz"), Arrays.asList(it.next()));
    assertEquals(Arrays.asList("true", "opera"), Arrays.asList(it.next()));
    assertFalse(it.hasNext());
  }

  public void testTwoDimensionPermWithExpansion() {
    ModuleDef md = new ModuleDef("testTwoDimensionsWithExpansion");
    Properties properties = md.getProperties();

    {
      BindingProperty bindingProperty = properties.createBinding("user.agent");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "moz");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "ie6");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "ie8");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "opera");
    }

    {
      BindingProperty bindingProperty = properties.createBinding("stackTraces");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "false");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "true");
      // <set-property name="stackTraces" value="false" />
      bindingProperty.setValues(bindingProperty.getRootCondition(), "false");

      /*
       * <set-property name="stackTraces" value="true,false"> <when user.agent
       * is ie6 or ie 8> </set-property>
       */
      ConditionAny cond = new ConditionAny();
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "ie6"));
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "ie8"));
      bindingProperty.setValues(cond, "true", "false");
    }

    validateTwoDimensionPerm(properties, md.getActiveLinkerNames());
  }

  public void testTwoDimensionPermWithExtension() {
    // This is what you'd get with a conditional <extend-property>
    ModuleDef md = new ModuleDef("testTwoDimensionsWithConditions");
    Properties properties = md.getProperties();

    {
      BindingProperty bindingProperty = properties.createBinding("user.agent");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "moz");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "ie6");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "ie8");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "opera");
    }

    {
      BindingProperty bindingProperty = properties.createBinding("stackTraces");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "false");

      ConditionAny cond = new ConditionAny();
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "ie6"));
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "ie8"));

      bindingProperty.addDefinedValue(cond, "true");
    }

    validateTwoDimensionPerm(properties, md.getActiveLinkerNames());
  }

  public void testTwoDimensionPermWithRestriction() {
    // This is what you'd get with a conditional <set-property value="false">
    ModuleDef md = new ModuleDef("testTwoDimensionsWithRestriction");
    Properties properties = md.getProperties();

    {
      BindingProperty bindingProperty = properties.createBinding("user.agent");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "moz");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "ie6");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "ie8");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "opera");
    }

    {
      BindingProperty bindingProperty = properties.createBinding("stackTraces");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "false");
      bindingProperty.addDefinedValue(bindingProperty.getRootCondition(), "true");

      ConditionAny cond = new ConditionAny();
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "moz"));
      cond.getConditions().add(
          new ConditionWhenPropertyIs("user.agent", "opera"));

      bindingProperty.setValues(cond, "false");
    }

    validateTwoDimensionPerm(properties, md.getActiveLinkerNames());
  }

  private void validateTwoDimensionPerm(Properties props, Set<String> activeLinkerNames) {
    PropertyCombinations permutations = new PropertyCombinations(props, activeLinkerNames);

    // Order is alphabetical in dependency order
    Iterator<String[]> it = permutations.iterator();
    checkNextPermutation(it, "ie6", "false");
    checkNextPermutation(it, "ie6", "true");
    checkNextPermutation(it, "ie8", "false");
    checkNextPermutation(it, "ie8", "true");
    checkNextPermutation(it, "moz", "false");
    checkNextPermutation(it, "opera", "false");
    assertFalse(it.hasNext());
  }

  private void checkNextPermutation(Iterator<String[]> it, String expectedName,
      String expectedValue) {
    assertTrue("expected " + expectedName + " but got nothing", it.hasNext());
    String[] perm = it.next();
    assertEquals(expectedName, perm[0]);
    assertEquals(expectedValue, perm[1]);
  }
}
