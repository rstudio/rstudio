// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

import java.util.List;

/**
 * Java for statement.
 */
public class JForStatement extends JStatement {

  private final List/* <JStatement> */initializers;
  private final Holder testExpr = new Holder();
  private final List/* <JExpressionStatement> */increments;
  public final JStatement body;

  public JForStatement(JProgram program, List/* <JStatement> */initializers,
      JExpression testExpr, List/* <JExpressionStatement> */increments,
      JStatement body) {
    super(program);
    this.initializers = initializers;
    this.testExpr.set(testExpr);
    this.increments = increments;
    this.body = body;
  }

  public List/* <JStatement> */getInitializers() {
    return initializers;
  }

  public JExpression getTestExpr() {
    return testExpr.get();
  }

  public List/* <JExpressionStatement> */getIncrements() {
    return increments;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      for (int i = 0; i < initializers.size(); ++i) {
        JStatement stmt = (JStatement) initializers.get(i);
        stmt.traverse(visitor);
      }
      testExpr.traverse(visitor);
      for (int i = 0; i < increments.size(); ++i) {
        JExpressionStatement stmt = (JExpressionStatement) increments.get(i);
        stmt.traverse(visitor);
      }
      if (body != null) {
        body.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
