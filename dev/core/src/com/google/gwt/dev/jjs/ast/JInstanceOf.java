// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java instance of expression. 
 */
public class JInstanceOf extends JExpression {

  public final JReferenceType testType;
  public final Holder expr = new Holder();

  public JInstanceOf(JProgram program, JReferenceType testType,
      JExpression expression) {
    super(program);
    this.testType = testType;
    this.expr.set(expression);
  }

  public JExpression getExpression() {
    return expr.get();
  }

  public JType getType() {
    return fProgram.getTypePrimitiveBoolean();
  }

  public boolean hasSideEffects() {
    return false;
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
