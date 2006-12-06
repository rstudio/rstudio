// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

public abstract class Mutator {

  public abstract void insertAfter(JExpression node);

  public abstract void insertBefore(JExpression node);

  public abstract void remove();

  public abstract JExpression get();

  public abstract JExpression set(JExpression value);

}
