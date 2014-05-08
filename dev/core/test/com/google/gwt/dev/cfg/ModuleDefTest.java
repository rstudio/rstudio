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
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
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
    public ArtifactSet link(TreeLogger logger, LinkerContext context,
        ArtifactSet artifacts, boolean onePermutation) {
      return null;
    }

    @Override
    public ArtifactSet relink(TreeLogger logger, LinkerContext context,
        ArtifactSet newArtifacts) {
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
    assertEquals(Arrays.asList("a", "b"), new ArrayList<String>(
        collapsedValues.get(0)));
    assertEquals(Arrays.asList("c", "d", "e"), new ArrayList<String>(
        collapsedValues.get(1)));
    assertEquals(Arrays.asList("f1", "f2", "f3"), new ArrayList<String>(
        collapsedValues.get(2)));
    assertEquals(Arrays.asList("g1a", "g2a"), new ArrayList<String>(
        collapsedValues.get(3)));

    // Collapse everything
    b.addCollapsedValues("*");
    b.normalizeCollapsedValues();

    collapsedValues = b.getCollapsedValues();
    assertEquals(1, collapsedValues.size());
    assertEquals(Arrays.asList(b.getDefinedValues()), new ArrayList<String>(
        collapsedValues.get(0)));
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

    Class<?>[] expectedClasses = {
        FakeLinkerPre2.class, FakeLinkerPre.class, FakeLinkerPost.class,
        FakeLinkerPost2.class, FakeLinkerPrimary.class};
    assertEquals(FakeLinkerPrimary.class, def.getActivePrimaryLinker());
    // Test iteration order
    assertEquals(Arrays.asList(expectedClasses),
        new ArrayList<Class<? extends Linker>>(def.getActiveLinkers()));
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

    Class<?>[] expectedClasses = {
        FakeLinkerPre2.class, FakeLinkerPost2.class, FakeLinkerPrimary2.class};
    assertEquals(FakeLinkerPrimary2.class, def.getActivePrimaryLinker());
    // Test iteration order
    assertEquals(Arrays.asList(expectedClasses),
        new ArrayList<Class<? extends Linker>>(def.getActiveLinkers()));
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

  public void testGetTransitiveDeps() {
    ModuleDef module = new ModuleDef("Level1");

    module.addDirectDependency("Level1", "Level2Left");
    module.addDirectDependency("Level1", "Level2Right");

    module.addDirectDependency("Level2Left", "Level3LeftCircular");

    // Creates (Left <-> Middle <-> Right), circular references and a sprinkling of extra deps.
    {
      module.addDirectDependency("Level3LeftCircular", "BranchA");
      module.addDirectDependency("Level3LeftCircular", "Level3MiddleCircular");
      module.addDirectDependency("Level3LeftCircular", "BranchB");

      module.addDirectDependency("Level3MiddleCircular", "BranchC");
      module.addDirectDependency("Level3MiddleCircular", "Level3LeftCircular");
      module.addDirectDependency("Level3MiddleCircular", "Level3RightCircular");
      module.addDirectDependency("Level3MiddleCircular", "BranchD");

      module.addDirectDependency("Level3RightCircular", "BranchE");
      module.addDirectDependency("Level3RightCircular", "Level3MiddleCircular");
      module.addDirectDependency("Level3RightCircular", "BranchF");
    }

    module.addDirectDependency("BranchA", "LeafA");
    module.addDirectDependency("BranchB", "LeafB");
    module.addDirectDependency("BranchC", "LeafC");
    module.addDirectDependency("BranchD", "LeafD");
    module.addDirectDependency("BranchE", "LeafE");
    module.addDirectDependency("BranchF", "LeafF");

    assertEquals(Sets.newHashSet("Level1", "Level2Left", "Level2Right",
        "Level3LeftCircular", "Level3MiddleCircular", "Level3RightCircular",
        "BranchA", "BranchB", "BranchC", "BranchD", "BranchE", "BranchF",
        "LeafA", "LeafB", "LeafC", "LeafD", "LeafE", "LeafF"),
        module.getTransitiveDepModuleNames("Level1"));

    assertEquals(Sets.newHashSet("Level2Left", "Level3LeftCircular",
        "Level3MiddleCircular", "Level3RightCircular", "BranchA", "BranchB",
        "BranchC", "BranchD", "BranchE", "BranchF", "LeafA", "LeafB", "LeafC",
        "LeafD", "LeafE", "LeafF"),
        module.getTransitiveDepModuleNames("Level2Left"));

    assertEquals(Sets.newHashSet("Level2Right"),
        module.getTransitiveDepModuleNames("Level2Right"));

    assertEquals(Sets.newHashSet("Level3LeftCircular", "Level3MiddleCircular",
        "Level3RightCircular", "BranchA", "BranchB", "BranchC", "BranchD",
        "BranchE", "BranchF", "LeafA", "LeafB", "LeafC", "LeafD", "LeafE",
        "LeafF"), module.getTransitiveDepModuleNames("Level3LeftCircular"));

    assertEquals(Sets.newHashSet("Level3LeftCircular", "Level3MiddleCircular",
        "Level3RightCircular", "BranchA", "BranchB", "BranchC", "BranchD",
        "BranchE", "BranchF", "LeafA", "LeafB", "LeafC", "LeafD", "LeafE",
        "LeafF"), module.getTransitiveDepModuleNames("Level3MiddleCircular"));

    assertEquals(Sets.newHashSet("Level3LeftCircular", "Level3MiddleCircular",
        "Level3RightCircular", "BranchA", "BranchB", "BranchC", "BranchD",
        "BranchE", "BranchF", "LeafA", "LeafB", "LeafC", "LeafD", "LeafE",
        "LeafF"), module.getTransitiveDepModuleNames("Level3RightCircular"));
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

    Class<?>[] expectedClasses = {
        FakeLinkerPre.class, FakeLinkerPost.class, FakeLinkerPrimary2.class};
    assertEquals(FakeLinkerPrimary2.class, def.getActivePrimaryLinker());
    // Test iteration order
    assertEquals(Arrays.asList(expectedClasses),
        new ArrayList<Class<? extends Linker>>(def.getActiveLinkers()));
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
    // required to be a valid ident.  In the past, naming rules
    // were enforced for top level modules, but not nested modules.
    assertTrue(ModuleDef.isValidModuleName("com.foo.F-oo"));
    assertTrue(ModuleDef.isValidModuleName("com.foo.7Foo"));
    assertTrue(ModuleDef.isValidModuleName("com.foo.+Foo"));
  }
}
