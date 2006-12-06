// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java double literal expression. 
 */
public class JDoubleLiteral extends JLiteral {

  public final double value;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JDoubleLiteral(JProgram program, double value) {
    super(program);
    this.value = value;
  }

  public JType getType() {
    return fProgram.getTypePrimitiveDouble();
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
