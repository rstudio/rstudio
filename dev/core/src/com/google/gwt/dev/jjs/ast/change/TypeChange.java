/*
 * Copyright 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.HasSettableType;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * Changes the <code>JType</code> of a <code>JNode</code>.
 */
public class TypeChange extends ChangeBase {

  final JType targetType;
  private final HasSettableType node;
  private final JType oldType;

  public TypeChange(HasSettableType node, JType targetType) {
    this.node = node;
    this.oldType = node.getType();
    this.targetType = targetType;
  }

  public void apply() {
    assert (oldType == node.getType());
    node.setType(targetType);
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Change type of " + ChangeList.getNodeString((JNode) node)
        + " from " + ChangeList.getNodeString(node.getType()) + " to "
        + ChangeList.getNodeString(targetType), null);
  }

}
