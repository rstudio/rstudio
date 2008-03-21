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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.lang.LongLib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Translates Java literals into JavaScript literals.
 */
public class GenerateJavaScriptLiterals extends JVisitor {

  static {
    LongLib.RUN_IN_JVM = true;
  }

  private final JsProgram program;
  private final Stack<JsVisitable<?>> nodeStack = new Stack<JsVisitable<?>>();

  public GenerateJavaScriptLiterals(JsProgram program) {
    this.program = program;
  }

  @Override
  public final void endVisit(JBooleanLiteral x, Context ctx) {
    push(x.getValue() ? program.getTrueLiteral() : program.getFalseLiteral());
  }

  @Override
  public final void endVisit(JCharLiteral x, Context ctx) {
    push(program.getNumberLiteral(x.getValue()));
  }

  @Override
  public final void endVisit(JDoubleLiteral x, Context ctx) {
    push(program.getNumberLiteral(x.getValue()));
  }

  @Override
  public final void endVisit(JFloatLiteral x, Context ctx) {
    push(program.getNumberLiteral(x.getValue()));
  }

  @Override
  public final void endVisit(JIntLiteral x, Context ctx) {
    push(program.getNumberLiteral(x.getValue()));
  }

  @Override
  public void endVisit(JLongLiteral x, Context ctx) {
    JsArrayLiteral arrayLit = new JsArrayLiteral();
    double[] doubleArray = LongLib.typeChange(x.getValue());
    arrayLit.getExpressions().add(program.getNumberLiteral(doubleArray[0]));
    arrayLit.getExpressions().add(program.getNumberLiteral(doubleArray[1]));
    push(arrayLit);
  }

  @Override
  public final void endVisit(JNullLiteral x, Context ctx) {
    push(program.getNullLiteral());
  }

  @Override
  public final void endVisit(JStringLiteral x, Context ctx) {
    push(program.getStringLiteral(x.getValue()));
  }

  @SuppressWarnings("unchecked")
  public final <T extends JsVisitable> T peek() {
    return (T) nodeStack.peek();
  }

  @SuppressWarnings("unchecked")
  protected final <T extends JsVisitable> T pop() {
    return (T) nodeStack.pop();
  }

  @SuppressWarnings("unchecked")
  protected final <T extends JsVisitable> List<T> popList(int count) {
    List<T> list = new ArrayList<T>();
    while (count > 0) {
      T item = this.<T> pop();
      if (item != null) {
        list.add(item);
      }
      --count;
    }
    Collections.reverse(list);
    return list;
  }

  @SuppressWarnings("unchecked")
  protected final <T extends JsVisitable> void popList(List<T> collection,
      int count) {
    List<T> list = new ArrayList<T>();
    while (count > 0) {
      T item = this.<T> pop();
      if (item != null) {
        list.add(item);
      }
      --count;
    }
    Collections.reverse(list);
    collection.addAll(list);
  }

  @SuppressWarnings("unchecked")
  protected final <T extends JsVisitable> void push(T node) {
    nodeStack.push(node);
  }
}