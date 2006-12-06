// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

public interface HasSettableType extends HasType {
  void setType(JType newType);
}
