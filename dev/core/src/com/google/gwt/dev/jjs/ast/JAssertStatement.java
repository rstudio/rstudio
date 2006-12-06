// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java assert statement.
 */
public class JAssertStatement extends JStatement {

  private final Holder testExpr = new Holder();
  private final Holder arg = new Holder();

  public JAssertStatement(JProgram program, JExpression testExpr, JExpression arg) {
    super(program);
    this.testExpr.set(testExpr);
    this.arg.set(arg);
  }

  public JExpression getArg() {
    return arg.get();
  }

  public JExpression getTestExpr() {
    return testExpr.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      testExpr.traverse(visitor);
      arg.traverse(visitor);
    }
    visitor.endVisit(this);
  }

}
