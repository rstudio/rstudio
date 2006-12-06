// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java character literal expression.
 */
public class JCharLiteral extends JLiteral {

  public final char value;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JCharLiteral(JProgram program, char value) {
    super(program);
    this.value = value;
  }

  public JType getType() {
    return fProgram.getTypePrimitiveChar();
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
