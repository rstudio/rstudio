// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java new instance expression.
 */
public class JNewInstance extends JExpression {

  private final JClassType classType;
  
  public JNewInstance(JProgram program, JClassType classType) {
    super(program);
    this.classType = classType;
  }

  public JType getType() {
    return classType;
  }

  public JClassType getClassType() {
    return classType;
  }

  public boolean hasSideEffects() {
    return true;
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
