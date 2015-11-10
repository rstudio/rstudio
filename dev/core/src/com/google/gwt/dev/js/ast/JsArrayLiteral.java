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
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a JavaScript expression for array literals.
 */
public final class JsArrayLiteral extends JsLiteral {

  private final List<JsExpression> exprs = Lists.newArrayList();

  private boolean internable = false;

  public JsArrayLiteral(SourceInfo sourceInfo, Iterable<JsExpression> expressions) {
    super(sourceInfo);
    Iterables.addAll(this.exprs, expressions);
  }

  public JsArrayLiteral(SourceInfo sourceInfo, JsExpression... expressions) {
    this(sourceInfo, Arrays.asList(expressions));
  }

  public List<JsExpression> getExpressions() {
    return exprs;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || this.getClass() != that.getClass()) {
      return false;
    }
    JsArrayLiteral thatLiteral = (JsArrayLiteral) that;
    return internable == thatLiteral.internable && exprs.equals(thatLiteral.exprs);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.ARRAY;
  }

  @Override
  public int hashCode() {
    return  exprs.hashCode() + 17 * (internable ? 0 : 1);
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

  /**
   * Some array literals are not mutated and hence internable.
   */
  @Override
  public boolean isInternable() {
    return internable;
  }

  public void setInternable() {
    internable = true;
  }
}
