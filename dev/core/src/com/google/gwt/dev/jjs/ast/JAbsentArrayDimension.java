// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

public class JAbsentArrayDimension extends JLiteral {

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JAbsentArrayDimension(JProgram program) {
    super(program);
  }

  public JType getType() {
    return fProgram.getTypeVoid();
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
    }
    visitor.endVisit(this, mutator);
  }

}
