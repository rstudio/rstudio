// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.Mutator;

import java.util.List;

class RemoveNodeMutator/*<N extends JNode>*/ extends ChangeBase {

  private final Mutator/*<N>*/ node;
  final List/*<N>*/ list;

  public RemoveNodeMutator(Mutator/*<N>*/ node, List/*<N>*/ list) {
    this.node = node;
    this.list = list;
    assert (list.contains(node.get()));
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Remove " + ChangeList.getNodeString(node.get())
      + ChangeList.getEnclosingTypeString(" from", node.get()), null);
  }

  public void apply() {
    boolean removed = list.remove(node.get());
    assert (removed);
  }

}