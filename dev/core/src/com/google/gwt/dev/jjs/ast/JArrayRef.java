// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java array reference expression.
 */
public class JArrayRef extends JExpression {

  public final Holder instance = new Holder();
  public final Holder indexExpr = new Holder();

  public JArrayRef(JProgram program, JExpression instance, JExpression indexExpr) {
    super(program);
    this.instance.set(instance);
    this.indexExpr.set(indexExpr);
  }

  public JExpression getInstance() {
    return instance.get();
  }

  public JExpression getIndexExpr() {
    return indexExpr.get();
  }

  public boolean hasSideEffects() {
    return instance.get().hasSideEffects() || indexExpr.get().hasSideEffects();
  }

  public JType getType() {
    JType type = instance.get().getType();
    if (type == fProgram.getTypeNull()) {
      return type;
    }
    JArrayType arrayType = (JArrayType) type;
    return arrayType.getElementType();
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      this.instance.traverse(visitor);
      this.indexExpr.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
