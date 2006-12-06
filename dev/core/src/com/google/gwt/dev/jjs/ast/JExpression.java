// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Base class for all Java expressions. 
 */
public abstract class JExpression extends JNode implements HasType {

  public JExpression(JProgram program) {
    super(program);
  }

  public abstract boolean hasSideEffects();

  public abstract void traverse(JVisitor visitor, Mutator mutator);
  
}
