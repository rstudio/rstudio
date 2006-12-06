// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java literal expression that evaluates to a string.
 */
public class JStringLiteral extends JLiteral {

  public final String value;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JStringLiteral(JProgram program, String value) {
    super(program);
    this.value = value;
  }

  public JType getType() {
    return fProgram.getTypeJavaLangString();
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
