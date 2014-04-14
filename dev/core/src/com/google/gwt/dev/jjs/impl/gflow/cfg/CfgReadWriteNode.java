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
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;

/**
 * A node corresponding to simultaneous read and write operation (e.g. ++).
 */
public class CfgReadWriteNode extends CfgSimpleNode<JNode> {
  private final JExpression target;
  private final JExpression value;

  public CfgReadWriteNode(CfgNode<?> parent, JNode node, JExpression target,
      JExpression value) {
    super(parent, node);
    this.target = target;
    this.value = value;
  }

  @Override
  public void accept(CfgVisitor visitor) {
    visitor.visitReadWriteNode(this);
  }

  /**
   * Get operation target. I.e. expression, describing what's changed.
   */
  public JExpression getTarget() {
    return target;
  }

  /**
   * Get target variable if target is variable reference. Returns
   * <code>null</code> otherwise (e.g. target is array reference).
   */
  public JVariable getTargetVariable() {
    return target instanceof JVariableRef ? ((JVariableRef) target).getTarget()
        : null;
  }

  /**
   * Get expression which is assigned to value.
   * <code>null</code> when new value expression can't be statically determined.
   */
  public JExpression getValue() {
    return value;
  }

  @Override
  public String toDebugString() {
    String targets = target.toString();
    if (getTargetVariable() != null) {
      targets = getTargetVariable().getName();
    }
    return "READWRITE(" + targets + ", " + value + ")";
  }

  @Override
  protected CfgNode<?> cloneImpl() {
    return new CfgReadWriteNode(getParent(), getJNode(), target, value);
  }
}
