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
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import java.util.Set;

/**
 * Test for the module def loading
 */
public class ModuleDefLoaderTest extends TestCase {

  public void testErrorReporting_badXml() {
    assertErrorsWhenLoading("com.google.gwt.dev.cfg.testdata.errors.BadModule",
        "Line 3, column 1 : Element type \"inherits\" must be followed by either "
            + "attribute specifications, \">\" or \"/>\".");
  }

  public void testErrorReporting_badLinker() {
    assertErrorsWhenLoading("com.google.gwt.dev.cfg.testdata.errors.BadLinker",
        "Line 2: Invalid linker name 'X'");
  }

  public void testErrorReporting_badProperty() {
    assertErrorsWhenLoading("com.google.gwt.dev.cfg.testdata.errors.BadProperty",
        "Line 2: Property 'X' not found");
  }

  public void testErrorReporting_badPropertyValue() {
    assertErrorsWhenLoading("com.google.gwt.dev.cfg.testdata.errors.BadPropertyValue",
        "Line 3: Value 'z' in not a valid value for property 'X'");
  }

  public void testErrorReporting_deepError() {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.DEBUG);
    builder.expectDebug(
        "Loading inherited module 'com.google.gwt.dev.cfg.testdata.errors.DeepInheritsError0'",
        null);
    builder.expectDebug(
        "Loading inherited module 'com.google.gwt.dev.cfg.testdata.errors.DeepInheritsError1'",
        null);
    builder.expectDebug(
        "Loading inherited module 'com.google.gwt.dev.cfg.testdata.errors.BadModule'", null);
    builder.expectError("Line 3, column 1 : Element type \"inherits\" must be followed by either "
        + "attribute specifications, \">\" or \"/>\".", null);

    UnitTestTreeLogger logger = builder.createLogger();

    try {
      ModuleDefLoader.loadFromClassPath(
          logger, "com.google.gwt.dev.cfg.testdata.errors.DeepInheritsError0");
      fail("Should have failed to load module.");
    } catch (UnableToCompleteException e) {
      // failure is expected.
    }
    logger.assertLogEntriesContainExpected();
  }

  public void testErrorReporting_inheritNotFound() {
    assertErrorsWhenLoading("com.google.gwt.dev.cfg.testdata.errors.InheritNotFound",
        "Unable to find 'com/google/gwt/dev/cfg/testdata/NonExistentModule.gwt.xml' on your "
            + "classpath; could be a typo, or maybe you forgot to include a classpath entry "
            + "for source?");
  }

  public void testErrorReporting_invalidName() {
    assertErrorsWhenLoading("com.google.gwt.dev.cfg.testdata.errors.InvalidName",
        "Line 2: Invalid property name '123:33'");
  }

  public void testErrorReporting_multipleErrors() {
    assertErrorsWhenLoading("com.google.gwt.dev.cfg.testdata.errors.MultipleErrors",
        "Line 1: Unexpected attribute 'blah' in element 'module'");
  }

  public void testErrorReporting_unexpectedAttribute() {
    assertErrorsWhenLoading("com.google.gwt.dev.cfg.testdata.errors.UnexpectedAttribute",
        "Line 2: Unexpected attribute 'blah' in element 'inherits'");
  }

  public void testErrorReporting_unexpectedTag() {
    assertErrorsWhenLoading("com.google.gwt.dev.cfg.testdata.errors.UnexpectedTag",
        "Line 2: Unexpected element 'inherited'");
  }

  /**
   * Test of merging multiple modules in the same package space.
   * This exercises the interaction of include, exclude, and skip attributes.
   */
  public void testModuleMerging() throws Exception {
    TreeLogger logger = TreeLogger.NULL;
    ModuleDef one = ModuleDefLoader.loadFromClassPath(
        logger, "com.google.gwt.dev.cfg.testdata.merging.One");
    assertNotNull(one.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InOne.java"));
    assertNotNull(one.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Shared.java"));
    assertNull(one.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InTwo.java"));
    assertNull(one.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Toxic.java"));

    ModuleDef two = ModuleDefLoader.loadFromClassPath(
        logger, "com.google.gwt.dev.cfg.testdata.merging.Two");
    assertNotNull(two.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InOne.java"));
    assertNotNull(two.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Shared.java"));
    assertNotNull(two.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/InTwo.java"));
    assertNull(two.findSourceFile("com/google/gwt/dev/cfg/testdata/merging/client/Toxic.java"));

    ModuleDef three = ModuleDefLoader.loadFromClassPath(
        logger, "com.google.gwt.dev.cfg.testdata.merging.Three");
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
          logger, "com.google.gwt.dev.cfg.testdata.naming.Invalid..Foo");
      fail("Expected exception from invalid module name.");
    } catch (UnableToCompleteException expected) {
    }
    logger.assertLogEntriesContainExpected();
  }

  public void testModuleNamingValid() throws Exception {
    TreeLogger logger = TreeLogger.NULL;

    ModuleDef module;
    module = ModuleDefLoader.loadFromClassPath(
        logger, "com.google.gwt.dev.cfg.testdata.naming.Foo-test");
    assertNotNull(module.findSourceFile("com/google/gwt/dev/cfg/testdata/naming/client/Mock.java"));

    module = ModuleDefLoader.loadFromClassPath(
        logger, "com.google.gwt.dev.cfg.testdata.naming.7Foo");
    assertNotNull(module.findSourceFile("com/google/gwt/dev/cfg/testdata/naming/client/Mock.java"));

    module = ModuleDefLoader.loadFromClassPath(
        logger, "com.google.gwt.dev.cfg.testdata.naming.Nested7Foo");
    assertNotNull(module.findSourceFile("com/google/gwt/dev/cfg/testdata/naming/client/Mock.java"));

    module = ModuleDefLoader.loadFromClassPath(
        logger, "com.google.gwt.dev.cfg.testdata.naming.Nested7Foo");
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
          "com.google.gwt.dev.cfg.testdata.naming.NestedInvalid", false);
      fail("Expected exception from invalid module name.");
    } catch (UnableToCompleteException expected) {
    }
    logger.assertLogEntriesContainExpected();
  }

  public void testResourcesVisible() throws Exception {
    TreeLogger logger = TreeLogger.NULL;
    ModuleDef one = ModuleDefLoader.loadFromClassPath(logger,
        "com.google.gwt.dev.cfg.testdata.merging.One");

    Set<String> visibleResourcePaths = one.getBuildResourceOracle().getPathNames();
    // Sees the logo.png image.
    assertTrue(visibleResourcePaths.contains(
        "com/google/gwt/dev/cfg/testdata/merging/resources/logo.png"));
    // But not the java source file.
    assertFalse(visibleResourcePaths.contains(
        "com/google/gwt/dev/cfg/testdata/merging/resources/NotAResource.java"));
  }

  public void testWritesTargetLibraryProperties() throws UnableToCompleteException {
    ModuleDef libraryOneModule = ModuleDefLoader.loadFromClassPath(TreeLogger.NULL,
        "com.google.gwt.dev.cfg.testdata.separate.libraryone.LibraryOne", false);

    // Library one sees all defined values for the "libraryTwoProperty" binding property and knows
    // which one was defined in this target library.
    for (BindingProperty bindingProperty :
        libraryOneModule.getProperties().getBindingProperties()) {
      if (!bindingProperty.getName().equals("libraryTwoProperty")) {
        continue;
      }
      assertEquals(Sets.newHashSet(bindingProperty.getDefinedValues()),
          Sets.newHashSet("yes", "no", "maybe"));
    }

    // Library one sees all defined values for the "libraryTwoConfigProperty" property and knows
    // which one was defined in this target library.
    for (ConfigurationProperty configurationProperty :
        libraryOneModule.getProperties().getConfigurationProperties()) {
      if (!configurationProperty.getName().equals("libraryTwoConfigProperty")) {
        continue;
      }
      assertEquals(Sets.newHashSet(configurationProperty.getValues()), Sets.newHashSet("false"));
    }
  }

  private void assertErrorsWhenLoading(String moduleName, String... errorMessages) {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);
    for (String errorMessage : errorMessages) {
      builder.expectError(errorMessage, null);

      UnitTestTreeLogger logger = builder.createLogger();

      try {
        ModuleDefLoader.loadFromClassPath(logger, moduleName);
        fail("Should have failed to load module.");
      } catch (UnableToCompleteException e) {
        // failure is expected.
      }
      logger.assertCorrectLogEntries();
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ModuleDefLoader.getModulesCache().clear();
  }
}
