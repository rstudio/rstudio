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

import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionMap;
import com.google.gwt.dev.jjs.impl.gflow.AssumptionUtil;
import com.google.gwt.dev.jjs.impl.gflow.TransformationFunction;
import com.google.gwt.dev.jjs.impl.gflow.cfg.Cfg;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgConditionalNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgEdge;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgReadNode;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgTransformer;
import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgVisitor;
import com.google.gwt.dev.util.Preconditions;

/**
 * Transformation function for ConstantsAnalysis. Checks if current node can
 * be simplified using assumptions
 */
public class ConstantsTransformationFunction implements
    TransformationFunction<CfgNode<?>, CfgEdge, CfgTransformer, Cfg, 
                           ConstantsAssumption> {
  private final class MyTransformationVisitor extends CfgVisitor {
    private final ConstantsAssumption assumption;
    private final Cfg graph;
    private Transformation<CfgTransformer, Cfg> result = null;

    private MyTransformationVisitor(Cfg graph, ConstantsAssumption assumption) {
      this.graph = graph;
      this.assumption = assumption;
    }

    @Override
    public void visitConditionalNode(final CfgConditionalNode<?> node) {
      JExpression condition = node.getCondition();
      if (condition instanceof JValueLiteral) {
        return;
      }
      
      Preconditions.checkNotNull(condition, 
          "Null condition in %s: %s", node, node.getJNode());

      if (condition.hasSideEffects()) {
        return;
      }

      JValueLiteral evaluatedCondition = 
        ExpressionEvaluator.evaluate(condition, assumption);
      
      if (evaluatedCondition == null || 
          !(evaluatedCondition instanceof JBooleanLiteral)) {
        super.visitConditionalNode(node);
        return;
      }
      
      final boolean b = ((JBooleanLiteral) evaluatedCondition).getValue();
      result = new ConstantConditionTransformation(graph, b, node);
    }

    @Override
    public void visitReadNode(CfgReadNode node) {
      if (assumption.hasAssumption(node.getTarget())) {
        result = new FoldConstantsTransformation(assumption, node, graph);
      }
    }
  }

  public Transformation<CfgTransformer, Cfg> transform(
      final CfgNode<?> node, final Cfg graph,
      final AssumptionMap<CfgEdge, ConstantsAssumption> assumptionMap) {
    ConstantsAssumption assumption = AssumptionUtil.join(
        graph.getInEdges(node), assumptionMap);
    if (assumption == null) {
      return null;
    }
    
    MyTransformationVisitor visitor = new MyTransformationVisitor(graph, 
        assumption);
    node.accept(visitor);
    return visitor.result;
  }
}
