// Copyright 2006 Google Inc. All Rights Reserved.
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
