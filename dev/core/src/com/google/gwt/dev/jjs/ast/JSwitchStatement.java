// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java switch statement. 
 */
public class JSwitchStatement extends JStatement {

  private final Holder expr = new Holder();
  public JBlock body;

  public JSwitchStatement(JProgram program, JExpression expr, JBlock body) {
    super(program);
    this.expr.set(expr);
    this.body = body;
  }

  public JExpression getExpression() {
    return expr.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      expr.traverse(visitor);
      body.traverse(visitor);
    }
    visitor.endVisit(this);
  }

}
