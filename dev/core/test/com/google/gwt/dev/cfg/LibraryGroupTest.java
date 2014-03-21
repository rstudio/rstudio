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

import com.google.gwt.dev.cfg.LibraryGroup.CollidingCompilationUnitException;
import com.google.gwt.dev.cfg.LibraryGroup.DuplicateLibraryNameException;
import com.google.gwt.dev.cfg.LibraryGroup.UnresolvedLibraryException;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.javac.MockCompilationUnit;
import com.google.gwt.dev.javac.MockCompiledClass;
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

  public void testGetCompilationUnitByTypeNamesSeesAllNested() {
    // Create a nested compilation units.
    MockCompilationUnit nestedTypeCompilationUnit =
        new MockCompilationUnit("com.google.gwt.user.Outer", "superblah") {
          @Override
          public Collection<CompiledClass> getCompiledClasses() {
            MockCompiledClass outerCompiledClass = new MockCompiledClass(null,
                "com/google/gwt/user/Outer", "com.google.gwt.user.Outer");
            MockCompiledClass innerCompiledClass = new MockCompiledClass(outerCompiledClass,
                "com/google/gwt/user/Outer$Inner", "com.google.gwt.user.Outer.Inner");
            return Lists.<CompiledClass> newArrayList(outerCompiledClass, innerCompiledClass);
          }
        };

    // Create the library.
    MockLibrary regularLibrary = new MockLibrary("LibraryA");
    regularLibrary.addCompilationUnit(nestedTypeCompilationUnit);

    // Stick it in a library group.
    LibraryGroup libraryGroup =
        LibraryGroup.fromLibraries(Lists.<Library> newArrayList(regularLibrary), true);

    // Shows that get by source or binary name works for nested types.
    assertEquals(nestedTypeCompilationUnit,
        libraryGroup.getCompilationUnitByTypeSourceName("com.google.gwt.user.Outer.Inner"));
    assertEquals(nestedTypeCompilationUnit,
        libraryGroup.getCompilationUnitByTypeBinaryName("com.google.gwt.user.Outer$Inner"));
  }

  public void testGetCompilationUnitTypeNamesSourceNamesSeesAll() {
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

    // Show that getCompilationUnitTypeSourceNames sees both kinds of compilation units.
    assertEquals(libraryGroup.getCompilationUnitTypeSourceNames(),
        Sets.newHashSet("com.google.gwt.Regular", "com.google.gwt.Super"));
  }

  public void testGetReboundTypeSourceNames() {
    // Build a large random acyclic library graph.
    List<MockLibrary> libraries = MockLibrary.createRandomLibraryGraph(210, 3);
    // Stick the libraries into a library group.
    LibraryGroup libraryGroup = LibraryGroup.fromLibraries(libraries, true);

    // Insert a differently named rebound type into every library.
    Set<String> expectedReboundTypeSourceNames = Sets.newHashSet();
    for (Library library : libraries) {
      String reboundTypeSourceName = "Type" + System.identityHashCode(library);
      library.getReboundTypeSourceNames().add(reboundTypeSourceName);
      expectedReboundTypeSourceNames.add(reboundTypeSourceName);
    }

    // Show that the library group collects and returns them all.
    assertTrue(expectedReboundTypeSourceNames.equals(libraryGroup.getReboundTypeSourceNames()));
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
   * See buildVariedPropertyGeneratorLibraryGroup() for test library group structure.
   */
  public void testPropertyCollectionByGenerator() {
    LibraryGroup libraryGroup = buildVariedPropertyGeneratorLibraryGroup();

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

  /**
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
  private LibraryGroup buildVariedPropertyGeneratorLibraryGroup() {
    String generatorNameOne = "UserAgentAsserter";
    String generatorNameTwo = "LocalizedDatePickerGenerator";
    return buildVariedPropertyGeneratorLibraryGroup(
        generatorNameOne, Sets.<String>newHashSet(), generatorNameTwo, Sets.<String>newHashSet());
  }

  public static LibraryGroup buildVariedPropertyGeneratorLibraryGroup(String generatorNameOne,
      Set<String> reboundTypeSourceNamesOne, String generatorNameTwo,
      Set<String> reboundTypeSourceNamesTwo) {
    // A root library that adds more legal user.agent and locale values but for which no generators
    // have been run.
    MockLibrary rootLibrary = new MockLibrary("RootLibrary");
    rootLibrary.getDependencyLibraryNames()
        .addAll(Lists.newArrayList("SubLibrary1", "SubLibrary2"));
    rootLibrary.getNewBindingPropertyValuesByName()
        .putAll("user.agent", Lists.newArrayList("webkit_phone", "webkit_tablet"));
    rootLibrary.getNewBindingPropertyValuesByName().putAll("locale", Lists.newArrayList("ru"));

    // A library that adds legal user.agent values and has already run the UserAgentAsserter
    // generator.
    MockLibrary subLibrary1 = new MockLibrary("SubLibrary1");
    subLibrary1.getNewBindingPropertyValuesByName()
        .putAll("user.agent", Lists.newArrayList("webkit", "mozilla", "ie"));
    subLibrary1.getRanGeneratorNames().add(generatorNameOne);
    subLibrary1.getReboundTypeSourceNames().addAll(reboundTypeSourceNamesOne);

    // A library that adds legal locale values and has already run the LocaleMessageGenerator
    // generator.
    MockLibrary subLibrary2 = new MockLibrary("SubLibrary2");
    subLibrary2.getNewBindingPropertyValuesByName()
        .putAll("locale", Lists.newArrayList("en", "fr"));
    subLibrary2.getRanGeneratorNames().add(generatorNameTwo);
    subLibrary2.getReboundTypeSourceNames().addAll(reboundTypeSourceNamesTwo);

    LibraryGroup libraryGroup = LibraryGroup.fromLibraries(
        Lists.<Library>newArrayList(rootLibrary, subLibrary1, subLibrary2), true);
    return libraryGroup;
  }

  public void testEnforcesUniqueCompilationUnits() {
    MockCompilationUnit compilationUnit = new MockCompilationUnit("com.google.gwt.Regular", "blah");

    // Creates libraries with colliding compilation units.
    MockLibrary libraryA = new MockLibrary("LibraryA");
    libraryA.addCompilationUnit(compilationUnit);
    MockLibrary libraryB = new MockLibrary("LibraryB");
    libraryB.addCompilationUnit(compilationUnit);

    // Stick them in a library group.
    LibraryGroup libraryGroup =
        LibraryGroup.fromLibraries(Lists.<Library> newArrayList(libraryA, libraryB), true);

    // Show that the library group catches the duplication.
    try {
      libraryGroup.getCompilationUnitByTypeSourceName("com.google.gwt.Regular");
      fail("The library group should have detected and rejected the duplicate compilation unit.");
    } catch (CollidingCompilationUnitException e) {
      // expected behavior
    }
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
