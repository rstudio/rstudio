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

import com.google.gwt.dev.jjs.ast.JNode;

/**
 * Unconditional control transfer node.
 *
 * @param <JNodeType> corresponding AST node type.
 */
public abstract class CfgGotoNode<JNodeType extends JNode>
    extends CfgNode<JNodeType> {
  public CfgGotoNode(CfgNode<?> parent, JNodeType node) {
    super(parent, node);
  }

  @Override
  public String toDebugString() {
    return "GOTO";
  }
}
