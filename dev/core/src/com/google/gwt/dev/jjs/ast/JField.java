// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java field definition. 
 */
public class JField extends JVariable implements CanBeStatic, HasEnclosingType {

  public JReferenceType enclosingType;
  public JLiteral constInitializer;
  private final boolean isStatic;
  private final boolean hasInitializer;

  JField(JProgram program, String name, JReferenceType enclosingType,
      JType type, boolean isStatic, boolean isFinal, boolean hasInitializer) {
    super(program, name, type, isFinal);
    this.enclosingType = enclosingType;
    this.isStatic = isStatic;
    this.hasInitializer  = hasInitializer;
  }

  public JReferenceType getEnclosingType() {
    return enclosingType;
  }

  public boolean hasInitializer() {
    return hasInitializer;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
    }
    visitor.endVisit(this);
  }

}
