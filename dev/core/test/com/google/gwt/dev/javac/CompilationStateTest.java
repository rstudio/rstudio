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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.javac.impl.MockResourceOracle;
import com.google.gwt.dev.javac.impl.TweakedMockJavaResource;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests {@link CompilationState}.
 */
public class CompilationStateTest extends CompilationStateTestBase {

  public void testAddGeneratedCompilationUnit() {
    validateCompilationState();

    // Add a unit and ensure it shows up.
    addGeneratedUnits(JavaResourceBase.FOO);
    validateCompilationState(Shared.getTypeName(JavaResourceBase.FOO));

    rebuildCompilationState();
    validateCompilationState();
  }

  /* test that a generated unit, if unchanged, is reused */
  public void testCaching() {
    testCaching(JavaResourceBase.FOO);
  }

  /* test that mutiple generated units, if unchanged, are reused */
  public void testCachingOfMultipleUnits() {
    testCaching(JavaResourceBase.BAR, JavaResourceBase.FOO);
  }

  public void testCompileError() {
    oracle.add(JavaResourceBase.BAR);
    rebuildCompilationState();

    CompilationUnit badUnit = state.getCompilationUnitMap().get(
        Shared.getTypeName(JavaResourceBase.BAR));
    assertTrue(badUnit.isError());

    Set<CompilationUnit> goodUnits = new HashSet<CompilationUnit>(
        state.getCompilationUnits());
    goodUnits.remove(badUnit);
    assertUnitsChecked(goodUnits);
  }

  public void testCompileWithGeneratedUnits() {
    assertUnitsChecked(state.getCompilationUnits());
    addGeneratedUnits(JavaResourceBase.FOO);
    assertUnitsChecked(state.getCompilationUnits());
  }

  public void testCompileWithGeneratedUnitsError() {
    assertUnitsChecked(state.getCompilationUnits());
    addGeneratedUnits(JavaResourceBase.BAR);

    CompilationUnit badUnit = state.getCompilationUnitMap().get(
        Shared.getTypeName(JavaResourceBase.BAR));
    assertTrue(badUnit.isError());

    Set<CompilationUnit> goodUnits = new HashSet<CompilationUnit>(
        state.getCompilationUnits());
    goodUnits.remove(badUnit);
    assertUnitsChecked(goodUnits);
  }

  public void testCompileWithGeneratedUnitsErrorAndDepedentGeneratedUnit() {
    assertUnitsChecked(state.getCompilationUnits());
    MockJavaResource badFoo = new MockJavaResource(
        Shared.getTypeName(JavaResourceBase.FOO)) {
      @Override
      protected CharSequence getContent() {
        return Shared.readSource(JavaResourceBase.FOO)
            + "\ncompilation error LOL!";
      }
    };
    oracle.add(badFoo);
    rebuildCompilationState();
    addGeneratedUnits(JavaResourceBase.BAR);

    CompilationUnit badUnit = state.getCompilationUnitMap().get(
        Shared.getTypeName(badFoo));
    assertTrue(badUnit.isError());
    CompilationUnit invalidUnit = state.getCompilationUnitMap().get(
        Shared.getTypeName(JavaResourceBase.BAR));
    assertTrue(invalidUnit.isError());

    Set<CompilationUnit> goodUnits = new HashSet<CompilationUnit>(
        state.getCompilationUnits());
    goodUnits.remove(badUnit);
    goodUnits.remove(invalidUnit);
    assertUnitsChecked(goodUnits);
  }

  /*
   * test if things work correctly when a generated unit can't be reused, but
   * another generated unit it depends on can be reused
   */
  public void testComplexCacheInvalidation() {
    testCachingOverMultipleRefreshes(new MockJavaResource[] {
        JavaResourceBase.FOO, JavaResourceBase.BAR},
        new MockJavaResource[] {
            JavaResourceBase.FOO,
            new TweakedMockJavaResource(JavaResourceBase.BAR)},
        Collections.singleton(JavaResourceBase.FOO.getTypeName()));

  }

  public void testInitialization() {
    assertUnitsChecked(state.getCompilationUnits());
  }

  public void testInvalidation() {
    testCachingOverMultipleRefreshes(
        new MockJavaResource[] {JavaResourceBase.FOO},
        new MockJavaResource[] {new TweakedMockJavaResource(
            JavaResourceBase.FOO)}, Collections.<String> emptySet());
  }

  public void testInvalidationOfMultipleUnits() {
    testCachingOverMultipleRefreshes(new MockJavaResource[] {
        JavaResourceBase.FOO, JavaResourceBase.BAR}, new MockJavaResource[] {
        new TweakedMockJavaResource(JavaResourceBase.FOO),
        new TweakedMockJavaResource(JavaResourceBase.BAR)},
        Collections.<String> emptySet());
  }

  public void testInvalidationWhenSourceUnitsChange() {
    /*
     * Steps: (i) Check compilation state. (ii) Add generated units. (iii)
     * Change unit in source oracle. (iv) Refresh oracle. (v) Add same generated
     * units. (v) Check that there is no reuse.
     */
    validateCompilationState();
    oracle.add(JavaResourceBase.FOO);
    rebuildCompilationState();

    // add generated units
    addGeneratedUnits(JavaResourceBase.BAR);
    assertUnitsChecked(state.getCompilationUnits());
    CompilationUnit oldBar = state.getCompilationUnitMap().get(
        JavaResourceBase.BAR.getTypeName());
    assertNotNull(oldBar);

    // change unit in source oracle
    oracle.replace(new TweakedMockJavaResource(JavaResourceBase.FOO));
    rebuildCompilationState();

    /*
     * Add same generated units. Verify that the original units are not used.
     */
    addGeneratedUnits(JavaResourceBase.BAR);
    assertUnitsChecked(state.getCompilationUnits());

    CompilationUnit newBar = state.getCompilationUnitMap().get(
        JavaResourceBase.BAR.getTypeName());
    assertNotNull(newBar);
    assertNotSame(oldBar, newBar);
  }

  public void testSourceOracleAdd() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.add(JavaResourceBase.FOO);
    rebuildCompilationState();
    assertEquals(size + 1, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleBasic() {
    validateCompilationState();
  }

  public void testSourceOracleEmpty() {
    oracle = new MockResourceOracle();
    rebuildCompilationState();
    validateCompilationState();
  }

  public void testSourceOracleRemove() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.remove(JavaResourceBase.MAP.getPath());
    rebuildCompilationState();
    assertEquals(size - 1, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleReplace() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.replace(new TweakedMockJavaResource(JavaResourceBase.OBJECT));
    rebuildCompilationState();
    assertEquals(size, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleReplaceWithSame() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.replace(JavaResourceBase.OBJECT);
    rebuildCompilationState();
    assertEquals(size, state.getCompilationUnits().size());
    validateCompilationState();
  }

  /* test if generatedUnits that depend on stale generatedUnits are invalidated */
  public void testTransitiveInvalidation() {
    testCachingOverMultipleRefreshes(new MockJavaResource[] {
        JavaResourceBase.FOO, JavaResourceBase.BAR},
        new MockJavaResource[] {
            new TweakedMockJavaResource(JavaResourceBase.FOO),
            JavaResourceBase.BAR}, Collections.<String> emptySet());
  }

  private void testCaching(MockJavaResource... resources) {
    Set<String> reusedTypes = new HashSet<String>();
    for (MockJavaResource resource : resources) {
      reusedTypes.add(resource.getTypeName());
    }
    testCachingOverMultipleRefreshes(resources, resources, reusedTypes);
  }

  /**
   * Test caching logic for generated units during refreshes. Steps:
   * <ol>
   * <li>Verify that there were no generated units before</li>
   * <li>Add 'initialSet' generatedUnits over a refresh cycle</li>
   * <li>Add 'updatedSet' generatedUnits over a refresh cycle</li>
   * <li>Add 'updatedSet' generatedUnits over the second refresh cycle</li>
   * </ol>
   * 
   * @param initialSet CompilationUnits that are generated the first time.
   * @param updatedSet CompilationUnits that are generated the next time.
   * @param reusedTypes Main type of the units that can be reused between the
   *          initialSet and updatedSet.
   * @param numInvalidated Number of types invalidated from graveyardUnits.
   */
  private void testCachingOverMultipleRefreshes(MockJavaResource[] initialSet,
      MockJavaResource[] updatedSet, Set<String> reusedTypes) {

    // Add 'initialSet' generatedUnits on the first cycle.
    rebuildCompilationState();
    assertEquals(oracle.getResources().size(),
        state.getCompilationUnits().size());
    addGeneratedUnits(initialSet);
    Map<String, CompilationUnit> units1 = new HashMap<String, CompilationUnit>(
        state.getCompilationUnitMap());
    assertEquals(oracle.getResources().size() + initialSet.length,
        units1.size());
    assertUnitsChecked(units1.values());

    // Add 'updatedSet' generatedUnits on the second cycle.
    rebuildCompilationState();
    assertEquals(oracle.getResources().size(),
        state.getCompilationUnits().size());
    addGeneratedUnits(updatedSet);
    Map<String, CompilationUnit> units2 = new HashMap<String, CompilationUnit>(
        state.getCompilationUnitMap());
    assertEquals(oracle.getResources().size() + updatedSet.length,
        units2.size());
    assertUnitsChecked(units2.values());

    // Validate that only 'reusedTypes' are reused.
    for (MockJavaResource resource : updatedSet) {
      String typeName = resource.getTypeName();
      if (reusedTypes.contains(typeName)) {
        assertSame(units1.get(typeName), units2.get(typeName));
      } else {
        assertNotSame(units1.get(typeName), units2.get(typeName));
      }
    }

    // Add 'updatedSet' generatedUnits on the third cycle.
    rebuildCompilationState();
    assertEquals(oracle.getResources().size(),
        state.getCompilationUnits().size());
    addGeneratedUnits(updatedSet);
    Map<String, CompilationUnit> units3 = new HashMap<String, CompilationUnit>(
        state.getCompilationUnitMap());
    assertEquals(oracle.getResources().size() + updatedSet.length,
        units3.size());
    assertUnitsChecked(units3.values());

    // Validate that all generatedUnits are reused.
    for (MockJavaResource resource : updatedSet) {
      String typeName = resource.getTypeName();
      assertSame(units2.get(typeName), units3.get(typeName));
    }
  }
}
