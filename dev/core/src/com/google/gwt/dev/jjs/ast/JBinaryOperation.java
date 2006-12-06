// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Binary operator expression. 
 */
public class JBinaryOperation extends JExpression implements HasSettableType {

  public JBinaryOperator op;
  private JType type;
  public final Holder lhs = new Holder();
  public final Holder rhs = new Holder();

  public JBinaryOperation(JProgram program, JType type, JBinaryOperator op,
      JExpression lhs, JExpression rhs) {
    super(program);
    this.op = op;
    this.type = type;
    this.lhs.set(lhs);
    this.rhs.set(rhs);
  }

  public boolean isAssignment() {
    return op.isAssignment();
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

  public void setType(JType newType) {
    type = newType;
  }

  public boolean hasSideEffects() {
    return op.isAssignment() || getLhs().hasSideEffects()
      || getRhs().hasSideEffects();
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
