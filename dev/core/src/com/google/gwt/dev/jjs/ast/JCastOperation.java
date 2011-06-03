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
 * Java cast expression.
 */
public class JCastOperation extends JExpression {

  private JType castType;
  private JExpression expr;

  public JCastOperation(SourceInfo info, JType castType, JExpression expr) {
    super(info);
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

  @Override
  public boolean hasSideEffects() {
    // Any live cast operations might throw a ClassCastException
    //
    // TODO: revisit this when we support the concept of whether something
    // can/must complete normally!
    return true;
  }

  /**
   * Resolve an external reference during AST stitching.
   */
  public void resolve(JType newType) {
    assert newType.replaces(castType);
    castType = newType;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      expr = visitor.accept(expr);
    }
    visitor.endVisit(this, ctx);
  }

}
