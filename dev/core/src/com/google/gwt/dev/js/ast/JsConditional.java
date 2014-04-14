/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Represents a JavaScript conditional expression.
 */
public final class JsConditional extends JsExpression {

  private JsExpression elseExpr;

  private JsExpression testExpr;

  private JsExpression thenExpr;

  public JsConditional(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  public JsConditional(SourceInfo sourceInfo, JsExpression testExpr, JsExpression thenExpr,
      JsExpression elseExpr) {
    super(sourceInfo);
    this.testExpr = testExpr;
    this.thenExpr = thenExpr;
    this.elseExpr = elseExpr;
  }

  public JsExpression getElseExpression() {
    return elseExpr;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.CONDITIONAL;
  }

  public JsExpression getTestExpression() {
    return testExpr;
  }

  public JsExpression getThenExpression() {
    return thenExpr;
  }

  @Override
  public boolean hasSideEffects() {
    return testExpr.hasSideEffects() || thenExpr.hasSideEffects() || elseExpr.hasSideEffects();
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return thenExpr.isDefinitelyNotNull() && elseExpr.isDefinitelyNotNull();
  }

  @Override
  public boolean isDefinitelyNull() {
    return thenExpr.isDefinitelyNull() && elseExpr.isDefinitelyNull();
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

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      testExpr = v.accept(testExpr);
      thenExpr = v.accept(thenExpr);
      elseExpr = v.accept(elseExpr);
    }
    v.endVisit(this, ctx);
  }
}
