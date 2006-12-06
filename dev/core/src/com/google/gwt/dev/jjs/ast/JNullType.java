// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java null reference type.
 */
public class JNullType extends JReferenceType {

  public JNullType(JProgram program) {
    super(program, "<null>");
  }

  public String getJavahSignatureName() {
    return "N";
  }

  public String getJsniSignatureName() {
    return "N";
  }

  public boolean isAbstract() {
    return false;
  }

  public boolean isFinal() {
    return true;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
    }
    visitor.endVisit(this);
  }

}
