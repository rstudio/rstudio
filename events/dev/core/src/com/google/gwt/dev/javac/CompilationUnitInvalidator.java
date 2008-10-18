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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Helper class to invalidate units in a set based on errors or references to
 * other invalidate units.
 * 
 * TODO: {@link ClassFileReader#hasStructuralChanges(byte[])} could help us
 * optimize this process!
 */
public class CompilationUnitInvalidator {

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

  public static void validateCompilationUnits(Set<CompilationUnit> units,
      Set<String> validBinaryTypeNames) {
    for (CompilationUnit unit : units) {
      if (unit.getState() == State.COMPILED) {
        CompilationUnitDeclaration jdtCud = unit.getJdtCud();
        JSORestrictionsChecker.check(jdtCud);
        LongFromJSNIChecker.check(jdtCud);
        BinaryTypeReferenceRestrictionsChecker.check(jdtCud,
            validBinaryTypeNames);
      }
    }
  }
}
