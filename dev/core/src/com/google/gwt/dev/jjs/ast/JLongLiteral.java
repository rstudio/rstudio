// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java literal expression that evaluates to a Long.
 */
public class JLongLiteral extends JLiteral {

  public final long value;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JLongLiteral(JProgram program, long value) {
    super(program);
    this.value = value;
  }

  public JType getType() {
    return fProgram.getTypePrimitiveLong();
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
