// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Interface implemented by entities that exist within a method.
 */
public interface HasEnclosingMethod {
  JMethod getEnclosingMethod();
}
