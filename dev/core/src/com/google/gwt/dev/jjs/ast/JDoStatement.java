// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java do statement.
 */
public class JDoStatement extends JStatement {

  private final Holder testExpr = new Holder();
  public JStatement body;

  public JDoStatement(JProgram program, JExpression testExpr, JStatement body) {
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
