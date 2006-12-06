// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java class type reference expression. 
 */
public class JClassType extends JReferenceType implements CanBeSetFinal {

  private final boolean isAbstract;
  private boolean isFinal;

  public JClassType(JProgram program, String name, boolean isAbstract, boolean isFinal) {
    super(program, name);
    this.isAbstract = isAbstract;
    this.isFinal = isFinal;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public void setFinal(boolean b) {
    isFinal = b;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      for (int i = 0; i < fields.size(); ++i) {
        JField field = (JField) fields.get(i);
        field.traverse(visitor);
      }
      for (int i = 0; i < methods.size(); ++i) {
        JMethod method = (JMethod) methods.get(i);
        method.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
