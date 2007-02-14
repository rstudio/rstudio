/*
 * Copyright 2006 Google Inc.
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

/**
 * Java cast expression.
 */
public class JCastOperation extends JExpression {

  private JExpression expr;
  private final JType castType;

  public JCastOperation(JProgram program, JSourceInfo info, JType castType,
      JExpression expr) {
    super(program, info);
    this.castType = castType;
    this.expr = expr;
  }

  public JType getCastType() {
    return castType;
  }

  public JExpression getExpr() {
    return expr;
  }

  public JType getType() {
    return castType;
  }

  public boolean hasSideEffects() {
    // Any live cast operations might throw a ClassCastException
    //
    // TODO: revisit this when we support the concept of whether something
    // can/must complete normally!
    return true;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      expr = visitor.accept(expr);
    }
    visitor.endVisit(this, ctx);
  }

}
