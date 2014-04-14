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
 * A JavaScript postfix operation.
 */
public final class JsPostfixOperation extends JsUnaryOperation {

  public JsPostfixOperation(SourceInfo sourceInfo, JsUnaryOperator op) {
    this(sourceInfo, op, null);
  }

  public JsPostfixOperation(SourceInfo sourceInfo, JsUnaryOperator op, JsExpression arg) {
    super(sourceInfo, op, arg);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.POSTFIX_OP;
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
      super.traverse(v, ctx);
    }
    v.endVisit(this, ctx);
  }
}
