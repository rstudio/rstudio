// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Base class for any Java literal expression.
 */
public abstract class JLiteral extends JExpression {

  public JLiteral(JProgram program) {
    super(program);
  }

  public boolean hasSideEffects() {
    return false;
  }

}
