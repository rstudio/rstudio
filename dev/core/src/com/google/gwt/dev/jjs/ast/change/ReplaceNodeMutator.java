// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.Mutator;

class ReplaceNodeMutator/*<N extends JNode>*/ extends ChangeBase {

  final Mutator/*<N>*/ original;
  final Mutator/*<N>*/ replace;

  public ReplaceNodeMutator(Mutator/*<N>*/ original, Mutator/*<N>*/ replace) {
    this.original = original;
    this.replace = replace;
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Replace " + ChangeList.getNodeString(original.get())
      + " with " + ChangeList.getNodeString(replace.get()), null);
  }

  public void apply() {
    original.set(replace.get());
  }

}