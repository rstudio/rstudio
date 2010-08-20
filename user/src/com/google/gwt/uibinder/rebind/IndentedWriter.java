/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import java.io.PrintWriter;

/**
 * Pleasant wrapper for PrintWriter, manages indentation levels.
 * Name is a misnomer, as this doesn't implement Writer.
 */
public class IndentedWriter {
  private final PrintWriter pw;

  private int indent;

  public IndentedWriter(PrintWriter pw) {
    super();
    this.pw = pw;
  }

  /**
   * Indents the generated code.
   */
  public void indent() {
    ++indent;
  }

  /**
   * Outputs a new line.
   */
  public void newline() {
    // Unix-style line endings for consistent behavior across platforms.
    pw.print('\n');
  }

  /**
   * Un-indents the generated code.
   */
  public void outdent() {
    if (indent == 0) {
      throw new IllegalStateException("Tried to outdent below zero");
    }
    --indent;
  }

  /**
   * Outputs the given string.
   */
  public void write(String format) {
    printIndent();
    pw.print(format);
    newline();
  }

  /**
   * Outputs the given string with replacements, using the Java message format.
   */
  public void write(String format, Object... args) {
    printIndent();
    pw.printf(format, args);
    newline();
  }

  private void printIndent() {
    for (int i = 0; i < indent; ++i) {
      pw.print("  ");
    }
  }
}
