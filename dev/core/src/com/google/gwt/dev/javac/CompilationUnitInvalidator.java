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
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;

import java.util.Collection;
import java.util.Collections;
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

  public static void reportErrors(TreeLogger logger, CompilationUnit unit) {
    reportErrors(logger, unit.getProblems(), unit.getDisplayLocation(),
        unit.isError());
  }

  public static void reportErrors(TreeLogger logger,
      CompilationUnitDeclaration cud, String sourceForDump) {
    CategorizedProblem[] problems = cud.compilationResult().getProblems();
    String fileName = String.valueOf(cud.getFileName());
    boolean isError = cud.compilationResult().hasErrors();
    TreeLogger branch = reportErrors(logger, problems, fileName, isError);
    if (branch != null) {
      Util.maybeDumpSource(branch, fileName, sourceForDump,
          String.valueOf(cud.getMainTypeName()));
    }
  }

  public static void retainValidUnits(Collection<CompilationUnit> units) {
    retainValidUnits(units, Collections.<ContentId> emptySet());
  }

  public static void retainValidUnits(Collection<CompilationUnit> units,
      Set<ContentId> knownValidRefs) {
    // Assume all units are valid at first.
    Set<CompilationUnit> currentlyValidUnits = new HashSet<CompilationUnit>();
    Set<ContentId> currentlyValidRefs = new HashSet<ContentId>(knownValidRefs);
    for (CompilationUnit unit : units) {
      if (unit.isCompiled()) {
        currentlyValidUnits.add(unit);
        currentlyValidRefs.add(unit.getContentId());
      }
    }

    boolean changed;
    do {
      changed = false;
      iterating : for (Iterator<CompilationUnit> it = currentlyValidUnits.iterator(); it.hasNext();) {
        CompilationUnit unitToCheck = it.next();
        for (ContentId ref : unitToCheck.getDependencies()) {
          if (!currentlyValidRefs.contains(ref)) {
            it.remove();
            currentlyValidRefs.remove(unitToCheck.getContentId());
            changed = true;
            continue iterating;
          }
        }
      }
    } while (changed);

    units.retainAll(currentlyValidUnits);
  }

  private static TreeLogger reportErrors(TreeLogger logger,
      CategorizedProblem[] problems, String fileName, boolean isError) {
    if (problems == null || problems.length == 0) {
      return null;
    }
    TreeLogger branch = null;
    // Log the errors and GWT warnings.
    for (CategorizedProblem problem : problems) {
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
        Type branchType = isError ? TreeLogger.ERROR : TreeLogger.WARN;
        String branchString = isError ? "Errors" : "Warnings";
        branch = logger.branch(branchType, branchString + " in '" + fileName
            + "'", null);
      }
      branch.log(logLevel, msgBuf.toString(), null, helpInfo);
    }
    return branch;
  }
}
