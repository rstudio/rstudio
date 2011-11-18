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
package com.google.gwt.dev.util;

import java.io.PrintWriter;
import java.util.Arrays;

/**
 * An abstract base type to build TextOutput implementations.
 */
public abstract class AbstractTextOutput implements TextOutput {
  private final boolean compact;
  private int identLevel = 0;
  private int indentGranularity = 2;
  private char[][] indents = new char[][] {new char[0]};
  private boolean justNewlined;
  private PrintWriter out;
  private int position = 0;
  private int line = 0;
  private int column = 0;

  protected AbstractTextOutput(boolean compact) {
    this.compact = compact;
  }

  public int getColumn() {
    return column;
  }

  public int getLine() {
    return line;
  }

  public int getPosition() {
    return position;
  }

  public void indentIn() {
    ++identLevel;
    if (identLevel >= indents.length) {
      // Cache a new level of indentation string.
      //
      char[] newIndentLevel = new char[identLevel * indentGranularity];
      Arrays.fill(newIndentLevel, ' ');
      char[][] newIndents = new char[indents.length + 1][];
      System.arraycopy(indents, 0, newIndents, 0, indents.length);
      newIndents[identLevel] = newIndentLevel;
      indents = newIndents;
    }
  }

  public void indentOut() {
    --identLevel;
  }

  public void newline() {
    if (compact) {
      out.print('\n');
    } else {
      out.print('\n');
    }
    position++;
    line++;
    column = 0;
    justNewlined = true;
  }

  public void newlineOpt() {
    if (!compact) {
      out.print('\n');
      position++;
      line++;
      column = 0;
      justNewlined = true;
    }
  }

  public void print(char c) {
    maybeIndent();
    out.print(c);
    position++;
    column++;
    justNewlined = false;
  }

  public void print(char[] s) {
    maybeIndent();
    printAndCount(s);
    justNewlined = false;
  }

  public void print(String s) {
    maybeIndent();
    printAndCount(s.toCharArray());
    justNewlined = false;
  }

  public void printOpt(char c) {
    if (!compact) {
      maybeIndent();
      out.print(c);
      position += 1;
      column++;
    }
  }

  public void printOpt(char[] s) {
    if (!compact) {
      maybeIndent();
      printAndCount(s);
    }
  }

  public void printOpt(String s) {
    if (!compact) {
      maybeIndent();
      printAndCount(s.toCharArray());
    }
  }

  protected void setPrintWriter(PrintWriter out) {
    this.out = out;
  }

  private void maybeIndent() {
    if (justNewlined && !compact) {
      printAndCount(indents[identLevel]);
      justNewlined = false;
    }
  }

  private void printAndCount(char[] chars) {
    position += chars.length;
    column += chars.length;
    out.print(chars);
  }
}
