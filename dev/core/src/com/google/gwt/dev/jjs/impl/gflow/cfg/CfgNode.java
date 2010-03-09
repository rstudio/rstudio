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
import com.google.gwt.dev.util.collect.Lists;

import java.util.List;

/**
 * Base class for nodes in CFG graph. 
 * 
 * @param <JNodeType> node's corresponding JNode type.
 */
public abstract class CfgNode<JNodeType extends JNode> {
  List<CfgEdge> in = Lists.create();
  List<CfgEdge> out = Lists.create();
  private final JNodeType node;
  private final CfgNode<?> parent;

  public CfgNode(CfgNode<?> parent, JNodeType node) {
    this.parent = parent;
    this.node = node;
  }

  public abstract void accept(CfgVisitor visitor);

  @Override
  public CfgNode<?> clone() {
    return cloneImpl();
  }

  public JNodeType getJNode() {
    return node;
  }

  public CfgNode<?> getParent() {
    return parent;
  }

  /**
   * @return debug string representation of the node.
   */
  public abstract String toDebugString();
  
  @Override
  public String toString() {
    return toDebugString();
  } 
  
  /**
   * @return node clone.
   */
  protected abstract CfgNode<?> cloneImpl();
}
