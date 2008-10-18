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

import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.PropertyPermutations;

import junit.framework.TestCase;

import java.util.Iterator;

public class PropertyPermutationsTest extends TestCase {

  public void testOneDimensionPerm() {
    ModuleDef md = new ModuleDef("testOneDimensionPerm");
    Properties props = md.getProperties();

    {
      BindingProperty prop = props.createBinding("debug");
      prop.addDefinedValue("false");
      prop.addDefinedValue("true");
    }

    // Permutations and their values are in stable alphabetical order.
    //
    PropertyPermutations perms = new PropertyPermutations(md.getProperties());
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
      prop.addDefinedValue("moz");
      prop.addDefinedValue("ie6");
      prop.addDefinedValue("opera");
    }

    {
      BindingProperty prop = props.createBinding("debug");
      prop.addDefinedValue("false");
      prop.addDefinedValue("true");
    }

    // String[]s and their values are in stable alphabetical order.
    //
    PropertyPermutations perms = new PropertyPermutations(md.getProperties());
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
}
