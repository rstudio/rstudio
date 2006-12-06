// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JNode;

import java.util.List;

class RemoveNode/*<N extends JNode>*/ extends ChangeBase {

  private final JNode node;
  final List/*<N>*/ list;

  public RemoveNode(JNode node, List/*<N>*/ list) {
    this.node = node;
    this.list = list;
    assert (list.contains(node));
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Remove " + ChangeList.getNodeString(node)
      + ChangeList.getEnclosingTypeString(" from", node), null);
  }

  public void apply() {
    boolean removed = list.remove(node);
    assert (removed);
  }

}