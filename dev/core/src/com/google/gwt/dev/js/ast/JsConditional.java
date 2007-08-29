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
package com.google.gwt.dev.js.ast;

/**
 * Represents a JavaScript conditional expression.
 */
public final class JsConditional extends JsExpression {

  private JsExpression testExpr;

  private JsExpression thenExpr;

  private JsExpression elseExpr;

  public JsConditional() {
  }

  public JsConditional(JsExpression testExpr, JsExpression thenExpr,
      JsExpression elseExpr) {
    this.testExpr = testExpr;
    this.thenExpr = thenExpr;
    this.elseExpr = elseExpr;
  }

  public JsExpression getElseExpression() {
    return elseExpr;
  }

  public JsExpression getTestExpression() {
    return testExpr;
  }

  public JsExpression getThenExpression() {
    return thenExpr;
  }

  public void setElseExpression(JsExpression elseExpr) {
    this.elseExpr = elseExpr;
  }

  public void setTestExpression(JsExpression testExpr) {
    this.testExpr = testExpr;
  }

  public void setThenExpression(JsExpression thenExpr) {
    this.thenExpr = thenExpr;
  }

  public void traverse(JsVisitor v, JsContext<JsExpression> ctx) {
    if (v.visit(this, ctx)) {
      testExpr = v.accept(testExpr);
      thenExpr = v.accept(thenExpr);
      elseExpr = v.accept(elseExpr);
    }
    v.endVisit(this, ctx);
  }
}
