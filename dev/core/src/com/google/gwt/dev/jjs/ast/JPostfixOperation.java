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
 * Java postfix expression. 
 */
public class JPostfixOperation extends JExpression {

  private final Holder arg = new Holder();
  public JUnaryOperator op;

  public JPostfixOperation(JProgram program, JUnaryOperator op, JExpression arg) {
    super(program);
    this.op = op;
    this.arg.set(arg);
  }

  public JExpression getArg() {
    return arg.get();
  }

  public JType getType() {
    // Unary operators don't change the type of their expression
    return arg.get().getType();
  }

  public boolean hasSideEffects() {
    return op == JUnaryOperator.DEC || op == JUnaryOperator.INC
      || arg.get().hasSideEffects();
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      arg.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
