/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.codegen.server;

/**
 * Base implementation of {@link SourceWriter} that implements all the indenting
 * and keeping track of comments.
 * <p>
 * Experimental API - subject to change.
 */
public abstract class SourceWriterBase implements SourceWriter {

  private boolean atStart;

  /**
   * Are we currently in a comment?
   */
  private boolean inComment;

  private int indent;

  public abstract void abort();

  public void beginJavaDocComment() {
    println("\n/**");
    inComment = true;
  }

  public void close() {
    outdent();
    println("}");
  }

  public void endJavaDocComment() {
    inComment = false;
    println("\n */");
  }

  public void indent() {
    indent++;
  }

  public void indentln(String string) {
    indent();
    println(string);
    outdent();
  }

  public void indentln(String format, Object... args) {
    indentln(String.format(format, args));
  }

  public void outdent() {
    if (indent > 0) {
      --indent;
    }
  }

  public void print(String s) {
    // If we just printed a newline, print an indent.
    //
    if (atStart) {
      for (int j = 0; j < indent; ++j) {
        writeString("  ");
      }
      if (inComment) {
        writeString(" * ");
      }
      atStart = false;
    }
    // Now print up to the end of the string or the next newline.
    //
    String rest = null;
    int i = s.indexOf("\n");
    if (i > -1 && i < s.length() - 1) {
      rest = s.substring(i + 1);
      s = s.substring(0, i + 1);
    }
    writeString(s);
    // If rest is non-null, then s ended with a newline and we recurse.
    //
    if (rest != null) {
      atStart = true;
      print(rest);
    }
  }

  public void print(String format, Object... args) {
    print(String.format(format, args));
  }

  public void println() {
    print("\n");
    atStart = true;
  }

  public void println(String string) {
    print(string);
    println();
  }

  public void println(String format, Object... args) {
    println(String.format(format, args));
  }

  /**
   * Write a string to the underlying output.
   * 
   * @param s
   */
  protected abstract void writeString(String s);
}
