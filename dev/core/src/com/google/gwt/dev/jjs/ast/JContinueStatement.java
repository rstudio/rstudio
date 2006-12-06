// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java continue statement.
 */
public class JContinueStatement extends JStatement {

  public JLabel label;

  public JContinueStatement(JProgram program, JLabel label) {
    super(program);
    this.label = label;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      if (label != null) {
        label.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
