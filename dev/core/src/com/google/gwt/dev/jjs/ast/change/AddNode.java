// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JNode;

import java.util.List;

class AddNode/*<N extends JNode>*/ extends ChangeBase {

  private final JNode node;
  private final int index;
  final List/*<N>*/ list;

  public AddNode(JNode node, int index, List/*<N>*/ list) {
    this.node = node;
    this.index = index;
    this.list = list;
    assert (!list.contains(node));
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    if (index < 0) {
      logger.log(type, "Add " + ChangeList.getNodeString(node)
        + ChangeList.getEnclosingTypeString(" to the end of", node), null);
    } else {
      logger.log(type,
        "Add " + ChangeList.getNodeString(node)
          + ChangeList.getEnclosingTypeString(" to", node) + " at index "
          + index, null);
    }
  }

  public void apply() {
    if (index < 0) {
      list.add(node);
    } else {
      list.add(index, node);
    }
  }

}