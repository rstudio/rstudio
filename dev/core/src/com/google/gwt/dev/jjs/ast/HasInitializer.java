// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Interface implemented by Java entities that can have an initialization 
 * expression.
 */
public interface HasInitializer {
  void setInitializer(JExpression expression);
}
