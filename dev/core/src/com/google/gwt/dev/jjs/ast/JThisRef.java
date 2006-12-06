// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java method this expression.
 */
public class JThisRef extends JExpression {

  public JClassType classType;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JThisRef(JProgram program, JClassType classType) {
    super(program);
    this.classType = classType;
  }

  public JClassType getClassType() {
    return classType;
  }

  public JType getType() {
    return classType;
  }

  public boolean hasSideEffects() {
    return false;
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
