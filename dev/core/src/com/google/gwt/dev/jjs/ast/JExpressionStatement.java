// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

public class JExpressionStatement extends JStatement {

  private final Holder expr = new Holder();

  public JExpressionStatement(JProgram program, JExpression expr) {
    super(program);
    this.expr.set(expr);
  }

  public JExpression getExpression() {
    return expr.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      expr.traverse(visitor);
    }
    visitor.endVisit(this);
  }

}
