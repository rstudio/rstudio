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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Helper class to invalidate units in a set based on errors or references to
 * other invalidate units.
 */
public class CompilationUnitInvalidator {

  public static void retainValidUnits(TreeLogger logger,
      Collection<CompilationUnit> units, Map<String, CompiledClass> validClasses) {
    logger = logger.branch(TreeLogger.TRACE, "Removing invalidated units");

    // Assume all units are valid at first.
    Set<CompilationUnit> currentlyValidUnits = new LinkedHashSet<CompilationUnit>();
    Set<String> currentlyValidClasses = new HashSet<String>();
    for (CompilationUnit unit : units) {
      if (!unit.isError()) {
        currentlyValidUnits.add(unit);
        for (CompiledClass cc : unit.getCompiledClasses()) {
          currentlyValidClasses.add(cc.getSourceName());
        }
      }
    }
    for (Entry<String, CompiledClass> entry : validClasses.entrySet()) {
      if (!entry.getValue().getUnit().isError()) {
        currentlyValidClasses.add(entry.getKey());
      }
    }

    boolean changed;
    do {
      changed = false;
      for (Iterator<CompilationUnit> it = currentlyValidUnits.iterator(); it.hasNext();) {
        CompilationUnit unitToCheck = it.next();
        List<String> invalidRefs = unitToCheck.getDependencies().findMissingApiRefs(
            currentlyValidClasses);
        if (invalidRefs.size() > 0) {
          it.remove();
          for (CompiledClass cc : unitToCheck.getCompiledClasses()) {
            currentlyValidClasses.remove(cc.getSourceName());
          }
          changed = true;
          TreeLogger branch = logger.branch(TreeLogger.DEBUG,
              "Compilation unit '" + unitToCheck
                  + "' is removed due to invalid reference(s):");
          for (String ref : invalidRefs) {
            branch.log(TreeLogger.DEBUG, ref);
          }
        }
      }
    } while (changed);

    units.retainAll(currentlyValidUnits);
  }
}
