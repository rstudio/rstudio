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
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;

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
    public ArtifactSet relink(TreeLogger logger, LinkerContext context,
        ArtifactSet newArtifacts) throws UnableToCompleteException {
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
        FakeLinkerPost2.class};
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

    Class<?>[] expectedClasses = {FakeLinkerPre2.class, FakeLinkerPost2.class};
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
}
