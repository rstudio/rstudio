// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.CanBeSetFinal;
import com.google.gwt.dev.jjs.ast.JNode;

class MakeFinal implements Change {
  private final CanBeSetFinal x;

  public MakeFinal(CanBeSetFinal x) {
    this.x = x;
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Make final " + ChangeList.getNodeString((JNode) x)
      + ChangeList.getEnclosingTypeString(" from", x), null);
  }

  public void apply() {
    x.setFinal(true);
  }
}