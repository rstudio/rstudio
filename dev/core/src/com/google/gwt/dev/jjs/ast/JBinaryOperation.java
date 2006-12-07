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
 * Binary operator expression.
 */
public class JBinaryOperation extends JExpression implements HasSettableType {

  public JBinaryOperator op;
  public final Holder lhs = new Holder();
  public final Holder rhs = new Holder();
  private JType type;

  public JBinaryOperation(JProgram program, JType type, JBinaryOperator op,
      JExpression lhs, JExpression rhs) {
    super(program);
    this.op = op;
    this.type = type;
    this.lhs.set(lhs);
    this.rhs.set(rhs);
  }

  public JExpression getLhs() {
    return lhs.get();
  }

  public JExpression getRhs() {
    return rhs.get();
  }

  public JType getType() {
    if (op == JBinaryOperator.ASG) {
      // Use rhs because (generality lhs >= generality rhs)
      return getRhs().getType();
    } else if (isAssignment()) {
      // Use lhs because this is really a write-then-read
      return getLhs().getType();
    } else {
      // Most binary operators never change type
      return type;
    }
  }

  public boolean hasSideEffects() {
    return op.isAssignment() || getLhs().hasSideEffects()
        || getRhs().hasSideEffects();
  }

  public boolean isAssignment() {
    return op.isAssignment();
  }

  public void setType(JType newType) {
    type = newType;
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      lhs.traverse(visitor);
      rhs.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
