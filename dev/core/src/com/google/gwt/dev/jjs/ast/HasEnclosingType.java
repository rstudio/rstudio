// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Interface implemented by anything that can be enclosed by a type. 
 */
public interface HasEnclosingType {
  JReferenceType getEnclosingType();
}
