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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * Adapts {@link TextOutput} to a print writer.
 */
public final class DefaultTextOutput implements TextOutput {

  private final boolean compact;
  private int identLevel = 0;
  private int indentGranularity = 2;
  private char[][] indents = new char[][] {new char[0]};
  private boolean justNewlined;
  private final PrintWriter p;
  private final StringWriter sw;

  public DefaultTextOutput(boolean compact) {
    this.compact = compact;
    sw = new StringWriter();
    p = new PrintWriter(sw, false);
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
      p.print('\n');
    } else {
      p.println();
    }
    justNewlined = true;
  }

  public void newlineOpt() {
    if (!compact) {
      p.println();
      justNewlined = true;
    }
  }

  public void print(char c) {
    maybeIndent();
    p.print(c);
    justNewlined = false;
  }

  public void print(char[] s) {
    maybeIndent();
    p.print(s);
    justNewlined = false;
  }

  public void print(String s) {
    maybeIndent();
    p.print(s);
    justNewlined = false;
  }

  public void printOpt(char c) {
    if (!compact) {
      maybeIndent();
      p.print(c);
    }
  }

  public void printOpt(char[] s) {
    if (!compact) {
      maybeIndent();
      p.print(s);
    }
  }

  public void printOpt(String s) {
    if (!compact) {
      maybeIndent();
      p.print(s);
    }
  }

  public String toString() {
    p.flush();
    return sw.toString();
  }

  private void maybeIndent() {
    if (justNewlined && !compact) {
      p.print(indents[identLevel]);
      justNewlined = false;
    }
  }
}
