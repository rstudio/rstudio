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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JavaScript expression for array literals.
 */
public final class JsArrayLiteral extends JsLiteral {

  private final List<JsExpression> exprs = new ArrayList<JsExpression>();

  public JsArrayLiteral(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  public List<JsExpression> getExpressions() {
    return exprs;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.ARRAY;
  }

  @Override
  public boolean hasSideEffects() {
    for (JsExpression expr : exprs) {
      if (expr.hasSideEffects()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isBooleanFalse() {
    return false;
  }

  @Override
  public boolean isBooleanTrue() {
    return true;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return true;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      v.acceptWithInsertRemove(exprs);
    }
    v.endVisit(this, ctx);
  }
}
