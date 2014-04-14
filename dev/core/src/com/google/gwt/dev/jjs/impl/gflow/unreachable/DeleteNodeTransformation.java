/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow.unreachable;

import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.impl.gflow.TransformationFunction.Transformation;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNopNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgStatementNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgUtil;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

class DeleteNodeTransformation implements Transformation<CfgTransformer, Cfg> {
  private final Cfg graph;
  private final CfgNode<?> node;

  public DeleteNodeTransformation(Cfg graph, CfgNode<?> node) {
    this.graph = graph;
    this.node = node;
  }

  @Override
  public CfgTransformer getGraphTransformer() {
    return new CfgTransformer() {
      @Override
      public boolean transform(CfgNode<?> node, Cfg cfgGraph) {
        if (node.getParent() == null) {
          throw new IllegalArgumentException("Null parent in " + node);
        }

        JNode jNode = node.getJNode();

        if (node instanceof CfgStatementNode<?> && !(jNode instanceof JBlock)) {
          // Don't try deleting inner expressions and blocks.
          // Delete statements only.
          CfgNode<?> parentNode = CfgUtil.findParentOfContainingStatement(node);
          JNode parentJNode = parentNode.getJNode();
          boolean didChange = DeleteNodeVisitor.delete(jNode, parentJNode);
          Preconditions.checkState(didChange,
              "Can't delete %s (%s) from under %s (%s)", jNode, node,
              parentJNode, parentNode);
          return true;
        }

        return false;
      }
    };
  }

  @Override
  public Cfg getNewSubgraph() {
    CfgNode<?> newNode = new CfgNopNode(node.getParent(), node.getJNode());
    return CfgUtil.createSingleNodeReplacementGraph(graph, node, newNode);
  }

  @Override
  public String toString() {
    return "DeleteNodeTransformation(node=" + node + ")";
  }
}