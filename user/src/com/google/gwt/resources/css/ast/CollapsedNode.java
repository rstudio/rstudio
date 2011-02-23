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
package com.google.gwt.resources.css.ast;

import java.util.List;

/**
 * This delegate class bypasses traversal of a node, instead traversing the
 * node's children. Any modifications made to the node list of the CollapsedNode
 * will be reflected in the original node.
 */
public class CollapsedNode extends CssNode implements HasNodes {

  private final List<CssNode> nodes;

  public CollapsedNode(HasNodes parent) {
    this(parent.getNodes());
  }

  public CollapsedNode(List<CssNode> nodes) {
    this.nodes = nodes;
  }

  public List<CssNode> getNodes() {
    return nodes;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  public void traverse(CssVisitor visitor, Context context) {
    visitor.acceptWithInsertRemove(getNodes());
  }
}