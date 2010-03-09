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
package com.google.gwt.dev.jjs.impl.gflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Directed graph abstraction for flow analysis. We specifically define all 
 * navigation methods in graph interface and do not ask nodes and edges to 
 * conform to any interface. This way graphs can be memory-efficient.
 * 
 * The graph can have one or more incoming edges (i.e. edges, which end on
 * some nodes in the graph, but originate somewhere outside the graph), and one
 * or more outgoing edges. 
 * 
 * @param <NodeType> graph node type.
 * @param <EdgeType> graph edge type.
 * @param <TransformerType> transformer type. Transformer instances can be used
 *          to change the graph and its underlying representation to apply
 *          optimizations.
 */
public interface Graph<NodeType, EdgeType, TransformerType> {

  Object getEdgeData(EdgeType edge);

  /**
   * Returns edge end node.
   */
  NodeType getEnd(EdgeType edge);

  /**
   * Returns graph incoming edges.
   */
  ArrayList<EdgeType> getGraphInEdges();

  /**
   * Returns graph outgoing edges.
   */
  ArrayList<EdgeType> getGraphOutEdges();

  /**
   * Returns edges coming into node.
   */
  List<EdgeType> getInEdges(NodeType n);

  /**
   * Returns all nodes in the graph.
   */
  ArrayList<NodeType> getNodes();

  /**
   * Returns edges originating from the node.
   */
  List<EdgeType> getOutEdges(NodeType node);

  /**
   * Returns edge start node.
   */
  NodeType getStart(EdgeType edge);

  /**
   * Returns string representation of the graph.
   */
  String print();

  /**
   * Returns string representation of the graph with all assumptions along its
   * edges.
   */
  <A extends Assumption<A>> String printWithAssumptions(
      Map<EdgeType, A> assumptions);
  
  void setEdgeData(EdgeType edge, Object data);
  
  /**
   * Transforms the node with transformer. This will be called by solver to
   * apply optimizations.
   * 
   * @return <code>true</code> if there were changes made by transformer. While
   * transformation should be always sound, it might be impossible to apply
   * it in current context due to complexities of underlying structures. E.g.
   * it is impossible to delete if statement test expression, while it is not
   * evaluated if statement is not reachable. In this case transformer can 
   * return <code>false</code> and do no changes.
   */
  boolean transform(NodeType node, TransformerType transformer);
}
