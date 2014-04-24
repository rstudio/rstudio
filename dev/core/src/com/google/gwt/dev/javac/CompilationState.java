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
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.CompilationStateBuilder.CompileMoreLater;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the state of active compilation units in a particular module.
 * State is accumulated throughout the life cycle of the containing module and
 * may be invalidated at certain times and recomputed.
 */
public class CompilationState {

  /**
   * Classes mapped by internal name.
   */
  protected final Map<String, CompiledClass> classFileMap = new HashMap<String, CompiledClass>();

  /**
   * Classes mapped by source name.
   */
  protected final Map<String, CompiledClass> classFileMapBySource =
      new HashMap<String, CompiledClass>();

  /**
   * All my compilation units.
   */
  protected final Map<String, CompilationUnit> unitMap = new HashMap<String, CompilationUnit>();

  private int cachedGeneratedSourceCount = 0;

  private int cachedStaticSourceCount = 0;

  private final CompileMoreLater compileMoreLater;

  private CompilerContext compilerContext;

  /**
   * Unmodifiable view of {@link #classFileMap}.
   */
  private final Map<String, CompiledClass> exposedClassFileMap =
      Collections.unmodifiableMap(classFileMap);

  /**
   * Unmodifiable view of {@link #classFileMapBySource}.
   */
  private final Map<String, CompiledClass> exposedClassFileMapBySource =
      Collections.unmodifiableMap(classFileMapBySource);

  /**
   * Unmodifiable view of {@link #unitMap}.
   */
  private final Map<String, CompilationUnit> exposedUnitMap = Collections.unmodifiableMap(unitMap);

  /**
   * Unmodifiable view of all units.
   */
  private final Collection<CompilationUnit> exposedUnits =
      Collections.unmodifiableCollection(unitMap.values());

  private int generatedSourceCount = 0;

  private int staticSourceCount = 0;

  /**
   * Our type oracle.
   */
  private final TypeOracle typeOracle;

  /**
   * Updates our type oracle.
   */
  private final CompilationUnitTypeOracleUpdater typeOracleUpdater;

  CompilationState(TreeLogger logger, CompilerContext compilerContext,
      TypeOracle typeOracle, CompilationUnitTypeOracleUpdater typeOracleUpdater,
      Collection<CompilationUnit> units, CompileMoreLater compileMoreLater) {
    this.compileMoreLater = compileMoreLater;
    this.compilerContext = compilerContext;
    this.typeOracle = typeOracle;
    this.typeOracleUpdater = typeOracleUpdater;
    assimilateUnits(logger, units, true);
  }

  /**
   * Compiles the given source files (unless cached) and adds them to the
   * CompilationState.
   * If the compiler aborts, logs the error and throws UnableToCompleteException.
   */
  public void addGeneratedCompilationUnits(TreeLogger logger,
      Collection<GeneratedUnit> generatedUnits) throws UnableToCompleteException {
    Event generatedUnitsAddEvent = SpeedTracerLogger.start(
        DevModeEventType.COMP_STATE_ADD_GENERATED_UNITS);
    try {
      logger = logger.branch(TreeLogger.DEBUG, "Adding '"
          + generatedUnits.size() + "' new generated units");
      generatedUnitsAddEvent.addData("# new generated units", "" + generatedUnits.size());
      Collection<CompilationUnit> newUnits = compileMoreLater.addGeneratedTypes(
          logger, generatedUnits, this);
      assimilateUnits(logger, newUnits, true);
    } finally {
      generatedUnitsAddEvent.end();
    }
  }

  /**
   * Indexes referenced external compilation units but does not save them in a library.
   */
  public void addReferencedCompilationUnits(TreeLogger logger,
      List<CompilationUnit> referencedUnits) {
    Event referencedUnitsAddEvent =
        SpeedTracerLogger.start(DevModeEventType.COMP_STATE_ADD_REFERENCED_UNITS);
    try {
      logger = logger.branch(TreeLogger.DEBUG,
          "Adding '" + referencedUnits.size() + "' new referenced units");
      referencedUnitsAddEvent.addData("# new referenced units", "" + referencedUnits.size());
      assimilateUnits(logger, referencedUnits, false);
    } finally {
      referencedUnitsAddEvent.end();
    }
  }

  public int getCachedGeneratedSourceCount() {
    return cachedGeneratedSourceCount;
  }

  public int getCachedStaticSourceCount() {
    return cachedStaticSourceCount;
  }

  /**
   * Returns a map of all compiled classes by internal name.
   */
  public Map<String, CompiledClass> getClassFileMap() {
    return exposedClassFileMap;
  }

  /**
   * Returns a map of all compiled classes by source name.
   */
  public Map<String, CompiledClass> getClassFileMapBySource() {
    return exposedClassFileMapBySource;
  }

  /**
   * Returns an unmodifiable view of the set of compilation units, mapped by the
   * main type's qualified source name.
   */
  public Map<String, CompilationUnit> getCompilationUnitMap() {
    return exposedUnitMap;
  }

  /**
   * Returns an unmodifiable view of the set of compilation units.
   */
  public Collection<CompilationUnit> getCompilationUnits() {
    return exposedUnits;
  }

  public CompilerContext getCompilerContext() {
    return compilerContext;
  }

  public int getGeneratedSourceCount() {
    return generatedSourceCount;
  }

  public int getStaticSourceCount() {
    return staticSourceCount;
  }

  public TypeOracle getTypeOracle() {
    return typeOracle;
  }

  @VisibleForTesting
  public Map<String, CompiledClass> getValidClasses() {
    return compileMoreLater.getValidClasses();
  }

  /**
   * Whether any errors were encountered while building this compilation state.
   */
  public boolean hasErrors() {
    for (CompilationUnit unit : unitMap.values()) {
      if (unit.isError()) {
        return true;
      }
    }
    return false;
  }

  public void incrementCachedGeneratedSourceCount(int extraCachedGeneratedSourceCount) {
    cachedGeneratedSourceCount += extraCachedGeneratedSourceCount;
  }

  public void incrementCachedStaticSourceCount(int extraCachedStaticSourceCount) {
    cachedStaticSourceCount += extraCachedStaticSourceCount;
  }

  public void incrementGeneratedSourceCount(int extraGeneratedSourceCount) {
    generatedSourceCount += extraGeneratedSourceCount;
  }

  public void incrementStaticSourceCount(int extraStaticSourceCount) {
    staticSourceCount += extraStaticSourceCount;
  }

  /**
   * For testing.
   */
  CompilationUnitTypeOracleUpdater getTypeOracleUpdater() {
    return typeOracleUpdater;
  }

  private void assimilateUnits(TreeLogger logger, Collection<CompilationUnit> units,
      boolean saveInLibrary) {
    for (CompilationUnit unit : units) {
      unitMap.put(unit.getTypeName(), unit);
      for (CompiledClass compiledClass : unit.getCompiledClasses()) {
        classFileMap.put(compiledClass.getInternalName(), compiledClass);
        classFileMapBySource.put(compiledClass.getSourceName(), compiledClass);
      }
    }

    // Performed after compilation unit invalidator because only valid units should be saved in the
    // library.
    if (saveInLibrary) {
      CompilationUnitInvalidator.retainValidUnits(logger, units,
          compileMoreLater.getValidClasses());
      for (CompilationUnit compilationUnit : units) {
        compilerContext.getLibraryWriter().addCompilationUnit(compilationUnit);
      }
    }
    typeOracleUpdater.addNewUnits(logger, units);
  }
}
