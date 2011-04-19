/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.AbstractTextOutput;
import com.google.gwt.dev.util.TextOutput;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A simple utility to dump a JProgram to a temp file, which can be called
 * sequentially during a compilation/optimization run, so intermediate steps can
 * be compared.
 * 
 * It uses the system property "gwt.jjs.dumpAst" to determine the name (or
 * prefix) of the file to dump the AST to.
 * 
 * TODO(jbrosenberg): Add proper logging and/or exception handling for the
 * potential IOException that might occur when writing the file.
 */
public class AstDumper {

  private static int autoVersionNumber = 0;

  /**
   * Appends a new version of the AST at the end of the file, each time it's
   * called.
   */
  public static void maybeDumpAST(JProgram jprogram) {
    maybeDumpAST(jprogram, null, true);
  }

  /**
   * Writes the AST to the file with a versioned extension, using an
   * auto-incrementing version number (starting from 1), each time it's called.
   * Any previous contents of the file written to will be overwritten.
   */
  public static void maybeDumpAST(JProgram jprogram, boolean autoIncrementVersion) {
    if (!autoIncrementVersion) {
      maybeDumpAST(jprogram);
    } else {
      maybeDumpAST(jprogram, autoVersionNumber++);
    }
  }

  /**
   * Writes the AST to the file with the provided version number extension. Any
   * previous contents of the file written to will be overwritten.
   */
  public static void maybeDumpAST(JProgram jprogram, int versionNumber) {
    String fileExtension = "." + versionNumber;
    maybeDumpAST(jprogram, fileExtension, false);
  }

  /**
   * Writes the AST to the file with the provided version string extension. Any
   * previous contents of the file written to will be overwritten.
   */
  public static void maybeDumpAST(JProgram jprogram, String versionString) {
    String fileExtension = "." + versionString;
    maybeDumpAST(jprogram, fileExtension, false);
  }

  private static void maybeDumpAST(JProgram jprogram, String fileExtension, boolean append) {
    String dumpFile = System.getProperty("gwt.jjs.dumpAst");
    if (dumpFile != null) {
      if (fileExtension != null) {
        dumpFile += fileExtension;
      }
      try {
        FileOutputStream os = new FileOutputStream(dumpFile, append);
        final PrintWriter pw = new PrintWriter(os);
        TextOutput out = new AbstractTextOutput(false) {
          {
            setPrintWriter(pw);
          }
        };
        SourceGenerationVisitor v = new SourceGenerationVisitor(out);
        v.accept(jprogram);
        pw.close();
      } catch (IOException e) {
        System.out.println("Could not dump AST");
        e.printStackTrace();
      }
    }
  }
}
