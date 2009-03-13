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
import com.google.gwt.core.ext.soyc.SplitPointRecorder;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.HtmlTextOutput;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Records split points to a file for SOYC reports.
 */
public class SplitPointRecorderImpl implements SplitPointRecorder {

  private FileOutputStream stream;
  private OutputStreamWriter writer;
  private PrintWriter pw;
  private HtmlTextOutput htmlOut;

  /**
   * Used to record (runAsync) split points of a program.
   * 
   * @param jprogram
   * @param workDir
   * @param permutationId
   * @param logger
   * @return The file that the dependencies are recorded in
   */
  public File recordSplitPoints(JProgram jprogram, File workDir,
      int permutationId, TreeLogger logger) {

    logger = logger.branch(TreeLogger.INFO,
        "Creating Split Point Map file for SOYC");

    File splitPointsFile = new File(workDir, "splitPoints"
        + Integer.toString(permutationId) + ".xml.gz");
    try {
      stream = new FileOutputStream(splitPointsFile, true);
      writer = new OutputStreamWriter(new GZIPOutputStream(stream), "UTF-8");
      splitPointsFile.getParentFile().mkdirs();
      pw = new PrintWriter(writer);
      htmlOut = new HtmlTextOutput(pw, false);

      String curLine = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      curLine = "<soyc>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();
      htmlOut.indentIn();
      htmlOut.indentIn();

      Map<Integer, String> splitPointMap = jprogram.getSplitPointMap();
      if (splitPointMap.size() > 0) {
        curLine = "<splitpoints>";
        htmlOut.printRaw(curLine);
        htmlOut.newline();
        htmlOut.indentIn();
        htmlOut.indentIn();
        for (Integer splitPointCount : splitPointMap.keySet()) {
          curLine = "<splitpoint id=\"" + splitPointCount + "\" location=\""
              + splitPointMap.get(splitPointCount) + "\"/>";
          htmlOut.printRaw(curLine);
          htmlOut.newline();
        }
        htmlOut.indentOut();
        htmlOut.indentOut();
        curLine = "</splitpoints>";
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

    return splitPointsFile;
  }
}
