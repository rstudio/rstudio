/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

/**
 * Test for the module def loading
 */
public class ModuleDefLoaderTest extends TestCase {

  private MockLibraryWriter mockLibraryWriter = new MockLibraryWriter();
  private CompilerContext compilerContext =
      new CompilerContext.Builder().libraryWriter(mockLibraryWriter).build();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ModuleDefLoader.getModulesCache().clear();
  }

  public void assertHonorsStrictResources(boolean strictResources)
      throws UnableToCompleteException {
    TreeLogger logger = TreeLogger.NULL;
    compilerContext.getOptions().setEnforceStrictResources(strictResources);
    ModuleDef emptyModule = ModuleDefLoader.loadFromClassPath(
        logger, compilerContext, "com.google.gwt.dev.cfg.testdata.merging.Empty");
    Resource sourceFile =
        emptyModule.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InOne.java");
    Resource publicFile = emptyModule.findPublicFile("Public.java");
    if (strictResources) {
      // Empty.gwt.xml did not register any source or public paths and the strictResource setting is
      // blocking the implicit addition of any default entries. So these resource searches should
      // fail.
      assertNull(sourceFile);
      assertNull(publicFile);
    } else {
      assertNotNull(sourceFile);
      assertNotNull(publicFile);
    }
  }

  public void testAllowsImpreciseResources() throws Exception {
    assertHonorsStrictResources(false);
  }

  /**
   * Test of merging multiple modules in the same package space.
   * This exercises the interaction of include, exclude, and skip attributes.
   */
  public void testModuleMerging() throws Exception {
    TreeLogger logger = TreeLogger.NULL;
    ModuleDef one = ModuleDefLoader.loadFromClassPath(
        logger, compilerContext, "com.google.gwt.dev.cfg.testdata.merging.One");
    assertNotNull(one.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InOne.java"));
    assertNotNull(one.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Shared.java"));
    assertNull(one.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InTwo.java"));
    assertNull(one.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Toxic.java"));

    ModuleDef two = ModuleDefLoader.loadFromClassPath(
        logger, compilerContext, "com.google.gwt.dev.cfg.testdata.merging.Two");
    assertNotNull(two.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InOne.java"));
    assertNotNull(two.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Shared.java"));
    assertNotNull(two.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InTwo.java"));
    assertNull(two.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Toxic.java"));

    ModuleDef three = ModuleDefLoader.loadFromClassPath(
        logger, compilerContext, "com.google.gwt.dev.cfg.testdata.merging.Three");
    assertNotNull(three.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InOne.java"));
    assertNotNull(three.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Shared.java"));
    assertNull(three.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InTwo.java"));
    assertNull(three.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Toxic.java"));
  }

  /**
   * The top level module has an invalid name.
   */
  public void testModuleNamingInvalid() {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    builder.expectError("Invalid module name: 'com.google.gwt.dev.cfg.testdata.naming.Invalid..Foo'", null);
    UnitTestTreeLogger logger = builder.createLogger();
    try {
      ModuleDefLoader.loadFromClassPath(
          logger, compilerContext, "com.google.gwt.dev.cfg.testdata.naming.Invalid..Foo");
      fail("Expected exception from invalid module name.");
    } catch (UnableToCompleteException expected) {
    }
    logger.assertLogEntriesContainExpected();
  }

  public void testModuleNamingValid() throws Exception {
    TreeLogger logger = TreeLogger.NULL;

    ModuleDef module;
    module = ModuleDefLoader.loadFromClassPath(
        logger, compilerContext, "com.google.gwt.dev.cfg.testdata.naming.Foo-test");
    assertNotNull(module.findSourceFile("com/google/gwt/dev/cfg/testdata/naming/client/Mock.java"));

    module = ModuleDefLoader.loadFromClassPath(
        logger, compilerContext, "com.google.gwt.dev.cfg.testdata.naming.7Foo");
    assertNotNull(module.findSourceFile("com/google/gwt/dev/cfg/testdata/naming/client/Mock.java"));

    module = ModuleDefLoader.loadFromClassPath(
        logger, compilerContext, "com.google.gwt.dev.cfg.testdata.naming.Nested7Foo");
    assertNotNull(module.findSourceFile("com/google/gwt/dev/cfg/testdata/naming/client/Mock.java"));

    module = ModuleDefLoader.loadFromClassPath(
        logger, compilerContext, "com.google.gwt.dev.cfg.testdata.naming.Nested7Foo");
    assertNotNull(module.findSourceFile("com/google/gwt/dev/cfg/testdata/naming/client/Mock.java"));
  }

  /**
   * The top level module has a valid name, but the inherited one does not.
   */
  public void testModuleNestedNamingInvalid() {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    builder.expectError("Invalid module name: 'com.google.gwt.dev.cfg.testdata.naming.Invalid..Foo'", null);
    UnitTestTreeLogger logger = builder.createLogger();
    try {
      ModuleDefLoader.loadFromClassPath(logger,
          compilerContext, "com.google.gwt.dev.cfg.testdata.naming.NestedInvalid", false, true);
      fail("Expected exception from invalid module name.");
    } catch (UnableToCompleteException expected) {
    }
    logger.assertLogEntriesContainExpected();
  }

  public void testRequiresStrictResources() throws Exception {
    assertHonorsStrictResources(true);
  }

  public void testSeparateRootFilesetFail() {
    TreeLogger logger = TreeLogger.NULL;
    try {
      ModuleDefLoader.loadFromClassPath(logger,
          compilerContext, "com.google.gwt.dev.cfg.testdata.separate.filesetone.FileSetOne", false,
          false);
      fail("Expected a fileset loaded as the root of a module tree to fail, but it didn't.");
    } catch (UnableToCompleteException e) {
      // Expected behavior.
    }
  }

  public void testSeparateLibraryName() throws UnableToCompleteException {
    ModuleDefLoader.loadFromClassPath(TreeLogger.NULL, compilerContext,
        "com.google.gwt.dev.cfg.testdata.separate.libraryone.LibraryOne", false, false);

    assertEquals("com.google.gwt.dev.cfg.testdata.separate.libraryone.LibraryOne",
        mockLibraryWriter.getLibraryName());
  }

  public void testSeparateLibraryModuleReferences() throws UnableToCompleteException {
    ModuleDefLoader.loadFromClassPath(TreeLogger.NULL, compilerContext,
        "com.google.gwt.dev.cfg.testdata.separate.libraryone.LibraryOne", false, false);

    // The library writer was given the module and it's direct fileset module xml files as build
    // resources.
    assertEquals(Sets.newHashSet(
        "com/google/gwt/dev/cfg/testdata/separate/filesetone/FileSetOne.gwt.xml",
        "com/google/gwt/dev/cfg/testdata/separate/libraryone/LibraryOne.gwt.xml"),
        mockLibraryWriter.getBuildResourcePaths());
    // The library writer was given LibraryTwo as a dependency library.
    assertEquals(Sets.newHashSet("com.google.gwt.dev.cfg.testdata.separate.librarytwo.LibraryTwo"),
        mockLibraryWriter.getDependencyLibraryNames());
  }

  public void testSeparateModuleReferences() throws UnableToCompleteException {
    ModuleDef libraryOneModule = ModuleDefLoader.loadFromClassPath(TreeLogger.NULL, compilerContext,
        "com.google.gwt.dev.cfg.testdata.separate.libraryone.LibraryOne", false, false);

    // The module sees itself and it's direct fileset module as "target" modules.
    assertEquals(Sets.newHashSet("com.google.gwt.dev.cfg.testdata.separate.libraryone.LibraryOne",
        "com.google.gwt.dev.cfg.testdata.separate.filesetone.FileSetOne"),
        libraryOneModule.getTargetLibraryModuleNames());
    // The module sees the referenced library module as a "library" module.
    assertEquals(Sets.newHashSet("com.google.gwt.dev.cfg.testdata.separate.librarytwo.LibraryTwo"),
        libraryOneModule.getExternalLibraryModuleNames());
  }

  public void testSeparateModuleResourcesLibraryOne() throws UnableToCompleteException {
    ModuleDef libraryOneModule = ModuleDefLoader.loadFromClassPath(TreeLogger.NULL, compilerContext,
        "com.google.gwt.dev.cfg.testdata.separate.libraryone.LibraryOne", false, false);

    // Includes own source.
    assertNotNull(libraryOneModule.findSourceFile(
        "com/google/gwt/dev/cfg/testdata/separate/libraryone/client/LibraryOne.java"));
    // Cascades to include the subtree of fileset sources.
    assertNotNull(libraryOneModule.findSourceFile(
        "com/google/gwt/dev/cfg/testdata/separate/filesetone/client/FileSetOne.java"));
    // Does not include source from referenced libraries.
    assertNull(libraryOneModule.findSourceFile(
        "com/google/gwt/dev/cfg/testdata/separate/librarytwo/client/LibraryTwo.java"));
  }
}
