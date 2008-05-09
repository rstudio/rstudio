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
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;
import com.google.gwt.dev.js.ast.JsProgram;

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
  protected final Map<String, CompilationUnit> unitMap = new HashMap<String, CompilationUnit>();
  private Set<JavaSourceFile> cachedSourceFiles = Collections.emptySet();
  private Map<String, CompiledClass> exposedClassFileMap = null;
  private final Map<String, CompilationUnit> exposedUnitMap = Collections.unmodifiableMap(unitMap);
  private Set<CompilationUnit> exposedUnits = Collections.emptySet();
  private final TypeOracleMediator mediator = new TypeOracleMediator();
  private final JavaSourceOracle sourceOracle;

  /**
   * Construct a new {@link CompilationState}.
   * 
   * @param sourceOracle an oracle used to retrieve source code and check for
   *          changes in the underlying source code base
   */
  public CompilationState(JavaSourceOracle sourceOracle) {
    this.sourceOracle = sourceOracle;
    refresh();
  }

  public void addGeneratedCompilationUnit(CompilationUnit unit) {
    String typeName = unit.getTypeName();
    assert (!unitMap.containsKey(typeName));
    unitMap.put(typeName, unit);
    updateExposedUnits();
  }

  /**
   * Compile all units and updates all internal state. Invalidate any units with
   * compile errors.
   */
  public void compile(TreeLogger logger) throws UnableToCompleteException {
    JdtCompiler.compile(getCompilationUnits());
    CompilationUnitInvalidator.validateCompilationUnits(getCompilationUnits(),
        getClassFileMap());

    // TODO: Move into validation & log errors?
    JsniCollector.collectJsniMethods(logger, getCompilationUnits(),
        new JsProgram());

    CompilationUnitInvalidator.invalidateUnitsWithErrors(logger,
        getCompilationUnits());

    mediator.refresh(logger, getCompilationUnits());

    // Any surviving units are now checked.
    for (CompilationUnit unit : getCompilationUnits()) {
      if (unit.getState() == State.COMPILED) {
        unit.setState(State.CHECKED);
      }
    }

    updateExposedUnits();
  }

  /**
   * Returns a map of all compiled classes by binary name.
   */
  public Map<String, CompiledClass> getClassFileMap() {
    if (exposedClassFileMap == null) {
      HashMap<String, CompiledClass> classFileMap = new HashMap<String, CompiledClass>();
      for (CompilationUnit unit : getCompilationUnits()) {
        if (unit.isCompiled()) {
          for (CompiledClass compiledClass : unit.getCompiledClasses()) {
            classFileMap.put(compiledClass.getBinaryName(), compiledClass);
          }
        }
      }
      exposedClassFileMap = Collections.unmodifiableMap(classFileMap);
    }
    return exposedClassFileMap;
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
  public void refresh() {
    // Always remove all generated compilation units.
    for (Iterator<CompilationUnit> it = unitMap.values().iterator(); it.hasNext();) {
      CompilationUnit unit = it.next();
      if (unit.isGenerated()) {
        unit.setState(State.FRESH);
        it.remove();
      }
    }

    refreshFromSourceOracle();
    // Don't log about invalidated units via refresh.
    CompilationUnitInvalidator.invalidateUnitsWithInvalidRefs(TreeLogger.NULL,
        getCompilationUnits());
    updateExposedUnits();
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
  }
}
