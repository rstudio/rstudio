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
 * Java prefix or postfix operation expression.
 */
public abstract class JUnaryOperation extends JExpression {

  private JExpression arg;
  private final JUnaryOperator op;

  public JUnaryOperation(SourceInfo info, JUnaryOperator op, JExpression arg) {
    super(info);
    this.op = op;
    this.arg = arg;
  }

  public JExpression getArg() {
    return arg;
  }

  public JUnaryOperator getOp() {
    return op;
  }

  @Override
  public JType getType() {
    // Unary operators don't change the type of their expression
    return arg.getType();
  }

  @Override
  public boolean hasSideEffects() {
    return getOp().isModifying() || arg.hasSideEffects();
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (getOp().isModifying()) {
      arg = visitor.acceptLvalue(arg);
    } else {
      arg = visitor.accept(arg);
    }
  }
}
