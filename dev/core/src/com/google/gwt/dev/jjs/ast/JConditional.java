// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Conditional expression. 
 */
public class JConditional extends JExpression {

  private final JType type;
  private final Holder ifTest = new Holder();
  public final Holder thenExpr = new Holder();
  public final Holder elseExpr = new Holder();

  public JConditional(JProgram program, JType type, JExpression ifTest,
      JExpression thenExpr, JExpression elseExpr) {
    super(program);
    this.type = type;
    this.ifTest.set(ifTest);
    this.thenExpr.set(thenExpr);
    this.elseExpr.set(elseExpr);
  }

  public JType getType() {
    // TODO(later): allow multiple types for Type Flow?
    if (type instanceof JReferenceType) {
      return fProgram.generalizeTypes(
        (JReferenceType) thenExpr.get().getType(), (JReferenceType) elseExpr
          .get().getType());
    } else {
      return type;
    }
  }

  public JExpression getIfTest() {
    return ifTest.get();
  }

  public JExpression getThenExpr() {
    return thenExpr.get();
  }

  public JExpression getElseExpr() {
    return elseExpr.get();
  }

  public boolean hasSideEffects() {
    return ifTest.get().hasSideEffects() || thenExpr.get().hasSideEffects()
      || elseExpr.get().hasSideEffects();
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      ifTest.traverse(visitor);
      thenExpr.traverse(visitor);
      elseExpr.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
