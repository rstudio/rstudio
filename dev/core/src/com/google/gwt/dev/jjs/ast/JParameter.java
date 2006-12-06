// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java method parameter definition.
 */
public class JParameter extends JVariable implements HasEnclosingMethod {

  private final JMethod enclosingMethod;

  JParameter(JProgram program, String name, JType type, boolean isFinal,
      JMethod enclosingMethod) {
    super(program, name, type, isFinal);
    this.enclosingMethod = enclosingMethod;
  }

  public JMethod getEnclosingMethod() {
    return enclosingMethod;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
    }
    visitor.endVisit(this);
  }
}
