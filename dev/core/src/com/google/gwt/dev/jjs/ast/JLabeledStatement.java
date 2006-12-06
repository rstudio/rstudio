// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java statement that has an associated label.
 */
public class JLabeledStatement extends JStatement {

  public final JLabel label;
  public JStatement body;
  
  public JLabeledStatement(JProgram program, JLabel label, JStatement body) {
    super(program);
    this.label = label;
    this.body = body;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      label.traverse(visitor);
      body.traverse(visitor);
    }
    visitor.endVisit(this);
  }
}
