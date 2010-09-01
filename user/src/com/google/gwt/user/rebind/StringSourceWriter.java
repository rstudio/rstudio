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
package com.google.gwt.user.rebind;

import com.google.gwt.core.ext.TreeLogger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A SourceWriter that accumulates source and returns it in the
 * {@link #toString()} method.
 */
public class StringSourceWriter implements SourceWriter {

  private final StringWriter buffer = new StringWriter();
  private int indentLevel = 0;
  private String indentPrefix = "";
  private boolean needsIndent;
  private final PrintWriter out = new PrintWriter(buffer);

  public void beginJavaDocComment() {
    println("/**");
    indent();
    indentPrefix = " * ";
  }

  /**
   * This is a no-op.
   */
  public void commit(TreeLogger logger) {
    out.flush();
  }

  public void endJavaDocComment() {
    out.println("*/");
    outdent();
    indentPrefix = "";
  }

  public void indent() {
    indentLevel++;
  }

  public void indentln(String s) {
    indent();
    println(s);
    outdent();
  }

  public void indentln(String s, Object... args) {
    indentln(String.format(s, args));
  }

  public void outdent() {
    indentLevel = Math.max(indentLevel - 1, 0);
  }

  public void print(String s) {
    maybeIndent();
    out.print(s);
  }

  public void print(String s, Object... args) {
    print(String.format(s, args));
  }

  public void println() {
    maybeIndent();
    // Unix-style line endings for consistent behavior across platforms.
    out.print('\n');
    needsIndent = true;
  }

  public void println(String s) {
    print(s);
    println();
  }

  public void println(String s, Object... args) {
    println(String.format(s, args));
  }

  @Override
  public String toString() {
    out.flush();
    return buffer.getBuffer().toString();
  }

  private void maybeIndent() {
    if (needsIndent) {
      needsIndent = false;
      for (int i = 0; i < indentLevel; i++) {
        out.print("  ");
        out.print(indentPrefix);
      }
    }
  }
}
