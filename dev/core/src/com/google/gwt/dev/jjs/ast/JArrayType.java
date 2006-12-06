// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Instances are shared.
 */
public class JArrayType extends JClassType {

  public JType leafType;
  public int dims;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JArrayType(JProgram program, JType leafType, int dims) {
    super(program, calcName(leafType, dims), false, false);
    this.leafType = leafType;
    this.dims = dims;
  }

  private static String calcName(JType leafType, int dims) {
    String name = leafType.getName();
    for (int i = 0; i < dims; ++i) {
      name = name + "[]";
    }
    return name;
  }

  public int getDims() {
    return dims;
  }

  public JType getElementType() {
    if (dims == 1) {
      return leafType;
    }
    return fProgram.getTypeArray(leafType, dims - 1);
  }

  public JType getLeafType() {
    return leafType;
  }

  public String getJavahSignatureName() {
    String s = leafType.getJavahSignatureName();
    for (int i = 0; i < dims; ++i) {
      s = "_3" + s;
    }
    return s;
  }

  public String getJsniSignatureName() {
    String s = leafType.getJsniSignatureName();
    for (int i = 0; i < dims; ++i) {
      s = "[" + s;
    }
    return s;
  }

  public boolean isAbstract() {
    return false;
  }

  public boolean isFinal() {
    return false;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
    }
    visitor.endVisit(this);
  }

}
