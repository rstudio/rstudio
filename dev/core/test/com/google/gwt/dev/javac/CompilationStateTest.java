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

import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.javac.impl.MockResourceOracle;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;

import java.util.Collections;
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
    validateCompilationState(SourceFileCompilationUnit.getTypeName(JavaResourceBase.FOO));

    // Ensure it disappears after a refresh.
    state.refresh(createTreeLogger());
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
    state.refresh(createTreeLogger());

    CompilationUnit badUnit = state.getCompilationUnitMap().get(
        SourceFileCompilationUnit.getTypeName(JavaResourceBase.BAR));
    assertSame(State.ERROR, badUnit.getState());

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
        SourceFileCompilationUnit.getTypeName(JavaResourceBase.BAR));
    assertSame(State.ERROR, badUnit.getState());

    Set<CompilationUnit> goodUnits = new HashSet<CompilationUnit>(
        state.getCompilationUnits());
    goodUnits.remove(badUnit);
    assertUnitsChecked(goodUnits);
  }

  public void testCompileWithGeneratedUnitsErrorAndDepedentGeneratedUnit() {
    assertUnitsChecked(state.getCompilationUnits());
    MockJavaResource badFoo = new MockJavaResource(
        SourceFileCompilationUnit.getTypeName(JavaResourceBase.FOO)) {
      @Override
      protected CharSequence getContent() {
        return SourceFileCompilationUnit.readSource(JavaResourceBase.FOO)
            + "\ncompilation error LOL!";
      }
    };
    state.addGeneratedCompilationUnits(createTreeLogger(), getCompilationUnits(
        badFoo, JavaResourceBase.BAR));

    CompilationUnit badUnit = state.getCompilationUnitMap().get(
        SourceFileCompilationUnit.getTypeName(badFoo));
    assertSame(State.ERROR, badUnit.getState());
    CompilationUnit invalidUnit = state.getCompilationUnitMap().get(
        SourceFileCompilationUnit.getTypeName(JavaResourceBase.BAR));
    assertSame(State.FRESH, invalidUnit.getState());

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
    Set<CompilationUnit> modifiedUnits = getCompilationUnits(JavaResourceBase.FOO);
    modifiedUnits.addAll(getModifiedCompilationUnits(JavaResourceBase.BAR));
    Set<String> reusedTypes = new HashSet<String>();
    reusedTypes.add(SourceFileCompilationUnit.getTypeName(JavaResourceBase.FOO));
    testCachingOverMultipleRefreshes(getCompilationUnits(JavaResourceBase.FOO,
        JavaResourceBase.BAR), modifiedUnits, reusedTypes, 1);
  }

  public void testInitialization() {
    assertUnitsChecked(state.getCompilationUnits());
  }

  public void testInvalidation() {
    testCachingOverMultipleRefreshes(getCompilationUnits(JavaResourceBase.FOO),
        getModifiedCompilationUnits(JavaResourceBase.FOO),
        Collections.<String> emptySet(), 1);
  }

  public void testInvalidationOfMultipleUnits() {
    testCachingOverMultipleRefreshes(getCompilationUnits(JavaResourceBase.BAR,
        JavaResourceBase.FOO), getModifiedCompilationUnits(
        JavaResourceBase.BAR, JavaResourceBase.FOO),
        Collections.<String> emptySet(), 2);
  }

  /*
   * Steps: (i) Check compilation state. (ii) Add generated units. (iii) Change
   * unit in source oracle. (iv) Refresh oracle. (v) Add same generated units.
   * (v) Check that there is no reuse.
   */
  public void testInvalidationWhenSourceUnitsChange() {
    validateCompilationState();
    oracle.add(JavaResourceBase.FOO);
    state.refresh(createTreeLogger());

    // add generated units
    Set<CompilationUnit> generatedCups = getCompilationUnits(JavaResourceBase.BAR);
    Map<String, CompilationUnit> usefulUnits = state.getUsefulGraveyardUnits(generatedCups);
    assertEquals(0, usefulUnits.size());
    state.addGeneratedCompilationUnits(createTreeLogger(), generatedCups,
        usefulUnits);
    assertUnitsChecked(state.getCompilationUnits());

    // change unit in source oracle
    oracle.replace(new MockJavaResource(
        SourceFileCompilationUnit.getTypeName(JavaResourceBase.FOO)) {
      @Override
      protected CharSequence getContent() {
        return SourceFileCompilationUnit.readSource(JavaResourceBase.FOO)
            + "\n";
      }
    });
    state.refresh(createTreeLogger());

    /*
     * Add same generated units. Verify that the generated units are not used.
     */
    usefulUnits = state.getUsefulGraveyardUnits(generatedCups);
    assertEquals(0, usefulUnits.size());
    state.addGeneratedCompilationUnits(createTreeLogger(), generatedCups,
        usefulUnits);
    assertUnitsChecked(state.getCompilationUnits());
  }

  public void testSourceOracleAdd() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.add(JavaResourceBase.FOO);
    state.refresh(createTreeLogger());
    assertEquals(size + 1, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleBasic() {
    validateCompilationState();
  }

  public void testSourceOracleEmpty() {
    oracle = new MockResourceOracle();
    state = new CompilationState(createTreeLogger(), oracle);
    validateCompilationState();
  }

  public void testSourceOracleRemove() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.remove(JavaResourceBase.OBJECT.getPath());
    state.refresh(createTreeLogger());
    assertEquals(size - 1, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleReplace() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.replace(new MockJavaResource("java.lang.Object") {
      @Override
      protected CharSequence getContent() {
        return SourceFileCompilationUnit.readSource(JavaResourceBase.OBJECT);
      }
    });
    state.refresh(createTreeLogger());
    assertEquals(size, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleReplaceWithSame() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.replace(JavaResourceBase.OBJECT);
    state.refresh(createTreeLogger());
    assertEquals(size, state.getCompilationUnits().size());
    validateCompilationState();
  }

  /* test if generatedUnits that depend on stale generatedUnits are invalidated */
  public void testTransitiveInvalidation() {
    Set<CompilationUnit> modifiedUnits = getModifiedCompilationUnits(JavaResourceBase.FOO);
    modifiedUnits.addAll(getCompilationUnits(JavaResourceBase.BAR));
    testCachingOverMultipleRefreshes(getCompilationUnits(JavaResourceBase.BAR,
        JavaResourceBase.FOO), modifiedUnits, Collections.<String> emptySet(),
        2);
  }

  private void testCaching(MockJavaResource... files) {
    Set<String> reusedTypes = new HashSet<String>();
    for (MockJavaResource file : files) {
      reusedTypes.add(SourceFileCompilationUnit.getTypeName(file));
    }
    testCachingOverMultipleRefreshes(getCompilationUnits(files),
        getCompilationUnits(files), reusedTypes, 0);
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
  private void testCachingOverMultipleRefreshes(
      Set<CompilationUnit> initialSet, Set<CompilationUnit> updatedSet,
      Set<String> reusedTypes, int numInvalidated) {

    // verify that there were no generated units before.
    state.refresh(createTreeLogger());
    assertEquals(0, state.graveyardUnits.size());

    // add 'initialSet' generatedUnits over the first refresh cycle.
    testCachingOverSingleRefresh(new HashSet<CompilationUnit>(initialSet), 0,
        Collections.<String> emptySet(), 0);

    // add 'updatedSet' generatedUnits over the second refresh cycle.
    testCachingOverSingleRefresh(new HashSet<CompilationUnit>(updatedSet),
        initialSet.size(), reusedTypes, numInvalidated);

    // add 'updatedSet' generatedUnits over the third refresh cycle.
    reusedTypes = new HashSet<String>();
    for (CompilationUnit unit : updatedSet) {
      reusedTypes.add(unit.getTypeName());
    }
    testCachingOverSingleRefresh(new HashSet<CompilationUnit>(updatedSet),
        updatedSet.size(), reusedTypes, 0);
  }

  /**
   * Steps:
   * <ol>
   * <li>Check graveyardUnits before refresh. assert size is 0.</li>
   * <li>Refresh. assert size is 'graveyardUnitsSize'.</li>
   * <li>Add generated cups. Confirm that the 'reusedTypes' and 'numInvalidated'
   * match.</li>
   * </ol>
   * 
   * @param generatedCups generated CompilationUnits to be added.
   * @param graveyardUnitsSize initial expected size of graveyard units.
   * @param reusedTypes Main type of the units that can be reused between the
   *          initialSet and updatedSet.
   * @param numInvalidated Number of types invalidated from graveyardUnits.
   */
  private void testCachingOverSingleRefresh(Set<CompilationUnit> generatedCups,
      int graveyardUnitsSize, Set<String> reusedTypes, int numInvalidated) {
    assertEquals(0, state.graveyardUnits.size());

    assertUnitsChecked(state.getCompilationUnits());
    state.refresh(createTreeLogger());
    assertEquals(graveyardUnitsSize, state.graveyardUnits.size());

    int initialSize = state.graveyardUnits.size();
    Map<String, CompilationUnit> usefulUnits = state.getUsefulGraveyardUnits(generatedCups);
    assertEquals(reusedTypes.size(), usefulUnits.size());
    for (String typeName : reusedTypes) {
      assertNotNull(usefulUnits.get(typeName));
    }
    assertEquals(numInvalidated, initialSize - reusedTypes.size()
        - state.graveyardUnits.size());
    state.addGeneratedCompilationUnits(createTreeLogger(), generatedCups,
        usefulUnits);
    assertUnitsChecked(state.getCompilationUnits());
  }
}
