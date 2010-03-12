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
package com.google.gwt.dev.jjs.impl.gflow.constants;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.impl.gflow.TransformationFunction.Transformation;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgCaseNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgConditionalNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNopNode;
import com.google.gwt.dev.util.Preconditions;

/**
 * Transformation to be applied when CfgConditionalNode's condition is constant
 * value. Transformation replaces conditional node with CfgNop node depending 
 * on condition value. It leaves the edge, which will be never executed
 * unconnected at source, thus making sure that it will be unreachable.
 * 
 * Doesn't try to optimize unreachable branches, since this is subject to other
 * optimizations.
 */
final class ConstantConditionTransformation implements
    Transformation<CfgTransformer, Cfg> {
  private final boolean conditionValue;
  private final CfgConditionalNode<?> node;
  private final Cfg graph;

  ConstantConditionTransformation(Cfg graph, boolean conditionValue,
      CfgConditionalNode<?> node) {
    this.graph = graph;
    this.conditionValue = conditionValue;
    this.node = node;
  }

  public CfgTransformer getGraphTransformer() {
    return new CfgTransformer() {
      public boolean transform(CfgNode<?> cfgNode, Cfg cfgGraph) {
        Preconditions.checkArgument(cfgNode == node);
        if (cfgNode instanceof CfgCaseNode) {
          // TODO: support case node optimization
          return false;
        }
        
        final JExpression oldCondition = node.getCondition();
        final JExpression newCondition = JBooleanLiteral.get(conditionValue);
        JModVisitor visitor = new JModVisitor() {
          @Override
          public boolean visit(JExpression x, Context ctx) {
            if (x == oldCondition) {
              ctx.replaceMe(newCondition);
              return false;
            }
            return true;
          }
        };
        JNode startNode = node.getJNode();
        visitor.accept(startNode);
        Preconditions.checkState(visitor.didChange(),
            "Couldn't replace %s with %s in %s",
            oldCondition, newCondition, startNode);
        
        return visitor.didChange();
      }
    };
  }

  public Cfg getNewSubgraph() {
    Cfg newSubgraph = new Cfg();
    CfgNode<?> newNode = new CfgNopNode(node.getParent(), node.getJNode());
    newSubgraph.addNode(newNode);
    
    // Add all incoming edges.
    for (int i = 0; i < graph.getInEdges(node).size(); ++i) {
      CfgEdge edge = new CfgEdge();
      newSubgraph.addIn(newNode, edge);
      newSubgraph.addGraphInEdge(edge);
    }

    for (CfgEdge e : graph.getOutEdges(node)) {
      CfgEdge edge = new CfgEdge(e.getRole());
      newSubgraph.addGraphOutEdge(edge);

      if (e.getRole() != null
          && ((e.getRole().equals(CfgConditionalNode.ELSE) && conditionValue) || 
              (e.getRole().equals(CfgConditionalNode.THEN) && !conditionValue))) {
        // Do not connect this edge due to constant condition.
      } else {
        newSubgraph.addOut(newNode, edge);
      }
    }

    return newSubgraph;
  }

  @Override
  public String toString() {
    return "ConstantConditionTransformation(node=" + node + 
        ", conditionValue=" + conditionValue + ")";
  }
}