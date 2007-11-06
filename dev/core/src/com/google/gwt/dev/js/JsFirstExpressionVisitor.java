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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsVisitor;

/**
 * Determines if an expression statement needs to be surrounded by parentheses.
 * 
 * The statement or the left-most expression needs to be surrounded by
 * parentheses if the left-most expression is an object literal or a function
 * object. Function declarations do not need parentheses.
 * 
 * For example the following require parentheses:<br>
 * <ul>
 * <li>{ key : 'value'}</li>
 * <li>{ key : 'value'}.key</li>
 * <li>function () {return 1;}()</li>
 * <li>function () {return 1;}.prototype</li>
 * </ul>
 * 
 * The following do not require parentheses:<br>
 * <ul>
 * <li>var x = { key : 'value'}</li>
 * <li>"string" + { key : 'value'}.key</li>
 * <li>function func() {}</li>
 * <li>function() {}</li>
 * </ul>
 */
public class JsFirstExpressionVisitor extends JsVisitor {

  public static boolean exec(JsExprStmt statement) {
    JsFirstExpressionVisitor visitor = new JsFirstExpressionVisitor();
    JsExpression expression = statement.getExpression();
    // Pure function declarations do not need parentheses
    if (expression instanceof JsFunction) {
      return false;
    }
    visitor.accept(statement.getExpression());
    return visitor.needsParentheses;
  }

  private boolean needsParentheses = false;

  private JsFirstExpressionVisitor() {
  }

  public boolean visit(JsArrayAccess x, JsContext<JsExpression> ctx) {
    accept(x.getArrayExpr());
    return false;
  }

  public boolean visit(JsArrayLiteral x, JsContext<JsExpression> ctx) {
    return false;
  }

  public boolean visit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
    accept(x.getArg1());
    return false;
  }

  public boolean visit(JsConditional x, JsContext<JsExpression> ctx) {
    accept(x.getTestExpression());
    return false;
  }

  public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
    needsParentheses = true;
    return false;
  }

  public boolean visit(JsInvocation x, JsContext<JsExpression> ctx) {
    accept(x.getQualifier());
    return false;
  }

  public boolean visit(JsNameRef x, JsContext<JsExpression> ctx) {
    if (!x.isLeaf()) {
      accept(x.getQualifier());
    }
    return false;
  }

  public boolean visit(JsNew x, JsContext<JsExpression> ctx) {
    return false;
  }

  public boolean visit(JsObjectLiteral x, JsContext<JsExpression> ctx) {
    needsParentheses = true;
    return false;
  }

  public boolean visit(JsPostfixOperation x, JsContext<JsExpression> ctx) {
    accept(x.getArg());
    return false;
  }

  public boolean visit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
    return false;
  }

  public boolean visit(JsRegExp x, JsContext<JsExpression> ctx) {
    return false;
  }

}
