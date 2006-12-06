// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JBlock;

class MoveBlock implements Change {
  final JBlock source;
  final JBlock target;

  public MoveBlock(JBlock source, JBlock target) {
    super();
    this.source = source;
    this.target = target;
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Move the body of " + ChangeList.getNodeString(source)
      + " to " + ChangeList.getNodeString(target), null);
  }

  public void apply() {
    assert (target.statements.size() == 0);
    target.statements.addAll(source.statements);
    source.statements.clear();
  }
}