// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.HasSettableType;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * Changes the <code>JType</code> of a <code>JNode</code>.
 */
public class TypeChange extends ChangeBase {

  private final HasSettableType node;
  private final JType oldType;
  final JType targetType;

  public TypeChange(HasSettableType node, JType targetType) {
    this.node = node;
    this.oldType = node.getType();
    this.targetType = targetType;
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Change type of " + ChangeList.getNodeString((JNode) node)
      + " from " + ChangeList.getNodeString(node.getType()) + " to "
      + ChangeList.getNodeString(targetType), null);
  }

  public void apply() {
    assert (oldType == node.getType());
    node.setType(targetType);
  }

}
