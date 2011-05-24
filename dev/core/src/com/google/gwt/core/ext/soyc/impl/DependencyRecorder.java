/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.ext.soyc.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.impl.CodeSplitter.MultipleDependencyGraphRecorder;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

/**
 * The control-flow dependency recorder for Compile Report.
 */
public class DependencyRecorder implements MultipleDependencyGraphRecorder {
  /**
   * DependencyRecorder is not allowed to throw checked exceptions, because if
   * it did then {@link com.google.gwt.dev.jjs.impl.CodeSplitter} and
   * {@link ControlFlowAnalyzer} would throw exceptions all over the place.
   * Instead, this class throws NestedIOExceptions that wrap them.
   */
  public static class NestedIOException extends RuntimeException {
    public NestedIOException(IOException e) {
      super(e);
    }
  }

  private final StringBuilder builder = new StringBuilder();
  private final OutputStream finalOutput;
  private OutputStreamWriter writer;

  public DependencyRecorder(OutputStream out) {
    this.finalOutput = out;
  }

  public void close() {
    printPost();
    flushOutput();
    try {
      writer.close();
    } catch (IOException e) {
      throw new NestedIOException(e);
    }
  }

  public void endDependencyGraph() {
    builder.append("</table>");
    flushOutput();
  }

  /**
   * Used to record the dependencies of a specific method.
   */
  public void methodIsLiveBecause(JMethod liveMethod, ArrayList<JMethod> dependencyChain) {
    printMethodDependency(dependencyChain);
  }

  public void open() {
    try {
      this.writer = new OutputStreamWriter(new GZIPOutputStream(finalOutput), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new InternalCompilerException("UTF-8 is an unsupported encoding", e);
    } catch (IOException e) {
      throw new NestedIOException(e);
    }

    printPre();
  }

  public void startDependencyGraph(String identifier, String extnds) {
    builder.append("<table name=\"");
    builder.append(identifier);
    builder.append("\"");
    if (extnds != null) {
      builder.append(" extends=\"");
      builder.append(extnds);
      builder.append("\"");
    }
    builder.append(">\n");
  }

  /**
   * Used to record dependencies of a program.
   */
  protected void recordDependenciesImpl(TreeLogger logger, JProgram jprogram) {

    logger = logger.branch(TreeLogger.DEBUG, "Creating dependencies file for the compile report");

    ControlFlowAnalyzer dependencyAnalyzer = new ControlFlowAnalyzer(jprogram);
    dependencyAnalyzer.setDependencyRecorder(this);

    try {
      printPre();
      for (JMethod method : jprogram.getEntryMethods()) {
        dependencyAnalyzer.traverseFrom(method);
        maybeFlushOutput();
      }
      for (JRunAsync runAsync : jprogram.getRunAsyncs()) {
        dependencyAnalyzer.traverseFromRunAsync(runAsync);
        maybeFlushOutput();
      }
      printPost();

      flushOutput();
      Utility.close(writer);

    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Could not write dependency file.", e);
    }
  }

  private void flushOutput() {
    try {
      writer.write(builder.toString());
    } catch (IOException e) {
      throw new NestedIOException(e);
    }
    builder.setLength(0);
  }

  private void maybeFlushOutput() {
    if (builder.length() > 8 * 1024) {
      flushOutput();
    }
  }

  /**
   * Records one dependency chain to a file. More specifically, it records the
   * last link of the dependency chain. The full dependency chain can be
   * recovered by code that reads the entire dependencies file, because it can
   * do repeated lookups into the dependencies table to follow the chain.
   */
  private void printMethodDependency(ArrayList<JMethod> dependencyChain) {
    int size = dependencyChain.size();
    if (size < 2) {
      return;
    }

    JMethod curMethod = dependencyChain.get(size - 1);
    builder.append("<method name=\"");
    if (curMethod.getEnclosingType() != null) {
      builder.append(curMethod.getEnclosingType().getName());
      builder.append("::");
    }
    builder.append(curMethod.getName());
    builder.append("\">\n");

    JMethod depMethod = dependencyChain.get(size - 2);
    builder.append("<called by=\"");
    if (depMethod.getEnclosingType() != null) {
      builder.append(depMethod.getEnclosingType().getName());
      builder.append("::");
    }
    builder.append(depMethod.getName());
    builder.append("\"/>\n");
    builder.append("</method>\n");
  }

  /**
   * Prints the closing lines necessary for the dependencies file.
   */
  private void printPost() {
    builder.append("</soyc-dependencies>\n");
  }

  /**
   * Prints the preamble necessary for the dependencies file.
   */
  private void printPre() {
    builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<soyc-dependencies>\n");
  }
}
