// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java boolean literal expression.
 */
public class JBooleanLiteral extends JLiteral {

  public final boolean value;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JBooleanLiteral(JProgram program, boolean value) {
    super(program);
    this.value = value;
  }

  public JType getType() {
    return fProgram.getTypePrimitiveBoolean();
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
