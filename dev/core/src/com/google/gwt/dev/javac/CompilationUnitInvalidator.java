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
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Helper class to invalidate units in a set based on errors or references to
 * other invalidate units.
 * 
 * TODO: {@link ClassFileReader#hasStructuralChanges(byte[])} could help us
 * optimize this process!
 */
public class CompilationUnitInvalidator {

  public static void invalidateUnitsWithErrors(TreeLogger logger,
      Set<CompilationUnit> units) {
    logger = logger.branch(TreeLogger.TRACE, "Removing units with errors");
    // Start by removing units with a known problem.
    boolean anyRemoved = false;
    for (CompilationUnit unit : units) {
      CompilationUnitDeclaration cud = unit.getJdtCud();
      if (cud == null) {
        continue;
      }
      CompilationResult result = cud.compilationResult();
      if (result.hasErrors()) {
        anyRemoved = true;

        // TODO: defer this until later?
        TreeLogger branch = logger.branch(TreeLogger.ERROR, "Errors in '"
            + unit.getDisplayLocation() + "'", null);

        for (CategorizedProblem error : result.getErrors()) {
          // Append 'Line #: msg' to the error message.
          StringBuffer msgBuf = new StringBuffer();
          int line = error.getSourceLineNumber();
          if (line > 0) {
            msgBuf.append("Line ");
            msgBuf.append(line);
            msgBuf.append(": ");
          }
          msgBuf.append(error.getMessage());

          HelpInfo helpInfo = null;
          if (error instanceof GWTProblem) {
            GWTProblem gwtProblem = (GWTProblem) error;
            helpInfo = gwtProblem.getHelpInfo();
          }
          branch.log(TreeLogger.ERROR, msgBuf.toString(), null, helpInfo);
        }

        Util.maybeDumpSource(branch, unit.getDisplayLocation(),
            unit.getSource(), unit.getTypeName());

        // TODO: hold onto errors?
        unit.setState(State.ERROR);
      }
    }

    if (anyRemoved) {
      // Then removing anything else that won't compile as a result.
      invalidateUnitsWithInvalidRefs(logger, units);
    }
  }

  public static void invalidateUnitsWithInvalidRefs(TreeLogger logger,
      Set<CompilationUnit> units) {
    logger = logger.branch(TreeLogger.TRACE, "Removing invalidate units");

    // Map all units by file name.
    Map<String, CompilationUnit> unitsByFileName = new HashMap<String, CompilationUnit>();
    for (CompilationUnit unit : units) {
      unitsByFileName.put(unit.getDisplayLocation(), unit);
    }
    // First, compute a map from all targets all referents.
    Map<CompilationUnit, Set<CompilationUnit>> refTargetToReferents = new HashMap<CompilationUnit, Set<CompilationUnit>>();
    for (CompilationUnit referentUnit : units) {
      if (referentUnit.isCompiled()) {
        Set<String> fileNameRefs = referentUnit.getFileNameRefs();
        for (String fileNameRef : fileNameRefs) {
          CompilationUnit targetUnit = unitsByFileName.get(fileNameRef);
          if (targetUnit != null) {
            Set<CompilationUnit> referents = refTargetToReferents.get(targetUnit);
            if (referents == null) {
              referents = new HashSet<CompilationUnit>();
              refTargetToReferents.put(targetUnit, referents);
            }
            // Add myself as a referent.
            referents.add(referentUnit);
          }
        }
      }
    }

    // Now use the map to transitively blow away invalid units.
    for (Entry<CompilationUnit, Set<CompilationUnit>> entry : refTargetToReferents.entrySet()) {
      CompilationUnit maybeInvalidUnit = entry.getKey();
      if (!maybeInvalidUnit.isCompiled()) {
        // Invalidate all dependent units.
        Set<CompilationUnit> invalidReferentUnits = entry.getValue();
        TreeLogger branch = logger.branch(TreeLogger.TRACE,
            "Compilation unit '" + maybeInvalidUnit + "' is invalid");
        State why = maybeInvalidUnit.getState();
        for (CompilationUnit invalidReferentUnit : invalidReferentUnits) {
          if (invalidReferentUnit.isCompiled()) {
            // Set it to the same state as the unit it depends on.
            invalidReferentUnit.setState(why);
            branch.log(TreeLogger.TRACE, "Removing dependent unit '"
                + invalidReferentUnit + "'");
          }
        }
      }
    }
  }

  public static void validateCompilationUnits(Set<CompilationUnit> units,
      Map<String, CompiledClass> compiledClasses) {
    for (CompilationUnit unit : units) {
      if (unit.getState() != State.CHECKED) {
        CompilationUnitDeclaration jdtCud = unit.getJdtCud();
        JSORestrictionsChecker.check(jdtCud);
        LongFromJSNIChecker.check(jdtCud);
        BinaryTypeReferenceRestrictionsChecker.check(jdtCud,
            compiledClasses.keySet());
      }
    }
  }
}
