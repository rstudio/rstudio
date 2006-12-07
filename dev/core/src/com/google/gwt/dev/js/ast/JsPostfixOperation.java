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
package com.google.gwt.dev.js.ast;

/**
 * A JavaScript postfix operation.
 */
public final class JsPostfixOperation extends JsExpression {

  private JsExpression arg;

  private final JsUnaryOperator op;

  public JsPostfixOperation(JsUnaryOperator op) {
    this(op, null);
  }

  public JsPostfixOperation(JsUnaryOperator op, JsExpression arg) {
    this.op = op;
    this.arg = arg;
  }

  public JsExpression getArg() {
    return arg;
  }

  public JsUnaryOperator getOperator() {
    return op;
  }

  public void setArg(JsExpression arg) {
    this.arg = arg;
  }

  public void traverse(JsVisitor v) {
    if (v.visit(this)) {
      arg.traverse(v);
    }
    v.endVisit(this);
  }
}
