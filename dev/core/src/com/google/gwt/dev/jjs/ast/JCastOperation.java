// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java cast expression.
 */
public class JCastOperation extends JExpression {

  public final JType castType;
  public final Holder expr = new Holder();

  public JCastOperation(JProgram program, JType castType, JExpression expression) {
    super(program);
    this.castType = castType;
    this.expr.set(expression);
  }

  public JExpression getExpression() {
    return expr.get();
  }

  public JType getType() {
    return castType;
  }

  public boolean hasSideEffects() {
    // technically this isn't true, but since the same cast on the same
    // expression always evaluates the same way, it effectively has no side
    // effects
    return getExpression().hasSideEffects();
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      expr.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
