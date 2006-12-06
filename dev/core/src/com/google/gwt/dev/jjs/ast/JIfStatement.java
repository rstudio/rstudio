// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java if statement. 
 */
public class JIfStatement extends JStatement {

  private final Holder ifExpr = new Holder();
  public JStatement thenStmt;
  public JStatement elseStmt;

  public JIfStatement(JProgram program, JExpression ifExpr,
      JStatement thenStmt, JStatement elseStmt) {
    super(program);
    this.ifExpr.set(ifExpr);
    this.thenStmt = thenStmt;
    this.elseStmt = elseStmt;
  }

  public JExpression getIfExpr() {
    return ifExpr.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      ifExpr.traverse(visitor);
      if (thenStmt != null) {
        thenStmt.traverse(visitor);
      }
      
      if (elseStmt != null) {
        elseStmt.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
