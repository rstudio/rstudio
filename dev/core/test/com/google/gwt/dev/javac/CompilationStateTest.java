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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.javac.StandardGeneratorContext.Generated;
import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.javac.impl.MockResourceOracle;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Tests {@link CompilationState}.
 */
public class CompilationStateTest extends TestCase {

  private static class GeneratedSourceFileCompilationUnit extends
      SourceFileCompilationUnit implements Generated {

    private final boolean modifySource;
    private String strongHash;

    public GeneratedSourceFileCompilationUnit(Resource sourceFile, boolean modifySource) {
      super(sourceFile);
      this.modifySource = modifySource;
    }

    public void abort() {
    }

    public void commit() {
    }

    @Override
    public String getSource() {
      String extraChars = "";
      if (modifySource) {
        extraChars = "\n";
      }
      return super.getSource() + extraChars;
    }

    public String getStrongHash() {
      if (strongHash == null) {
        strongHash = Util.computeStrongName(Util.getBytes(getSource()));
      }
      return strongHash;
    }

    @Override
    public boolean isGenerated() {
      return true;
    }
  }

  static void assertUnitsChecked(Collection<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      assertSame(State.CHECKED, unit.getState());
      assertNull(unit.getErrors());
      assertTrue(unit.getCompiledClasses().size() > 0);
    }
  }

  /**
   * Tweak this if you want to see the log output.
   */
  private static TreeLogger createTreeLogger() {
    boolean reallyLog = false;
    if (reallyLog) {
      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ALL);
      return logger;
    }
    return TreeLogger.NULL;
  }

  private MockResourceOracle oracle = new MockResourceOracle(
      JavaResourceBase.getStandardResources());

  private CompilationState state = new CompilationState(createTreeLogger(),
      oracle);

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

  private void addGeneratedUnits(MockJavaResource... sourceFiles) {
    Set<CompilationUnit> units = getCompilationUnits(sourceFiles);
    state.addGeneratedCompilationUnits(createTreeLogger(), units);
  }

  private Set<CompilationUnit> getCompilationUnits(
      MockJavaResource... sourceFiles) {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    for (MockJavaResource sourceFile : sourceFiles) {
      // keep the same source
      units.add(new GeneratedSourceFileCompilationUnit(sourceFile, false));
    }
    return units;
  }

  private Set<CompilationUnit> getModifiedCompilationUnits(
      MockJavaResource... sourceFiles) {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    for (MockJavaResource sourceFile : sourceFiles) {
      // modify the source
      units.add(new GeneratedSourceFileCompilationUnit(sourceFile, true));
    }
    return units;
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

    // add 'updatedSet' generatedUnits over the first refresh cycle.
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

  private void validateCompilationState(String... generatedTypeNames) {
    // Save off the reflected collections.
    Map<String, CompilationUnit> unitMap = state.getCompilationUnitMap();
    Collection<CompilationUnit> units = state.getCompilationUnits();

    // Validate that the collections are consistent with each other.
    assertEquals(new HashSet<CompilationUnit>(unitMap.values()),
        new HashSet<CompilationUnit>(units));

    // Save off a mutable copy of the source map and generated types to compare.
    Map<String, Resource> sourceMap = new HashMap<String, Resource>(
        oracle.getResourceMap());
    Set<String> generatedTypes = new HashSet<String>(
        Arrays.asList(generatedTypeNames));
    assertEquals(sourceMap.size() + generatedTypes.size(), units.size());
    for (Entry<String, CompilationUnit> entry : unitMap.entrySet()) {
      // Validate source file internally consistent.
      String className = entry.getKey();
      CompilationUnit unit = entry.getValue();
      assertEquals(className, unit.getTypeName());

      // Find the matching resource (and remove it).
      if (unit.isGenerated()) {
        assertTrue(generatedTypes.contains(className));
        assertNotNull(generatedTypes.remove(className));
      } else {
        String partialPath = className.replace('.', '/') + ".java";
        assertTrue(sourceMap.containsKey(partialPath));
        // TODO: Validate the source file matches the resource.
        assertNotNull(sourceMap.remove(partialPath));
      }
    }
    // The mutable sets should be empty now.
    assertEquals(0, sourceMap.size());
    assertEquals(0, generatedTypes.size());
  }
}
