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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.js.ast.JsVisitor;

/**
 * Searches for method invocations in constructor expressions that would not
 * normally be surrounded by parentheses.
 */
public class JsConstructExpressionVisitor extends JsVisitor {

  public static boolean exec(JsExpression expression) {
    if (JsPrecedenceVisitor.exec(expression) < JsPrecedenceVisitor.PRECEDENCE_NEW) {
      return true;
    }
    JsConstructExpressionVisitor visitor = new JsConstructExpressionVisitor();
    visitor.accept(expression);
    return visitor.containsInvocation;
  }

  private boolean containsInvocation = false;

  private JsConstructExpressionVisitor() {
  }

  /**
   * We only look at the array expression since the index has its own scope.
   */
  @Override
  public boolean visit(JsArrayAccess x, JsContext ctx) {
    accept(x.getArrayExpr());
    return false;
  }

  /**
   * Array literals have their own scoping.
   */
  @Override
  public boolean visit(JsArrayLiteral x, JsContext ctx) {
    return false;
  }

  /**
   * Functions have their own scoping.
   */
  @Override
  public boolean visit(JsFunction x, JsContext ctx) {
    return false;
  }

  @Override
  public boolean visit(JsInvocation x, JsContext ctx) {
    containsInvocation = true;
    return false;
  }

  @Override
  public boolean visit(JsNameRef x, JsContext ctx) {
    if (!x.isLeaf()) {
      accept(x.getQualifier());
    }
    return false;
  }

  /**
   * New constructs bind to the nearest set of parentheses.
   */
  @Override
  public boolean visit(JsNew x, JsContext ctx) {
    return false;
  }

  /**
   * Object literals have their own scope.
   */
  @Override
  public boolean visit(JsObjectLiteral x, JsContext ctx) {
    return false;
  }

  /**
   * We only look at nodes that would not normally be surrounded by parentheses.
   */
  @SuppressWarnings("cast")
  @Override
  protected <T extends JsVisitable> T doAccept(T node) {
    if (node instanceof JsExpression) {
      JsExpression expression = (JsExpression) node;
      int precedence = JsPrecedenceVisitor.exec(expression);
      // Only visit expressions that won't automatically be surrounded by
      // parentheses
      if (precedence < JsPrecedenceVisitor.PRECEDENCE_NEW) {
        return node;
      }
    }
    return super.doAccept(node);
  }

}
