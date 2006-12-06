// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java local variable reference.
 */
public class JLocalRef extends JVariableRef {

  /**
   * The referenced local.
   */
  public JLocal local;

  public JLocalRef(JProgram program, JLocal local) {
    super(program, local);
    this.local = local;
  }

  public JLocal getLocal() {
    return local;
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
