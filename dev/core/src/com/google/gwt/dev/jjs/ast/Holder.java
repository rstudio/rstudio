// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

public class Holder extends Mutator implements JVisitable {

  private JExpression value;

  public Holder() {
  }

  public Holder(JExpression value) {
    this.value = value;
  }

  public void insertAfter(JExpression node) {
    throw new UnsupportedOperationException();
  }

  public void insertBefore(JExpression node) {
    throw new UnsupportedOperationException();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  public JExpression get() {
    return value;
  }

  public JExpression set(JExpression value) {
    return this.value = value;
  }

  public void traverse(JVisitor visitor) {
    if (value != null) {
      value.traverse(visitor, this);
    }
  }
}
