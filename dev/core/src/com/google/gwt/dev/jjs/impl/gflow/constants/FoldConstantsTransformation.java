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

import com.google.gwt.dev.jjs.impl.gflow.TransformationFunction.Transformation;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNopNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgReadNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgUtil;

/**
 * Transformation that replaces read node with Nop node in graph, and replaces
 * JNode by constant value.
 */
final class FoldConstantsTransformation implements
    Transformation<CfgTransformer, Cfg> {
  private final ConstantsAssumption assumption;
  private final Cfg graph;
  private final CfgReadNode node;

  FoldConstantsTransformation(ConstantsAssumption assumptions,
      CfgReadNode node, Cfg graph) {
    this.assumption = assumptions;
    this.node = node;
    this.graph = graph;
  }

  @Override
  public CfgTransformer getGraphTransformer() {
    return new FoldConstantTransformer(assumption, node);
  }

  @Override
  public Cfg getNewSubgraph() {
    CfgNode<?> newNode = new CfgNopNode(node.getParent(), node.getJNode());
    return CfgUtil.createSingleNodeReplacementGraph(graph, node, newNode);
  }

  @Override
  public String toString() {
    return "FoldConstantsTransformation(node=" + node +
        ", assumptions=" + assumption + ")";
  }
}