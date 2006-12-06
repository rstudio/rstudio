// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java method parameter reference expression.
 */
public class JParameterRef extends JVariableRef {

  /**
   * The referenced parameter.
   */
  private final JParameter param;

  public JParameterRef(JProgram program, JParameter param) {
    super(program, param);
    this.param = param;
  }

  public JParameter getParameter() {
    return param;
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
