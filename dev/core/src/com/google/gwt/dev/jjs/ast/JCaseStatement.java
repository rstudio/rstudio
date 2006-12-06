// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java case statement. 
 */
public class JCaseStatement extends JStatement {

  private final Holder/*<JLiteral>*/ expr = new Holder/*<JLiteral>*/();

  public JCaseStatement(JProgram program, JLiteral expr) {
    super(program);
    this.expr.set(expr);
  }

  public JLiteral getExpression() {
    return (JLiteral) expr.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      expr.traverse(visitor);
    }
    visitor.endVisit(this);
  }

}
