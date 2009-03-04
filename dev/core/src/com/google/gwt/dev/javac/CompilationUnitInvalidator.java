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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Helper class to invalidate units in a set based on errors or references to
 * other invalidate units.
 * 
 * TODO: ClassFileReader#hasStructuralChanges(byte[]) could help us optimize
 * this process!
 */
public class CompilationUnitInvalidator {

  /**
   * Maintain cross-validation state.
   */
  public static class InvalidatorState {
    private final JSORestrictionsChecker.CheckerState jsoState = new JSORestrictionsChecker.CheckerState();

    public void retainAll(Collection<CompilationUnit> toRetain) {
      jsoState.retainAll(toRetain);
    }
  }

  /**
   * For all units containing one or more errors whose state is currently
   * {@link State#COMPILED}, each unit's error(s) will be logged to
   * <code>logger</code> and each unit's state will be set to
   * {@link State#ERROR}.
   * 
   * @return <code>true</code> if any units changed state
   */
  public static boolean invalidateUnitsWithErrors(TreeLogger logger,
      Set<CompilationUnit> units) {
    logger = logger.branch(TreeLogger.TRACE, "Removing units with errors");
    // Start by removing units with a known problem.
    boolean anyRemoved = false;
    for (CompilationUnit unit : units) {
      if (unit.getState() != State.COMPILED) {
        continue;
      }
      CompilationResult result = unit.getJdtCud().compilationResult();
      if (result.hasProblems()) {

        // Log the errors and GWT warnings.
        TreeLogger branch = null;
        for (CategorizedProblem problem : result.getProblems()) {
          TreeLogger.Type logLevel;
          if (problem.isError()) {
            // Log errors.
            logLevel = TreeLogger.ERROR;
            // Only log GWT-specific warnings.
          } else if (problem.isWarning() && problem instanceof GWTProblem) {
            logLevel = TreeLogger.WARN;
          } else {
            // Ignore all other problems.
            continue;
          }
          // Append 'Line #: msg' to the error message.
          StringBuffer msgBuf = new StringBuffer();
          int line = problem.getSourceLineNumber();
          if (line > 0) {
            msgBuf.append("Line ");
            msgBuf.append(line);
            msgBuf.append(": ");
          }
          msgBuf.append(problem.getMessage());

          HelpInfo helpInfo = null;
          if (problem instanceof GWTProblem) {
            GWTProblem gwtProblem = (GWTProblem) problem;
            helpInfo = gwtProblem.getHelpInfo();
          }
          if (branch == null) {
            Type branchType = result.hasErrors() ? TreeLogger.ERROR
                : TreeLogger.WARN;
            String branchString = result.hasErrors() ? "Errors" : "Warnings";
            branch = logger.branch(branchType, branchString + " in '"
                + unit.getDisplayLocation() + "'", null);
          }
          branch.log(logLevel, msgBuf.toString(), null, helpInfo);
        }

        if (branch != null) {
          Util.maybeDumpSource(branch, unit.getDisplayLocation(),
              unit.getSource(), unit.getTypeName());
        }

        // Invalidate the unit if there are errors.
        if (result.hasErrors()) {
          unit.setState(State.ERROR);
          anyRemoved = true;
        }
      }
    }

    return anyRemoved;
  }

  public static void invalidateUnitsWithInvalidRefs(TreeLogger logger,
      Set<CompilationUnit> units) {
    logger = logger.branch(TreeLogger.TRACE, "Removing invalidated units");

    // Assume all compiled units are valid at first.
    boolean changed;
    Set<CompilationUnit> validUnits = new HashSet<CompilationUnit>();
    for (CompilationUnit unit : units) {
      if (unit.isCompiled()) {
        validUnits.add(unit);
      }
    }
    do {
      changed = false;
      Set<String> validRefs = new HashSet<String>();
      for (CompilationUnit unit : validUnits) {
        validRefs.add(unit.getDisplayLocation());
      }
      for (Iterator<CompilationUnit> it = validUnits.iterator(); it.hasNext();) {
        CompilationUnit unit = it.next();
        TreeLogger branch = null;
        for (String ref : unit.getFileNameRefs()) {
          if (!validRefs.contains(ref)) {
            if (branch == null) {
              branch = logger.branch(TreeLogger.WARN, "Compilation unit '"
                  + unit + "' is removed due to invalid reference(s):");
              it.remove();
              changed = true;
              unit.setState(State.FRESH);
            }
            branch.log(TreeLogger.WARN, ref);
          }
        }
      }
    } while (changed);
  }

  public static void validateCompilationUnits(InvalidatorState state,
      Set<CompilationUnit> units, Set<String> validBinaryTypeNames) {
    for (CompilationUnit unit : units) {
      if (unit.getState() == State.COMPILED) {
        CompilationUnitDeclaration jdtCud = unit.getJdtCud();
        JSORestrictionsChecker.check(state.jsoState, jdtCud);
        JsniChecker.check(jdtCud);
        BinaryTypeReferenceRestrictionsChecker.check(jdtCud,
            validBinaryTypeNames);
      }
    }
    state.jsoState.finalCheck();
  }
}
