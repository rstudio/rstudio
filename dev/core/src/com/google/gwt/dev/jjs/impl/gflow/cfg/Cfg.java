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

import com.google.gwt.dev.jjs.impl.gflow.Assumption;
import com.google.gwt.dev.jjs.impl.gflow.Graph;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Control flow graph representation for gflow framework.
 */
public class Cfg implements Graph<CfgNode<?>, CfgEdge, CfgTransformer> {
  /**
   * Graph incoming edges.
   */
  private final ArrayList<CfgEdge> graphInEdges = new ArrayList<CfgEdge>();
  /**
   * Graph outgoing edges.
   */
  private final ArrayList<CfgEdge> graphOutEdges = new ArrayList<CfgEdge>();
  /**
   * List of all nodes.
   */
  private final ArrayList<CfgNode<?>> nodes = new ArrayList<CfgNode<?>>();

  /**
   * Add graph incoming edge.
   */
  public void addGraphInEdge(CfgEdge edge) {
    graphInEdges.add(edge);
  }

  /**
   * Add graph outgoing edge.
   */
  public void addGraphOutEdge(CfgEdge edge) {
    graphOutEdges.add(edge);
  }

  /**
   * Add incoming edge to the node.
   */
  public void addIn(CfgNode<?> node, CfgEdge edge) {
    Preconditions.checkNotNull(edge, "Null edge: %s", edge);
    Preconditions.checkArgument(edge.end == null,
        "Edge is already bound: %s", edge);
    node.in = Lists.add(node.in, edge);
    edge.end = node;
  }

  /**
   * Add new node to the graph.
   */
  public <N extends CfgNode<?>> N addNode(N node) {
    nodes.add(node);
    return node;
  }

  /**
   * Add outgoing edge from the node.
   */
  public void addOut(CfgNode<?> node, CfgEdge edge) {
    if (edge.start != null) {
      throw new IllegalArgumentException();
    }
    node.out = Lists.add(node.out, edge);
    edge.start = node;
  }

  @Override
  public Object getEdgeData(CfgEdge edge) {
    return edge.data;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CfgNode<?> getEnd(CfgEdge e) {
    return e.getEnd();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ArrayList<CfgEdge> getGraphInEdges() {
    return graphInEdges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ArrayList<CfgEdge> getGraphOutEdges() {
    return graphOutEdges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<CfgEdge> getInEdges(CfgNode<?> cfgNode) {
    return cfgNode.in;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ArrayList<CfgNode<?>> getNodes() {
    return nodes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<CfgEdge> getOutEdges(CfgNode<?> cfgNode) {
    return cfgNode.out;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CfgNode<?> getStart(CfgEdge e) {
    return e.getStart();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String print() {
    return new CfgPrinter(this).print();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Assumption<A>> String printWithAssumptions(
      Map<CfgEdge, A> map) {
    return new AssumptionsPrinter<A>(this, map).print();
  }

  @Override
  public void setEdgeData(CfgEdge edge, Object data) {
    edge.data = data;
  }

  @Override
  public String toString() {
    return print();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean transform(CfgNode<?> node, CfgTransformer actualizer) {
    if (actualizer == null) {
      throw new IllegalArgumentException();
    }
    return actualizer.transform(node, this);
  }
}
