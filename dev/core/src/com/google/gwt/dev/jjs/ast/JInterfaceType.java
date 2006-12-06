// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java interface type definition. 
 */
public class JInterfaceType extends JReferenceType {

  JInterfaceType(JProgram program, String name) {
    super(program, name);
  }

  public boolean isAbstract() {
    return true;
  }

  public boolean isFinal() {
    return false;
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
