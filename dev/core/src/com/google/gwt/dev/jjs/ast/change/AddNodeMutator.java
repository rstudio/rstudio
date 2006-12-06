// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.Mutator;

import java.util.List;

class AddNodeMutator/*<N extends JNode>*/ extends ChangeBase {

  private final Mutator/*<N>*/ node;
  private final int index;
  final List/*<N>*/ list;

  public AddNodeMutator(Mutator/*<N>*/ node, int index, List/*<N>*/ list) {
    this.node = node;
    this.index = index;
    this.list = list;
    assert (!list.contains(node));
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    if (index < 0) {
      logger.log(type, "Add the eventual value of "
        + ChangeList.getNodeString(node.get()) + " to a list", null);
    } else {
      logger.log(type,
        "Add the eventual value of " + ChangeList.getNodeString(node.get())
          + " to a list at index " + index, null);
    }
  }

  public void apply() {
    assert (!list.contains(node));
    if (index < 0) {
      list.add(node.get());
    } else {
      list.add(index, node.get());
    }
  }

}