// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java null literal expression.
 */
public class JNullLiteral extends JLiteral {

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JNullLiteral(JProgram program) {
    super(program);
  }

  public JType getType() {
    return fProgram.getTypeNull();
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
