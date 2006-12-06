// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;

import java.util.List;

class AddAll/*<N extends JNode>*/ extends ChangeBase {

  final List/*<N>*/ source;
  private final int index;
  final List/*<N>*/ list;

  public AddAll(List/*<N>*/ source, int index, List/*<N>*/ list) {
    this.source = source;
    this.index = index;
    this.list = list;
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    if (index < 0) {
      logger.log(type, "Add a list to a list", null);
    } else {
      logger.log(type, "Add a list to a list at index " + index, null);
    }
  }

  public void apply() {
    if (index < 0) {
      list.addAll(source);
    } else { 
      list.addAll(index, source);
    }
  }

}