// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.ast.HolderList;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;

public class JsonArray extends JExpression {

  public HolderList exprs = new HolderList();

  public JsonArray(JProgram program) {
    super(program);
  }

  public JType getType() {
    return fProgram.getTypeVoid();
  }

  public boolean hasSideEffects() {
    return true;
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      exprs.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
