// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java break statement.
 */
public class JBreakStatement extends JStatement {

  public JLabel label;

  public JBreakStatement(JProgram program, JLabel label) {
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
