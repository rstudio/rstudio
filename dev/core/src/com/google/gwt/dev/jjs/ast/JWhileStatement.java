// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java while statement.
 */
public class JWhileStatement extends JStatement {

  private final Holder testExpr = new Holder();
  public JStatement body;

  public JWhileStatement(JProgram program, JExpression testExpr, JStatement body) {
    super(program);
    this.testExpr.set(testExpr);
    this.body = body;
  }

  public JExpression getTestExpr() {
    return testExpr.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      testExpr.traverse(visitor);
      if (body != null) {
        body.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
