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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.javac.impl.MockJavaSourceFile;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Tests {@link CompilationState}.
 */
public class CompilationStateTest extends TestCase {

  private MockJavaSourceOracle oracle = new MockJavaSourceOracle(
      JavaSourceCodeBase.getStandardResources());

  private CompilationState state = new CompilationState(oracle);

  public void testAddGeneratedCompilationUnit() {
    validateCompilationState();

    // Add a unit and ensure it shows up.
    addGeneratedUnit(JavaSourceCodeBase.FOO);
    validateCompilationState(JavaSourceCodeBase.FOO.getTypeName());

    // Ensure it disappears after a refresh.
    state.refresh();
    validateCompilationState();
  }

  static void assertUnitsChecked(Collection<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      assertSame(State.CHECKED, unit.getState());
      assertNull(unit.getErrors());
      assertTrue(unit.getCompiledClasses().size() > 0);
    }
  }

  public void testCompile() throws UnableToCompleteException {
    validateUncompiled();
    state.compile(createTreeLogger());
    assertUnitsChecked(state.getCompilationUnits());
  }

  public void testCompileError() throws UnableToCompleteException {
    oracle.add(JavaSourceCodeBase.BAR);
    state.refresh();
    validateUncompiled();
    state.compile(createTreeLogger());

    CompilationUnit badUnit = state.getCompilationUnitMap().get(
        JavaSourceCodeBase.BAR.getTypeName());
    assertSame(State.ERROR, badUnit.getState());

    Set<CompilationUnit> goodUnits = new HashSet<CompilationUnit>(
        state.getCompilationUnits());
    goodUnits.remove(badUnit);
    assertUnitsChecked(goodUnits);
  }

  public void testCompileWithGeneratedUnits() throws UnableToCompleteException {
    validateUncompiled();
    state.compile(createTreeLogger());
    assertUnitsChecked(state.getCompilationUnits());
    addGeneratedUnit(JavaSourceCodeBase.FOO);
    state.compile(createTreeLogger());
    assertUnitsChecked(state.getCompilationUnits());
  }

  public void testCompileWithGeneratedUnitsError()
      throws UnableToCompleteException {
    validateUncompiled();
    state.compile(createTreeLogger());
    assertUnitsChecked(state.getCompilationUnits());
    addGeneratedUnit(JavaSourceCodeBase.BAR);
    state.compile(createTreeLogger());

    CompilationUnit badUnit = state.getCompilationUnitMap().get(
        JavaSourceCodeBase.BAR.getTypeName());
    assertSame(State.ERROR, badUnit.getState());

    Set<CompilationUnit> goodUnits = new HashSet<CompilationUnit>(
        state.getCompilationUnits());
    goodUnits.remove(badUnit);
    assertUnitsChecked(goodUnits);
  }

  public void testSourceOracleAdd() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.add(JavaSourceCodeBase.FOO);
    state.refresh();
    assertEquals(size + 1, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleBasic() {
    validateCompilationState();
  }

  public void testSourceOracleEmpty() {
    oracle = new MockJavaSourceOracle();
    state = new CompilationState(oracle);
    validateCompilationState();
  }

  public void testSourceOracleRemove() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.remove(JavaSourceCodeBase.OBJECT.getTypeName());
    state.refresh();
    assertEquals(size - 1, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleReplace() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.replace(new MockJavaSourceFile(JavaSourceCodeBase.OBJECT));
    state.refresh();
    assertEquals(size, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleReplaceWithSame() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.replace(JavaSourceCodeBase.OBJECT);
    state.refresh();
    assertEquals(size, state.getCompilationUnits().size());
    validateCompilationState();
  }

  private void addGeneratedUnit(JavaSourceFile sourceFile) {
    state.addGeneratedCompilationUnit(new SourceFileCompilationUnit(sourceFile) {
      @Override
      public boolean isGenerated() {
        return true;
      }
    });
  }

  private void validateCompilationState(String... generatedTypeNames) {
    // Save off the reflected collections.
    Map<String, CompilationUnit> unitMap = state.getCompilationUnitMap();
    Set<CompilationUnit> units = state.getCompilationUnits();

    // Validate that the collections are consistent with each other.
    assertEquals(new HashSet<CompilationUnit>(unitMap.values()), units);

    // Save off a mutable copy of the source map and generated types to compare.
    Map<String, JavaSourceFile> sourceMap = new HashMap<String, JavaSourceFile>(
        oracle.getSourceMap());
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
        assertTrue(sourceMap.containsKey(className));
        // TODO: Validate the source file matches the resource.
        assertNotNull(sourceMap.remove(className));
      }
    }
    // The mutable sets should be empty now.
    assertEquals(0, sourceMap.size());
    assertEquals(0, generatedTypes.size());
  }

  private void validateUncompiled() {
    for (CompilationUnit unit : state.getCompilationUnits()) {
      assertNull(unit.getJdtCud());
    }
  }

  /**
   * Tweak this if you want to see the log output.
   */
  private TreeLogger createTreeLogger() {
    boolean reallyLog = false;
    if (reallyLog) {
      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ALL);
      return logger;
    } else {
      return TreeLogger.NULL;
    }
  }
}
