// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * New array experssion.
 */
public class JNewArray extends JExpression implements HasSettableType {

  private JArrayType arrayType;
  public HolderList dims = null;
  public HolderList initializers = null;
  
  public JNewArray(JProgram program, JArrayType arrayType) {
    super(program);
    this.arrayType = arrayType;
  }

  public JArrayType getArrayType() {
    return arrayType;
  }
  
  public JType getType() {
    return arrayType;
  }

  public void setType(JType arrayType) {
    this.arrayType = (JArrayType) arrayType;
  }
  
  public boolean hasSideEffects() {
    return true;
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      assert ((dims != null) ^ (initializers != null));
      
      if (dims != null) {
        dims.traverse(visitor);
      }
      
      if (initializers != null) {
        initializers.traverse(visitor);
      }
    }
    visitor.endVisit(this, mutator);
  }
}
