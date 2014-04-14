/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow.cfg;

import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JNode;

/**
 * Base class for all conditional execution nodes.
 *
 * @param <JNodeType> corresponding AST type
 */
public abstract class CfgConditionalNode<JNodeType extends JNode> extends
    CfgNode<JNodeType> {
  /**
   * Else edge role.
   */
  public static final String ELSE = "ELSE";
  /**
   * Then edge role.
   */
  public static final String THEN = "THEN";

  public CfgConditionalNode(CfgNode<?> parent, JNodeType node) {
    super(parent, node);
  }

  @Override
  public void accept(CfgVisitor visitor) {
    visitor.visitConditionalNode(this);
  }

  /**
   * Condition which is used to determine the branch.
   */
  public abstract JExpression getCondition();

  @Override
  public String toDebugString() {
    JExpression condition = getCondition();
    return "COND (" + (condition != null ? condition.toSource() : "") + ")";
  }
}
