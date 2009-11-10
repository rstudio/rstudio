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
import com.google.gwt.dev.javac.StandardGeneratorContext.Generated;
import com.google.gwt.dev.javac.impl.Shared;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.PerfLogger;

import java.util.Collection;
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

  private static Set<CompilationUnit> concatSet(Collection<CompilationUnit> a,
      Collection<CompilationUnit> b) {
    Set<CompilationUnit> result = new HashSet<CompilationUnit>(a.size()
        + b.size());
    result.addAll(a);
    result.addAll(b);
    return result;
  }

  private static void markSurvivorsChecked(Collection<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      if (unit.getState() == State.COMPILED
          || unit.getState() == State.GRAVEYARD) {
        unit.setChecked();
      }
    }
  }

  protected final Map<String, CompilationUnit> unitMap = new HashMap<String, CompilationUnit>();

  /**
   * Generated units become graveyardUnits when refresh is hit. Package
   * protected for testing.
   */
  Map<String, CompilationUnit> graveyardUnits;

  private Set<Resource> cachedSourceFiles = Collections.emptySet();

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
  private final Collection<CompilationUnit> exposedUnits = Collections.unmodifiableCollection(unitMap.values());

  private CompilationUnitInvalidator.InvalidatorState invalidatorState = new CompilationUnitInvalidator.InvalidatorState();

  /**
   * Recreated on refresh, allows incremental compiles.
   */
  private JdtCompiler jdtCompiler;

  /**
   * Controls our type oracle.
   */
  private TypeOracleMediator mediator = new TypeOracleMediator();

  /**
   * Our source file inputs.
   */
  private final ResourceOracle sourceOracle;

  /**
   * Construct a new {@link CompilationState}.
   * 
   * @param sourceOracle an oracle used to retrieve source code and check for
   *          changes in the underlying source code base
   */
  public CompilationState(TreeLogger logger, ResourceOracle sourceOracle) {
    this.sourceOracle = sourceOracle;
    refresh(logger);
  }

  /**
   * The method processes generatedCompilationUnits and adds them to the
   * TypeOracle, using graveyardUnits wherever possible.
   */
  public void addGeneratedCompilationUnits(TreeLogger logger,
      Set<? extends CompilationUnit> generatedCups) {
    logger = logger.branch(TreeLogger.DEBUG, "Adding '" + generatedCups.size()
        + "' new generated units");
    Map<String, CompilationUnit> usefulGraveyardUnits = getUsefulGraveyardUnits(generatedCups);
    logger.log(TreeLogger.DEBUG, "Using " + usefulGraveyardUnits.values()
        + " units from graveyard");
    addGeneratedCompilationUnits(logger, generatedCups, usefulGraveyardUnits);
  }

  /**
   * Clear up all internal state to free up memory. Resets all units to FRESH
   * and clears TypeOracle.
   */
  public void clear() {
    // Always remove all generated compilation units.
    for (Iterator<CompilationUnit> it = unitMap.values().iterator(); it.hasNext();) {
      CompilationUnit unit = it.next();
      unit.setFresh();
      if (unit.isGenerated()) {
        it.remove();
      }
    }
    unitMap.clear();
    invalidateClassFileMaps();
    jdtCompiler = null;
    mediator = new TypeOracleMediator();
    invalidatorState = new CompilationUnitInvalidator.InvalidatorState();
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
  public Collection<CompilationUnit> getCompilationUnits() {
    return exposedUnits;
  }

  public TypeOracle getTypeOracle() {
    return mediator.getTypeOracle();
  }

  /**
   * Synchronize against the source oracle to check for added/removed/updated
   * units. Updated units are invalidated, and any units depending on changed
   * units are also invalidated. All generated units are moved to GRAVEYARD.
   */
  public void refresh(TreeLogger logger) {
    logger = logger.branch(TreeLogger.DEBUG, "Refreshing module from source");
    /*
     * Clear out existing graveyard units that were never regenerated during the
     * current refresh cycle.
     * 
     * TODO: we could possibly <i>not</i> clear this out entirely, but it would
     * be slightly more complicated.
     */
    graveyardUnits = new HashMap<String, CompilationUnit>();

    // Always remove all generated compilation units.
    for (Iterator<CompilationUnit> it = unitMap.values().iterator(); it.hasNext();) {
      CompilationUnit unit = it.next();
      if (unit.isGenerated()) {
        if (unit.getState() == State.CHECKED) {
          unit.setGraveyard();
          graveyardUnits.put(unit.getTypeName(), unit);
        } else {
          unit.setFresh();
        }
        it.remove();
      }
    }

    refreshFromSourceOracle();
    invalidateClassFileMaps();

    // Don't log about invalidated units via refresh.
    Set<CompilationUnit> allUnitsPlusGraveyard = concatSet(unitMap.values(),
        graveyardUnits.values());
    CompilationUnitInvalidator.invalidateUnitsWithInvalidRefs(TreeLogger.NULL,
        allUnitsPlusGraveyard);
    removeInvalidatedGraveyardUnits(graveyardUnits);

    /*
     * Only retain state for units marked as CHECKED; because CHECKED units
     * won't be revalidated.
     */
    Set<CompilationUnit> toRetain = new HashSet<CompilationUnit>(
        unitMap.values());
    for (Iterator<CompilationUnit> it = toRetain.iterator(); it.hasNext();) {
      CompilationUnit unit = it.next();
      if (unit.getState() != State.CHECKED) {
        it.remove();
      }
    }
    invalidatorState.retainAll(toRetain);

    jdtCompiler = new JdtCompiler();
    compile(logger, unitMap.values(), Collections.<CompilationUnit> emptySet());
    mediator.refresh(logger, unitMap.values());
    markSurvivorsChecked(unitMap.values());
  }

  /**
   * This method processes generatedCups using usefulGraveyardUnits wherever
   * possible.
   */
  void addGeneratedCompilationUnits(TreeLogger logger,
      Set<? extends CompilationUnit> newGeneratedCups,
      Map<String, CompilationUnit> usefulGraveyardUnitsMap) {

    Set<CompilationUnit> usefulGeneratedCups = new HashSet<CompilationUnit>();
    for (CompilationUnit newGeneratedUnit : newGeneratedCups) {
      String typeName = newGeneratedUnit.getTypeName();
      CompilationUnit oldGeneratedUnit = usefulGraveyardUnitsMap.get(typeName);
      if (oldGeneratedUnit != null) {
        usefulGeneratedCups.add(oldGeneratedUnit);
        unitMap.put(typeName, oldGeneratedUnit);
      } else {
        usefulGeneratedCups.add(newGeneratedUnit);
        unitMap.put(typeName, newGeneratedUnit);
      }
    }
    assert (newGeneratedCups.size() == usefulGeneratedCups.size());
    for (CompilationUnit generatedUnit : usefulGeneratedCups) {
      unitMap.put(generatedUnit.getTypeName(), generatedUnit);
    }
    invalidateClassFileMaps();

    compile(logger, usefulGeneratedCups, unitMap.values());
    mediator.addNewUnits(logger, usefulGeneratedCups);
    markSurvivorsChecked(usefulGeneratedCups);
  }

  /**
   * Given a set of generatedCups, returns the useful graveyardUnits that do not
   * need to be compiled. Additionally, updates the graveyardUnits by removing
   * units that are either stale or depend on some other stale unit.
   */
  Map<String, CompilationUnit> getUsefulGraveyardUnits(
      Set<? extends CompilationUnit> generatedCups) {
    boolean anyGraveyardUnitsWereInvalidated = false;
    Map<String, CompilationUnit> usefulGraveyardUnits = new HashMap<String, CompilationUnit>();
    for (CompilationUnit unit : generatedCups) {
      String typeName = unit.getTypeName();
      assert (!unitMap.containsKey(typeName));
      CompilationUnit graveyardUnit = graveyardUnits.remove(typeName);
      if (graveyardUnit != null) {
        assert graveyardUnit.getState() == State.GRAVEYARD;
        assert unit instanceof Generated;
        assert graveyardUnit instanceof Generated;
        if (((Generated) unit).getStrongHash().equals(
            ((Generated) graveyardUnit).getStrongHash())) {
          usefulGraveyardUnits.put(typeName, graveyardUnit);
        } else {
          // The old unit is invalidated.
          anyGraveyardUnitsWereInvalidated = true;
          graveyardUnit.setFresh();
        }
      }
    }

    assert Collections.disjoint(usefulGraveyardUnits.values(),
        graveyardUnits.values());

    /*
     * If any units became fresh, we need to ensure that any units that might
     * refer to that unit get invalidated.
     */
    if (anyGraveyardUnitsWereInvalidated) {
      /* Remove units that refer the graveyard units marked FRESH */
      Set<CompilationUnit> allGraveyardUnits = concatSet(
          graveyardUnits.values(), usefulGraveyardUnits.values());
      CompilationUnitInvalidator.invalidateUnitsWithInvalidRefs(
          TreeLogger.NULL, allGraveyardUnits, unitMap.values());

      removeInvalidatedGraveyardUnits(graveyardUnits);
      removeInvalidatedGraveyardUnits(usefulGraveyardUnits);
    }
    return usefulGraveyardUnits;
  }

  /**
   * Compile units and update their internal state. Invalidate any units with
   * compile errors.
   */
  private void compile(TreeLogger logger, Collection<CompilationUnit> newUnits,
      Collection<CompilationUnit> existingUnits) {
    PerfLogger.start("CompilationState.compile");
    if (jdtCompiler.doCompile(newUnits)) {
      logger = logger.branch(TreeLogger.DEBUG,
          "Validating newly compiled units");

      // Dump all units with direct errors; we cannot safely check them.
      boolean anyErrors = CompilationUnitInvalidator.invalidateUnitsWithErrors(
          logger, newUnits);

      // Check all units using our custom checks.
      CompilationUnitInvalidator.validateCompilationUnits(invalidatorState,
          newUnits);

      // More units may have errors now.
      anyErrors |= CompilationUnitInvalidator.invalidateUnitsWithErrors(logger,
          newUnits);

      if (anyErrors) {
        CompilationUnitInvalidator.invalidateUnitsWithInvalidRefs(logger,
            newUnits, existingUnits);
      }

      JsniCollector.collectJsniMethods(logger, newUnits, new JsProgram());
    }

    PerfLogger.end();
  }

  private void invalidateClassFileMaps() {
    // TODO: see if we can call this in fewer places.
    exposedClassFileMap = null;
    exposedClassFileMapBySource = null;
  }

  private void rebuildClassMaps() {
    HashMap<String, CompiledClass> classFileMap = new HashMap<String, CompiledClass>();
    HashMap<String, CompiledClass> classFileMapBySource = new HashMap<String, CompiledClass>();
    for (CompilationUnit unit : unitMap.values()) {
      if (unit.isCompiled()) {
        for (CompiledClass compiledClass : unit.getCompiledClasses()) {
          classFileMap.put(compiledClass.getInternalName(), compiledClass);
          classFileMapBySource.put(compiledClass.getSourceName(), compiledClass);
        }
      }
    }
    exposedClassFileMap = Collections.unmodifiableMap(classFileMap);
    exposedClassFileMapBySource = Collections.unmodifiableMap(classFileMapBySource);
  }

  private void refreshFromSourceOracle() {
    // See if the source oracle has changed.
    Set<Resource> newSourceFiles = sourceOracle.getResources();
    if (cachedSourceFiles == newSourceFiles) {
      return;
    }

    // Divide resources into changed and unchanged.
    Set<Resource> unchanged = new HashSet<Resource>(cachedSourceFiles);
    unchanged.retainAll(newSourceFiles);

    Set<Resource> changed = new HashSet<Resource>(newSourceFiles);
    changed.removeAll(unchanged);

    // First remove any stale units.
    for (Iterator<CompilationUnit> it = unitMap.values().iterator(); it.hasNext();) {
      CompilationUnit unit = it.next();
      SourceFileCompilationUnit sourceFileUnit = (SourceFileCompilationUnit) unit;
      if (!unchanged.contains(sourceFileUnit.getSourceFile())) {
        unit.setFresh();
        it.remove();
      }
    }

    // Then add any new source files.
    for (Resource newSourceFile : changed) {
      String typeName = Shared.getTypeName(newSourceFile);
      assert (!unitMap.containsKey(typeName));
      unitMap.put(typeName, new SourceFileCompilationUnit(newSourceFile));
      // invalid a graveyard unit, if a new unit has the same type.
      CompilationUnit graveyardUnit = graveyardUnits.remove(typeName);
      if (graveyardUnit != null) {
        graveyardUnit.setFresh();
      }
    }

    // Record the update.
    cachedSourceFiles = newSourceFiles;
  }

  private void removeInvalidatedGraveyardUnits(
      Map<String, CompilationUnit> tempUnits) {
    for (Iterator<CompilationUnit> it = tempUnits.values().iterator(); it.hasNext();) {
      CompilationUnit graveyardUnit = it.next();
      if (graveyardUnit.getState() != State.GRAVEYARD) {
        it.remove();
      }
    }
  }
}
