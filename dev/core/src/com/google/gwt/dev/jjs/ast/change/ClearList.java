// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;

import java.util.List;

class ClearList/*<N extends JNode>*/ extends ChangeBase {

  private final List/*<N>*/ list;

  public ClearList(List/*<N>*/ list) {
    this.list = list;
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Clear a list", null);
  }

  public void apply() {
    list.clear();
  }

}