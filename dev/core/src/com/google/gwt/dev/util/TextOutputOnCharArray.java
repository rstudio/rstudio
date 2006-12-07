/*
 * Copyright 2006 Google Inc.
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

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Arrays;

public class TextOutputOnCharArray implements TextOutput {

  private final CharArrayWriter charArrayWriter;
  private final boolean compact;
  private int indent = 0;
  private int indentGranularity;
  private char[][] indents = new char[][] {new char[0]};
  private boolean justNewlined;
  private final PrintWriter p;

  public TextOutputOnCharArray(boolean compact) {
    indentGranularity = 2;
    charArrayWriter = new CharArrayWriter(50 * 1024);
    p = new PrintWriter(charArrayWriter);
    this.compact = compact;
  }

  public int getIndent() {
    return indent;
  }

  public char[] getText() {
    return charArrayWriter.toCharArray();
  }

  public void indentIn() {
    ++indent;
    if (indent >= indents.length) {
      // Cache a new level of indentation string.
      //
      char[] newIndentLevel = new char[indent * indentGranularity];
      Arrays.fill(newIndentLevel, ' ');
      char[][] newIndents = new char[indents.length + 1][];
      System.arraycopy(indents, 0, newIndents, 0, indents.length);
      newIndents[indent] = newIndentLevel;
      indents = newIndents;
    }
  }

  public void indentOut() {
    --indent;
  }

  public void newline() {
    p.print('\n');
    // TODO: remove flush calls
    p.flush();
    justNewlined = true;
  }

  public void newlineOpt() {
    if (!compact) {
      p.print('\n');
      // TODO: remove flush calls
      p.flush();
    }
    justNewlined = true;
  }

  public void print(char c) {
    maybeIndent();
    p.print(c);
    // TODO: remove flush calls
    p.flush();
    justNewlined = false;
  }

  public void print(char[] s) {
    maybeIndent();
    p.print(s);
    // TODO: remove flush calls
    p.flush();
    justNewlined = false;
  }

  public void print(String s) {
    maybeIndent();
    p.print(s);
    // TODO: remove flush calls
    p.flush();
    justNewlined = false;
  }

  public void printOpt(char c) {
    if (!compact) {
      maybeIndent();
      p.print(c);
      // TODO: remove flush calls
      p.flush();
    }
  }

  public void printOpt(char[] s) {
    if (!compact) {
      maybeIndent();
      p.print(s);
      // TODO: remove flush calls
      p.flush();
    }
  }

  public void printOpt(String s) {
    if (!compact) {
      maybeIndent();
      p.print(s);
      // TODO: remove flush calls
      p.flush();
    }
  }

  public void setIndent(int indent) {
    this.indent = indent;
  }

  private void maybeIndent() {
    if (justNewlined && !compact) {
      p.print(indents[indent]);
      // TODO: remove flush calls
      p.flush();
      justNewlined = false;
    }
  }
}
