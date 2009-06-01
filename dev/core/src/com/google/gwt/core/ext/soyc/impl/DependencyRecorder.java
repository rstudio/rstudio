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
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;
import com.google.gwt.util.tools.Utility;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

/**
 * The control-flow dependency recorder for SOYC.
 */
public class DependencyRecorder implements
    ControlFlowAnalyzer.DependencyRecorder {

  /**
   * Used to record dependencies of a program.
   */
  public static void recordDependencies(TreeLogger logger, OutputStream out,
      JProgram jprogram) {
    new DependencyRecorder().recordDependenciesImpl(logger, out, jprogram);
  }

  private StringBuilder builder;

  private DependencyRecorder() {
  }

  /**
   * Used to record the dependencies of a specific method.
   * 
   * @param liveMethod
   * @param dependencyChain
   */
  public void methodIsLiveBecause(JMethod liveMethod,
      ArrayList<JMethod> dependencyChain) {
    printMethodDependency(dependencyChain);
  }

  /**
   * Used to record dependencies of a program.
   */
  protected void recordDependenciesImpl(TreeLogger logger, OutputStream out,
      JProgram jprogram) {

    logger = logger.branch(TreeLogger.INFO,
        "Creating Dependencies file for SOYC");

    ControlFlowAnalyzer dependencyAnalyzer = new ControlFlowAnalyzer(jprogram);
    dependencyAnalyzer.setDependencyRecorder(this);

    try {
      OutputStreamWriter writer
         = new OutputStreamWriter(new GZIPOutputStream(out), "UTF-8");
      
      StringBuilder localBuilder = new StringBuilder();
      this.builder = localBuilder;

      printPre();
      for (JMethod method : jprogram.getAllEntryMethods()) {
        dependencyAnalyzer.traverseFrom(method);
        if (localBuilder.length() > 8 * 1024) {
          writer.write(localBuilder.toString());
          localBuilder.setLength(0);
        }
      }
      printPost();

      writer.write(localBuilder.toString());
      Utility.close(writer);

      logger.log(TreeLogger.INFO, "Done");
    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Could not write dependency file.", e);
    }
  }

  /**
   * Prints the control-flow dependencies to a file in a specific format.
   * 
   * @param liveMethod
   * @param dependencyChain
   */
  private void printMethodDependency(ArrayList<JMethod> dependencyChain) {
    int size = dependencyChain.size();
    if (size == 0) {
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

    for (int i = size - 2; i >= 0; i--) {
      curMethod = dependencyChain.get(i);
      builder.append("<called by=\"");
      if (curMethod.getEnclosingType() != null) {
        builder.append(curMethod.getEnclosingType().getName());
        builder.append("::");
      }
      builder.append(curMethod.getName());
      builder.append("\"/>\n");
    }
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
    builder.append(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<soyc-dependencies>\n");
  }
}
