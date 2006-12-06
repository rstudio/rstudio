// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;

/**
 * Interface implemented by classes that apply changes to a set of statements.
 */
public interface Change {
  
  void describe(TreeLogger logger, TreeLogger.Type type);
  void apply();
  // void undo();
  // void verify();
}
