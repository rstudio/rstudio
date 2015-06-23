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
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.CompilationUnitBuilder.GeneratedCompilationUnit;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.InternalCompilerException.NodeInfo;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.util.Messages;
import com.google.gwt.dev.util.Util;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Handles some details of reporting errors in {@link CompilationUnit}s to the
 * console.
 */
public class CompilationProblemReporter {

  /**
   * Traverses a set of compilation units to record enough information to enable accurate and
   * detailed compilation error cause traces.
   */
  public static void indexErrors(CompilationErrorsIndex compilationErrorsIndex,
      List<CompilationUnit> units) {
    for (CompilationUnit unit : units) {
      if (unit.isError()) {
        Dependencies dependencies = unit.getDependencies();
        compilationErrorsIndex.add(unit.getTypeName(), unit.getResourceLocation(),
            dependencies.getApiRefs(), CompilationProblemReporter.formatErrors(unit));
      }
    }
  }

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
   * Provides meaningful error messages and hints for types that failed to compile or are otherwise
   * missing.
   */
  public static int logErrorTrace(TreeLogger logger, Type logLevel,
      CompilerContext compilerContext, List<CompilationUnit> units, boolean hint) {
    int errorCount = 0;
    for (CompilationUnit unit : units) {
      if (unit.isError()) {
        logErrorTrace(logger, logLevel, compilerContext, unit.getTypeName(), hint);
        errorCount++;

        if (logger.isLoggable(TreeLogger.INFO) && unit instanceof GeneratedCompilationUnit) {
          CompilationProblemReporter.maybeDumpSource(logger,
              ((GeneratedCompilationUnit) unit).getSource(), unit.getTypeName());
        }
      }
    }
    return errorCount++;
  }

  /**
   * Provides a meaning error message and hints for a type that failed to compile or is otherwise
   * missing.
   */
  public static void logErrorTrace(TreeLogger logger, Type logLevel,
      CompilerContext compilerContext, String typeSourceName, boolean hint) {
    TreeLogger branch = logger.branch(TreeLogger.TRACE,
        "Tracing compile failure path for type '" + typeSourceName + "'");
    if (logErrorChain(branch, logLevel, typeSourceName,
        compilerContext.getCompilationErrorsIndex())) {
      return;
    }

    if (hint) {
      logHints(logger, typeSourceName);
    }
  }

  public static int logWarnings(TreeLogger logger, Type logLevelForWarnings,
      List<CompilationUnit> units) {
    int warningCount = 0;
    for (CompilationUnit unit : units) {
      if (CompilationProblemReporter.logWarnings(logger, logLevelForWarnings, unit)) {
        warningCount++;
      }
    }
    return warningCount++;
  }

  /**
   * Logs errors to the console.
   *
   * @param logger logger for reporting errors to the console
   * @param unit Compilation unit that may have errors
   * @param suppressErrors Controls he log level for logging errors. If <code>false</code> is passed,
   *          compilation errors are logged at TreeLogger.ERROR and warnings logged at
   *          TreeLogger.WARN. If <code>true</code> is passed, compilation errors are logged at
   *          TreeLogger.TRACE and TreeLogger.DEBUG.
   * @return <code>true</code> if an error was logged.
   */
  public static boolean reportErrors(TreeLogger logger, CompilationUnit unit,
      boolean suppressErrors) {
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
      branch.log(logLevel, toMessageWithLineNumber(problem), null, helpInfo);
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

  private static void addUnitToVisit(CompilationErrorsIndex compilationErrorsIndex,
      String typeSourceName, Queue<String> toVisit, Set<String> visited) {
    if (compilationErrorsIndex.hasCompileErrors(typeSourceName)) {
      if (!visited.contains(typeSourceName)) {
        toVisit.add(typeSourceName);
        visited.add(typeSourceName);
      }
    }
  }

  /**
   * Returns readable compilation error messages for a compilation unit.
   * <p>
   * Should only be run on CompilationUnits that actually have problems.
   */
  private static List<String> formatErrors(CompilationUnit unit) {
    CategorizedProblem[] problems = unit.getProblems();
    assert problems != null && problems.length > 0;

    List<String> errorMessages = Lists.newArrayList();
    for (CategorizedProblem problem : problems) {
      if (!problem.isError()) {
        continue;
      }

      errorMessages.add(toMessageWithLineNumber(problem));
    }

    return errorMessages;
  }

  private static boolean hasWarnings(CompilationUnit unit) {
    CategorizedProblem[] problems = unit.getProblems();
    if (problems == null || problems.length == 0) {
      return false;
    }
    for (CategorizedProblem problem : problems) {
      if (problem.isWarning() && problem instanceof GWTProblem) {
        return true;
      }
    }
    return false;
  }

  private static boolean logErrorChain(TreeLogger logger, Type logLevel,
      String typeSourceName, CompilationErrorsIndex compilationErrorsIndex) {
    final Set<String> visited = new HashSet<String>();
    final Queue<String> toVisit = new LinkedList<String>();

    /*
     * Traverses CompilationUnits enqueued in toVisit(), calling {@link
     * #addUnitsToVisit(String)} as it encounters dependencies on the node. Each
     * CompilationUnit is visited only once, and only if it is reachable via the
     * {@link Dependencies} graph.
     */
    addUnitToVisit(compilationErrorsIndex, typeSourceName, toVisit, visited);

    while (!toVisit.isEmpty()) {
      String dependentTypeSourceName = toVisit.remove();

      Set<String> compileErrors = compilationErrorsIndex.getCompileErrors(dependentTypeSourceName);
      TreeLogger branch = logger.branch(logLevel,
          "Errors in '" + compilationErrorsIndex.getFileName(dependentTypeSourceName) + "'");
      for (String compileError : compileErrors) {
        branch.log(logLevel, compileError);
      }

      Set<String> typeReferences =
          compilationErrorsIndex.getTypeReferences(dependentTypeSourceName);
      if (typeReferences != null) {
        for (String typeReference : typeReferences) {
          addUnitToVisit(compilationErrorsIndex, typeReference, toVisit, visited);
        }
      }
    }
    logger.log(TreeLogger.DEBUG, "Checked " + visited.size() + " dependencies for errors.");
    return visited.size() > 1;
  }

  private static void logHints(TreeLogger logger, String typeSourceName) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();

    URL sourceURL = Util.findSourceInClassPath(cl, typeSourceName);
    if (sourceURL != null) {
      if (typeSourceName.indexOf(".client.") != -1) {
        Messages.HINT_CHECK_MODULE_INHERITANCE.log(logger, null);
      } else {
        Messages.HINT_CHECK_MODULE_NONCLIENT_SOURCE_DECL.log(logger, null);
      }
    } else if (!typeSourceName.equals("java.lang.Object")) {
      Messages.HINT_CHECK_TYPENAME.log(logger, typeSourceName, null);
      Messages.HINT_CHECK_CLASSPATH_SOURCE_ENTRIES.log(logger, null);
    }

    /*
     * For missing JRE emulated classes (e.g. Object), or the main GWT libraries, there are special
     * warnings.
     */
    if (typeSourceName.indexOf("java.lang.") == 0
        || typeSourceName.indexOf("com.google.gwt.core.") == 0) {
      Messages.HINT_CHECK_INHERIT_CORE.log(logger, null);
    } else if (typeSourceName.indexOf("com.google.gwt.user.") == 0) {
      Messages.HINT_CHECK_INHERIT_USER.log(logger, null);
    }
  }

  private static boolean logWarnings(TreeLogger logger, TreeLogger.Type logLevel,
      CompilationUnit unit) {
    if (!hasWarnings(unit)) {
      return false;
    }

    TreeLogger branch =
        logger.branch(logLevel, "Warnings in '" + unit.getResourceLocation() + "'", null);
    for (CategorizedProblem problem : unit.getProblems()) {
      if (!problem.isWarning() || !(problem instanceof GWTProblem)) {
        continue;
      }

      branch.log(logLevel, toMessageWithLineNumber(problem), null,
          ((GWTProblem) problem).getHelpInfo());
    }

    if (branch.isLoggable(TreeLogger.INFO) && unit instanceof GeneratedCompilationUnit) {
      CompilationProblemReporter.maybeDumpSource(branch,
          ((GeneratedCompilationUnit) unit).getSource(), unit.getTypeName());
    }
    return true;
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

  private static String toMessageWithLineNumber(CategorizedProblem problem) {
    int lineNumber = problem.getSourceLineNumber();
    return (lineNumber > 0 ? "Line " + lineNumber + ": " : "") + problem.getMessage();
  }
}
