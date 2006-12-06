// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java class literal expression. 
 */
public class JClassLiteral extends JLiteral {

  public final JType refType;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JClassLiteral(JProgram program, JType type) {
    super(program);
    refType = type;
  }

  public JType getType() {
    return fProgram.getTypeJavaLangClass();
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
