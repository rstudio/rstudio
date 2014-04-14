/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.util.TextOutput;

/**
 * A convenience base class that combines a {@link JVisitor} with a
 * {@link TextOutput}.
 */
public class TextOutputVisitor extends JVisitor implements TextOutput {

  private final TextOutput textOutput;

  public TextOutputVisitor(TextOutput textOutput) {
    this.textOutput = textOutput;
  }

  @Override
  public int getColumn() {
    return textOutput.getColumn();
  }

  @Override
  public int getLine() {
    return textOutput.getLine();
  }

  @Override
  public int getPosition() {
    return textOutput.getPosition();
  }

  @Override
  public void indentIn() {
    textOutput.indentIn();
  }

  @Override
  public void indentOut() {
    textOutput.indentOut();
  }

  @Override
  public void newline() {
    textOutput.newline();
  }

  @Override
  public void newlineOpt() {
    textOutput.newlineOpt();
  }

  @Override
  public void print(char c) {
    textOutput.print(c);
  }

  @Override
  public void print(char[] s) {
    textOutput.print(s);
  }

  @Override
  public void print(String s) {
    textOutput.print(s);
  }

  @Override
  public void printOpt(char c) {
    textOutput.printOpt(c);
  }

  @Override
  public void printOpt(char[] s) {
    textOutput.printOpt(s);
  }

  @Override
  public void printOpt(String s) {
    textOutput.printOpt(s);
  }
}
