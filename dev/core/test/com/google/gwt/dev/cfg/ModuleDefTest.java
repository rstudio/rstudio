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

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

/**
 * Runs tests directly on ModuleDef.
 */
public class ModuleDefTest extends TestCase {

  @Shardable
  static class FakeLinker extends Linker {
    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public ArtifactSet link(TreeLogger logger, LinkerContext context, ArtifactSet artifacts,
        boolean onePermutation) {
      return null;
    }

    @Override
    public ArtifactSet relink(TreeLogger logger, LinkerContext context, ArtifactSet newArtifacts) {
      return null;
    }
  }

  @LinkerOrder(Order.POST)
  @Shardable
  static class FakeLinkerPost extends FakeLinker {
  }

  @LinkerOrder(Order.POST)
  @Shardable
  static class FakeLinkerPost2 extends FakeLinker {
  }

  @LinkerOrder(Order.PRE)
  @Shardable
  static class FakeLinkerPre extends FakeLinker {
  }

  @LinkerOrder(Order.PRE)
  @Shardable
  static class FakeLinkerPre2 extends FakeLinker {
  }

  @LinkerOrder(Order.PRIMARY)
  @Shardable
  static class FakeLinkerPrimary extends FakeLinker {
  }

  @LinkerOrder(Order.PRIMARY)
  @Shardable
  static class FakeLinkerPrimary2 extends FakeLinker {
  }

  public void testCollapsedProperties() {
    ModuleDef def = new ModuleDef("fake");

    Properties p = def.getProperties();
    BindingProperty b = p.createBinding("fake");
    b.addDefinedValue(b.getRootCondition(), "a");
    b.addDefinedValue(b.getRootCondition(), "b");
    b.addDefinedValue(b.getRootCondition(), "c");
    b.addDefinedValue(b.getRootCondition(), "d");
    b.addDefinedValue(b.getRootCondition(), "e");
    b.addDefinedValue(b.getRootCondition(), "f1");
    b.addDefinedValue(b.getRootCondition(), "f2");
    b.addDefinedValue(b.getRootCondition(), "f3");
    b.addDefinedValue(b.getRootCondition(), "g1a");
    b.addDefinedValue(b.getRootCondition(), "g2a");
    b.addDefinedValue(b.getRootCondition(), "g1b");
    b.addDefinedValue(b.getRootCondition(), "g2b");

    // Check de-duplication
    b.addCollapsedValues("a", "b");
    b.addCollapsedValues("b", "a");

    // Check transitivity
    b.addCollapsedValues("c", "d");
    b.addCollapsedValues("c", "e");

    // Check globs
    b.addCollapsedValues("f*");
    b.addCollapsedValues("g*a");

    b.normalizeCollapsedValues();

    List<SortedSet<String>> collapsedValues = b.getCollapsedValues();
    assertEquals(4, collapsedValues.size());
    assertEquals(Arrays.asList("a", "b"), new ArrayList<String>(collapsedValues.get(0)));
    assertEquals(Arrays.asList("c", "d", "e"), new ArrayList<String>(collapsedValues.get(1)));
    assertEquals(Arrays.asList("f1", "f2", "f3"), new ArrayList<String>(collapsedValues.get(2)));
    assertEquals(Arrays.asList("g1a", "g2a"), new ArrayList<String>(collapsedValues.get(3)));

    // Collapse everything
    b.addCollapsedValues("*");
    b.normalizeCollapsedValues();

    collapsedValues = b.getCollapsedValues();
    assertEquals(1, collapsedValues.size());
    assertEquals(Arrays.asList(b.getDefinedValues()), new ArrayList<String>(collapsedValues.get(0)));
  }

  /**
   * This tests the behavior of properties that depend on other properties with
   * collapsed values. A dependency on a collapsed value forms an equivalent set
   * for the dependent values. In other words, collapsing is contagious.
   */
  public void testCollapsedPropertiesWithDependencies() {
    ModuleDef def = new ModuleDef("fake");
    Properties p = def.getProperties();
    // <define-property name="p1" values="a,b,c" />
    BindingProperty p1 = p.createBinding("p1");
    p1.addDefinedValue(p1.getRootCondition(), "a");
    p1.addDefinedValue(p1.getRootCondition(), "b");
    p1.addDefinedValue(p1.getRootCondition(), "c");
    // <collapse-property name="p1" values="b,c" />
    p1.addCollapsedValues("b", "c");

    BindingProperty p2 = p.createBinding("p2");
    // <define-property name="p2" values="d,e,f" />
    p2.addDefinedValue(p2.getRootCondition(), "d");
    p2.addDefinedValue(p2.getRootCondition(), "e");
    p2.addDefinedValue(p2.getRootCondition(), "f");
    /*-
     * <set-property name="p2" values="e,f">
     *  <when-property-is name="p1" value="c" />
     * </set-property>
     */
    p2.setAllowedValues(new ConditionWhenPropertyIs("p1", "c"), "e", "f");

    /*
     * The net effect of the above is to delete the [c,d] permutation and to
     * make the e and f values dependent on the c value. Because c is a
     * collapsed value, e and f are then automatically collapsed together when
     * p2 is normalized.
     */

    p1.normalizeCollapsedValues();
    p2.normalizeCollapsedValues();

    PropertyPermutations perms = new PropertyPermutations(p, Collections.<String> emptySet());
    List<PropertyPermutations> hardPerms = perms.collapseProperties();

    assertEquals(4, hardPerms.size());
    {
      // {[a,d]}
      PropertyPermutations perm = hardPerms.get(0);
      assertEquals(1, perm.size());
      assertEquals(Arrays.asList("a", "d"), Arrays.asList(perm.getOrderedPropertyValues(0)));
    }
    {
      // {[a,e], [a,f]}
      PropertyPermutations perm = hardPerms.get(1);
      assertEquals(2, perm.size());
      assertEquals(Arrays.asList("a", "e"), Arrays.asList(perm.getOrderedPropertyValues(0)));
      assertEquals(Arrays.asList("a", "f"), Arrays.asList(perm.getOrderedPropertyValues(1)));
    }
    {
      // {[b,d]}
      PropertyPermutations perm = hardPerms.get(2);
      assertEquals(1, perm.size());
      assertEquals(Arrays.asList("b", "d"), Arrays.asList(perm.getOrderedPropertyValues(0)));
    }
    {
      // {[b,e], [b,f], [c,e], [c,f]}
      PropertyPermutations perm = hardPerms.get(3);
      assertEquals(4, perm.size());
      assertEquals(Arrays.asList("b", "e"), Arrays.asList(perm.getOrderedPropertyValues(0)));
      assertEquals(Arrays.asList("b", "f"), Arrays.asList(perm.getOrderedPropertyValues(1)));
      assertEquals(Arrays.asList("c", "e"), Arrays.asList(perm.getOrderedPropertyValues(2)));
      assertEquals(Arrays.asList("c", "f"), Arrays.asList(perm.getOrderedPropertyValues(3)));
    }
  }

  public void testLinkerOrder() throws UnableToCompleteException {
    ModuleDef def = new ModuleDef("fake");

    def.defineLinker(TreeLogger.NULL, "pre", FakeLinkerPre.class);
    def.defineLinker(TreeLogger.NULL, "pre2", FakeLinkerPre2.class);
    def.defineLinker(TreeLogger.NULL, "post", FakeLinkerPost.class);
    def.defineLinker(TreeLogger.NULL, "post2", FakeLinkerPost2.class);
    def.defineLinker(TreeLogger.NULL, "primary", FakeLinkerPrimary.class);

    def.addLinker("pre2");
    def.addLinker("pre");
    def.addLinker("post");
    def.addLinker("post2");
    def.addLinker("primary");

    Class<?>[] expectedClasses =
        {
            FakeLinkerPre2.class, FakeLinkerPre.class, FakeLinkerPost.class, FakeLinkerPost2.class,
            FakeLinkerPrimary.class};
    assertEquals(FakeLinkerPrimary.class, def.getActivePrimaryLinker());
    // Test iteration order
    assertEquals(Arrays.asList(expectedClasses), new ArrayList<Class<? extends Linker>>(def
        .getActiveLinkers()));
  }

  public void testLinkerRedefinition() throws UnableToCompleteException {
    ModuleDef def = new ModuleDef("fake");

    def.defineLinker(TreeLogger.NULL, "pre", FakeLinkerPre.class);
    def.defineLinker(TreeLogger.NULL, "post", FakeLinkerPost.class);
    def.defineLinker(TreeLogger.NULL, "primary", FakeLinkerPrimary.class);
    def.addLinker("pre");
    def.addLinker("post");
    def.addLinker("primary");

    def.defineLinker(TreeLogger.NULL, "pre", FakeLinkerPre2.class);
    def.defineLinker(TreeLogger.NULL, "post", FakeLinkerPost2.class);
    def.defineLinker(TreeLogger.NULL, "primary", FakeLinkerPrimary2.class);
    // Intentional duplication
    def.addLinker("post");

    Class<?>[] expectedClasses =
        {FakeLinkerPre2.class, FakeLinkerPost2.class, FakeLinkerPrimary2.class};
    assertEquals(FakeLinkerPrimary2.class, def.getActivePrimaryLinker());
    // Test iteration order
    assertEquals(Arrays.asList(expectedClasses), new ArrayList<Class<? extends Linker>>(def
        .getActiveLinkers()));
  }

  public void testLinkerRedefinitionErrors() throws UnableToCompleteException {
    ModuleDef def = new ModuleDef("fake");

    def.defineLinker(TreeLogger.NULL, "pre", FakeLinkerPre.class);
    def.defineLinker(TreeLogger.NULL, "post", FakeLinkerPost.class);
    def.defineLinker(TreeLogger.NULL, "primary", FakeLinkerPrimary.class);
    def.addLinker("pre");
    def.addLinker("post");
    def.addLinker("primary");

    try {
      def.defineLinker(TreeLogger.NULL, "pre", FakeLinkerPrimary.class);
      fail();
    } catch (UnableToCompleteException e) {
      // OK
    }
    try {
      def.defineLinker(TreeLogger.NULL, "post", FakeLinkerPrimary.class);
      fail();
    } catch (UnableToCompleteException e) {
      // OK
    }
    try {
      def.defineLinker(TreeLogger.NULL, "primary", FakeLinkerPre.class);
      fail();
    } catch (UnableToCompleteException e) {
      // OK
    }
  }

  public void testTwoPrimaries() throws UnableToCompleteException {
    ModuleDef def = new ModuleDef("fake");

    def.defineLinker(TreeLogger.NULL, "pre", FakeLinkerPre.class);
    def.defineLinker(TreeLogger.NULL, "post", FakeLinkerPost.class);
    def.defineLinker(TreeLogger.NULL, "primary", FakeLinkerPrimary.class);
    def.defineLinker(TreeLogger.NULL, "primary2", FakeLinkerPrimary2.class);

    def.addLinker("pre");
    def.addLinker("post");
    def.addLinker("primary");
    def.addLinker("primary2");

    Class<?>[] expectedClasses =
        {FakeLinkerPre.class, FakeLinkerPost.class, FakeLinkerPrimary2.class};
    assertEquals(FakeLinkerPrimary2.class, def.getActivePrimaryLinker());
    // Test iteration order
    assertEquals(Arrays.asList(expectedClasses), new ArrayList<Class<? extends Linker>>(def
        .getActiveLinkers()));
  }

  public void testValidModuleName() {
    // Package names must contain valid Java identifiers.
    assertFalse(ModuleDef.isValidModuleName("com.foo.."));
    assertFalse(ModuleDef.isValidModuleName("com..Foo"));
    assertFalse(ModuleDef.isValidModuleName("com.7.Foo"));
    assertFalse(ModuleDef.isValidModuleName("com.7foo.Foo"));

    assertTrue(ModuleDef.isValidModuleName("com.foo.Foo"));
    assertTrue(ModuleDef.isValidModuleName("com.$foo.Foo"));
    assertTrue(ModuleDef.isValidModuleName("com._foo.Foo"));
    assertTrue(ModuleDef.isValidModuleName("com.foo7.Foo"));

    // For legacy reasons, allow the last part of the name is not
    // required to be a valid ident. In the past, naming rules
    // were enforced for top level modules, but not nested modules.
    assertTrue(ModuleDef.isValidModuleName("com.foo.F-oo"));
    assertTrue(ModuleDef.isValidModuleName("com.foo.7Foo"));
    assertTrue(ModuleDef.isValidModuleName("com.foo.+Foo"));
  }
}
