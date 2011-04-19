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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.SourceInfo;
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
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsStringLiteral;
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

  private final Stack<JsVisitable> nodeStack = new Stack<JsVisitable>();

  @Override
  public final void endVisit(JBooleanLiteral x, Context ctx) {
    push(JsBooleanLiteral.get(x.getValue()));
  }

  @Override
  public final void endVisit(JCharLiteral x, Context ctx) {
    push(new JsNumberLiteral(x.getSourceInfo(), x.getValue()));
  }

  @Override
  public final void endVisit(JDoubleLiteral x, Context ctx) {
    push(new JsNumberLiteral(x.getSourceInfo(), x.getValue()));
  }

  @Override
  public final void endVisit(JFloatLiteral x, Context ctx) {
    push(new JsNumberLiteral(x.getSourceInfo(), x.getValue()));
  }

  @Override
  public final void endVisit(JIntLiteral x, Context ctx) {
    push(new JsNumberLiteral(x.getSourceInfo(), x.getValue()));
  }

  @Override
  public void endVisit(JLongLiteral x, Context ctx) {
    SourceInfo sourceInfo = x.getSourceInfo();
    int[] intArray = LongLib.getAsIntArray(x.getValue());
    JsObjectLiteral objectLit = new JsObjectLiteral(sourceInfo);
    List<JsPropertyInitializer> inits = objectLit.getPropertyInitializers();
    JsExpression label0 = new JsNameRef(sourceInfo, "l");
    JsExpression label1 = new JsNameRef(sourceInfo, "m");
    JsExpression label2 = new JsNameRef(sourceInfo, "h");
    JsExpression value0 = new JsNumberLiteral(sourceInfo, intArray[0]);
    JsExpression value1 = new JsNumberLiteral(sourceInfo, intArray[1]);
    JsExpression value2 = new JsNumberLiteral(sourceInfo, intArray[2]);
    inits.add(new JsPropertyInitializer(sourceInfo, label0, value0));
    inits.add(new JsPropertyInitializer(sourceInfo, label1, value1));
    inits.add(new JsPropertyInitializer(sourceInfo, label2, value2));
    push(objectLit);
  }

  @Override
  public final void endVisit(JNullLiteral x, Context ctx) {
    push(JsNullLiteral.INSTANCE);
  }

  @Override
  public final void endVisit(JStringLiteral x, Context ctx) {
    push(new JsStringLiteral(x.getSourceInfo(), x.getValue()));
  }

  @SuppressWarnings("unchecked")
  public final <T extends JsVisitable> T peek() {
    return (T) nodeStack.peek();
  }

  @SuppressWarnings("unchecked")
  protected final <T extends JsVisitable> T pop() {
    return (T) nodeStack.pop();
  }

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

  protected final <T extends JsVisitable> void popList(List<T> collection, int count) {
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

  protected final <T extends JsVisitable> void push(T node) {
    nodeStack.push(node);
  }
}