/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.cfg;

import com.google.gwt.dev.cfg.LibraryGroup.DuplicateLibraryNameException;
import com.google.gwt.dev.cfg.LibraryGroup.UnresolvedLibraryException;
import com.google.gwt.dev.javac.MockCompilationUnit;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Tests for LibraryGroup.
 */
public class LibraryGroupTest extends TestCase {

  public void testCreateSubGroup() {
    LibraryGroup looseLibraryGroup = LibraryGroup.fromLibraries(Lists.<Library> newArrayList(
        new MockLibrary("LibraryA"), new MockLibrary("LibraryB"), new MockLibrary("LibraryC"),
        new MockLibrary("LibraryD")), true);

    assertEquals(4, looseLibraryGroup.getLibraries().size());

    LibraryGroup strictLibraryGroup =
        looseLibraryGroup.createSubgroup(Lists.newArrayList("LibraryA", "LibraryA", "LibraryC"));

    assertEquals(2, strictLibraryGroup.getLibraries().size());
  }

  public void testDuplicateLibraryNames() {
    // Create regular/super source libraries.
    MockLibrary fooLibrary = new MockLibrary("FooLibrary");
    MockLibrary alsoFooLibrary = new MockLibrary("FooLibrary");

    // Try but fail to stick them in a library group.
    try {
      LibraryGroup.fromLibraries(
          Lists.<Library> newArrayList(fooLibrary, alsoFooLibrary, fooLibrary), true);
      fail("library group construction sould have failed because of a library name collision.");
    } catch (DuplicateLibraryNameException e) {
      // Expected behavior.
    }
  }

  public void testGetCompilationUnitTypeNamesSeesAll() {
    // Create regular/super source compilation units.
    MockCompilationUnit regularCompilationUnit =
        new MockCompilationUnit("com.google.gwt.Regular", "blah");
    MockCompilationUnit superSourceCompilationUnit =
        new MockCompilationUnit("com.google.gwt.Super", "blah");

    // Create regular/super source libraries.
    MockLibrary regularLibrary = new MockLibrary("LibraryA");
    regularLibrary.addCompilationUnit(regularCompilationUnit);
    MockLibrary superSourceLibrary = new MockLibrary("LibraryB");
    superSourceLibrary.addSuperSourceCompilationUnit(superSourceCompilationUnit);

    // Stick them in a library group.
    LibraryGroup libraryGroup = LibraryGroup.fromLibraries(
        Lists.<Library> newArrayList(regularLibrary, superSourceLibrary), true);

    // Show that getCompilationUnitTypeNames sees both kinds of compilation units.
    assertEquals(libraryGroup.getCompilationUnitTypeNames(),
        Sets.newHashSet("com.google.gwt.Regular", "com.google.gwt.Super"));
  }

  public void testGetReboundTypeNames() {
    // Build a large random acyclic library graph.
    List<MockLibrary> libraries = MockLibrary.createRandomLibraryGraph(210, 3);
    // Stick the libraries into a library group.
    LibraryGroup libraryGroup = LibraryGroup.fromLibraries(libraries, true);

    // Insert a differently named rebound type into every library.
    Set<String> expectedReboundTypeNames = Sets.newHashSet();
    for (Library library : libraries) {
      String reboundTypeName = "Type" + System.identityHashCode(library);
      library.getReboundTypeNames().add(reboundTypeName);
      expectedReboundTypeNames.add(reboundTypeName);
    }

    // Show that the library group collects and returns them all.
    assertTrue(expectedReboundTypeNames.equals(libraryGroup.getReboundTypeNames()));
  }

  public void testLibraryLinkOrder() {
    // Build a large random acyclic library graph.
    List<MockLibrary> libraries = MockLibrary.createRandomLibraryGraph(210, 3);

    // Stick it in a LibraryGroup.
    LibraryGroup libraryGroup = LibraryGroup.fromLibraries(libraries, true);

    // Verify that each parent comes after all of its children after the LibraryGroup has performed
    // link ordering.
    List<Library> linkOrderLibraries = libraryGroup.getLibraries();
    for (Library parentLibrary : libraries) {
      int parentLibraryIndex = linkOrderLibraries.indexOf(parentLibrary);
      assertTrue(parentLibraryIndex != -1);
      Collection<Library> childLibraries =
          libraryGroup.getLibraries(parentLibrary.getDependencyLibraryNames());
      for (Library childLibrary : childLibraries) {
        int childLibraryIndex = linkOrderLibraries.indexOf(childLibrary);
        assertTrue(childLibraryIndex != -1);
        assertTrue(childLibraryIndex < parentLibraryIndex);
      }
    }
    assertEquals(libraries.size(), linkOrderLibraries.size());
  }

  /**
   * Test library tree looks like:
   *
   * <pre>
   * root library: {
   *   user.agent: [webkit_phone, webkit_tablet],
   *   locale: [ru]
   *   ranGenerators: [],
   *   libraries: [
   *     sub library 1: {
   *       user.agent: [webkit, mozilla, ie],
   *       locale: [],
   *       ranGenerators: [UserAgentAsserter]
   *     },
   *     sub library 2: {
   *       user.agent: [],
   *       locale: [en, fr],
   *       ranGenerators: [LocalizedDatePickerGenerator]
   *     }
   *   ]
   * }
   * </pre>
   */
  public void testPropertyCollectionByGenerator() {
    // A root library that adds more legal user.agent and locale values but for which no generators
    // have been run.
    MockLibrary rootLibrary = new MockLibrary("RootLibrary");
    rootLibrary.getDependencyLibraryNames().addAll(
        Lists.newArrayList("SubLibrary1", "SubLibrary2"));
    rootLibrary.getNewBindingPropertyValuesByName().putAll(
        "user.agent", Lists.newArrayList("webkit_phone", "webkit_tablet"));
    rootLibrary.getNewBindingPropertyValuesByName().putAll("locale", Lists.newArrayList("ru"));

    // A library that adds legal user.agent values and has already run the UserAgentAsserter
    // generator.
    MockLibrary subLibrary1 = new MockLibrary("SubLibrary1");
    subLibrary1.getNewBindingPropertyValuesByName().putAll(
        "user.agent", Lists.newArrayList("webkit", "mozilla", "ie"));
    subLibrary1.getRanGeneratorNames().add("UserAgentAsserter");

    // A library that adds legal locale values and has already run the LocaleMessageGenerator
    // generator.
    MockLibrary subLibrary2 = new MockLibrary("SubLibrary2");
    subLibrary2.getNewBindingPropertyValuesByName().putAll(
        "locale", Lists.newArrayList("en", "fr"));
    subLibrary2.getRanGeneratorNames().add("LocalizedDatePickerGenerator");

    LibraryGroup libraryGroup = LibraryGroup.fromLibraries(
        Lists.<Library> newArrayList(rootLibrary, subLibrary1, subLibrary2), true);

    // Collect "new" legal binding property values from the perspective of each generator.
    Collection<String> newUserAgentsForUserAgentAsserter =
        libraryGroup.gatherNewBindingPropertyValuesForGenerator("UserAgentAsserter").get(
            "user.agent");
    Collection<String> newUserAgentsForLocalizedDatePickerGenerator =
        libraryGroup.gatherNewBindingPropertyValuesForGenerator("LocalizedDatePickerGenerator").get(
            "user.agent");
    Collection<String> newLocalesForLocalizedDatePickerGenerator =
        libraryGroup.gatherNewBindingPropertyValuesForGenerator("LocalizedDatePickerGenerator").get(
            "locale");

    // Verify that the results are as expected.
    assertEquals(
        Sets.newHashSet("webkit_phone", "webkit_tablet"), newUserAgentsForUserAgentAsserter);
    assertEquals(Sets.newHashSet("webkit_phone", "webkit_tablet", "webkit", "mozilla", "ie"),
        newUserAgentsForLocalizedDatePickerGenerator);
    assertEquals(Sets.newHashSet("ru"), newLocalesForLocalizedDatePickerGenerator);
  }

  public void testSuperSourceOverridesRegularCompilationUnitAccess() {
    // Create regular/super source compilation units.
    MockCompilationUnit regularCompilationUnit =
        new MockCompilationUnit("com.google.gwt.Regular", "blah");
    MockCompilationUnit superSourceCompilationUnit =
        new MockCompilationUnit("com.google.gwt.Regular", "blah");

    // Create regular/super source libraries.
    MockLibrary regularLibrary = new MockLibrary("LibraryA");
    regularLibrary.addCompilationUnit(regularCompilationUnit);
    MockLibrary superSourceLibrary = new MockLibrary("LibraryB");
    superSourceLibrary.addSuperSourceCompilationUnit(superSourceCompilationUnit);

    // Stick them in a library group.
    LibraryGroup libraryGroup = LibraryGroup.fromLibraries(
        Lists.<Library> newArrayList(regularLibrary, superSourceLibrary), true);

    // Show that the library group prefers to return the super source version.
    assertEquals(libraryGroup.getCompilationUnitByTypeName("com.google.gwt.Regular"),
        superSourceCompilationUnit);
  }

  public void testUnresolvedLibraryReference() {
    // Create a library that references some library which is not available.
    MockLibrary library = new MockLibrary("RootLibrary");
    library.getDependencyLibraryNames().add("com.something.Missing");

    try {
      // Attempt to build a library group with strict library reference enforcement.
      LibraryGroup.fromLibraries(Lists.<Library> newArrayList(library), true);
      fail("Expected library group construction to fail on the missing referenced library.");
    } catch (UnresolvedLibraryException e) {
      // Expected behavior.
    }

    // Successfully build the library group when allowing missing references.
    LibraryGroup.fromLibraries(Lists.<Library> newArrayList(library), false);
  }
}
