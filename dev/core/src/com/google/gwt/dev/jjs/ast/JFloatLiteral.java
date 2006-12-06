// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java literal typed as a float.
 */
public class JFloatLiteral extends JLiteral {

  public final float value;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JFloatLiteral(JProgram program, float value) {
    super(program);
    this.value = value;
  }

  public JType getType() {
    return fProgram.getTypePrimitiveFloat();
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
