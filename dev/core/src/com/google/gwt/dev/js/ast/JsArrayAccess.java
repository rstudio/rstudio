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
 * Represents a javascript expression for array access.
 */
public final class JsArrayAccess extends JsExpression {

  private JsExpression arrayExpr;

  private JsExpression indexExpr;

  public JsArrayAccess(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  public JsArrayAccess(SourceInfo sourceInfo, JsExpression arrayExpr, JsExpression indexExpr) {
    super(sourceInfo);
    this.arrayExpr = arrayExpr;
    this.indexExpr = indexExpr;
  }

  public JsExpression getArrayExpr() {
    return arrayExpr;
  }

  public JsExpression getIndexExpr() {
    return indexExpr;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.ARRAY_ACCESS;
  }

  @Override
  public boolean hasSideEffects() {
    return arrayExpr.hasSideEffects() || indexExpr.hasSideEffects();
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return false;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  public void setArrayExpr(JsExpression arrayExpr) {
    this.arrayExpr = arrayExpr;
  }

  public void setIndexExpr(JsExpression indexExpr) {
    this.indexExpr = indexExpr;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      arrayExpr = v.accept(arrayExpr);
      indexExpr = v.accept(indexExpr);
    }
    v.endVisit(this, ctx);
  }
}
