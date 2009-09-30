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
import com.google.gwt.dev.jjs.impl.ReplaceRunAsyncs.RunAsyncReplacement;
import com.google.gwt.dev.util.HtmlTextOutput;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.util.tools.Utility;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Records split points to a file for SOYC reports.
 */
public class SplitPointRecorder {
  /**
   * Used to record (runAsync) split points of a program.
   */
  public static void recordSplitPoints(JProgram jprogram, OutputStream out,
      TreeLogger logger) {

    logger = logger.branch(TreeLogger.TRACE,
        "Creating split point map file for the compile report");

    try {
      OutputStreamWriter writer = new OutputStreamWriter(new GZIPOutputStream(
          out), "UTF-8");
      PrintWriter pw = new PrintWriter(writer);
      HtmlTextOutput htmlOut = new HtmlTextOutput(pw, false);
      String curLine = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      curLine = "<soyc>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();
      htmlOut.indentIn();
      htmlOut.indentIn();

      Map<Integer, String> splitPointMap = splitPointNames(jprogram);
      if (splitPointMap.size() > 0) {
        curLine = "<splitpoints>";
        htmlOut.printRaw(curLine);
        htmlOut.newline();
        htmlOut.indentIn();
        htmlOut.indentIn();
        for (int sp = 1; sp <= splitPointMap.size(); sp++) {
          String location = splitPointMap.get(sp);
          assert location != null;
          curLine = "<splitpoint id=\"" + sp + "\" location=\"" + location
              + "\"/>";
          htmlOut.printRaw(curLine);
          htmlOut.newline();
          logger.log(TreeLogger.TRACE, "Assigning split point #" + sp
              + " in method " + location);
        }
        htmlOut.indentOut();
        htmlOut.indentOut();
        curLine = "</splitpoints>";
        htmlOut.printRaw(curLine);
        htmlOut.newline();
      }

      if (!jprogram.getSplitPointInitialSequence().isEmpty()) {
        curLine = "<initialseq>";
        htmlOut.printRaw(curLine);
        htmlOut.newline();
        htmlOut.indentIn();

        for (int sp : jprogram.getSplitPointInitialSequence()) {
          curLine = "<splitpointref id=\"" + sp + "\"/>";
          htmlOut.printRaw(curLine);
          htmlOut.newline();
        }

        htmlOut.indentOut();
        curLine = "</initialseq>";
        htmlOut.printRaw(curLine);
        htmlOut.newline();
      }

      htmlOut.indentOut();
      htmlOut.indentOut();
      curLine = "</soyc>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      Utility.close(writer);
      pw.close();

      logger.log(TreeLogger.INFO, "Done");

    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Could not open dependency file.", e);
    }
  }

  private static String fullMethodDescription(JMethod method) {
    return (method.getEnclosingType().getName() + "." + JProgram.getJsniSig(method));
  }

  /**
   * Choose human-readable names for the split points.
   */
  private static Map<Integer, String> splitPointNames(JProgram program) {
    Map<Integer, String> names = new HashMap<Integer, String>();
    Map<String, Integer> counts = new HashMap<String, Integer>();
    for (RunAsyncReplacement replacement : program.getRunAsyncReplacements().values()) {
      int entryNumber = replacement.getNumber();
      String methodDescription;
      if (replacement.getName() != null) {
        methodDescription = replacement.getName();
      } else {
        methodDescription = "@"
            + fullMethodDescription(replacement.getEnclosingMethod());
        if (counts.containsKey(methodDescription)) {
          counts.put(methodDescription, counts.get(methodDescription) + 1);
          methodDescription += "#"
              + Integer.toString(counts.get(methodDescription));
        } else {
          counts.put(methodDescription, 1);
        }
      }

      names.put(entryNumber, methodDescription);
    }

    return names;
  }

  private SplitPointRecorder() {
  }
}
