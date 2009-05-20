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
import com.google.gwt.dev.util.HtmlTextOutput;
import com.google.gwt.util.tools.Utility;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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

  private HtmlTextOutput htmlOut;
  private PrintWriter pw;

  private OutputStreamWriter writer;

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
      writer = new OutputStreamWriter(new GZIPOutputStream(out), "UTF-8");
      pw = new PrintWriter(writer);
      htmlOut = new HtmlTextOutput(pw, false);
    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Could not open dependency file.", e);
    }

    printPre();
    for (JMethod method : jprogram.getAllEntryMethods()) {
      dependencyAnalyzer.traverseFrom(method);
    }
    printPost();
    pw.close();
    Utility.close(writer);

    logger.log(TreeLogger.INFO, "Done");
  }

  /**
   * Prints the control-flow dependencies to a file in a specific format.
   * 
   * @param liveMethod
   * @param dependencyChain
   */
  private void printMethodDependency(ArrayList<JMethod> dependencyChain) {
    String curLine;
    for (int i = dependencyChain.size() - 1; i >= 0; i--) {
      JMethod curMethod = dependencyChain.get(i);
      String sFullMethodString = curMethod.getName();
      if (curMethod.getEnclosingType() != null) {
        sFullMethodString = curMethod.getEnclosingType().getName() + "::"
            + curMethod.getName();
      }
      if (i == dependencyChain.size() - 1) {
        curLine = "<method name=\"" + sFullMethodString + "\">";
        htmlOut.printRaw(curLine);
        htmlOut.newline();
        htmlOut.indentIn();
        htmlOut.indentIn();
      } else {
        curLine = "<called by=\"" + sFullMethodString + "\"/>";
        htmlOut.printRaw(curLine);
        htmlOut.newline();
      }
    }
    htmlOut.indentOut();
    htmlOut.indentOut();
    curLine = "</method>";
    htmlOut.printRaw(curLine);
    htmlOut.newline();
  }

  /**
   * Prints the closing lines necessary for the dependencies file.
   */
  private void printPost() {
    String curLine = "</soyc-dependencies>";
    htmlOut.indentOut();
    htmlOut.indentOut();
    htmlOut.printRaw(curLine);
    htmlOut.newline();
  }

  /**
   * Prints the preamble necessary for the dependencies file.
   */
  private void printPre() {
    String curLine = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    htmlOut.printRaw(curLine);
    htmlOut.newline();
    curLine = "<soyc-dependencies>";
    htmlOut.printRaw(curLine);
    htmlOut.newline();
    htmlOut.indentIn();
    htmlOut.indentIn();
  }
}
