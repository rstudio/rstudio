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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.CompilationUnitBuilder.GeneratedCompilationUnit;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.InternalCompilerException.NodeInfo;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.util.Messages;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Handles some details of reporting errors in {@link CompilationUnit}s to the
 * console.
 */
public class CompilationProblemReporter {

  /**
   * Used as a convenience to catch all exceptions thrown by the compiler. For
   * instances of {@link InternalCompilerException}, extra diagnostics are
   * printed.
   * 
   * @param logger logger used to report errors to the console
   * @param e the exception to analyze and log
   * @return Always returns an instance of {@link UnableToCompleteException} so
   *         that the calling method can declare a more narrow 'throws
   *         UnableToCompleteException'
   */
  public static UnableToCompleteException logAndTranslateException(TreeLogger logger, Throwable e) {
    if (e instanceof UnableToCompleteException) {
      // just rethrow
      return (UnableToCompleteException) e;
    } else if (e instanceof InternalCompilerException) {
      TreeLogger topBranch =
          logger.branch(TreeLogger.ERROR, "An internal compiler exception occurred", e);
      List<NodeInfo> nodeTrace = ((InternalCompilerException) e).getNodeTrace();
      for (NodeInfo nodeInfo : nodeTrace) {
        SourceInfo info = nodeInfo.getSourceInfo();
        String msg;
        if (info != null) {
          String fileName = info.getFileName();
          fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
          fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
          msg = "at " + fileName + "(" + info.getStartLine() + "): ";
        } else {
          msg = "<no source info>: ";
        }

        String description = nodeInfo.getDescription();
        if (description != null) {
          msg += description;
        } else {
          msg += "<no description available>";
        }
        TreeLogger nodeBranch = topBranch.branch(TreeLogger.ERROR, msg, null);
        String className = nodeInfo.getClassName();
        if (className != null) {
          nodeBranch.log(TreeLogger.INFO, className, null);
        }
      }
      return new UnableToCompleteException();
    } else if (e instanceof VirtualMachineError) {
      // Always rethrow VM errors (an attempt to wrap may fail).
      throw (VirtualMachineError) e;
    } else {
      logger.log(TreeLogger.ERROR, "Unexpected internal compiler error", e);
      return new UnableToCompleteException();
    }
  }

  /**
   * Provides a meaningful error message when a type is missing from the
   * {@link com.google.gwt.core.ext.typeinfo.TypeOracle} or
   * {@link com.google.gwt.dev.shell.CompilingClassLoader}.
   * 
   * @param logger logger for logging errors to the console
   * @param missingType The qualified source name of the type to report
   */
  public static void logMissingTypeErrorWithHints(TreeLogger logger, String missingType,
      CompilationState compilationState) {
    logDependentErrors(logger, missingType, compilationState);
    logger = logger.branch(TreeLogger.ERROR, "Unable to find type '" + missingType + "'", null);

    ClassLoader cl = Thread.currentThread().getContextClassLoader();

    URL sourceURL = Util.findSourceInClassPath(cl, missingType);
    if (sourceURL != null) {
      Messages.HINT_PRIOR_COMPILER_ERRORS.log(logger, null);
      if (missingType.indexOf(".client.") != -1) {
        Messages.HINT_CHECK_MODULE_INHERITANCE.log(logger, null);
      } else {
        Messages.HINT_CHECK_MODULE_NONCLIENT_SOURCE_DECL.log(logger, null);
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

  /**
   * Logs errors to the console.
   * 
   * @param logger logger for reporting errors to the console
   * @param unit Compilation unit that may have errors
   * @param suppressErrors Controls he log level for logging errors. If
   *          <code>false</code> is passed, compilation errors are logged at
   *          TreeLogger.ERROR and warnings logged at TreeLogger.WARN. If
   *          <code>true</code> is passed, compilation errors are logged at
   *          TreeLogger.TRACE and TreeLogger.DEBUG.
   * @return <code>true</code> if an error was logged.
   */
  public static boolean reportErrors(TreeLogger logger, CompilationUnit unit, boolean suppressErrors) {
    CategorizedProblem[] problems = unit.getProblems();
    if (problems == null || problems.length == 0) {
      return false;
    }
    String fileName = unit.getResourceLocation();
    boolean isError = unit.isError();
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

    if (branch != null && branch.isLoggable(TreeLogger.INFO)) {
      if (unit instanceof GeneratedCompilationUnit) {
        GeneratedCompilationUnit generatedUnit = (GeneratedCompilationUnit) unit;
        CompilationProblemReporter.maybeDumpSource(branch, generatedUnit.getSource(), unit
            .getTypeName());
      }
    }
    return branch != null;
  }

  private static void addUnitToVisit(Map<String, CompiledClass> classMap, String typeName,
      Queue<CompilationUnit> toVisit, Set<CompilationUnit> visited) {
    CompiledClass found = classMap.get(typeName);
    if (found != null) {
      CompilationUnit unit = found.getUnit();
      if (!visited.contains(unit)) {
        toVisit.add(unit);
        visited.add(unit);
      }
    }
  }

  private static void logDependentErrors(TreeLogger logger, String missingType,
      CompilationState compilationState) {
    final Set<CompilationUnit> visited = new HashSet<CompilationUnit>();
    final Queue<CompilationUnit> toVisit = new LinkedList<CompilationUnit>();
    Map<String, CompiledClass> classMap = compilationState.getClassFileMapBySource();

    /*
     * Traverses CompilationUnits enqueued in toVisit(), calling {@link
     * #addUnitsToVisit(String)} as it encounters dependencies on the node. Each
     * CompilationUnit is visited only once, and only if it is reachable via the
     * {@link Dependencies} graph.
     */
    addUnitToVisit(classMap, missingType, toVisit, visited);

    while (!toVisit.isEmpty()) {
      CompilationUnit unit = toVisit.remove();
      CompilationProblemReporter.reportErrors(logger, unit, false);

      for (String apiRef : unit.getDependencies().getApiRefs()) {
        addUnitToVisit(classMap, apiRef, toVisit, visited);
      }
    }
    logger.log(TreeLogger.DEBUG, "Checked " + visited.size() + " dependencies for errors.");
  }

  /**
   * Give the developer a chance to see the in-memory source that failed.
   */
  private static void maybeDumpSource(TreeLogger logger, String source, String typeName) {
    File tmpSrc;
    Throwable caught = null;
    try {
      // The tempFile prefix must be at least 3 characters
      while (typeName.length() < 3) {
        typeName = "_" + typeName;
      }
      tmpSrc = File.createTempFile(typeName, ".java");
      Util.writeStringAsFile(tmpSrc, source);
      String dumpPath = tmpSrc.getAbsolutePath();
      if (logger.isLoggable(TreeLogger.INFO)) {
        logger.log(TreeLogger.INFO, "See snapshot: " + dumpPath, null);
      }
      return;
    } catch (IOException e) {
      caught = e;
    }
    logger.log(TreeLogger.INFO, "Unable to dump source to disk", caught);
  }
}
