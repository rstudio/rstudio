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

/**
 * Adapts {@link TextOutput} to a character array.
 */
public class TextOutputOnCharArray implements TextOutput {

<<<<<<< .mine
  private final CharArrayWriter fCharArrayWriter;
  private final boolean fCompact;
  private int fIndent = 0;
  private int fIndentGranularity;
  private char[][] fIndents = new char[][] {new char[0]};
  private boolean fJustNewlined;
  private final PrintWriter p;
=======
  private final CharArrayWriter charArrayWriter;
  private final boolean compact;
  private int indent = 0;
  private int indentGranularity;
  private char[][] indents = new char[][] {new char[0]};
  private boolean justNewlined;
  private final PrintWriter p;
>>>>>>> .r47

<<<<<<< .mine
  public TextOutputOnCharArray(boolean compact) {
    fIndentGranularity = 2;
    fCharArrayWriter = new CharArrayWriter(50 * 1024);
    p = new PrintWriter(fCharArrayWriter);
    fCompact = compact;
  }
=======
  public TextOutputOnCharArray(boolean compact) {
    indentGranularity = 2;
    charArrayWriter = new CharArrayWriter(50 * 1024);
    p = new PrintWriter(charArrayWriter);
    this.compact = compact;
  }
>>>>>>> .r47

<<<<<<< .mine
  public int getIndent() {
    return fIndent;
  }
=======
  public int getIndent() {
    return indent;
  }
>>>>>>> .r47

<<<<<<< .mine
  public char[] getText() {
    return fCharArrayWriter.toCharArray();
  }
=======
  public char[] getText() {
    return charArrayWriter.toCharArray();
  }
>>>>>>> .r47

<<<<<<< .mine
  public void indentIn() {
    ++fIndent;
    if (fIndent >= fIndents.length) {
      // Cache a new level of indentation string.
      //
      char[] newIndentLevel = new char[fIndent * fIndentGranularity];
      Arrays.fill(newIndentLevel, ' ');
      char[][] newIndents = new char[fIndents.length + 1][];
      System.arraycopy(fIndents, 0, newIndents, 0, fIndents.length);
      newIndents[fIndent] = newIndentLevel;
      fIndents = newIndents;
=======
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
>>>>>>> .r47
    }
  }

<<<<<<< .mine
  public void indentOut() {
    --fIndent;
  }
=======
  public void indentOut() {
    --indent;
  }
>>>>>>> .r47

<<<<<<< .mine
  public void newline() {
    p.print('\n');
    // TODO: remove flush calls
    p.flush();
    fJustNewlined = true;
  }
=======
  public void newline() {
    p.print('\n');
    // TODO: remove flush calls
    p.flush();
    justNewlined = true;
  }
>>>>>>> .r47

<<<<<<< .mine
  public void newlineOpt() {
    if (!fCompact) {
      p.print('\n');
      // TODO: remove flush calls
      p.flush();
=======
  public void newlineOpt() {
    if (!compact) {
      p.print('\n');
      // TODO: remove flush calls
      p.flush();
>>>>>>> .r47
    }
<<<<<<< .mine
    fJustNewlined = true;
  }
=======
    justNewlined = true;
  }
>>>>>>> .r47

<<<<<<< .mine
  public void print(char c) {
    maybeIndent();
    p.print(c);
    // TODO: remove flush calls
    p.flush();
    fJustNewlined = false;
  }
=======
  public void print(char c) {
    maybeIndent();
    p.print(c);
    // TODO: remove flush calls
    p.flush();
    justNewlined = false;
  }
>>>>>>> .r47

<<<<<<< .mine
  public void print(char[] s) {
    maybeIndent();
    p.print(s);
    // TODO: remove flush calls
    p.flush();
    fJustNewlined = false;
  }
=======
  public void print(char[] s) {
    maybeIndent();
    p.print(s);
    // TODO: remove flush calls
    p.flush();
    justNewlined = false;
  }
>>>>>>> .r47

<<<<<<< .mine
  public void print(String s) {
    maybeIndent();
    p.print(s);
    // TODO: remove flush calls
    p.flush();
    fJustNewlined = false;
  }
=======
  public void print(String s) {
    maybeIndent();
    p.print(s);
    // TODO: remove flush calls
    p.flush();
    justNewlined = false;
  }
>>>>>>> .r47

<<<<<<< .mine
  public void printOpt(char c) {
    if (!fCompact) {
      maybeIndent();
      p.print(c);
      // TODO: remove flush calls
      p.flush();
=======
  public void printOpt(char c) {
    if (!compact) {
      maybeIndent();
      p.print(c);
      // TODO: remove flush calls
      p.flush();
>>>>>>> .r47
    }
  }

<<<<<<< .mine
  public void printOpt(char[] s) {
    if (!fCompact) {
      maybeIndent();
      p.print(s);
      // TODO: remove flush calls
      p.flush();
=======
  public void printOpt(char[] s) {
    if (!compact) {
      maybeIndent();
      p.print(s);
      // TODO: remove flush calls
      p.flush();
>>>>>>> .r47
    }
  }

<<<<<<< .mine
  public void printOpt(String s) {
    if (!fCompact) {
      maybeIndent();
      p.print(s);
      // TODO: remove flush calls
      p.flush();
=======
  public void printOpt(String s) {
    if (!compact) {
      maybeIndent();
      p.print(s);
      // TODO: remove flush calls
      p.flush();
>>>>>>> .r47
    }
  }

<<<<<<< .mine
  public void setIndent(int indent) {
    fIndent = indent;
  }
=======
  public void setIndent(int indent) {
    this.indent = indent;
  }
>>>>>>> .r47

<<<<<<< .mine
  private void maybeIndent() {
    if (fJustNewlined && !fCompact) {
      p.print(fIndents[fIndent]);
      // TODO: remove flush calls
      p.flush();
      fJustNewlined = false;
=======
  private void maybeIndent() {
    if (justNewlined && !compact) {
      p.print(indents[indent]);
      // TODO: remove flush calls
      p.flush();
      justNewlined = false;
>>>>>>> .r47
    }
  }
}
