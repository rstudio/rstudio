// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Base class for any reference to a <code>JVariable</code> derived class.
 */
public abstract class JVariableRef extends JExpression {

  public JVariable target;
  
  public JVariableRef(JProgram program, JVariable target) {
    super(program);
    this.target = target;
  }

  public JVariable getTarget() {
    return target;
  }
  
  public JType getType() {
    return target.getType();
  }

}
