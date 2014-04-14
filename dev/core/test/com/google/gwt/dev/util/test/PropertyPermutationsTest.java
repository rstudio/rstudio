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
import com.google.gwt.dev.cfg.PropertyPermutations;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Tests the PropertyPermutations code.
 */
public class PropertyPermutationsTest extends TestCase {

  /**
   * Make sure that a cycle doesn't cause an infinite loop.
   */
  public void testCycle() {
    // This is what you'd get with a conditional <set-property value="false">
    ModuleDef md = new ModuleDef("testCycle");
    Properties props = md.getProperties();

    {
      BindingProperty prop = props.createBinding("A");
      prop.addDefinedValue(prop.getRootCondition(), "a1");
      prop.addDefinedValue(prop.getRootCondition(), "a2");

      prop.addDefinedValue(new ConditionWhenPropertyIs("B", "b3"), "a3");
    }

    {
      BindingProperty prop = props.createBinding("B");
      prop.addDefinedValue(prop.getRootCondition(), "b1");
      prop.addDefinedValue(prop.getRootCondition(), "b2");

      prop.addDefinedValue(new ConditionWhenPropertyIs("A", "a3"), "b3");
    }

    try {
      new PropertyPermutations(props, md.getActiveLinkerNames());
      fail();
    } catch (IllegalStateException e) {
      // OK
    }
  }

  public void testOneDimensionPerm() {
    ModuleDef md = new ModuleDef("testOneDimensionPerm");
    Properties props = md.getProperties();

    {
      BindingProperty prop = props.createBinding("debug");
      prop.addDefinedValue(prop.getRootCondition(), "false");
      prop.addDefinedValue(prop.getRootCondition(), "true");
    }

    // Permutations and their values are in stable alphabetical order.
    //
    PropertyPermutations perms = new PropertyPermutations(md.getProperties(),
        md.getActiveLinkerNames());
    String[] perm;
    Iterator<String[]> iter = perms.iterator();

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("false", perm[0]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("true", perm[0]);
  }

  public void testOneDimensionPermWithCollapse() {
    ModuleDef md = new ModuleDef("testOneDimensionPerm");
    Properties props = md.getProperties();

    {
      BindingProperty prop = props.createBinding("debug");
      prop.addDefinedValue(prop.getRootCondition(), "false");
      prop.addDefinedValue(prop.getRootCondition(), "true");
      prop.addCollapsedValues("true", "false");
    }

    // Permutations and their values are in stable alphabetical order.
    //
    PropertyPermutations perms = new PropertyPermutations(md.getProperties(),
        md.getActiveLinkerNames());

    List<PropertyPermutations> collapsed = perms.collapseProperties();
    assertEquals("size", 1, collapsed.size());
    perms = collapsed.get(0);

    String[] perm;
    Iterator<String[]> iter = perms.iterator();

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("false", perm[0]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("true", perm[0]);
  }

  public void testTwoDimensionPerm() {
    ModuleDef md = new ModuleDef("testTwoDimensionPerm");
    Properties props = md.getProperties();

    {
      BindingProperty prop = props.createBinding("user.agent");
      prop.addDefinedValue(prop.getRootCondition(), "moz");
      prop.addDefinedValue(prop.getRootCondition(), "ie6");
      prop.addDefinedValue(prop.getRootCondition(), "opera");
    }

    {
      BindingProperty prop = props.createBinding("debug");
      prop.addDefinedValue(prop.getRootCondition(), "false");
      prop.addDefinedValue(prop.getRootCondition(), "true");
    }

    // String[]s and their values are in stable alphabetical order.
    //
    PropertyPermutations perms = new PropertyPermutations(md.getProperties(),
        md.getActiveLinkerNames());
    String[] perm;
    Iterator<String[]> iter = perms.iterator();

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("false", perm[0]);
    assertEquals("ie6", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("false", perm[0]);
    assertEquals("moz", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("false", perm[0]);
    assertEquals("opera", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("true", perm[0]);
    assertEquals("ie6", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("true", perm[0]);
    assertEquals("moz", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("true", perm[0]);
    assertEquals("opera", perm[1]);
  }

  public void testTwoDimensionPermWithCollapse() {
    ModuleDef md = new ModuleDef("testTwoDimensionPerm");
    Properties props = md.getProperties();

    {
      BindingProperty prop = props.createBinding("user.agent");
      prop.addDefinedValue(prop.getRootCondition(), "moz");
      prop.addDefinedValue(prop.getRootCondition(), "ie6");
      prop.addDefinedValue(prop.getRootCondition(), "opera");
      prop.addCollapsedValues("moz", "ie6", "opera");
    }

    {
      BindingProperty prop = props.createBinding("debug");
      prop.addDefinedValue(prop.getRootCondition(), "false");
      prop.addDefinedValue(prop.getRootCondition(), "true");
    }

    // String[]s and their values are in stable alphabetical order.
    //
    PropertyPermutations perms = new PropertyPermutations(md.getProperties(),
        md.getActiveLinkerNames());

    List<PropertyPermutations> collapsed = perms.collapseProperties();
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
    Properties props = md.getProperties();

    {
      BindingProperty prop = props.createBinding("user.agent");
      prop.addDefinedValue(prop.getRootCondition(), "moz");
      prop.addDefinedValue(prop.getRootCondition(), "ie6");
      prop.addDefinedValue(prop.getRootCondition(), "ie8");
      prop.addDefinedValue(prop.getRootCondition(), "opera");
    }

    {
      BindingProperty prop = props.createBinding("stackTraces");
      prop.addDefinedValue(prop.getRootCondition(), "false");
      prop.addDefinedValue(prop.getRootCondition(), "true");
      // <set-property name="stackTraces" value="false" />
      prop.setAllowedValues(prop.getRootCondition(), "false");

      /*
       * <set-property name="stackTraces" value="true,false"> <when user.agent
       * is ie6 or ie 8> </set-property>
       */
      ConditionAny cond = new ConditionAny();
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "ie6"));
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "ie8"));
      prop.setAllowedValues(cond, "true", "false");
    }

    validateTwoDimensionPerm(props, md.getActiveLinkerNames());
  }

  public void testTwoDimensionPermWithExtension() {
    // This is what you'd get with a conditional <extend-property>
    ModuleDef md = new ModuleDef("testTwoDimensionsWithConditions");
    Properties props = md.getProperties();

    {
      BindingProperty prop = props.createBinding("user.agent");
      prop.addDefinedValue(prop.getRootCondition(), "moz");
      prop.addDefinedValue(prop.getRootCondition(), "ie6");
      prop.addDefinedValue(prop.getRootCondition(), "ie8");
      prop.addDefinedValue(prop.getRootCondition(), "opera");
    }

    {
      BindingProperty prop = props.createBinding("stackTraces");
      prop.addDefinedValue(prop.getRootCondition(), "false");

      ConditionAny cond = new ConditionAny();
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "ie6"));
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "ie8"));

      prop.addDefinedValue(cond, "true");
    }

    validateTwoDimensionPerm(props, md.getActiveLinkerNames());
  }

  public void testTwoDimensionPermWithRestriction() {
    // This is what you'd get with a conditional <set-property value="false">
    ModuleDef md = new ModuleDef("testTwoDimensionsWithRestriction");
    Properties props = md.getProperties();

    {
      BindingProperty prop = props.createBinding("user.agent");
      prop.addDefinedValue(prop.getRootCondition(), "moz");
      prop.addDefinedValue(prop.getRootCondition(), "ie6");
      prop.addDefinedValue(prop.getRootCondition(), "ie8");
      prop.addDefinedValue(prop.getRootCondition(), "opera");
    }

    {
      BindingProperty prop = props.createBinding("stackTraces");
      prop.addDefinedValue(prop.getRootCondition(), "false");
      prop.addDefinedValue(prop.getRootCondition(), "true");

      ConditionAny cond = new ConditionAny();
      cond.getConditions().add(new ConditionWhenPropertyIs("user.agent", "moz"));
      cond.getConditions().add(
          new ConditionWhenPropertyIs("user.agent", "opera"));

      prop.setAllowedValues(cond, "false");
    }

    validateTwoDimensionPerm(props, md.getActiveLinkerNames());
  }

  private void validateTwoDimensionPerm(Properties props,
      Set<String> activeLinkerNames) {
    PropertyPermutations perms = new PropertyPermutations(props,
        activeLinkerNames);

    assertEquals(6, perms.size());

    // Order is alphabetical in dependency order
    String[] perm;
    Iterator<String[]> iter = perms.iterator();

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("ie6", perm[0]);
    assertEquals("false", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("ie6", perm[0]);
    assertEquals("true", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("ie8", perm[0]);
    assertEquals("false", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("ie8", perm[0]);
    assertEquals("true", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("moz", perm[0]);
    assertEquals("false", perm[1]);

    assertTrue(iter.hasNext());
    perm = iter.next();
    assertEquals("opera", perm[0]);
    assertEquals("false", perm[1]);

    assertFalse(iter.hasNext());
  }
}
