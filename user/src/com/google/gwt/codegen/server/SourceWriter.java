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
 * A mechanism to write source files.
 * 
 * @see JavaSourceWriterBuilder
 * <p>
 * Experimental API - subject to change.
 */
public interface SourceWriter {

  /**
   * Abort the source file being generated.
   */
  void abort();

  /**
   * Begin emitting a JavaDoc comment.
   */
  void beginJavaDocComment();

  /**
   * Close the source file being generated.
   */
  void close();

  /**
   * End emitting a JavaDoc comment.
   */
  void endJavaDocComment();

  /**
   * Increase indent level.
   */
  void indent();

  /**
   * Print a line at an increased indentation level without altering the indent
   * level for the next line.
   * 
   * @param string
   */
  void indentln(String string);

  /**
   * Format and print a line at an increased indentation level without altering
   * the indent level for the next line.
   * 
   * @param format format string, as in {@link String#format(String, Object...)}
   * @param args arguments for the format string
   */
  void indentln(String format, Object... args);

  /**
   * Decrease indent level.
   */
  void outdent();

  /**
   * Write a string without a line terminator.
   * 
   * @param s
   */
  void print(String s);

  /**
   * Format and print a string without a line terminator.
   * 
   * @param format format string, as in {@link String#format(String, Object...)}
   * @param args arguments for the format string
   */
  void print(String format, Object... args);

  /**
   * Write a line terminator.
   */
  void println();

  /**
   * Write a string with a line terminator.
   * 
   * @param string
   */
  void println(String string);

  /**
   * Format and print a string with a line terminator.
   * 
   * @param format format string, as in {@link String#format(String, Object...)}
   * @param args arguments for the format string
   */
  void println(String format, Object... args);
}
