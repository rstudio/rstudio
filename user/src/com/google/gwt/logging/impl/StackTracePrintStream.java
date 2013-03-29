/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.logging.impl;

import java.io.FilterOutputStream;
import java.io.PrintStream;

/**
 * A {@link PrintStream} implementation that implements only a subset of methods that is enough to
 * be used with {@link Throwable#printStackTrace(PrintStream)}.
 * <p>
 * Note that all implemented methods marked as final except two methods that are safe to be
 * overridden by the subclasses: {@link #append(String)} {@link #newLine()}.
 */
public class StackTracePrintStream extends PrintStream {

  private final StringBuilder builder;

  public StackTracePrintStream(StringBuilder builder) {
    super(new FilterOutputStream(null));
    this.builder = builder;
  }

  /**
   * Appends some text to the output.
   */
  protected void append(String text) {
    builder.append(text);
  }

  /**
   * Appends a newline to the output.
   */
  protected void newLine() {
    builder.append("\n");
  }

  @Override
  public final void print(Object obj) {
    append(String.valueOf(obj));
  }

  @Override
  public final void println(Object obj) {
    append(String.valueOf(obj));
    newLine();
  }

  @Override
  public final void print(String str) {
    append(str);
  }

  @Override
  public final void println() {
    newLine();
  }

  @Override
  public final void println(String str) {
    append(str);
    newLine();
  }
}