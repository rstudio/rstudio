/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.converter;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.resources.css.GenerateCssAst;
import com.google.gwt.resources.css.ast.CssStylesheet;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;

/**
 * Converter from Css to Gss.
 */
public class Css2Gss {
  private final URL cssFile;
  private final TreeLogger treeLogger;
  private final boolean lenient;

  private PrintWriter printWriter;
  private Map<String, String> defNameMapping;

  public Css2Gss(String filePath) throws MalformedURLException {
    this(new File(filePath).toURI().toURL(), false);
  }

  public Css2Gss(URL fileUrl, TreeLogger treeLogger) {
    this(fileUrl, treeLogger, false);
  }

  public Css2Gss(URL fileUrl, TreeLogger treeLogger, boolean lenient) {
    cssFile = fileUrl;
    this.treeLogger = treeLogger;
    this.lenient = lenient;
  }

  public Css2Gss(URL resource, boolean lenient) {
    cssFile = resource;
    printWriter = new PrintWriter(System.err);
    this.treeLogger = new PrintWriterTreeLogger(printWriter);
    this.lenient = lenient;
  }

  public String toGss() throws UnableToCompleteException {
      try {
        CssStylesheet sheet = GenerateCssAst.exec(treeLogger, cssFile);

        DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(lenient, treeLogger);
        defCollectorVisitor.accept(sheet);
        defNameMapping = defCollectorVisitor.getDefMapping();

        new UndefinedConstantVisitor(new HashSet<String>(defNameMapping.values()),
            lenient, treeLogger).accept(sheet);

        new ElseNodeCreator().accept(sheet);

        new AlternateAnnotationCreatorVisitor().accept(sheet);

        GssGenerationVisitor gssGenerationVisitor = new GssGenerationVisitor(
            new DefaultTextOutput(false), defNameMapping, lenient, treeLogger);
        gssGenerationVisitor.accept(sheet);

        return gssGenerationVisitor.getContent();
      } finally {
        if (printWriter != null) {
          printWriter.flush();
        }
      }
  }

  /**
   * GSS allows only uppercase letters and numbers for a name of the constant. The constants
   * need to be renamed in order to be compatible with GSS. This method returns a mapping
   * between the old name and the new name compatible with GSS.
   */
  public Map<String, String> getDefNameMapping() {
    return defNameMapping;
  }

  public static void main(String... args) {
    if (args.length != 1) {
      printUsage();
      System.exit(-1);
    }

    try {
      System.out.println(new Css2Gss(args[0]).toGss());
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }

    System.exit(0);
  }

  private static void printUsage() {
    System.err.println("Usage :");
    System.err.println("java " + Css2Gss.class.getName() + " fileNameToConvertPath");
  }
}
