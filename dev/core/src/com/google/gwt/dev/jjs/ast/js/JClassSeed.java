// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;

/**
 * An AST node representing a class's constructor function. Only used by
 * generated code, it doesn't represent any user construct.
 */
public class JClassSeed extends JLiteral {

  /**
   * The class being referred to.
   */
  public final JClassType refType;

  public JClassSeed(JProgram program, JClassType type) {
    super(program);
    refType = type;
  }

  public JType getType() {
    return program.getTypeJavaLangObject();
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
