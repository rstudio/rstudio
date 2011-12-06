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
 * Represents a JavaScript binary operation.
 */
public final class JsBinaryOperation extends JsExpression {

  private JsExpression arg1;

  private JsExpression arg2;

  private final JsBinaryOperator op;

  public JsBinaryOperation(SourceInfo sourceInfo, JsBinaryOperator op) {
    this(sourceInfo, op, null, null);
  }

  public JsBinaryOperation(SourceInfo sourceInfo, JsBinaryOperator op, JsExpression arg1,
      JsExpression arg2) {
    super(sourceInfo);
    this.op = op;
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  public JsExpression getArg1() {
    return arg1;
  }

  public JsExpression getArg2() {
    return arg2;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.BINARY_OP;
  }

  public JsBinaryOperator getOperator() {
    return op;
  }

  @Override
  public boolean hasSideEffects() {
    return op.isAssignment() || arg1.hasSideEffects() || arg2.hasSideEffects();
  }

  @Override
  public boolean isDefinitelyNotNull() {
    // Precarious coding, but none of these can have null results.
    if (op.getPrecedence() > 5) {
      return true;
    }
    if (op == JsBinaryOperator.OR) {
      if (arg1 instanceof CanBooleanEval) {
        if (((CanBooleanEval) arg1).isBooleanTrue()) {
          assert arg1.isDefinitelyNotNull();
          return true;
        }
      }
    }
    // AND and OR can return nulls
    if (op.isAssignment()) {
      if (op == JsBinaryOperator.ASG) {
        return arg2.isDefinitelyNotNull();
      } else {
        // All other ASG's are math ops.
        return true;
      }
    }

    if (op == JsBinaryOperator.COMMA) {
      return arg2.isDefinitelyNotNull();
    }

    return false;
  }

  @Override
  public boolean isDefinitelyNull() {
    if (op == JsBinaryOperator.AND) {
      return arg1.isDefinitelyNull();
    }
    return false;
  }

  public void setArg1(JsExpression arg1) {
    this.arg1 = arg1;
  }

  public void setArg2(JsExpression arg2) {
    this.arg2 = arg2;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      if (op.isAssignment()) {
        arg1 = v.acceptLvalue(arg1);
      } else {
        arg1 = v.accept(arg1);
      }
      arg2 = v.accept(arg2);
    }
    v.endVisit(this, ctx);
  }
}
