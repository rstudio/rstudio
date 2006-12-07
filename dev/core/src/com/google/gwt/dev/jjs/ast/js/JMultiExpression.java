// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.ast.HolderList;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;

public class JMultiExpression extends JExpression {

  public HolderList exprs = new HolderList();

  public JMultiExpression(JProgram program) {
    super(program);
  }

  public JType getType() {
    int c = exprs.size();
    if (c == 0) {
      return program.getTypeVoid();
    } else {
      return exprs.getExpr(c - 1).getType();
    }
  }

  public boolean hasSideEffects() {
    
    for (int i = 0; i < exprs.size(); ++i) {
      JExpression expr = exprs.getExpr(i);
      if (expr.hasSideEffects()) {
        return true;
      }
    }
    return false;
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
