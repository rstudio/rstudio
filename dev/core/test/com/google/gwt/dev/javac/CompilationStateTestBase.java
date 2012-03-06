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
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
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
import java.util.Map.Entry;
import java.util.Set;

/**
 * Base class for tests that need a mock type compilation state and everything
 * that goes with it (compilation units, type oracle, resources, ...).
 */
public abstract class CompilationStateTestBase extends TestCase {

  /**
   * Tweak this if you want to see the log output.
   */
  public static TreeLogger createTreeLogger() {
    boolean reallyLog = false;
    if (reallyLog) {
      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.WARN);
      return logger;
    }
    return TreeLogger.NULL;
  }

  public static Set<GeneratedUnit> getGeneratedUnits(
      MockResource... sourceFiles) {
    Set<GeneratedUnit> units = new HashSet<GeneratedUnit>();
    for (final MockResource sourceFile : sourceFiles) {
      units.add(new GeneratedUnit() {
        public long creationTime() {
          return sourceFile.getLastModified();
        }

        public String getSource() {
          return sourceFile.getString();
        }

        public String getSourceMapPath() {
          return getTypeName().replace(".", "/") + ".java";
        }

        public long getSourceToken() {
          return -1;
        }

        public String getStrongHash() {
          return Util.computeStrongName(Util.getBytes(getSource()));
        }

        public String getTypeName() {
          return Shared.getTypeName(sourceFile);
        }
        
        public String optionalFileLocation() {
          return sourceFile.getLocation();
        }
      });
    }
    return units;
  }

  static void assertUnitsChecked(Collection<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      assertFalse(unit.isError());
      assertTrue(unit.getCompiledClasses().size() > 0);
    }
  }

  /**
   * Ensure a clean cache at the beginning of every test run!
   */
  protected final CompilationStateBuilder isolatedBuilder = new CompilationStateBuilder();

  protected MockResourceOracle oracle = new MockResourceOracle(
      JavaResourceBase.getStandardResources());

  protected CompilationState state = isolatedBuilder.doBuildFrom(
      createTreeLogger(), oracle.getResources(), false);

  protected void addGeneratedUnits(MockResource... sourceFiles) {
    state.addGeneratedCompilationUnits(createTreeLogger(),
        getGeneratedUnits(sourceFiles));
  }

  protected void rebuildCompilationState() {
    state = isolatedBuilder.doBuildFrom(createTreeLogger(),
        oracle.getResources(), false);
  }

  protected void validateCompilationState(String... generatedTypeNames) {
    // Save off the reflected collections.
    Map<String, CompilationUnit> unitMap = state.getCompilationUnitMap();
    Collection<CompilationUnit> units = state.getCompilationUnits();

    // Validate that we have as many units as resources.
    assertEquals(oracle.getResources().size() + generatedTypeNames.length,
        units.size());

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
      if (generatedTypes.contains(className)) {
        // Not always true due to caching! A source unit for FOO can b
        // identical to the generated FOO and already be cached.
        // assertTrue(unit.isGenerated());
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
