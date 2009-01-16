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
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.PerfLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the state of active compilation units in a particular module.
 * State is accumulated throughout the life cycle of the containing module and
 * may be invalidated at certain times and recomputed.
 */
public class CompilationState {

  private static void markSurvivorsChecked(Set<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      if (unit.getState() == State.COMPILED) {
        unit.setState(State.CHECKED);
      }
    }
  }

  protected final Map<String, CompilationUnit> unitMap = new HashMap<String, CompilationUnit>();

  private Set<JavaSourceFile> cachedSourceFiles = Collections.emptySet();
  /**
   * Classes mapped by binary name.
   */
  private Map<String, CompiledClass> exposedClassFileMap = null;

  /**
   * Classes mapped by source name.
   */
  private Map<String, CompiledClass> exposedClassFileMapBySource = null;

  /**
   * Unmodifiable view of {@link #unitMap}.
   */
  private final Map<String, CompilationUnit> exposedUnitMap = Collections.unmodifiableMap(unitMap);

  /**
   * Unmodifiable view of all units.
   */
  private Set<CompilationUnit> exposedUnits = Collections.emptySet();

  /**
   * Recreated on refresh, allows incremental compiles.
   */
  private JdtCompiler jdtCompiler;

  /**
   * Controls our type oracle.
   */
  private final TypeOracleMediator mediator = new TypeOracleMediator();

  /**
   * Our source file inputs.
   */
  private final JavaSourceOracle sourceOracle;

  /**
   * Construct a new {@link CompilationState}.
   * 
   * @param sourceOracle an oracle used to retrieve source code and check for
   *          changes in the underlying source code base
   */
  public CompilationState(TreeLogger logger, JavaSourceOracle sourceOracle) {
    this.sourceOracle = sourceOracle;
    refresh(logger);
  }

  @SuppressWarnings("unchecked")
  public void addGeneratedCompilationUnits(TreeLogger logger,
      Set<? extends CompilationUnit> generatedCups) {
    for (CompilationUnit unit : generatedCups) {
      String typeName = unit.getTypeName();
      assert (!unitMap.containsKey(typeName));
      unitMap.put(typeName, unit);
    }
    updateExposedUnits();
    compile(logger, (Set<CompilationUnit>) generatedCups);
    mediator.addNewUnits(logger, (Set<CompilationUnit>) generatedCups);
    markSurvivorsChecked((Set<CompilationUnit>) generatedCups);
  }

  /**
   * Returns a map of all compiled classes by binary name.
   */
  public Map<String, CompiledClass> getClassFileMap() {
    if (exposedClassFileMap == null) {
      rebuildClassMaps();
    }
    return exposedClassFileMap;
  }

  /**
   * Returns a map of all compiled classes by source name.
   */
  public Map<String, CompiledClass> getClassFileMapBySource() {
    if (exposedClassFileMapBySource == null) {
      rebuildClassMaps();
    }
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
  public Set<CompilationUnit> getCompilationUnits() {
    return exposedUnits;
  }

  public TypeOracle getTypeOracle() {
    return mediator.getTypeOracle();
  }

  /**
   * Synchronize against the source oracle to check for added/removed/updated
   * units. Updated units are invalidated, and any units depending on changed
   * units are also invalidated. All generated units are removed.
   * 
   * TODO: something more optimal with generated files?
   */
  public void refresh(TreeLogger logger) {
    // Always remove all generated compilation units.
    for (Iterator<CompilationUnit> it = unitMap.values().iterator(); it.hasNext();) {
      CompilationUnit unit = it.next();
      if (unit.isGenerated()) {
        unit.setState(State.FRESH);
        it.remove();
      }
    }

    refreshFromSourceOracle();
    updateExposedUnits();

    // Don't log about invalidated units via refresh.
    CompilationUnitInvalidator.invalidateUnitsWithInvalidRefs(TreeLogger.NULL,
        getCompilationUnits());

    jdtCompiler = new JdtCompiler();
    compile(logger, getCompilationUnits());
    mediator.refresh(logger, getCompilationUnits());
    markSurvivorsChecked(getCompilationUnits());
  }

  /**
   * Compile units and update their internal state. Invalidate any units with
   * compile errors.
   */
  private void compile(TreeLogger logger, Set<CompilationUnit> newUnits) {
    PerfLogger.start("CompilationState.compile");
    if (jdtCompiler.doCompile(newUnits)) {
      // Dump all units with direct errors; we cannot safely check them.
      boolean anyErrors = CompilationUnitInvalidator.invalidateUnitsWithErrors(
          logger, newUnits);

      // Check all units using our custom checks.
      CompilationUnitInvalidator.validateCompilationUnits(newUnits,
          jdtCompiler.getBinaryTypeNames());

      // More units may have errors now.
      anyErrors |= CompilationUnitInvalidator.invalidateUnitsWithErrors(logger,
          newUnits);

      if (anyErrors) {
        CompilationUnitInvalidator.invalidateUnitsWithInvalidRefs(logger,
            newUnits);
      }

      JsniCollector.collectJsniMethods(logger, newUnits, new JsProgram());
    }

    PerfLogger.end();
  }

  private void rebuildClassMaps() {
    HashMap<String, CompiledClass> classFileMap = new HashMap<String, CompiledClass>();
    HashMap<String, CompiledClass> classFileMapBySource = new HashMap<String, CompiledClass>();
    for (CompilationUnit unit : getCompilationUnits()) {
      if (unit.isCompiled()) {
        for (CompiledClass compiledClass : unit.getCompiledClasses()) {
          classFileMap.put(compiledClass.getBinaryName(), compiledClass);
          classFileMapBySource.put(compiledClass.getSourceName(), compiledClass);
        }
      }
    }
    exposedClassFileMap = Collections.unmodifiableMap(classFileMap);
    exposedClassFileMapBySource = Collections.unmodifiableMap(classFileMapBySource);
  }

  private void refreshFromSourceOracle() {
    // See if the source oracle has changed.
    Set<JavaSourceFile> newSourceFiles = sourceOracle.getSourceFiles();
    if (cachedSourceFiles == newSourceFiles) {
      return;
    }

    // Divide resources into changed and unchanged.
    Set<JavaSourceFile> unchanged = new HashSet<JavaSourceFile>(
        cachedSourceFiles);
    unchanged.retainAll(newSourceFiles);

    Set<JavaSourceFile> changed = new HashSet<JavaSourceFile>(newSourceFiles);
    changed.removeAll(unchanged);

    // First remove any stale units.
    for (Iterator<CompilationUnit> it = unitMap.values().iterator(); it.hasNext();) {
      CompilationUnit unit = it.next();
      SourceFileCompilationUnit sourceFileUnit = (SourceFileCompilationUnit) unit;
      if (!unchanged.contains(sourceFileUnit.getSourceFile())) {
        unit.setState(State.FRESH);
        it.remove();
      }
    }

    // Then add any new source files.
    for (JavaSourceFile newSourceFile : changed) {
      String typeName = newSourceFile.getTypeName();
      assert (!unitMap.containsKey(typeName));
      unitMap.put(typeName, new SourceFileCompilationUnit(newSourceFile));
    }

    // Record the update.
    cachedSourceFiles = newSourceFiles;
  }

  private void updateExposedUnits() {
    exposedUnits = Collections.unmodifiableSet(new HashSet<CompilationUnit>(
        unitMap.values()));
    exposedClassFileMap = null;
    exposedClassFileMapBySource = null;
  }
}
