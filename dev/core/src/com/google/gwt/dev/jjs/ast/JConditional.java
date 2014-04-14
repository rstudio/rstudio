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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Conditional expression.
 */
public class JConditional extends JExpression {

  private JExpression elseExpr;
  private JExpression ifTest;
  private JExpression thenExpr;
  private JType type;

  public JConditional(SourceInfo info, JType type, JExpression ifTest, JExpression thenExpr,
      JExpression elseExpr) {
    super(info);
    this.type = type;
    this.ifTest = ifTest;
    this.thenExpr = thenExpr;
    this.elseExpr = elseExpr;
  }

  public JExpression getElseExpr() {
    return elseExpr;
  }

  public JExpression getIfTest() {
    return ifTest;
  }

  public JExpression getThenExpr() {
    return thenExpr;
  }

  @Override
  public JType getType() {
    return type;
  }

  @Override
  public boolean hasSideEffects() {
    return ifTest.hasSideEffects() || thenExpr.hasSideEffects() || elseExpr.hasSideEffects();
  }

  public void setType(JType newType) {
    type = newType;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      ifTest = visitor.accept(ifTest);
      thenExpr = visitor.accept(thenExpr);
      elseExpr = visitor.accept(elseExpr);
    }
    visitor.endVisit(this, ctx);
  }

}
