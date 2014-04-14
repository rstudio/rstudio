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

import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.impl.gflow.Assumption;
import com.google.gwt.dev.jjs.impl.gflow.SubgraphAssumptions;

import java.util.ArrayList;

/**
 * Utilities for working with Cfg.
 */
public class CfgUtil {
  public static void addGraphEdges(Cfg originalGraph, CfgNode<?> originalNode,
      CfgNode<?> newStartNode, CfgNode<?> newEndNode, Cfg newSubgraph) {
    for (int i = 0; i < originalGraph.getInEdges(originalNode).size(); ++i) {
      CfgEdge edge = new CfgEdge();
      newSubgraph.addIn(newStartNode, edge);
      newSubgraph.addGraphInEdge(edge);
    }

    for (CfgEdge e : originalGraph.getOutEdges(originalNode)) {
      CfgEdge edge = new CfgEdge(e.getRole());
      newSubgraph.addOut(newEndNode, edge);
      newSubgraph.addGraphOutEdge(edge);
    }
  }

  public static <A extends Assumption<?>> SubgraphAssumptions<A>
  createGraphBottomAssumptions(Cfg graph) {
    ArrayList<A> in = new ArrayList<A>();
    for (int i = 0; i < graph.getGraphInEdges().size(); ++i) {
      in.add(null);
    }

    ArrayList<A> out = new ArrayList<A>();
    for (int i = 0; i < graph.getGraphOutEdges().size(); ++i) {
      out.add(null);
    }

    return new SubgraphAssumptions<A>(in, out);
  }

  /**
   * Create a graph with single node. Resulting graph will have same amount of
   * input/output edges as the original node.
   */
  public static Cfg createSingleNodeReplacementGraph(
      Cfg originalGraph,
      CfgNode<?> originalNode,
      CfgNode<?> newNode) {
    Cfg newSubgraph = new Cfg();
    newSubgraph.addNode(newNode);
    addGraphEdges(originalGraph, originalNode, newNode, newNode, newSubgraph);
    return newSubgraph;
  }

  /**
   * Find CFG node corresponding to the nearest statement, containing the AST
   * node of the passed node.
   */
  public static CfgNode<?> findContainingStatement(CfgNode<?> node) {
    while (!(node.getJNode() instanceof JStatement)) {
      node = node.getParent();
    }

    return node;
  }

  /**
   * Find parent of containing statement.
   */
  public static CfgNode<?> findParentOfContainingStatement(CfgNode<?> node) {
    CfgNode<?> stmtNode = findContainingStatement(node);
    CfgNode<?> result = stmtNode;
    while (stmtNode.getJNode() == result.getJNode()) {
      result = result.getParent();
      if (result == null) {
        return null;
      }
      // Preconditions.checkNotNull(result, "Can't find parent for: %s", node);
    }

    return result;
  }

  private CfgUtil() {
    //
  }
}
