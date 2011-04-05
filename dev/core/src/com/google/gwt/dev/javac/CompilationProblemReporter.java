/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dev.util.Messages;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Handles some details of reporting errors in {@link CompilationUnit}s to the
 * console.
 */
public class CompilationProblemReporter {

  /**
   * Used to lazily retrieve source if needed for reporting an error.
   */
  public interface SourceFetcher {
    String getSource();
  }

  /**
   * Provides a meaningful error message when a type is missing from the {@link
   * import com.google.gwt.core.ext.typeinfo.TypeOracle} or
   * {@link com.google.gwt.dev.shell.CompilingClassLoader}.
   * 
   * @param logger logger for logging errors to the console
   * @param missingType The qualified source name of the type to report
   * @param unitMap if available, pass
   *          {@link CompilationState#getCompilationUnitMap()}.
   */
  public static void logMissingTypeErrorWithHints(TreeLogger logger, String missingType,
      CompilationState compilationState) {
    logDependentErrors(logger, missingType, compilationState);
    logger = logger.branch(TreeLogger.ERROR, "Unable to find type '" + missingType + "'", null);

    ClassLoader cl = Thread.currentThread().getContextClassLoader();

    URL sourceURL = Util.findSourceInClassPath(cl, missingType);
    if (sourceURL != null) {
      if (missingType.indexOf(".client.") != -1) {
        Messages.HINT_PRIOR_COMPILER_ERRORS.log(logger, null);
        Messages.HINT_CHECK_MODULE_INHERITANCE.log(logger, null);
      } else {
        // Give the best possible hint here.
        //
        if (Util.findSourceInClassPath(cl, missingType) == null) {
          Messages.HINT_CHECK_MODULE_NONCLIENT_SOURCE_DECL.log(logger, null);
        } else {
          Messages.HINT_PRIOR_COMPILER_ERRORS.log(logger, null);
        }
      }
    } else if (!missingType.equals("java.lang.Object")) {
      Messages.HINT_CHECK_TYPENAME.log(logger, missingType, null);
      Messages.HINT_CHECK_CLASSPATH_SOURCE_ENTRIES.log(logger, null);
    }

    /*
     * For missing JRE emulated classes (e.g. Object), or the main GWT
     * libraries, there are special warnings.
     */
    if (missingType.indexOf("java.lang.") == 0 || missingType.indexOf("com.google.gwt.core.") == 0) {
      Messages.HINT_CHECK_INHERIT_CORE.log(logger, null);
    } else if (missingType.indexOf("com.google.gwt.user.") == 0) {
      Messages.HINT_CHECK_INHERIT_USER.log(logger, null);
    }
  }

  public static void reportAllErrors(TreeLogger logger, CompilationState compilationState,
      boolean suppressErrors) {
    for (CompilationUnit unit : compilationState.getCompilationUnits()) {
      if (unit.isError()) {
        reportErrors(logger, unit, suppressErrors);
      }
    }
  }

  /**
   * Report an error in a compilation unit to the console.
   * 
   * @param logger logger for reporting errors to the console
   * @param problems problems to report on the console.
   * @param fileName Name of the source file for the unit where the problem
   *          originated.
   * @param isError <code>true</code> if this is considered a fatal compilation
   *          error.
   * @param supressErrors Controls the log level for logging errors. See
   *          {@link #reportErrors(TreeLogger, CompilationUnit, boolean)}.
   * @return a branch of the logger parameter for logging further problems.
   */
  public static TreeLogger reportErrors(TreeLogger logger, CategorizedProblem[] problems,
      String fileName, boolean isError, SourceFetcher fetcher, String typeName,
      boolean suppressErrors) {
    if (problems == null || problems.length == 0) {
      return null;
    }
    TreeLogger.Type warnLogLevel;
    TreeLogger.Type errorLogLevel;
    if (suppressErrors) {
      errorLogLevel = TreeLogger.TRACE;
      warnLogLevel = TreeLogger.DEBUG;
    } else {
      errorLogLevel = TreeLogger.ERROR;
      warnLogLevel = TreeLogger.WARN;
    }

    TreeLogger branch = null;
    // Log the errors and GWT warnings.
    for (CategorizedProblem problem : problems) {
      TreeLogger.Type logLevel;
      if (problem.isError()) {
        // Log errors.
        logLevel = errorLogLevel;
        // Only log GWT-specific warnings.
      } else if (problem.isWarning() && problem instanceof GWTProblem) {
        logLevel = warnLogLevel;
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
        Type branchType = isError ? errorLogLevel : warnLogLevel;
        String branchString = isError ? "Errors" : "Warnings";
        branch = logger.branch(branchType, branchString + " in '" + fileName + "'", null);
      }
      branch.log(logLevel, msgBuf.toString(), null, helpInfo);
    }

    if (branch != null && fetcher != null) {
      CompilationProblemReporter.maybeDumpSource(branch, fileName, fetcher, typeName);
    }

    return branch;
  }

  /**
   * Logs errors to the console.
   * 
   * @param logger logger for reporting errors to the console
   * @param unit Compilation unit that may have errors
   * @param supressErrors Controls he log level for logging errors. If
   *          <code>false</code> is passed, compilation errors are logged at
   *          TreeLogger.ERROR and warnings logged at TreeLogger.WARN. If
   *          <code>true</code> is passed, compilation errors are logged at
   *          TreeLogger.TRACE and TreeLogger.DEBUG.
   * @return <code>true</code> if an error was logged.
   */
  @SuppressWarnings("deprecation")
  public static boolean reportErrors(TreeLogger logger, final CompilationUnit unit,
      boolean suppressErrors) {
    CategorizedProblem[] problems = unit.getProblems();
    if (problems == null || problems.length == 0) {
      return false;
    }
    TreeLogger branch =
        CompilationProblemReporter.reportErrors(logger, unit.getProblems(), unit
            .getResourceLocation(), unit.isError(), new SourceFetcher() {

          public String getSource() {
            return unit.getSource();
          }

        }, unit.getTypeName(), suppressErrors);
    return branch != null;
  }

  private static void addUnitToVisit(Map<String, CompilationUnit> unitMap, String typeName,
      Queue<CompilationUnit> toVisit) {
    CompilationUnit found = unitMap.get(typeName);
    if (found != null) {
      toVisit.add(found);
    }
  }

  private static boolean isCompilationUnitOnDisk(String loc) {
    try {
      if (new File(loc).exists()) {
        return true;
      }

      URL url = new URL(loc);
      String s = url.toExternalForm();
      if (s.startsWith("file:") || s.startsWith("jar:file:") || s.startsWith("zip:file:")) {
        return true;
      }
    } catch (MalformedURLException e) {
      // Probably not really on disk.
    }
    return false;
  }

  private static void logDependentErrors(TreeLogger logger, String missingType,
      CompilationState compilationState) {
    final Set<CompilationUnit> visited = new HashSet<CompilationUnit>();
    final Queue<CompilationUnit> toVisit = new LinkedList<CompilationUnit>();

    Map<String, CompilationUnit> unitMap = compilationState.unitMap;

    /*
     * Traverses CompilationUnits enqueued in toVisit(), calling {@link
     * #addUnitsToVisit(String)} as it encounters dependencies on the node. Each
     * CompilationUnit is visited only once, and only if it is reachable via the
     * {@link Dependencies} graph.
     */
    addUnitToVisit(unitMap, missingType, toVisit);

    while (!toVisit.isEmpty()) {
      CompilationUnit unit = toVisit.remove();
      if (visited.contains(unit)) {
        continue;
      }
      visited.add(unit);
      CompilationProblemReporter.reportErrors(logger, unit, false);

      Dependencies deps = unit.getDependencies();
      for (String ref : deps.getApiRefs()) {
        addUnitToVisit(unitMap, ref, toVisit);
      }
    }
  }

  /**
   * Give the developer a chance to see the in-memory source that failed.
   */
  private static void maybeDumpSource(TreeLogger logger, String location, SourceFetcher fetcher,
      String typeName) {

    if (location.startsWith("/mock/")) {
      // Unit test mocks, don't dump to disk.
      return;
    }

    if (CompilationProblemReporter.isCompilationUnitOnDisk(location)) {
      // Don't write another copy.
      return;
    }

    if (!logger.isLoggable(TreeLogger.INFO)) {
      // Don't bother dumping source if they can't see the related message.
      return;
    }

    File tmpSrc;
    Throwable caught = null;
    try {
      // The tempFile prefix must be at least 3 characters
      while (typeName.length() < 3) {
        typeName = "_" + typeName;
      }
      tmpSrc = File.createTempFile(typeName, ".java");
      Util.writeStringAsFile(tmpSrc, fetcher.getSource());
      String dumpPath = tmpSrc.getAbsolutePath();
      logger.log(TreeLogger.INFO, "See snapshot: " + dumpPath, null);
      return;
    } catch (IOException e) {
      caught = e;
    }
    logger.log(TreeLogger.INFO, "Unable to dump source to disk", caught);
  }
}
