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
 * Java instance of expression.
 */
public class JInstanceOf extends JExpression {

  private JExpression expr;
  private JReferenceType testType;

  public JInstanceOf(SourceInfo info, JReferenceType testType, JExpression expression) {
    super(info);
    this.testType = testType;
    this.expr = expression;
  }

  public JExpression getExpr() {
    return expr;
  }

  public JReferenceType getTestType() {
    return testType;
  }

  @Override
  public JType getType() {
    return JPrimitiveType.BOOLEAN;
  }

  @Override
  public boolean hasSideEffects() {
    return expr.hasSideEffects();
  }

  /**
   * Resolve an external reference during AST stitching.
   */
  public void resolve(JReferenceType newType) {
    assert newType.replaces(testType);
    testType = newType;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      expr = visitor.accept(expr);
    }
    visitor.endVisit(this, ctx);
  }

}
