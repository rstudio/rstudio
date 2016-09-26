/*
 * Copyright 2016 Google Inc.
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
 * An unsafe type coercion (mostly resulting form the UncheckedCast annotation).
 */
public class JUnsafeTypeCoercion extends JExpression {

  private JType coercionType;
  private JExpression expression;

  public JUnsafeTypeCoercion(SourceInfo info, JType coercionType, JExpression expression) {
    super(info);
    this.coercionType = coercionType;
    this.expression = expression;
  }

  public JType getCoercionType() {
    return coercionType;
  }

  public JExpression getExpression() {
    return expression;
  }

  @Override
  public JType getType() {
    if (!expression.getType().canBeNull() && !coercionType.isNullType()) {
      // Strengthen type to non null unless it has been determined that the type is not instantiable
      // (and that is reflected by replacing the coercion type by the null type).
      return coercionType.strengthenToNonNull();
    }
    return coercionType;
  }

  @Override
  public boolean hasSideEffects() {
    return expression.hasSideEffects();
  }

  /**
   * Resolve an external reference during AST stitching.
   */
  public void resolve(JType coercionType) {
    assert coercionType.replaces(this.coercionType);
    this.coercionType = coercionType;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      expression = visitor.accept(expression);
    }
    visitor.endVisit(this, ctx);
  }

}
