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
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Helper class to invalidate units in a set based on errors or references to
 * other invalidate units.
 */
public class CompilationUnitInvalidator {

  /**
   * Mutates {@code units} by retaining only valid units.  A unit is invalid if
   * it
   * <ul>
   * <li>has errors,
   * <li>depends on a member of {@code validClasses} that has errors,
   * <li>depends on another member of {@code units} that has errors, or
   * <li>references a source name that is not provided by a good member of
   * {@code units} or {@code validClasses}.
   * </ul>
   */
  public static void retainValidUnits(TreeLogger logger,
      Collection<CompilationUnit> units, Map<String, CompiledClass> validClasses) {
    logger = logger.branch(TreeLogger.TRACE, "Removing invalidated units");

    // Build a map of api-refs -> dependent units.
    // This map excludes refs provided by good validClasses; it only contains
    // dependencies that need to be provided by members of units.
    Multimap<String, CompilationUnit> depsNeeded = HashMultimap.create();

    // This set contains the source names of types provided by units, and after
    // initial population, may shrink as problems are discovered in individual
    // units.
    Set<String> depsProvided = new HashSet<String>();

    // For fast membership checking of the initial set of units
    Set<CompilationUnit> initialUnits = (units instanceof Set)
        ? (Set<CompilationUnit>) units
        : ImmutableSet.<CompilationUnit>copyOf(units);

    // These are all of the invalid units
    Set<CompilationUnit> allBrokenUnits = new HashSet<CompilationUnit>();

    // Populate depsNeeded, depsProvided, and allBrokenUnits with their initial values.
    // At first, only compilation units that directly contain an error are known to be
    // broken, not their dependencies.
    for (CompilationUnit unit : units) {
      if (unit.isError()) {
        // It is bad and can be removed immediately
        allBrokenUnits.add(unit);
      } else {
        // Update set of dependencies the unit provides
        for (CompiledClass cc : unit.getCompiledClasses()) {
          depsProvided.add(cc.getSourceName());
        }

        // Update map of dependencies that the unit needs
        for (String ref : unit.getDependencies().getApiRefs()) {
          // Check validClasses
          CompiledClass compiledClass = validClasses.get(ref);
          if ((compiledClass == null)
              || compiledClass.getUnit().isError()
              || initialUnits.contains(compiledClass.getUnit())) {
            // we'll put this into the double-check pot
            depsNeeded.put(ref, unit);
          }
        }
      }
    }

    // Repeatedly remove CompilationUnits that have a dependency that's known to
    // be broken.
    Multimap<String, CompilationUnit> missing;
    do {
      // Find the missing deps for this pass
      missing = HashMultimap.create();
      missing.putAll(depsNeeded);
      missing.keySet().removeAll(depsProvided);

      // Process the units with missing deps
      for (Map.Entry<String, CompilationUnit> brokenEntry : missing.entries()) {
        CompilationUnit brokenUnit = brokenEntry.getValue();

        // Modify depsProvided for newly discovered broken units
        // (side-effect add in 'if' condition)
        if (allBrokenUnits.add(brokenUnit)) {
          // Remove the broken unit from the provides set
          for (CompiledClass cc : brokenUnit.getCompiledClasses()) {
            depsProvided.remove(cc.getSourceName());
          }

          // Log it to maintain some logging compatibility with prior versions
          // of this class.
          TreeLogger branch = logger.branch(TreeLogger.DEBUG,
              "Compilation unit '" + brokenUnit
              + "' is removed due to invalid reference(s):");
          branch.log(TreeLogger.DEBUG, brokenEntry.getKey());
        }
      }

      // Having found and removed some units with missing deps, remove their
      // needs.
      depsNeeded.keySet().removeAll(missing.keySet());
    } while (!missing.isEmpty());

    units.removeAll(allBrokenUnits);
  }
}
