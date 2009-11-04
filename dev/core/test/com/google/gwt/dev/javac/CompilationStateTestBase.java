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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Base class for tests that need a mock type compilation state and everything
 * that goes with it (compilation units, type oracle, resources, ...).
 */
public abstract class CompilationStateTestBase extends TestCase {

  protected static class GeneratedSourceFileCompilationUnit extends
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
  protected static TreeLogger createTreeLogger() {
    boolean reallyLog = false;
    if (reallyLog) {
      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ALL);
      return logger;
    }
    return TreeLogger.NULL;
  }

  protected MockResourceOracle oracle = new MockResourceOracle(
      JavaResourceBase.getStandardResources());

  protected CompilationState state = new CompilationState(createTreeLogger(),
      oracle);

  protected void addGeneratedUnits(MockJavaResource... sourceFiles) {
    Set<CompilationUnit> units = getCompilationUnits(sourceFiles);
    state.addGeneratedCompilationUnits(createTreeLogger(), units);
  }

  protected Set<CompilationUnit> getCompilationUnits(
      MockJavaResource... sourceFiles) {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    for (MockJavaResource sourceFile : sourceFiles) {
      // keep the same source
      units.add(new GeneratedSourceFileCompilationUnit(sourceFile, false));
    }
    return units;
  }

  protected Set<CompilationUnit> getModifiedCompilationUnits(
      MockJavaResource... sourceFiles) {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    for (MockJavaResource sourceFile : sourceFiles) {
      // modify the source
      units.add(new GeneratedSourceFileCompilationUnit(sourceFile, true));
    }
    return units;
  }

  protected void validateCompilationState(String... generatedTypeNames) {
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
